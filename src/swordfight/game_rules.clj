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
                         :turn "white"
                         :moves-cnt 0
                         :edited false
                         :last-move nil})

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

(def flip-color {\B \W
                 \W \B})

(defn on-board? [[y x]]
  (when (and (<= 0 y 7) (<= 0 x 7))
    [y x]))


(defmulti legal-destination-indexes (fn [board square-coords piece last-move]
                                      (piece-type piece)))


(defmethod legal-destination-indexes \N [board square-coords piece _]
  (filter (fn [[y x]] (and (on-board? [y x])
                           (not= (color (get-in board [y x])) (color piece))))
          (map #(map + square-coords %)
               [[-2 -1] [-1 -2] [+1 -2] [+2 -1]
                [-2 +1] [-1 +2] [+1 +2] [+2 +1]])))


;; FIXME: refactor this
(defmethod legal-destination-indexes \R [board square-coords piece _]
  (let [non-blocked-square?
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
      (map #(map + square-coords %) (for [dx (range +1 +8 +1)] [0 dx]))))))


(defmethod legal-destination-indexes \P [board [square-y square-x] piece last-move]
  (let [[forward-direction starting-row en-passant-row promotion-row]
        (if (black? (get-in board [square-y square-x]))
          [+1 1 4 7]
          [-1 6 3 0])
        forward-y (+ square-y forward-direction)
        attack-left-x (dec square-x)
        attack-right-x (inc square-x)
        jump-forward-y (+ square-y (* 2 forward-direction))
        forward-square (when (on-board? [forward-y square-x])
                         (get-in board [forward-y square-x]))
        attack-left (when (on-board? [forward-y attack-left-x])
                      (get-in board [forward-y attack-left-x]))
        attack-right (when (on-board? [forward-y attack-right-x])
                       (get-in board [forward-y attack-right-x]))
        jump-forward-square (when (on-board? [jump-forward-y square-x])
                              (get-in board [jump-forward-y square-x]))]
    (remove nil?
            [(when (and forward-square (empty-square? forward-square))
               [forward-y square-x])
             (when (and (= square-y starting-row)
                        (empty-square? forward-square)
                        (empty-square? jump-forward-square))
               [jump-forward-y square-x])
             (when (and attack-left (opposite-color? piece attack-left))
               [forward-y attack-left-x])
             (when (and attack-right (opposite-color? piece attack-right))
               [forward-y attack-right-x])
             (when (and
                    attack-left
                    (= square-y en-passant-row)
                    (= (piece-type (first last-move)) \P)
                    (= (second last-move) [[(- square-y (* 2 (- forward-direction)))
                                            (dec square-x)]
                                           [square-y (dec square-x)]]))
               [forward-y attack-left-x])
             (when (and
                    attack-right
                    (= square-y en-passant-row)
                    (= (piece-type (first last-move)) \P)
                    (= (second last-move) [[(- square-y (* 2 (- forward-direction)))
                                            (inc square-x)]
                                           [square-y (inc square-x)]]))
               [forward-y attack-right-x])])))


(defmethod legal-destination-indexes \Q [board square-coords piece _]
  (let [non-blocked-square?
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


(defmethod legal-destination-indexes \B [board square-coords piece _]
  (let [non-blocked-square?
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


(defmethod legal-destination-indexes \K [board square-coords piece _]
  (filter (fn [[y x]]
            (and (on-board? [y x])
                 (let [square-piece (get-in board [y x])]
                   (or (empty-square? square-piece)
                       (opposite-color? piece square-piece)))))
          (map #(map + square-coords %) [[-1  0] [-1 -1] [-1 +1]
                                         [ 0 -1]         [ 0 +1]
                                         [+1 -1] [+1  0] [+1 +1]])))


(defmethod legal-destination-indexes :default [_ _ _ _]
  [])


(defn possible-moves [board from-square player-color last-move]
  (let [piece (get-in board from-square)
        [piece-color piece-type] piece]
    (if (not= piece-color player-color) ;; this case also covers empty squares
      []
      (legal-destination-indexes board from-square piece last-move))))

(defn remove-piece [board position]
  (let [[y x] (board-coords position)
        piece (get-in board [y x])]
    [(assoc-in board [y x] empty-square) piece]))

(defn put-piece [board position piece]
  (let [[y x] (board-coords position)]
    (assoc-in board [y x] piece)))

(defn move [board from-pos to-pos]
  (let [[board' piece] (remove-piece board from-pos)]
    (cond (and (= (piece-type piece) \K) ;; castling queenside - assume it's legal
               (= [from-pos to-pos] ["e1" "c1"])) (let [board'' (move board' "a1" "d1")]
                                                    (put-piece board'' to-pos piece))
          (and (= (piece-type piece) \K) ;; castling kingside
               (= [from-pos to-pos] ["e1" "g1"])) (let [board'' (move board' "h1" "f1")]
                                                    (put-piece board'' to-pos piece))
          :else (put-piece board' to-pos piece))))

(defn promote [board position new-piece-type]
  (let [new-piece-type (.toUpperCase new-piece-type)
        [y x] (board-coords position)
        piece-color (color (get-in board [y x]))]
    (put-piece board position (str piece-color new-piece-type))))