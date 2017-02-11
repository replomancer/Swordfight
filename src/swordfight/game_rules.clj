(ns swordfight.game-rules
  (:use [clojure.set :only [map-invert]]))

(def initial-board
  [[ "BR"  "BN"  "BB"  "BQ"  "BK"  "BB"  "BN"  "BR" ]
   [ "BP"  "BP"  "BP"  "BP"  "BP"  "BP"  "BP"  "BP" ]
   [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
   [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
   [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
   [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
   [ "WP"  "WP"  "WP"  "WP"  "WP"  "WP"  "WP"  "WP" ]
   [ "WR"  "WN"  "WB"  "WQ"  "WK"  "WB"  "WN"  "WR" ]])

(def empty-square "  ")

(defn empty-square? [square] (= square empty-square))

(def empty-board
  (vec (repeat 8 (vec (repeat 8 empty-square)))))

(def initial-game-state {:board initial-board
                         :turn \W
                         :moves-cnt 0
                         :edited false
                         :white-can-castle-ks true
                         :white-can-castle-qs true
                         :black-can-castle-ks true
                         :black-can-castle-qs true
                         :last-move ["  " "  "]})

(def notation->coords
  {"a8" [0 0] "b8" [0 1] "c8" [0 2] "d8" [0 3] "e8" [0 4] "f8" [0 5] "g8" [0 6] "h8" [0 7]
   "a7" [1 0] "b7" [1 1] "c7" [1 2] "d7" [1 3] "e7" [1 4] "f7" [1 5] "g7" [1 6] "h7" [1 7]
   "a6" [2 0] "b6" [2 1] "c6" [2 2] "d6" [2 3] "e6" [2 4] "f6" [2 5] "g6" [2 6] "h6" [2 7]
   "a5" [3 0] "b5" [3 1] "c5" [3 2] "d5" [3 3] "e5" [3 4] "f5" [3 5] "g5" [3 6] "h5" [3 7]
   "a4" [4 0] "b4" [4 1] "c4" [4 2] "d4" [4 3] "e4" [4 4] "f4" [4 5] "g4" [4 6] "h4" [4 7]
   "a3" [5 0] "b3" [5 1] "c3" [5 2] "d3" [5 3] "e3" [5 4] "f3" [5 5] "g3" [5 6] "h3" [5 7]
   "a2" [6 0] "b2" [6 1] "c2" [6 2] "d2" [6 3] "e2" [6 4] "f2" [6 5] "g2" [6 6] "h2" [6 7]
   "a1" [7 0] "b1" [7 1] "c1" [7 2] "d1" [7 3] "e1" [7 4] "f1" [7 5] "g1" [7 6] "h1" [7 7]})

(def coords->notation (map-invert notation->coords))

(def color first)

(def piece-type second)

(defn white? [piece] (= (color piece) \W))

(defn black? [piece] (= (color piece) \B))

(defn same-color? [piece1 piece2]
  (= (color piece1) (color piece2)))
  ;; two empty squares also have the same color

(defn opposite-color? [piece1 piece2]
  (let [colors (map color [piece1 piece2])]
    (or (= colors [\B \W])
        (= colors [\W \B]))))

(def change-side {\B \W
                  \W \B})

(defn on-board? [[y x]]
  (when (and (<= 0 y 7) (<= 0 x 7))
    [y x]))


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


(defmulti legal-destination-indexes
  (fn [game-state square-coords [piece-color piece-type]] piece-type))


(defn possible-moves-from-square [game-state from-square]
  (let [from-coords (notation->coords from-square)
        piece (get-in (:board game-state) from-coords)
        [piece-color piece-type] piece]
    (if (not= piece-color (:turn game-state))  ;; this check also covers empty squares
      []
      (mapcat
       (fn [dest-yx] (let [[dest-y dest-x] dest-yx
                           dest-notation (coords->notation dest-yx)]
                       (if (and (= piece-type \P)
                                (some #{dest-y} [0 7]))
                         ;; This is pawn promotion.
                         ;; One target index translates to two moves.
                         ;; Queen is almost always best. Knight is the only one
                         ;; that is sometimes better.
                         [(str dest-notation "Q")
                          (str dest-notation "N")]
                         ;; default case:
                         [dest-notation])))
       (legal-destination-indexes game-state from-coords piece)))))


(defn find-available-moves [game-state]
  (mapcat (fn [[coords]]
            (let [pos-from (coords->notation coords)]
              (for [possible-move (possible-moves-from-square game-state
                                                              pos-from)]
                [pos-from possible-move])))
          (for [y (range 8) x (range 8)] [[y x]])))


(defn squares-attacked-by-opponent [game-state]
  (map second (find-available-moves
               (assoc (update game-state :turn change-side)
                 :white-can-castle-ks false
                 :white-can-castle-qs false
                 :black-can-castle-ks false
                 :black-can-castle-qs false))))


(defn take-while-and-next-one [pred coll]
  (let [[take-while-part remaining] (split-with pred coll)]
    [take-while-part (first remaining)]))


(defn squares-to-go-in-dir [board piece-color pos dir]
  (let [positions-on-board-in-dir (take-while on-board? (iterate (partial map + dir)
                                                                 (map + pos dir)))
        [empty-positions-in-dir first-non-empty-position]
        (take-while-and-next-one #(empty-square? (get-in board %))
                                 positions-on-board-in-dir)
        blocking-piece (get-in board first-non-empty-position)]
    (if (= piece-color (change-side (color blocking-piece)))
      (concat empty-positions-in-dir [first-non-empty-position]) ;; attacking move
      empty-positions-in-dir)))


(defn squares-to-go-in-directions [board piece-color pos dirs]
  (mapcat (partial squares-to-go-in-dir board piece-color pos) dirs))


(defmethod legal-destination-indexes \Q [{board :board} pos [piece-color _]]
  (squares-to-go-in-directions board piece-color pos all-directions))


(defmethod legal-destination-indexes \R [game-state pos [piece-color _]]
  (squares-to-go-in-directions (:board game-state) piece-color pos rook-directions))


(defmethod legal-destination-indexes \B [{board :board} pos [piece-color _]]
  (squares-to-go-in-directions board piece-color pos bishop-directions))


(defmethod legal-destination-indexes \N [{board :board} pos piece]
  (filter (fn [[y x]] (let [square-piece (get-in board [y x])]
                        (or (empty-square? square-piece)
                            (opposite-color? piece square-piece))))
          (map (partial map + pos) knight-moves)))


(defmethod legal-destination-indexes \P [{:keys [board last-move]} [square-y square-x] piece]
  (let [[forward-direction starting-row en-passant-row promotion-row] (if (black? piece)
                                                                        [+1 1 4 7]
                                                                        [-1 6 3 0])
        [last-move-from last-move-to] (map notation->coords last-move)
        last-moved-piece (get-in board last-move-to)
        forward-y (+ square-y forward-direction)
        left-x (dec square-x)
        right-x (inc square-x)
        jump-forward-y (+ square-y (* 2 forward-direction))
        forward-square (get-in board [forward-y square-x])
        forward-left-square (get-in board [forward-y left-x])
        forward-right-square (get-in board [forward-y right-x])
        jump-forward-square (get-in board [jump-forward-y square-x])]
    (concat
     (when (empty-square? forward-square)
       [[forward-y square-x]])
     (when (and (= square-y starting-row)
                (empty-square? forward-square)
                (empty-square? jump-forward-square))
       [[jump-forward-y square-x]])
     (when (opposite-color? piece forward-left-square)
       [[forward-y left-x]])
     (when (opposite-color? piece forward-right-square)
       [[forward-y right-x]])
     (when (and (= square-y en-passant-row)
                (= (piece-type last-moved-piece) \P))
       (cond (= [last-move-from last-move-to]
                [[jump-forward-y left-x] [square-y left-x]])
             [[forward-y left-x]]
             (= [last-move-from last-move-to]
                [[jump-forward-y right-x] [square-y right-x]])
             [[forward-y right-x]])))))


(defmethod legal-destination-indexes \K [{:keys [board turn] :as game-state} square-coords piece]
  (let [[can-castle-ks can-castle-qs ks-squares qs-squares] (if (= turn \W)
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
             (map (partial map + square-coords) king-basic-moves))
     (for [[castling-condition [middle-squares must-be-safe-squares]]
           [[can-castle-ks ks-squares]
            [can-castle-qs qs-squares]]
           :when (and castling-condition
                      (every? (comp empty-square? (partial get-in board)) middle-squares)
                      (not-any? must-be-safe-squares
                                (squares-attacked-by-opponent game-state)))]
       (last middle-squares)))))


(defn remove-piece [board position]
  (let [yx (notation->coords position)
        piece (get-in board yx)]
    [(assoc-in board yx empty-square) piece]))


(defn put-piece [board position piece]
  (let [yx (notation->coords position)]
    (assoc-in board yx piece)))


(defn update-castling-info [game-state piece-moved-from]
  ;; If a piece is moved from e1 square, it means the king
  ;; has moved (just now or before - making place for another piece).
  ;; Same with black king and rooks.
  (cond (= piece-moved-from "e1")
        (assoc game-state
          :white-can-castle-ks false
          :white-can-castle-qs false)
        (= piece-moved-from "a1")
        (assoc game-state
          :white-can-castle-qs false)
        (= piece-moved-from "h1")
        (assoc game-state
          :white-can-castle-ks false)
        (= piece-moved-from "e8")
        (assoc game-state
          :black-can-castle-ks false
          :black-can-castle-qs false)
        (= piece-moved-from "a8")
        (assoc game-state
          :black-can-castle-qs false)
        (= piece-moved-from "h8")
        (assoc game-state
          :black-can-castle-ks false)
        :else game-state))


(defn move-piece-on-board [board [from-pos to-pos]]
  (let [[board' piece] (remove-piece board from-pos)
        to-pos' (subs to-pos 0 2)]  ;; ignore promotions, just move
    [(put-piece board' to-pos' piece) piece]))


(defn promote [board promotion-notation]
  (let [position (subs promotion-notation 0 2)
        new-piece-type (.toUpperCase (subs promotion-notation 2 3))
        yx (notation->coords position)
        piece-color (color (get-in board yx))]
    (put-piece board position (str piece-color new-piece-type))))


;; This function evaluates to a new board, not a [board piece] pair
(defn possibly-extra-board-change [game-state new-board piece piece-move]
  (cond (= (piece-type piece) \K)
        ;; extra tower moves when castling:
        (cond (= piece-move ["e1" "c1"])  ;; white castling queenside
              (first (move-piece-on-board new-board ["a1" "d1"]))

              (= piece-move ["e1" "g1"]) ;; white castling kingside
              (first (move-piece-on-board new-board ["h1" "f1"]))

              (= piece-move ["e8" "c8"])  ;; black castling queenside
              (first (move-piece-on-board new-board ["a8" "d8"]))

              (= piece-move ["e8" "g8"]) ;; black castling kingside
              (first (move-piece-on-board new-board ["h8" "f8"]))

              :else new-board)

        (= (piece-type piece) \P)
        (let [[from-pos to-pos] piece-move
              pawn-promotion (> (.length to-pos) 2)]
          (if pawn-promotion
            (promote new-board to-pos)
            ;; en passant moves require removing pawns:
            (let [[[from-y from-x] [to-y to-x]] (map notation->coords piece-move)

                  [[last-m-from-y last-m-from-x] [last-m-to-y last-m-to-x]]
                  (map (comp notation->coords #(subs % 0 2))
                       (:last-move game-state))

                  last-moved-piece
                  (get-in (:board game-state) [last-m-to-y last-m-to-x])]
              ;; We assume here the moves being made are legal:
              (if (and (= (piece-type last-moved-piece) \P)
                       (= (Math/abs (- last-m-from-y last-m-to-y)) 2)
                       (= last-m-to-y from-y)
                       (= last-m-to-x to-x))
                (first (remove-piece new-board
                                     (coords->notation [last-m-to-y last-m-to-x])))
                new-board))))

        :else new-board))


(defn move [game-state [from-pos to-pos]]
  ;; This function always assumes the move is legal
  (let [new-board
        (let [[board' piece] (move-piece-on-board
                              (:board game-state) [from-pos to-pos])
              ;; special cases where extra pieces need to be moved or removed
              board'' (possibly-extra-board-change game-state board' piece [from-pos to-pos])]
          board'')
        new-game-state (-> (assoc game-state :board new-board)
                           (update-castling-info from-pos)
                           (update :turn change-side)
                           (update :moves-cnt inc)
                           (assoc :last-move [from-pos to-pos]))]
    new-game-state))
