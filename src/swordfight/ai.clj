(ns swordfight.ai
  (:use [swordfight.game-rules :only [black? color change-side piece-type move find-available-moves]]))


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


(def minimax-depth 3)


(defn choose-best-move-at-depth [game-state depth]
  (let [available-moves (find-available-moves game-state)
        mapping-fn ;; simple parallelization only for bigger cases
                   (if (> depth 1) pmap map)]
    (apply (if (= (:turn game-state) \B) max-key min-key)
           second ;; board evaluation is second in the pair
           (mapping-fn
            (fn [piece-move]
              (let [state-after-move (move game-state piece-move)
                    board-value (if-not (pos? depth)
                                  (eval-board (:board state-after-move))
                                  (second (choose-best-move-at-depth
                                           state-after-move
                                           (dec depth))))]
                [piece-move board-value]))
            available-moves))))


(defn choose-best-move [game-state]
  (let [[best-mv _] (choose-best-move-at-depth game-state minimax-depth)]
    best-mv))


(defn mexican-defense [game-state game-settings msg]
  (let [first-moves [[nil nil] ["b8" "c6"] [nil nil] ["g8" "f6"]]
        moves-cnt (:moves-cnt game-state)
        [square-from square-to] (if (and (< moves-cnt (count first-moves))
                                         (false? (:edited game-state)))
                                  (first-moves moves-cnt)
                                  (choose-best-move game-state))]
    (if (= (:turn game-state) \B)
      [(move game-state [square-from square-to])
       game-settings
       (str "move " square-from square-to)]
      [game-state
       game-settings
       msg])))
