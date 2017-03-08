(ns swordfight.board
  (:require [clojure.set :refer [map-invert]]))

(def initial-board [[\r \n \b \q \k \b \n \r]
                    [\p \p \p \p \p \p \p \p]
                    [\. \. \. \. \. \. \. \.]
                    [\. \. \. \. \. \. \. \.]
                    [\. \. \. \. \. \. \. \.]
                    [\. \. \. \. \. \. \. \.]
                    [\P \P \P \P \P \P \P \P]
                    [\R \N \B \Q \K \B \N \R]])

(def empty-board [[\. \. \. \. \. \. \. \.]
                  [\. \. \. \. \. \. \. \.]
                  [\. \. \. \. \. \. \. \.]
                  [\. \. \. \. \. \. \. \.]
                  [\. \. \. \. \. \. \. \.]
                  [\. \. \. \. \. \. \. \.]
                  [\. \. \. \. \. \. \. \.]
                  [\. \. \. \. \. \. \. \.]])

(def white \W)
(def black \B)

(def king \K)
(def queen \Q)
(def bishop \B)
(def knight \N)
(def rook \R)
(def pawn \P)

(def white-king \K)
(def white-queen \Q)
(def white-bishop \B)
(def white-knight \N)
(def white-rook \R)
(def white-pawn \P)
(def black-king \k)
(def black-queen \q)
(def black-bishop \b)
(def black-knight \n)
(def black-rook \r)
(def black-pawn \p)
(def empty-square \.)

(defn piece-type [piece] (Character/toUpperCase piece))

(def white-piece? #{white-king white-queen white-bishop white-knight
                    white-rook white-pawn})
(def black-piece? #{black-king black-queen black-bishop black-knight
                    black-rook black-pawn})
(def empty-square? #(= empty-square %))
(def pawn? #(= (piece-type %) pawn))

(defn ->piece [p-color p-type] (if (= p-color white)
                                 p-type
                                 (Character/toLowerCase p-type)))

(defn ->board [board-files-vector]
  (mapv (fn [row] (vec (remove #(= % \ ) row))) board-files-vector))

(defn color [piece] (cond (white-piece? piece) white
                          (black-piece? piece) black))

(defn opposite-color? [piece piece']
  (let [colors (map color [piece piece'])]
    (or (= colors [black white])
        (= colors [white black]))))

(def change-side {black white white black})

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

(def all-coords (for [y (range 8) x (range 8)] [y x]))

(defn on-board? [[y x]]
  (and (<= 0 y 7) (<= 0 x 7)))

(defn get-piece [board position]
  (get-in board (notation->coords position)))

(defn remove-piece [board position]
  (let [yx (notation->coords position)
        piece (get-in board yx)]
    [(assoc-in board yx empty-square) piece]))

(defn put-piece [board position piece]
  (let [yx (notation->coords position)]
    (assoc-in board yx piece)))

(defn move-piece [board [from-pos to-pos]]
  (let [[board' piece] (remove-piece board from-pos)
        to-pos' (subs to-pos 0 2)]  ;; ignore promotions, just move
    [(put-piece board' to-pos' piece) piece]))

(defn promote-pawn [board promotion-notation]
  (let [position (subs promotion-notation 0 2)
        new-piece-type (-> (subs promotion-notation 2 3)
                           (clojure.string/upper-case)
                           (get 0))
        new-piece (->piece (color (get-piece board position))
                           new-piece-type)]
    [(put-piece board position new-piece) new-piece]))
