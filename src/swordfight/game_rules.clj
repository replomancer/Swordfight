(ns swordfight.game-rules)

(def initial-board
  ["BR"  "BN"  "BB"  "BQ"  "BK"  "BB"  "BN"  "BR"
   "B "  "B "  "B "  "B "  "B "  "B "  "B "  "B "
   "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  "
   "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  "
   "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  "
   "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  "
   "W "  "W "  "W "  "W "  "W "  "W "  "W "  "W "
   "WR"  "WN"  "WB"  "WQ"  "WK"  "WB"  "WN"  "WR"])

(def empty-square "  ")

(def empty-board
  (for [_ (range 64)]
    empty-square))

(def initial-game-state {:board initial-board
                         :turn "white"})

(def squares
  ["a8" "b8" "c8" "d8" "e8" "f8" "g8" "h8"
   "a7" "b7" "c7" "d7" "e7" "f7" "g7" "h7"
   "a6" "b6" "c6" "d6" "e6" "f6" "g6" "h6"
   "a5" "b5" "c5" "d5" "e5" "f5" "g5" "h5"
   "a4" "b4" "c4" "d4" "e4" "f4" "g4" "h4"
   "a3" "b3" "c3" "d3" "e3" "f3" "g3" "h3"
   "a2" "b2" "c2" "d2" "e2" "f2" "g2" "h2"
   "a1" "b1" "c1" "d1" "e1" "f1" "g1" "h1"])

(defn white? [piece] (= (first piece) \W))

(defn black? [piece] (= (first piece) \B))

(defn pos2idx [pos]
  (.indexOf squares pos))

(defn remove-piece [board position]
  (let [index (pos2idx position)
        piece (board index)]
    [(assoc board index empty-square) piece]))

(defn put-piece [board position piece]
  (let [index (pos2idx position)]
    (assoc board index piece)))

(defn move [board from-pos to-pos]
  (let [[board' piece] (remove-piece board from-pos)]
    (put-piece board' to-pos piece)))
