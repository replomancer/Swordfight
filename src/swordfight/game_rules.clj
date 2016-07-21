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
                         :last-move [nil nil]})

(def board-coords
  (let [one-way
        {"a8" [0 0] "b8" [0 1] "c8" [0 2] "d8" [0 3] "e8" [0 4] "f8" [0 5] "g8" [0 6] "h8" [0 7]
         "a7" [1 0] "b7" [1 1] "c7" [1 2] "d7" [1 3] "e7" [1 4] "f7" [1 5] "g7" [1 6] "h7" [1 7]
         "a6" [2 0] "b6" [2 1] "c6" [2 2] "d6" [2 3] "e6" [2 4] "f6" [2 5] "g6" [2 6] "h6" [2 7]
         "a5" [3 0] "b5" [3 1] "c5" [3 2] "d5" [3 3] "e5" [3 4] "f5" [3 5] "g5" [3 6] "h5" [3 7]
         "a4" [4 0] "b4" [4 1] "c4" [4 2] "d4" [4 3] "e4" [4 4] "f4" [4 5] "g4" [4 6] "h4" [4 7]
         "a3" [5 0] "b3" [5 1] "c3" [5 2] "d3" [5 3] "e3" [5 4] "f3" [5 5] "g3" [5 6] "h3" [5 7]
         "a2" [6 0] "b2" [6 1] "c2" [6 2] "d2" [6 3] "e2" [6 4] "f2" [6 5] "g2" [6 6] "h2" [6 7]
         "a1" [7 0] "b1" [7 1] "c1" [7 2] "d1" [7 3] "e1" [7 4] "f1" [7 5] "g1" [7 6] "h1" [7 7]}]
    (merge one-way (map-invert one-way)))) ;; it makes it bi-directional

(def board-notation board-coords) ;; FIXME

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


(defmulti legal-destination-indexes (fn [game-state square-coords piece]                                      (piece-type piece)))


(defn possible-moves-from-square [game-state from-square]
  (let [piece (get-in (:board game-state) from-square)
        [piece-color piece-type] piece]
    (if (not= piece-color (:turn game-state))  ;; this check also covers empty squares
      []
      (mapcat
       (fn [dest-yx] (let [[dest-y dest-x] dest-yx
                           dest-notation (board-notation dest-yx)]
                       (if (and (= piece-type \P)
                                (or (= dest-y 0) (= dest-y 7)))
                         ;; This is pawn promotion.
                         ;; One target index translates to two moves.
                         ;; Queen is almost always best. Knight is the only one
                         ;; that is sometimes better.
                         [(str dest-notation "Q")
                          (str dest-notation "N")]
                         ;; default case:
                         [dest-notation])))
       (legal-destination-indexes game-state from-square piece)))))


(defn find-available-moves [game-state]
  (mapcat (fn [[coords]]
            (for [possible-move (possible-moves-from-square game-state
                                                            coords)]
              [(board-notation coords) possible-move]))
          (for [y (range 8) x (range 8)] [[y x]])))


(defn squares-attacked-by-opponent [game-state]
  (map second (find-available-moves
               (-> (update game-state :turn change-side)
                   (assoc :white-can-castle-ks false
                          :white-can-castle-qs false
                          :black-can-castle-ks false
                          :black-can-castle-qs false)))))


(defmethod legal-destination-indexes \N [game-state square-coords piece]
  (filter (fn [[y x]] (and (on-board? [y x])
                           (not= (color (get-in (:board game-state) [y x])) (color piece))))
          (map #(map + square-coords %)
               [[-2 -1] [-1 -2] [+1 -2] [+2 -1]
                [-2 +1] [-1 +2] [+1 +2] [+2 +1]])))


