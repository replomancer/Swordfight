(ns swordfight.game-rules
  (:use [clojure.set :only [map-invert]]))

(def initial-board
  [[ "BR"  "BN"  "BB"  "BQ"  "BK"  "BB"  "BN"  "BR" ]
   [ "B "  "B "  "B "  "B "  "B "  "B "  "B "  "B " ]
   [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
   [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
   [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
   [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
   [ "W "  "W "  "W "  "W "  "W "  "W "  "W "  "W " ]
   [ "WR"  "WN"  "WB"  "WQ"  "WK"  "WB"  "WN"  "WR" ]])

(def empty-square "  ")

(defn empty-square? [square] (= square empty-square))

(def empty-board
  (for [_ (range 8)]
      (for [_ (range 8)]
        empty-square)))

(def initial-game-state {:board initial-board
                         :turn "white"
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

(defn white? [piece] (= (color piece) \W))

(defn black? [piece] (= (color piece) \B))

(defn same-color? [piece1 piece2]
  (= (color piece1) (color piece2)))
  ;; two empty squares also have the same color

(defn opposite-color? [piece1 piece2]
  (let [colors (map color [piece1 piece2])]
    (or (= colors [\B \W])
        (= colors [\W \B]))))

(defn on-board? [[y x]]
  (when (and (<= 0 y 7) (<= 0 x 7))
    [y x]))

(defn square-left [[square-y square-x]]
  (on-board? [square-y (dec square-x)]))

(defn square-right [[square-y square-x]]
  (on-board? [square-y (inc square-x)]))

(defn square-up [[square-y square-x]]
  (on-board? [(dec square-y) square-x]))

(defn square-down [[square-y square-x]]
  (on-board? [(inc square-y) square-x]))

(defn square-up-left [[square-y square-x]]
  (on-board? [(dec square-y) (dec square-x)]))

(defn square-up-right [[square-y square-x]]
  (on-board? [(dec square-y) (inc square-x)]))

(defn square-down-left [[square-y square-x]]
  (on-board? [(inc square-y) (dec square-x)]))

(defn square-down-right [[square-y square-x]]
  (on-board? [(inc square-y) (inc square-x)]))

;; This is an experiment: dispatching on piece type.
(defmulti legal-destination-indexes (fn [board square-coords piece last-move]
                                      (second piece)))

(defmethod legal-destination-indexes \N [board square-coords piece _]
  (filter (fn [[y x]] (and (on-board? [y x])
                           (not (black? ((board y) x)))))
          (map #(map + square-coords %)
               [[-2 -1] [-1 -2] [+1 -2] [+2 -1]
                [-2 +1] [-1 +2] [+1 +2] [+2 +1]])))


;; FIXME: refactor this
(defmethod legal-destination-indexes \R [board square-coords piece _]
  (let [non-blocked-square?
        (fn [[dy dx] [y x]]
          (and (on-board? [y x])
               (let [square-piece ((board y) x)]
                 (and (not (same-color? piece square-piece))
                      (let [[prev-square-y prev-square-x] (map - [y x] [dy dx])
                            prev-square-piece ((board prev-square-y) prev-square-x)]
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

(defmethod legal-destination-indexes :default [_ _ _ _]
  [])

(defn possible-moves [board from-square player-color last-move]
  (let [square-idx (board-coords from-square)
        piece (board square-idx)
        [piece-color piece-type] piece]
    (if (not= piece-color player-color) ;; this case also covers empty squares
      []
      (legal-destination-indexes board square-idx piece last-move))))

(defn remove-piece [board position]
  (let [[y x] (board-coords position)
        piece ((board y) x)]
    [(update-in board [y x] (fn [_] empty-square)) piece]))

(defn put-piece [board position piece]
  (let [[y x] (board-coords position)]
    (update-in board [y x] (fn [_] piece))))

(defn move [board from-pos to-pos]
  (let [[board' piece] (remove-piece board from-pos)]
    (put-piece board' to-pos piece)))
