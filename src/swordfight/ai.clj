(ns swordfight.ai
  (:require [swordfight.game-rules :refer [move find-available-moves
                                           squares-attacked-by
                                           pieces-attacked-by-opponent]]
            [swordfight.board :refer [change-side]]
            [taoensso.tufte :refer [defnp p profiled profile]]
            [swordfight.profiling]))

(defn eval-board [board]
  ;; TODO: Currently it only cares about material
  (reduce + (map {\k  20000 \q  900 \r  500 \b  300 \n  300 \p  100
                  \K -20000 \Q -900 \R -500 \B -300 \N -300 \P -100
                  \. 0}
                 (flatten board))))

(def minimax-depth 4)

(defn choose-best-move [game-state depth]
  (let [available-moves (find-available-moves game-state)
        mapping-fn ;; simple parallelization only for bigger cases
        (if (> depth 2) pmap map)]
    (apply (if (= (:turn game-state) \B) max-key min-key)
           second ;; board evaluation is second in the pair
           (mapping-fn
            (fn [piece-move]
              (let [state-after-move (move game-state piece-move)
                    board-value (if (= depth 1)
                                  (eval-board (:board state-after-move))
                                  (second (choose-best-move
                                           state-after-move
                                           (dec depth))))]
                [piece-move board-value]))
            available-moves))))

(defn compute-midgame-move [game-state]
  (let [[best-mv _] (choose-best-move game-state minimax-depth)]
    best-mv))

(def mexican-defense [[nil nil] ["b8" "c6"] [nil nil] ["g8" "f6"]])

(def early-game-turns 4)

(defn compute-early-move [{turn :turn :as game-state}]
  (let [available-moves (find-available-moves game-state)
        attacked-pieces (pieces-attacked-by-opponent game-state)]
    (if (seq attacked-pieces)
      (compute-midgame-move game-state)
      (let [moves [["d7" "d5"] ["e7" "e5"] ["d7" "d6"] ["e7" "e6"]]]
        (let [good-moves (filter (fn [mv]
                                   (and (some #{mv} available-moves)
                                        (not (some #{(second mv)}
                                                   (squares-attacked-by
                                                    (move game-state mv)
                                                    (change-side turn))))))
                                 moves)]
          (if (seq good-moves)
            (first good-moves)
            (compute-midgame-move game-state)))))))

(defn compute-move [game-state]
  (let [moves-cnt (:moves-cnt game-state)
        [square-from square-to] (if (and (< moves-cnt early-game-turns)
                                         (false? (:edited game-state)))
                                  (compute-early-move game-state)
                                  (compute-midgame-move game-state))]
    [square-from square-to]))
