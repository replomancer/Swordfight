(ns swordfight.rules
  (:require  [clojure.set :refer [map-invert]]
             [swordfight.board :refer
              [initial-board all-coords empty-square?
               piece-type color notation->coords coords->notation
               change-side on-board? opposite-color? black-piece?
               get-piece move-piece remove-piece promote-pawn
               white black black-king
               king queen bishop knight rook pawn pawn?
               ->piece]]))

(def initial-game-state {:board initial-board
                         :turn white
                         :moves-cnt 0
                         :edited false
                         :white-can-castle-ks true
                         :white-can-castle-qs true
                         :black-can-castle-ks true
                         :black-can-castle-qs true
                         ;; the last move is: [first-pos second-pos current-piece-on-second-pos]
                         ;; current-piece-on-second-pos can be a queen just promoted from pawn
                         :last-move ["  " "  " black-king]})

(def dir-up [-1 0])
(def dir-down [+1 0])
(def dir-left [0 -1])
(def dir-right [0 +1])
(def dir-up-left [-1 -1])
(def dir-up-right [-1 +1])
(def dir-down-left [+1 -1])
(def dir-down-right [+1 +1])
(def rook-directions [dir-up dir-down dir-left dir-right])
(def bishop-directions [dir-up-left dir-up-right dir-down-left dir-down-right])
(def all-directions (concat rook-directions bishop-directions))
(def knight-moves [[-2 -1] [-1 -2] [+1 -2] [+2 -1]
                   [-2 +1] [-1 +2] [+1 +2] [+2 +1]])
(def king-basic-moves all-directions)

(defmulti pseudolegal-destination-indexes
  (fn [game-state square-coords piece] (piece-type piece)))

