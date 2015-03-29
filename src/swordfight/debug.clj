(ns swordfight.debug)

(def pretty-unicode-piece
  {
   "BR" "♜"
   "WR" "♖"
   "BN" "♞"
   "WN" "♘"
   "BB" "♝"
   "WB" "♗"
   "BQ" "♛"
   "WQ" "♕"
   "BK" "♚"
   "WK" "♔"
   "B " "♟"
   "W " "♙"
   "  " " "
   })

(defn show-board [board]
  (println)
  (dotimes [row 8]
    (print (- 8 row) " ")
    (apply print (map pretty-unicode-piece (take 8 (drop (* row 8) board))))
    (println))
  (println)
  (print "   ")
  (println \a \b \c \d \e \f \g \h)
  (println))