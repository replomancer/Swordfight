(ns swordfight.ai
  (:require [swordfight.game-rules :refer [move find-legal-moves
                                           find-pseudolegal-moves
                                           squares-attacked-by
                                           pieces-attacked-by-opponent
                                           king-in-check?]]
            [swordfight.board :refer [all-coords change-side empty-square?]]
            [taoensso.tufte :refer [defnp p profiled profile]]
            [swordfight.profiling]))

(def piece-value {\K  20000 \Q  900 \R  500 \B  300 \N  300 \P  100
                  \k -20000 \q -900 \r -500 \b -300 \n -300 \p -100
                  \. 0})

(def checkmated-val {\W -100000 \B 100000})
(def stalemated-val {\W -50000  \B 50000})

;;;  Tomasz Michniewski's  Simplified evaluation function
(def white-bonus-eval-tables
  {\P [[  0   0   0   0   0   0   0   0 ]
       [ 50  50  50  50  50  50  50  50 ]
       [ 10  10  20  30  30  20  10  10 ]
       [  5   5  10  25  25  10   5   5 ]
       [  0   0   0  20  20   0   0   0 ]
       [  5  -5 -10   0   0 -10  -5   5 ]
       [  5  10  10 -20 -20  10  10   5 ]
       [  0   0   0   0   0   0   0   0 ]]
   \N [[-50 -40 -30 -30 -30 -30 -40 -50 ]
       [-40 -20   0   0   0   0 -20 -40 ]
       [-30   0  10  15  15  10   0 -30 ]
       [-30   5  15  20  20  15   5 -30 ]
       [-30   0  15  20  20  15   0 -30 ]
       [-30   5  10  15  15  10   5 -30 ]
       [-40 -20   0   5   5   0 -20 -40 ]
       [-50 -40 -30 -30 -30 -30 -40 -50 ]]
   \B [[-20 -10 -10 -10 -10 -10 -10 -20 ]
       [-10   0   0   0   0   0   0 -10 ]
       [-10   0   5  10  10   5   0 -10 ]
       [-10   5   5  10  10   5   5 -10 ]
       [-10   0  10  10  10  10   0 -10 ]
       [-10  10  10  10  10  10  10 -10 ]
       [-10   5   0   0   0   0   5 -10 ]
       [-20 -10 -10 -10 -10 -10 -10 -20 ]]
   \R [[  0   0   0   0   0   0   0   0 ]
       [  5  10  10  10  10  10  10   5 ]
       [ -5   0   0   0   0   0   0  -5 ]
       [ -5   0   0   0   0   0   0  -5 ]
       [ -5   0   0   0   0   0   0  -5 ]
       [ -5   0   0   0   0   0   0  -5 ]
       [ -5   0   0   0   0   0   0  -5 ]
       [  0   0   0   5   5   0   0   0 ]]
   \Q [[-20 -10 -10  -5  -5 -10 -10 -20 ]
       [-10   0   0   0   0   0   0 -10 ]
       [-10   0   5   5   5   5   0 -10 ]
       [ -5   0   5   5   5   5   0  -5 ]
       [  0   0   5   5   5   5   0  -5 ]
       [-10   5   5   5   5   5   0 -10 ]
       [-10   0   5   0   0   0   0 -10 ]
       [-20 -10 -10  -5  -5 -10 -10 -20 ]]
   \K [[-30 -40 -40 -50 -50 -40 -40 -30 ]
       [-30 -40 -40 -50 -50 -40 -40 -30 ]
       [-30 -40 -40 -50 -50 -40 -40 -30 ]
       [-30 -40 -40 -50 -50 -40 -40 -30 ]
       [-20 -30 -30 -40 -40 -30 -30 -20 ]
       [-10 -20 -20 -20 -20 -20 -20 -10 ]
       [ 20  20   0   0   0   0  20  20 ]
       [ 20  30  10   0   0  10  30  20 ]]})

(def black-bonus-eval-tables
  (into {}
        (for [[c t] white-bonus-eval-tables]
          [(Character/toLowerCase c)
           (vec (reverse (map (fn [row] (mapv - row)) t)))])))

(def bonus-eval-tables
  (merge white-bonus-eval-tables black-bonus-eval-tables))

(defn piece-val-with-bonus [piece yx]
  (if (empty-square? piece)
    0
    (+ (piece-value piece) (get-in (bonus-eval-tables piece) yx))))

(defn eval-board [board]
  (reduce + (map (fn [[piece [y x]]]
                   (piece-val-with-bonus piece [y x]))
                 (for [[y x] all-coords]
                   [(get-in board [y x]) [y x]]))))

(def minimax-depth 4)

(defn choose-best-move [{:keys [board turn] :as game-state} depth]
  (let [available-moves (if (= depth minimax-depth)
                          (find-legal-moves game-state)
                          ;; This is "cheating" which harms how well we predict
                          ;; but it's very useful for performance (speed)
                          (find-pseudolegal-moves game-state))
        mapping-fn ;; simple parallelization only for bigger cases
        (if (> depth 2) pmap map)]
    (if (empty? available-moves)
      [nil (if (king-in-check? game-state turn)  ;; current player is checkmated
             (checkmated-val turn)
             (stalemated-val turn))]
      (apply (if (= turn \W) max-key min-key)
             second  ;; move evaluation is second in pair
             (mapping-fn
              (fn [piece-move]
                (let [state-after-move (move game-state piece-move)
                      move-value (if (= depth 1)
                                   (eval-board (:board state-after-move))
                                   (second (choose-best-move
                                            state-after-move
                                            (dec depth))))]
                  [piece-move move-value]))
              available-moves)))))

(defn compute-midgame-move [game-state]
  (let [[best-mv _] (choose-best-move game-state minimax-depth)]
    best-mv))

(def early-game-turns 4)

(defn compute-early-move [{turn :turn :as game-state}]
  (let [available-moves (find-legal-moves game-state)
        attacked-pieces (pieces-attacked-by-opponent game-state)]
    (if (seq attacked-pieces)
      (compute-midgame-move game-state)
      (let [moves [["d7" "d5"] ["e7" "e5"] ["d7" "d6"] ["e7" "e6"]]
            good-moves (filter (fn [mv]
                                 (and (some #{mv} available-moves)
                                      (not-any? #{(second mv)}
                                                (squares-attacked-by
                                                 (move game-state mv)
                                                 (change-side turn)))))
                               moves)]
        (if (seq good-moves)
          (first good-moves)
          (compute-midgame-move game-state))))))

(defn compute-move [game-state]
  (let [moves-cnt (:moves-cnt game-state)
        [square-from square-to] (if (and (< moves-cnt early-game-turns)
                                         (false? (:edited game-state)))
                                  (compute-early-move game-state)
                                  (compute-midgame-move game-state))]
    [square-from square-to]))