(defn possible-moves-from-square [{:keys [board turn] :as game-state} from-square]
  (let [piece (get-piece board from-square)]
    (if (not= (color piece) turn)  ;; this check also covers empty squares
      []
      (mapcat
       (fn [[dest-y dest-x]] (let [dest-notation (coords->notation [dest-y dest-x])]
                               (if (and (pawn? piece) (#{0 7} dest-y))
                                 ;; This is pawn promotion.
                                 ;; One target index translates to four moves.
                                 [(str dest-notation "q")
                                  (str dest-notation "r")
                                  (str dest-notation "b")
                                  (str dest-notation "n")]
                                 ;; default case:
                                 [dest-notation])))
       (pseudolegal-destination-indexes game-state (notation->coords from-square) piece)))))

(defn pseudolegal-moves [game-state]
  (mapcat
   (fn [pos-from] (map (fn [pos-to] [pos-from pos-to])
                       (possible-moves-from-square game-state pos-from)))
   (map coords->notation all-coords)))

(defn squares-attacked-by [game-state attacker]
  (map second (pseudolegal-moves
               (assoc game-state
                      :turn attacker
                      :white-can-castle-ks false
                      :white-can-castle-qs false
                      :black-can-castle-ks false
                      :black-can-castle-qs false))))

(defn squares-attacked-by-opponent [{turn :turn :as game-state}]
  (squares-attacked-by game-state (change-side turn)))

(defn pieces-attacked-by-opponent [{:keys [turn board] :as game-state}]
  (filter (fn [sq] (= (color
                       (get-in board (notation->coords sq)))
                      turn))
          (squares-attacked-by-opponent game-state)))

(defn take-while-and-next-one [pred coll]
  (let [[take-while-part remaining] (split-with pred coll)]
    [take-while-part (first remaining)]))

(defn squares-to-go-in-dir [board piece pos dir]
  (let [positions-on-board-in-dir (take-while on-board? (iterate #(map + dir %)
                                                                 (map + pos dir)))
        [empty-positions-in-dir first-non-empty-position]
        (take-while-and-next-one #(empty-square? (get-in board %))
                                 positions-on-board-in-dir)
        blocking-piece (if first-non-empty-position
                         (get-in board first-non-empty-position))]
    (if (and blocking-piece (opposite-color? piece blocking-piece))
      (concat empty-positions-in-dir [first-non-empty-position]) ;; attacking move
      empty-positions-in-dir)))

(defn squares-to-go-in-directions [board piece pos dirs]
  (mapcat (partial squares-to-go-in-dir board piece pos) dirs))

(defmethod pseudolegal-destination-indexes queen [{board :board} pos piece]
  (squares-to-go-in-directions board piece pos all-directions))

(defmethod pseudolegal-destination-indexes rook [{board :board} pos piece]
  (squares-to-go-in-directions board piece pos rook-directions))

(defmethod pseudolegal-destination-indexes bishop [{board :board} pos piece]
  (squares-to-go-in-directions board piece pos bishop-directions))

(defmethod pseudolegal-destination-indexes knight [{board :board} pos piece]
  (filter (fn [yx] (let [square-piece (get-in board yx)]
                     (or (empty-square? square-piece)
                         (opposite-color? piece square-piece))))
          (map #(map + pos %) knight-moves)))

(defmethod pseudolegal-destination-indexes pawn [{:keys [board last-move]} [square-y square-x] piece]
  (let [[forward-direction starting-row en-passant-row promotion-row]
        (if (black-piece? piece)
          [+1 1 4 7]
          [-1 6 3 0])
        [notation-from notation-to last-moved-piece] last-move
        [last-move-from last-move-to] (map notation->coords [notation-from notation-to])
        forward-y (+ square-y forward-direction)
        left-x (dec square-x)
        right-x (inc square-x)
        jump-forward-y (+ square-y (* 2 forward-direction))
        forward-square (get-in board [forward-y square-x])
        forward-left-square (get-in board [forward-y left-x])
        forward-right-square (get-in board [forward-y right-x])
        jump-forward-square (get-in board [jump-forward-y square-x])]
    (remove nil?
            [(when (empty-square? forward-square)
               [forward-y square-x])
             (when (and (= square-y starting-row)
                        (empty-square? forward-square)
                        (empty-square? jump-forward-square))
               [jump-forward-y square-x])
             (when (opposite-color? piece forward-left-square)
               [forward-y left-x])
             (when (opposite-color? piece forward-right-square)
               [forward-y right-x])
             (when (and (= square-y en-passant-row)
                        (pawn? last-moved-piece))
               (condp = [last-move-from last-move-to]
                 [[jump-forward-y left-x] [square-y left-x]] [forward-y left-x]
                 [[jump-forward-y right-x] [square-y right-x]] [forward-y right-x]
                 nil))])))

(defmethod pseudolegal-destination-indexes king [{:keys [board turn] :as game-state} square-coords piece]
  (let [[can-castle-ks can-castle-qs ks-squares qs-squares]
        (if (= turn white)
          [(:white-can-castle-ks game-state)
           (:white-can-castle-qs game-state)
           [[[7 5] [7 6]] #{"e1" "f1" "g1"}]
           [[[7 3] [7 2]] #{"e1" "d1" "c1"}]]
          [(:black-can-castle-ks game-state)
           (:black-can-castle-qs game-state)
           [[[0 5] [0 6]] #{"e8" "f8" "g8"}]
           [[[0 3] [0 2]] #{"e8" "d8" "c8"}]])]
    (concat
     (filter (fn [pos] (let [square-piece (get-in board pos)]
                         (or (empty-square? square-piece)
                             (opposite-color? piece square-piece))))
             (map #(map + square-coords %) king-basic-moves))
     (for [[castling-condition [middle-squares must-be-safe-squares]]
           [[can-castle-ks ks-squares]
            [can-castle-qs qs-squares]]
           :when (and castling-condition
                      (every? (comp empty-square? (partial get-in board)) middle-squares)
                      (not-any? must-be-safe-squares
                                (squares-attacked-by-opponent game-state)))]
       (last middle-squares)))))

(defn update-castling-info [game-state piece-moved-from]
  ;; If a piece is moved from e1 square, it means the king
  ;; has moved (just now or before - making place for another piece).
  ;; Same with black king and rooks.
  (let [castling-updates ({"e1" [:white-can-castle-ks :white-can-castle-qs]
                           "a1" [:white-can-castle-qs]
                           "h1" [:white-can-castle-ks]
                           "e8" [:black-can-castle-ks :black-can-castle-qs]
                           "a8" [:black-can-castle-qs]
                           "h8" [:black-can-castle-ks]} piece-moved-from)]
    (if castling-updates
      (apply assoc game-state (interleave castling-updates (repeat false)))
      game-state)))

(defn possibly-extra-board-change [game-state new-board piece piece-move]
  (condp = (piece-type piece)

    king
    ;; extra tower moves when castling:
    (let [extra-tower-move
          (condp = piece-move
            ;; white queenside, white kingside, black queenside, black kingside
            ["e1" "c1"] ["a1" "d1"]
            ["e1" "g1"] ["h1" "f1"]
            ["e8" "c8"] ["a8" "d8"]
            ["e8" "g8"] ["h8" "f8"]
            nil)]
      (if extra-tower-move
        (move-piece new-board extra-tower-move)
        [new-board piece]))

    pawn
    (let [[_ to-pos] piece-move
          pawn-promotion (> (.length to-pos) 2)]
      (if pawn-promotion
        (promote-pawn new-board to-pos)
        ;; en passant moves require removing pawns:
        (let [[[from-y _] [_ to-x]] (map notation->coords piece-move)
              [last-m-from last-m-to last-moved-piece] (:last-move game-state)
              [[last-m-from-y _] [last-m-to-y last-m-to-x]]
              (map notation->coords [last-m-from last-m-to])]
          ;; We assume here the moves being made are pseudolegal:
          (if (and (pawn? last-moved-piece)
                   (= (Math/abs (- last-m-from-y last-m-to-y)) 2)
                   (= last-m-to-y from-y)
                   (= last-m-to-x to-x))
            [(first (remove-piece new-board last-m-to)) piece]
            [new-board piece]))))

    [new-board piece]))

(defn move [game-state [from-pos to-pos]]
  ;; This function assumes the move is pseudolegal
  (let [[new-board new-piece]
        (let [[board' piece] (move-piece
                              (:board game-state) [from-pos to-pos])
              ;; special cases where extra pieces need to be moved or removed
              [board'' piece'] (possibly-extra-board-change game-state board' piece [from-pos to-pos])]
          [board'' piece'])
        new-game-state (-> (assoc game-state :board new-board)
                           (update-castling-info from-pos)
                           (update :turn change-side)
                           (update :moves-cnt inc)
                           (assoc :last-move [from-pos to-pos new-piece]))]
    new-game-state))

(defn king-in-check? [{:keys [turn board] :as game-state} king-color]
  (let [poor-king (->piece king-color king)
        attacked-squares
        (squares-attacked-by game-state (change-side king-color))]
    (some #{poor-king} (map #(get-piece board %) attacked-squares))))

(defn king-in-check-after-move? [{:keys [turn board] :as game-state} mv]
  (king-in-check? (move game-state mv) turn))

(defn legal-moves [game-state]
  (remove #(king-in-check-after-move? game-state %)
          (pseudolegal-moves game-state)))
