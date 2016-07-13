(ns swordfight.core-test
  (:use [midje.sweet :only [facts fact]]
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


(facts "about numbers of moves"
  ;; "How many moves are generated. Compare against known values."
  (fact (legal-moves-cnt-in-turn 1) => 20)
  (fact (legal-moves-cnt-in-turn 2) => 400)
  (fact (legal-moves-cnt-in-turn 3) => 8902)
  (fact (legal-moves-cnt-in-turn 4) => 197742)
  ;; TODO:
  ;; - The test fails for turn 5
  ;; - Find the right value for turn 6
  ;; - Improve the test speed
  ;;(fact (legal-moves-cnt-in-turn 5) => 4897256)
  ;;(fact (legal-moves-cnt-in-turn 6) => 120000000)
)


(facts "about en passant moves"
  (let [board
        [[ "BR"  "BN"  "BB"  "BQ"  "BK"  "BB"  "BN"  "BR" ]
         [ "BP"  "  "  "BP"  "BP"  "BP"  "BP"  "BP"  "BP" ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "WP"  "BP"  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "WP"  "  "  "WN"  "  "  "  " ]
         [ "  "  "WP"  "WP"  "  "  "WP"  "WP"  "WP"  "WP" ]
         [ "WR"  "WN"  "WB"  "WQ"  "WK"  "WB"  "  "  "WR" ]]
        board'
        [[ "BR"  "BN"  "BB"  "BQ"  "BK"  "BB"  "BN"  "BR" ]
         [ "BP"  "  "  "BP"  "BP"  "BP"  "BP"  "BP"  "BP" ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "BP"  "  "  "  "  "WP"  "  "  "WN"  "  "  "  " ]
         [ "  "  "WP"  "WP"  "  "  "WP"  "WP"  "WP"  "WP" ]
         [ "WR"  "WN"  "WB"  "WQ"  "WK"  "WB"  "  "  "WR" ]]
        last-move ["a2" "a4"]
        found-moves (set (map (partial map board-notation)
                              (find-available-moves \B board last-move)))]
    (fact "Engine knows en passant moves to the left"
      (contains? found-moves ["b4" "a3"]) => true)
    (fact "Engine understands results of en passant moves to the left"
      (move board "b4" "a3") => board'))


  (let [board
        [[ "BR"  "BN"  "BB"  "BQ"  "BK"  "BB"  "BN"  "BR" ]
         [ "BP"  "  "  "BP"  "BP"  "BP"  "BP"  "BP"  "BP" ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "  "  "BP"  "WP"  "  "  "  "  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "WP"  "  "  "WN"  "  "  "  " ]
         [ "WP"  "WP"  "  "  "  "  "WP"  "WP"  "WP"  "WP" ]
         [ "WR"  "WN"  "WB"  "WQ"  "WK"  "WB"  "  "  "WR" ]]
        board'
        [[ "BR"  "BN"  "BB"  "BQ"  "BK"  "BB"  "BN"  "BR" ]
         [ "BP"  "  "  "BP"  "BP"  "BP"  "BP"  "BP"  "BP" ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "  "  "  "  "BP"  "WP"  "  "  "WN"  "  "  "  " ]
         [ "WP"  "WP"  "  "  "  "  "WP"  "WP"  "WP"  "WP" ]
         [ "WR"  "WN"  "WB"  "WQ"  "WK"  "WB"  "  "  "WR" ]]
        last-move ["c2" "c4"]
        found-moves (set (map (partial map board-notation)
                              (find-available-moves \B board last-move)))]
    (fact "Engine knows en passant moves to the right"
      (contains? found-moves ["b4" "c3"]) => true)
    (fact "Engine understands results of en passant moves to the right"
      (move board "b4" "c3") => board'))


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
    (fact "Engine makes en passant moves in an obvious scenario"
      (choose-best-move \B board last-move) => ["b4" "a3"]))


  (let [board
        [[ "  "  "  "  "  "  "  "  "BK"  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "WP"  "BP"  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "WK"  "  "  "  "  "  " ]]
        last-move ["a3" "a4"]]
    (fact "Engine checks the last move for en passant"
      (let [found-moves (set (map (partial map board-notation)
                                  (find-available-moves \B board last-move)))]
        (contains? found-moves ["b4" "a3"])) => false)
    (fact "Engine makes an obvious best move when en passant not possible"
      (choose-best-move \B board last-move) => ["b4" "b3"])))
