(ns swordfight.ai
  (:use [swordfight.game-rules :only [black? board-coords move legal-destination-indexes]]))


(defn mexican-defense [game-state game-settings _]
  (let [board (:board game-state)]
    (if (black? ((board 0) 1)) ;; b8
      [(assoc game-state :board (move board "b8" "c6"))
       game-settings
       "move b8c6"]
      (if (black? ((board 0) 6)) ;; g8
        [(assoc game-state :board (move board "g8" "f6"))
         game-settings
         "move g8f6"]
        (let [some-moves
                (map (fn [coords] [coords (legal-destination-indexes board coords "BN" nil)])
                     (filter (complement nil?)
                             (for [y (range 8)
                                   x (range 8)]
                               (when (= ((board y) x) "BN")
                                 [y x]))))]
            (if (seq some-moves)
              (let [piece-moves (rand-nth some-moves)
                    square-from (board-coords (first piece-moves))
                    square-to (board-coords (rand-nth (second piece-moves)))]
                [(assoc game-state :board (move board square-from square-to))
                 game-settings
                 (str "tellopponent Wait! He isn't dead! Mexican Surprise!\n move " square-from square-to)])
              [game-state
               game-settings
               "tellopponent Good Game! I give up.\nresign"]))))))