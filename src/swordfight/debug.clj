(ns swordfight.debug
  (:require [swordfight.board :refer [white-king white-queen white-rook
                                      white-bishop white-knight white-pawn
                                      black-king black-queen black-rook
                                      black-bishop black-knight black-pawn
                                      empty-square]]))

(def pretty-unicode-piece
  {black-rook "♜" black-knight "♞" black-bishop "♝"
   black-queen "♛" black-king "♚" black-pawn "♟"
   white-rook "♖" white-knight "♘" white-bishop "♗"
   white-queen "♕" white-king "♔" white-pawn "♙"
   empty-square " "})

(defn show-board [board]
  (println "#")
  (dotimes [row 8]
    (print "#" (- 8 row) " ")
    (doseq [piece (board row)]
      (print (str (pretty-unicode-piece piece) " ")))
    (println))
  (println "#")
  (print "#    ")
  (println "a b c d e f g h")
  (println "#"))

(defn show-game-state [game-state]
  (println "#" (dissoc game-state :board))
  (show-board (:board game-state)))

(defn show-game-settings [game-settings]
  (println "#" game-settings))

(defn print-debug-output [game-state game-settings]
  (show-game-settings game-settings)
  (show-game-state game-state))
