(ns swordfight.ai
  (:require [swordfight.game-rules :refer [move find-available-moves]]))

(defn eval-board [board]
  ;; TODO: Currently it only cares about material
  (reduce + (map {\k  20000 \q  900 \r  500 \b  300 \n  300 \p  100
                  \K -20000 \Q -900 \R -500 \B -300 \N -300 \P -100
                  \. 0}
                 (flatten board))))

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
