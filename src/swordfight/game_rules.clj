(ns swordfight.game-rules)

(def initial-board
  ["br"  "bb"  "bk"  "bq"  "bK"  "bk"  "bb"  "br"
   "bp"  "bp"  "bp"  "bp"  "bp"  "bp"  "bp"  "bp"
   "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  "
   "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  "
   "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  "
   "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  "
   "wp"  "wp"  "wp"  "wp"  "wp"  "wp"  "wp"  "wp"
   "wr"  "wb"  "wk"  "wq"  "wK"  "wk"  "wb"  "wr"])


(defn white? [piece] (= (first piece) \w))

(defn black? [piece] (= (first piece) \b))

(def empty-square "  ")

(defn move [board from to]
  (assoc (assoc board to (board from)) from empty-square))


(def a8 0)
(def b8 1)
(def c8 2)
(def d8 3)
(def e8 4)
(def f8 5)
(def g8 6)
(def h8 7)
(def a7 8)
(def b7 9)
(def c7 10)
(def d7 11)
(def e7 12)
(def f7 13)
(def g7 14)
(def h7 15)
(def a6 16)
(def b6 17)
(def c6 18)
(def d6 19)
(def e6 20)
(def f6 21)
(def g6 22)
(def h6 23)
(def a5 24)
(def b5 25)
(def c5 26)
(def d5 27)
(def e5 28)
(def f5 29)
(def g5 30)
(def h5 31)
(def a4 32)
(def b4 33)
(def c4 34)
(def d4 35)
(def e4 36)
(def f4 37)
(def g4 38)
(def h4 39)
(def a3 40)
(def b3 41)
(def c3 42)
(def d3 43)
(def e3 44)
(def f3 45)
(def g3 46)
(def h3 47)
(def a2 48)
(def b2 49)
(def c2 50)
(def d2 51)
(def e2 52)
(def f2 53)
(def g2 54)
(def h2 55)
(def a1 56)
(def b1 57)
(def c1 58)
(def d1 59)
(def e1 60)
(def f1 61)
(def g1 62)
(def h1 63)
