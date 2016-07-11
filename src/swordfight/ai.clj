(ns swordfight.ai
  (:use [swordfight.game-rules :only [black? color change-side piece-type board-coords board-notation move possible-moves]]))


(defn eval-board [board]
  ;; TODO: Currently it only cares about material
  (reduce + (map {
                              ;; FIXME:
                  "BK" 200000 ;; higher value prevents engine from sacrificing
                              ;; his king for white king in his plans
                  "WK" -20000
                  "BQ" 900
                  "WQ" -900
                  "BR" 500
                  "WR" -500
                  "BB" 300
                  "WB" -300
                  "BN" 300
                  "WN" -300
                  "BP" 100
                  "WP" -100
                  "  " 0}
                 (for [y (range 8)
                       x (range 8)]
                   (get-in board [y x])))))


(def minimax-depth 2)


(defn find-available-moves [side board last-move]
  (mapcat (fn [[coords]]
            (for [possible-move (possible-moves board coords side last-move)]
              [coords possible-move]))
          (for [y (range 8) x (range 8)] [[y x]])))


(defn choose-best-move
  ([side board last-move]
   (first (choose-best-move side board last-move minimax-depth)))
  ([side board last-move depth]
   (let [available-moves (find-available-moves side board last-move)]
     (apply (if (= side \B) max-key min-key)
            second ;; board evaluation is second in the pair
            (for [piece-move available-moves]
              (let [square-from (board-coords (first piece-move))
                    square-to (board-coords (second piece-move))
                    board-after-move (move board square-from square-to)
                    board-value (if-not (pos? depth)
                                  (eval-board board-after-move)
                                  (second (choose-best-move
                                           (change-side side)
                                           board-after-move
                                           (map board-notation piece-move)
                                           (dec depth))))]
                [[square-from square-to] board-value]))))))


(defn mexican-defense [game-state game-settings _]
  (let [first-moves [["b8" "c6"] ["g8" "f6"]]
        moves-cnt (:moves-cnt game-state)
        [square-from square-to] (if (and (< moves-cnt (count first-moves))
                                         (false? (:edited game-state)))
                                  (first-moves moves-cnt)
                                  (choose-best-move
                                   \B
                                   (:board game-state)
                                   (:last-move game-state)))]
    [(-> (update game-state :board move square-from square-to)
         (update :moves-cnt inc)
         (assoc :last-move [square-from square-to]))
     game-settings
     (str "move " square-from square-to)]))
