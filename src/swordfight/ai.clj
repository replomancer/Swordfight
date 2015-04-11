(ns swordfight.ai
  (:use [swordfight.game-rules :only [black? color flip-color piece-type board-coords move legal-destination-indexes]]))


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
                   ((board y) x)))))


(def minimax-depth 2)


(defn choose-best-move
  ([side board last-move]
     (first
      (choose-best-move side board last-move minimax-depth)))
  ([side board last-move depth]
     (let [available-moves
              (remove #(empty? (second %))
                      (map (fn [[coords piece]] [coords (legal-destination-indexes board coords piece nil)])
                           (remove nil?
                                   (for [y (range 8)
                                         x (range 8)]
                                     (let [piece ((board y) x)]
                                       (when (= (color piece) side)
                                         [[y x] piece]))))))]
       (apply (if (= side \B) max-key
                              min-key)
              second ;; board evaluation is second in the pair
              (apply concat
                     (for [piece-moves available-moves]
                       (let [square-from (board-coords (first piece-moves))]
                         (for [square-to-notation (second piece-moves)]
                           (let [square-to (board-coords square-to-notation)
                                 board-after-move (move board square-from square-to)]
                             (let [board-value (if-not (pos? depth)
                                                 (eval-board board-after-move)
                                                 (second (choose-best-move
                                                          (flip-color side)
                                                          board-after-move
                                                          ;;FIXME: last-move
                                                          [[nil nil] [square-from square-to]]
                                                          (dec depth))))]
                               [[square-from square-to] board-value]))))))))))


(defn mexican-defense [game-state game-settings _]
  (let [first-moves [["b8" "c6"] ["g8" "f6"]]
        moves-cnt (:moves-cnt game-state)
        [square-from square-to] (if (< moves-cnt (count first-moves))
                                  (first-moves moves-cnt)
                                  (choose-best-move
                                   \B
                                   (:board game-state)
                                   (:last-move game-state)))]
    [(update (update game-state :board move square-from square-to)
             :moves-cnt inc)
     game-settings
     (str "move " square-from square-to)]))