;; FIXME: refactor this
(defmethod legal-destination-indexes \R [game-state square-coords piece]
  (let [non-blocked-square?
        (fn [[dy dx] [y x]]
          (and (on-board? [y x])
               (let [square-piece (get-in game-state [:board y x])]
                 (and (not (same-color? piece square-piece))
                      (let [[prev-square-y prev-square-x] (map - [y x] [dy dx])
                            prev-square-piece (get-in game-state
                                                      [:board
                                                       prev-square-y
                                                       prev-square-x])]
                        (or (empty-square? prev-square-piece)
                            (not (opposite-color? piece prev-square-piece))))))))]
    (concat
     ;; moves upward
     (take-while
      (partial non-blocked-square? [-1 0])
      (map #(map + square-coords %) (for [dy (range -1 -8 -1)] [dy 0])))
     ;; moves downward
     (take-while
      (partial non-blocked-square? [+1 0])
      (map #(map + square-coords %) (for [dy (range +1 +8 +1)] [dy 0])))
     ;; moves left
     (take-while
      (partial non-blocked-square? [0 -1])
      (map #(map + square-coords %) (for [dx (range -1 -8 -1)] [0 dx])))
     ;; moves right
     (take-while
      (partial non-blocked-square? [0 +1])
      (map #(map + square-coords %) (for [dx (range +1 +8 +1)] [0 dx]))))))


(defmethod legal-destination-indexes \P [game-state [square-y square-x] piece]
  (let [[forward-direction starting-row en-passant-row promotion-row]
        (if (black? (get-in game-state [:board square-y square-x]))
          [+1 1 4 7]
          [-1 6 3 0])
        board (:board game-state)
        last-move (:last-move game-state)
        forward-y (+ square-y forward-direction)
        left-x (dec square-x)
        right-x (inc square-x)
        jump-forward-y (+ square-y (* 2 forward-direction))
        forward-square (when (on-board? [forward-y square-x])
                         (get-in board [forward-y square-x]))
        attack-left (when (on-board? [forward-y left-x])
                      (get-in board [forward-y left-x]))
        attack-right (when (on-board? [forward-y right-x])
                       (get-in board [forward-y right-x]))
        jump-forward-square (when (on-board? [jump-forward-y square-x])
                              (get-in game-state [:board
                                                  jump-forward-y square-x]))]
    (concat
     (when (and forward-square (empty-square? forward-square))
       [[forward-y square-x]])
     (when (and (= square-y starting-row)
                (empty-square? forward-square)
                (empty-square? jump-forward-square))
       [[jump-forward-y square-x]])
     (when (and attack-left (opposite-color? piece attack-left))
       [[forward-y left-x]])
     (when (and attack-right (opposite-color? piece attack-right))
       [[forward-y right-x]])

     (when (= square-y en-passant-row)
       (let [last-move-from (board-coords (first last-move))
             last-move-to (board-coords (second last-move))
             last-moved-piece (get-in (:board game-state)
                                      (board-coords (second last-move)))]
         (when (= (piece-type last-moved-piece) \P)
           (if (and attack-left
                    (= last-move-from [jump-forward-y left-x])
                    (= last-move-to [square-y left-x]))
             [[forward-y left-x]]
             (if (and attack-right
                      (= last-move-from [jump-forward-y right-x])
                      (= last-move-to [square-y right-x]))
               [[forward-y right-x]]))))))))


(defmethod legal-destination-indexes \Q [game-state square-coords piece]
  (let [board (:board game-state)
        non-blocked-square?
        (fn [[dy dx] [y x]]
          (and (on-board? [y x])
               (let [square-piece (get-in board [y x])]
                 (and (not (same-color? piece square-piece))
                      (let [[prev-square-y prev-square-x] (map - [y x] [dy dx])
                            prev-square-piece (get-in board [prev-square-y prev-square-x])]
                        (or (empty-square? prev-square-piece)
                            (not (opposite-color? piece prev-square-piece))))))))]
    (concat
     ;; moves upward
     (take-while
      (partial non-blocked-square? [-1 0])
      (map #(map + square-coords %) (for [dy (range -1 -8 -1)] [dy 0])))
     ;; moves downward
     (take-while
      (partial non-blocked-square? [+1 0])
      (map #(map + square-coords %) (for [dy (range +1 +8 +1)] [dy 0])))
     ;; moves left
     (take-while
      (partial non-blocked-square? [0 -1])
      (map #(map + square-coords %) (for [dx (range -1 -8 -1)] [0 dx])))
     ;; moves right
     (take-while
      (partial non-blocked-square? [0 +1])
      (map #(map + square-coords %) (for [dx (range +1 +8 +1)] [0 dx])))
     ;; moves up-left
     (take-while
      (partial non-blocked-square? [-1 -1])
      (map #(map + square-coords %) (for [d (range -1 -8 -1)] [d d])))
     ;; moves up-right
     (take-while
      (partial non-blocked-square? [-1 +1])
      (map #(map + square-coords %) (for [d (range +1 +8 +1)] [(- d) d])))
     ;; moves down-left
     (take-while
      (partial non-blocked-square? [+1 -1])
      (map #(map + square-coords %) (for [d (range +1 +8 +1)] [d (- d)])))
     ;; moves down-left
     (take-while
      (partial non-blocked-square? [+1 +1])
      (map #(map + square-coords %) (for [d (range +1 +8 +1)] [d d]))))))


(defmethod legal-destination-indexes \B [game-state square-coords piece]
  (let [board (:board game-state)
        non-blocked-square?
        (fn [[dy dx] [y x]]
          (and (on-board? [y x])
               (let [square-piece (get-in board [y x])]
                 (and (not (same-color? piece square-piece))
                      (let [[prev-square-y prev-square-x] (map - [y x] [dy dx])
                            prev-square-piece (get-in board [prev-square-y prev-square-x])]
                        (or (empty-square? prev-square-piece)
                            (not (opposite-color? piece prev-square-piece))))))))]
    (concat
     ;; moves up-left
     (take-while
      (partial non-blocked-square? [-1 -1])
      (map #(map + square-coords %) (for [d (range -1 -8 -1)] [d d])))
     ;; moves up-right
     (take-while
      (partial non-blocked-square? [-1 +1])
      (map #(map + square-coords %) (for [d (range +1 +8 +1)] [(- d) d])))
     ;; moves down-left
     (take-while
      (partial non-blocked-square? [+1 -1])
      (map #(map + square-coords %) (for [d (range +1 +8 +1)] [d (- d)])))
     ;; moves down-left
     (take-while
      (partial non-blocked-square? [+1 +1])
      (map #(map + square-coords %) (for [d (range +1 +8 +1)] [d d]))))))


(defmethod legal-destination-indexes \K [game-state square-coords piece]
  (let [board (:board game-state)]
    (concat
     (filter (fn [[y x]]
               (and (on-board? [y x])
                    (let [square-piece (get-in board [y x])]
                      (or (empty-square? square-piece)
                          (opposite-color? piece square-piece)))))
             (map #(map + square-coords %) [[-1  0] [-1 -1] [-1 +1]
                                            [ 0 -1]         [ 0 +1]
                                            [+1 -1] [+1  0] [+1 +1]]))
     (when (and (= (:turn game-state) \W)
                (= square-coords [7 4]))
       (if (and (:white-can-castle-ks game-state)
                (= [(get-in board [7 5]) (get-in board [7 6])]
                   [empty-square empty-square])
                (not (some #{"e1" "f1" "g1"}
                           (squares-attacked-by-opponent game-state))))
         [[7 6]]
         (if (and (:white-can-castle-qs game-state)
                  (= [(get-in board [7 3]) (get-in board [7 2])]
                     [empty-square empty-square])
                  (not (some #{"e1" "d1" "c1"}
                             (squares-attacked-by-opponent game-state))))
           [[7 2]])))
     (when (and (= (:turn game-state) \B)
                (= square-coords [0 4]))
       (if (and (:black-can-castle-ks game-state)
                (= [(get-in board [0 5]) (get-in board [0 6])]
                   [empty-square empty-square])
                (not (some #{"e8" "f8" "g8"}
                           (squares-attacked-by-opponent game-state))))
         [[0 6]]
         (if (and (:black-can-castle-qs game-state)
                  (= [(get-in board [0 3]) (get-in board [0 2])]
                     [empty-square empty-square])
                  (not (some #{"e8" "d8" "c8"}
                             (squares-attacked-by-opponent game-state))))
           [[0 2]]))))))


(defmethod legal-destination-indexes :default [_ _ _]
  [])


(defn remove-piece [board position]
  (let [[y x] (board-coords position)
        piece (get-in board [y x])]
    [(assoc-in board [y x] empty-square) piece]))


(defn put-piece [board position piece]
  (let [[y x] (board-coords position)]
    (assoc-in board [y x] piece)))


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
        [y x] (board-coords position)
        piece-color (color (get-in board [y x]))]
    (put-piece board position (str piece-color new-piece-type))))


;; This function evaluates to a new board, not a [board piece] pair
(defn possibly-extra-board-change [board piece piece-move]
  (cond (= (piece-type piece) \K)
        ;; extra tower moves when castling:
        (cond (= piece-move ["e1" "c1"])  ;; white castling queenside
              (first (move-piece-on-board board ["a1" "d1"]))

              (= piece-move ["e1" "g1"]) ;; white castling kingside
              (first (move-piece-on-board board ["h1" "f1"]))

              (= piece-move ["e8" "c8"])  ;; black castling queenside
              (first (move-piece-on-board board ["a8" "d8"]))

              (= piece-move ["e8" "g8"]) ;; black castling kingside
              (first (move-piece-on-board board ["h8" "f8"]))

              :else board)

        (= (piece-type piece) \P)
        (let [[from-pos to-pos] piece-move
              pawn-promotion (> (.length to-pos) 2)]
          (if pawn-promotion
            (promote board to-pos)
            (let [[[from-y from-x] [to-y to-x]] (map board-coords piece-move)]
              ;; en passant moves require removing pawns
              ;; Condition: the pawn is changing column
              ;;            (we assume here the move was legal)
              (if (not= from-x to-x)
                (first (remove-piece board
                                     (board-notation [from-y to-x])))
                board))))

        :else board))


(defn move [game-state [from-pos to-pos]]
  ;; This function always assumes the move is legal
  (let [new-board
        (let [[board' piece] (move-piece-on-board
                              (:board game-state) [from-pos to-pos])
              ;; special cases where extra pieces need to be moved or removed
              board'' (possibly-extra-board-change board' piece [from-pos to-pos])]
          board'')
        new-game-state (-> (assoc game-state :board new-board)
                           (update-castling-info from-pos)
                           (update :turn change-side)
                           (update :moves-cnt inc)
                           (assoc :last-move [from-pos to-pos]))]
    new-game-state))
