(ns swordfight.ai
  (:use [swordfight.game-rules :only [black? board-coords move legal-destination-indexes]]))


(defn mexican-defense [game-state game-settings _]
  (let [board (:board game-state)]
    (if (= ((board 0) 1) "BN") ;; b8
      [(assoc game-state :board (move board "b8" "c6"))
       game-settings
       "move b8c6"]
      (if (= ((board 0) 6) "BN") ;; g8
        [(assoc game-state :board (move board "g8" "f6"))
         game-settings
         "move g8f6"]
        (let [some-moves
              (remove #(empty? (second %))
                      (map (fn [[coords piece]] [coords (legal-destination-indexes board coords piece nil)])
                           (remove nil?
                                   (for [y (range 8)
                                         x (range 8)]
                                     (let [piece ((board y) x)]
                                       (when (some #{piece} ["BN" "BR" "BP"])
                                         [[y x] piece]))))))]
          (if (seq some-moves)
            (let [piece-moves (rand-nth some-moves)
                  square-from (board-coords (first piece-moves))
                  square-to (board-coords (rand-nth (second piece-moves)))]
              (println "# some-moves:" some-moves)
              [(assoc game-state :board (move board square-from square-to))
               game-settings
               (str "move " square-from square-to)])
            [game-state
             game-settings
             "tellopponent Good Game! I give up.\nresign"]))))))
