(ns swordfight.core-test
  (:use [midje.sweet :only [facts fact contains]]
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


(defn found-moves-notation [side board last-move]
  (set (map (partial map board-notation)
            (find-available-moves side board last-move))))


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
        last-move ["a2" "a4"]]
    (fact "Engine knows en passant moves to the left"
      (contains? (found-moves-notation \B board last-move) ["b4" "a3"]) => true)
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
        last-move ["c2" "c4"]]
    (fact "Engine knows en passant moves to the right"
      (contains? (found-moves-notation \B board last-move) ["b4" "c3"]) => true)
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
      (contains? (found-moves-notation \B board last-move) ["b4" "a3"]) => false)
    (fact "Engine makes an obvious best move when en passant not possible"
      (choose-best-move \B board last-move) => ["b4" "b3"])))


(facts "about castling"
  (let [board
        [[ "BR"  "  "  "BB"  "BQ"  "BK"  "BB"  "BN"  "BR" ]
         [ "  "  "  "  "  "  "BP"  "  "  "BP"  "BP"  "BP" ]
         [ "BN"  "BP"  "BP"  "  "  "BP"  "  "  "  "  "  " ]
         [ "BP"  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "  "  "WP"  "WN"  "WP"  "WB"  "  "  "  "  "  " ]
         [ "WP"  "  "  "WP"  "WQ"  "WP"  "WP"  "WP"  "WP" ]
         [ "WR"  "  "  "  "  "  "  "WK"  "WB"  "WN"  "WR" ]]
        board'
        [[ "BR"  "  "  "BB"  "BQ"  "BK"  "BB"  "BN"  "BR" ]
         [ "  "  "  "  "  "  "BP"  "  "  "BP"  "BP"  "BP" ]
         [ "BN"  "BP"  "BP"  "  "  "BP"  "  "  "  "  "  " ]
         [ "BP"  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "  "  "WP"  "WN"  "WP"  "WB"  "  "  "  "  "  " ]
         [ "WP"  "  "  "WP"  "WQ"  "WP"  "WP"  "WP"  "WP" ]
         [ "  "  "  "  "WK"  "WR"  "  "  "WB"  "WN"  "WR" ]]
        last-move ["b2" "b3"]
        white-castling-queenside ["e1" "c1"]]
    (fact "Engine considers white castling queenside"
      (found-moves-notation \W board last-move) =>
      (contains [white-castling-queenside]))
    (fact "Engine understands the result of white castling queenside"
      (move board "e1" "c1") => board'))


(let [board
      [[ "BR"  "WN"  "BB"  "BQ"  "BK"  "BB"  "BN"  "BR" ]
       [ "  "  "  "  "  "  "BP"  "  "  "BP"  "BP"  "BP" ]
       [ "BP"  "  "  "BP"  "  "  "BP"  "  "  "  "  "  " ]
       [ "  "  "BP"  "  "  "  "  "  "  "  "  "  "  "  " ]
       [ "  "  "  "  "  "  "  "  "  "  "  "  "WP"  "  " ]
       [ "  "  "  "  "  "  "  "  "  "  "WP"  "  "  "WN" ]
       [ "WP"  "WP"  "WP"  "WP"  "WP"  "  "  "WB"  "WP" ]
       [ "WR"  "WN"  "WB"  "WQ"  "WK"  "  "  "  "  "WR" ]]
      board'
      [[ "BR"  "WN"  "BB"  "BQ"  "BK"  "BB"  "BN"  "BR" ]
       [ "  "  "  "  "  "  "BP"  "  "  "BP"  "BP"  "BP" ]
       [ "BP"  "  "  "BP"  "  "  "BP"  "  "  "  "  "  " ]
       [ "  "  "BP"  "  "  "  "  "  "  "  "  "  "  "  " ]
       [ "  "  "  "  "  "  "  "  "  "  "  "  "WP"  "  " ]
       [ "  "  "  "  "  "  "  "  "  "  "WP"  "  "  "WN" ]
       [ "WP"  "WP"  "WP"  "WP"  "WP"  "  "  "WB"  "WP" ]
       [ "WR"  "WN"  "WB"  "WQ"  "  "  "WR"  "WK"  "  " ]]
      last-move ["a7" "a6"]
      white-castling-kingside ["e1" "g1"]]
    (fact "Engine considers white castling kingside"
      (found-moves-notation \W board last-move) =>
      (contains [white-castling-kingside]))
    (fact "Engine understands the result of white castling kingside"
      (move board "e1" "g1") => board'))

  (let [board
        [[ "BR"  "  "  "  "  "  "  "BK"  "BB"  "BN"  "BR" ]
         [ "BP"  "BP"  "BQ"  "BB"  "BP"  "BP"  "BP"  "BP" ]
         [ "WN"  "  "  "BP"  "BP"  "  "  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "WP"  "  " ]
         [ "  "  "  "  "WP"  "  "  "  "  "WP"  "  "  "  " ]
         [ "WP"  "WP"  "WQ"  "WP"  "WP"  "  "  "  "  "WP" ]
         [ "WR"  "WN"  "WB"  "  "  "WK"  "WB"  "WN"  "WR" ]]
        board'
        [[ "  "  "  "  "BK"  "BR"  "  "  "BB"  "BN"  "BR" ]
         [ "BP"  "BP"  "BQ"  "BB"  "BP"  "BP"  "BP"  "BP" ]
         [ "WN"  "  "  "BP"  "BP"  "  "  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "WP"  "  " ]
         [ "  "  "  "  "WP"  "  "  "  "  "WP"  "  "  "  " ]
         [ "WP"  "WP"  "WQ"  "WP"  "WP"  "  "  "  "  "WP" ]
         [ "WR"  "WN"  "WB"  "  "  "WK"  "WB"  "WN"  "WR" ]]]
    (fact "Engine understands the result of black castling queenside"
      (move board "e8" "c8") => board'))

  (let [board
        [[ "BR"  "WN"  "BB"  "BQ"  "BK"  "  "  "  "  "BR" ]
         [ "BP"  "BP"  "  "  "BP"  "BB"  "BP"  "BP"  "BP" ]
         [ "  "  "  "  "BP"  "  "  "BP"  "BN"  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "WP"  "  " ]
         [ "  "  "  "  "WP"  "  "  "  "  "WP"  "  "  "  " ]
         [ "WP"  "WP"  "WQ"  "WP"  "WP"  "  "  "  "  "WP" ]
         [ "WR"  "WN"  "WB"  "  "  "WK"  "WB"  "WN"  "WR" ]]
        board'
        [[ "BR"  "WN"  "BB"  "BQ"  "  "  "BR"  "BK"  "  " ]
         [ "BP"  "BP"  "  "  "BP"  "BB"  "BP"  "BP"  "BP" ]
         [ "  "  "  "  "BP"  "  "  "BP"  "BN"  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "WP"  "  " ]
         [ "  "  "  "  "WP"  "  "  "  "  "WP"  "  "  "  " ]
         [ "WP"  "WP"  "WQ"  "WP"  "WP"  "  "  "  "  "WP" ]
         [ "WR"  "WN"  "WB"  "  "  "WK"  "WB"  "WN"  "WR" ]]]
    (fact "Engine understands the result of black castling kingside"
      (move board "e8" "g8") => board')))
