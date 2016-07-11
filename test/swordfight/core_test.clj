(ns swordfight.core-test
  (:use clojure.test
        [swordfight.core :only [initial-settings]]
        [swordfight.game-rules :only [initial-board change-side move
                                      board-coords board-notation empty-square?]]
        [swordfight.ai :only [find-available-moves choose-best-move]]))


(defn legal-moves-cnt-in-turn
  ([turn-nr] (legal-moves-cnt-in-turn turn-nr \W initial-board [nil nil]))
  ([turn-nr side board last-move]
   (if (= turn-nr 1)
     (count (find-available-moves side board last-move))
     (apply +
            (for [[square-from square-to]
                  (find-available-moves side board last-move)]
              (legal-moves-cnt-in-turn (dec turn-nr)
                                       (change-side side)
                                       (move board
                                             (board-coords square-from)
                                             (board-coords square-to))
                                       [square-from square-to]))))))


(deftest moves-generation-test
  (testing "How many moves are generated. Compare against known values."
    (is (= (legal-moves-cnt-in-turn 1) 20))
    (is (= (legal-moves-cnt-in-turn 2) 400))
    (is (= (legal-moves-cnt-in-turn 3) 8902))
    (is (= (legal-moves-cnt-in-turn 4) 197742))
    ;; TODO:
    ;; - The test fails for turn 5
    ;; - Find the right value for turn 6
    ;; - Improve the test speed
    ;;(is (= (legal-moves-cnt-in-turn 5) 4897256))
    ;;(is (= (legal-moves-cnt-in-turn 6) 120000000))
    ))


(deftest en-passant-test-case1
  (testing "Engine knows en passant moves 1."
    (let [board
          [[ "BR"  "BN"  "BB"  "BQ"  "BK"  "BB"  "BN"  "BR" ]
           [ "BP"  "  "  "BP"  "BP"  "BP"  "BP"  "BP"  "BP" ]
           [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
           [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
           [ "WP"  "BP"  "  "  "  "  "  "  "  "  "  "  "  " ]
           [ "  "  "  "  "  "  "WP"  "  "  "WN"  "  "  "  " ]
           [ "  "  "WP"  "WP"  "  "  "WP"  "WP"  "WP"  "WP" ]
           [ "WR"  "WN"  "WB"  "WQ"  "WK"  "WB"  "  "  "WR" ]]
          last-move ["a2" "a4"]
          found-moves (set (map (partial map board-notation)
                                     (find-available-moves \B board last-move)))]
      (is (contains? found-moves ["b4" "a3"]))
      (is (empty-square? (get-in (move board "b4" "a3") (board-coords "a4")))))))


(deftest en-passant-test-case2
  (testing "Engine knows en passant moves 2."
    (let [board
          [[ "BR"  "BN"  "BB"  "BQ"  "BK"  "BB"  "BN"  "BR" ]
           [ "BP"  "  "  "BP"  "BP"  "BP"  "BP"  "BP"  "BP" ]
           [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
           [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
           [ "  "  "BP"  "WP"  "  "  "  "  "  "  "  "  "  " ]
           [ "  "  "  "  "  "  "WP"  "  "  "WN"  "  "  "  " ]
           [ "WP"  "WP"  "  "  "  "  "WP"  "WP"  "WP"  "WP" ]
           [ "WR"  "WN"  "WB"  "WQ"  "WK"  "WB"  "  "  "WR" ]]
          last-move ["c2" "c4"]
          found-moves (set (map (partial map board-notation)
                                     (find-available-moves \B board last-move)))]
      (is (contains? found-moves ["b4" "c3"]))
      (is (empty-square? (get-in (move board "b4" "c3") (board-coords "c4")))))))


(deftest en-passant-test-case3
  (testing "Engine picks en passant moves."
    (let [board
          [[ "  "  "  "  "  "  "  "  "BK"  "  "  "  "  "  " ]
           [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
           [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
           [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
           [ "WP"  "BP"  "  "  "  "  "  "  "  "  "  "  "  " ]
           [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
           [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
           [ "  "  "  "  "  "  "  "  "WK"  "  "  "  "  "  " ]]
          last-move ["a2" "a4"]]

      (is (= (choose-best-move \B board last-move)
             ["b4" "a3"]))
      (is (empty-square? (get-in (move board "b4" "a3") (board-coords "a4")))))))


(deftest en-passant-test-case4
  (testing "Engine checks the last move for en passant."
    (let [board
          [[ "  "  "  "  "  "  "  "  "BK"  "  "  "  "  "  " ]
           [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
           [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
           [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
           [ "WP"  "BP"  "  "  "  "  "  "  "  "  "  "  "  " ]
           [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
           [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
           [ "  "  "  "  "  "  "  "  "WK"  "  "  "  "  "  " ]]
          last-move ["a3" "a4"]
          found-pawn-moves (set (map (partial map board-notation)
                                     (find-available-moves \B board last-move)))]
      (is (not (contains? found-pawn-moves ["b4" "a3"])))
      ;; obvious best move
      (is (= (choose-best-move \B board last-move)
             ["b4" "b3"])))))
