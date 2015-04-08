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
   "BP" "♟"
   "WP" "♙"
   "  " " "
   })

(defn show-board [board]
  (println "#")
  (dotimes [row 8]
    (print "#" (- 8 row) " ")
    (apply print (map pretty-unicode-piece (board row)))
    (println))
  (println "#")
  (print "#    ")
  (println \a \b \c \d \e \f \g \h)
  (println "#"))

(defn show-game-state [game-state]
  (println "#" (dissoc game-state :board))
  (show-board (:board game-state)))

(defn show-game-settings [game-settings]
  (println "#" game-settings))

(defn print-debug-output [game-state game-settings]
  (show-game-settings game-settings)
  (show-game-state game-state))
