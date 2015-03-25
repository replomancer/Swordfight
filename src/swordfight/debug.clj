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
   "  " "  "
   })

(defn show-board [board]
  (dotimes [row 8]
    (println (map pretty-unicode-piece (take 8 (drop (* row 8) board))))))