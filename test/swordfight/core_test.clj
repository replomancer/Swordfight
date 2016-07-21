(ns swordfight.core-test
  (:use [midje.sweet :only [facts fact contains]]
        [swordfight.core :only [initial-settings]]
        [swordfight.game-rules :only [initial-game-state change-side move
                                      board-coords board-notation empty-square?
                                      find-available-moves]]
        [swordfight.ai :only [choose-best-move]]))


(defn legal-moves-cnt-in-turn
  ([turn-nr] (legal-moves-cnt-in-turn turn-nr initial-game-state))
  ([turn-nr game-state]
   (if (= turn-nr 1)
     (count (find-available-moves game-state))
     (apply +
            (for [piece-move (find-available-moves game-state)]
              (legal-moves-cnt-in-turn (dec turn-nr)
                                       (move game-state piece-move)))))))


(facts "about numbers of moves"
  ;; "How many moves are generated. Compare against known values."
  (fact (legal-moves-cnt-in-turn 1) => 20)
  (fact (legal-moves-cnt-in-turn 2) => 400)
  (fact (legal-moves-cnt-in-turn 3) => 8902)
  (fact (legal-moves-cnt-in-turn 4) => 197742)
  ;; This test passes but it's too memory consuming for current Travis setup
  ;;(fact (legal-moves-cnt-in-turn 5) => 4897256)

  ;; TODO:
  ;; - Find the right value for turn 6
  ;; - Improve the test speed
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
        en-passant-move ["b4" "a3"]
        game-state {:board board :last-move ["a2" "a4"] :turn \B :moves-cnt 0}]
    (fact "Engine knows en passant moves to the left"
      (find-available-moves game-state) => (contains [en-passant-move]))
    (fact "Engine understands results of en passant moves to the left"
      (:board (move game-state en-passant-move))  =>  board'))


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
        en-passant-move ["b4" "c3"]
        game-state {:board board :last-move ["c2" "c4"] :turn \B :moves-cnt 0}]
    (fact "Engine knows en passant moves to the right"
      (find-available-moves game-state) => (contains [en-passant-move]))
    (fact "Engine understands results of en passant moves to the right"
      (:board (move game-state en-passant-move)) => board'))


  (let [board
        [[ "  "  "  "  "  "  "  "  "BK"  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "WP"  "BP"  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "WK"  "  "  "  "  "  " ]]
        en-passant-move ["b4" "a3"]
        game-state {:board board :turn \B :last-move ["a2" "a4"] :moves-cnt 0}]
    (fact "Engine makes en passant moves in an obvious scenario"
      (choose-best-move game-state) => en-passant-move))


  (let [board
        [[ "  "  "  "  "  "  "  "  "BK"  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "WP"  "BP"  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "WK"  "  "  "  "  "  " ]]
        en-passant-move ["b4" "a3"]
        game-state {:board board :turn \B :last-move ["a3" "a4"] :moves-cnt 0}]
    (fact "Engine checks the last move for en passant"
      (find-available-moves game-state) =not=> (contains [en-passant-move]))
    (fact "Engine makes an obvious best move when en passant not possible"
      (choose-best-move game-state) => ["b4" "b3"])))


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
        game-state {:board board :turn \W :last-move ["b2" "b3"] :moves-cnt 0
                    :white-can-castle-qs true}
        white-castling-queenside ["e1" "c1"]]
    (fact "Engine considers white castling queenside"
      (find-available-moves game-state) =>
      (contains [white-castling-queenside]))
    (fact "Engine understands the result of white castling queenside"
      (:board (move game-state white-castling-queenside)) => board'))


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
        white-castling-kingside ["e1" "g1"]
        game-state {:board board :turn \W :last-move ["a7" "a6"] :moves-cnt 0
                    :white-can-castle-ks true}]
    (fact "Engine considers white castling kingside"
      (find-available-moves game-state) =>
      (contains [white-castling-kingside]))
    (fact "Engine understands the result of white castling kingside"
      (:board (move game-state white-castling-kingside)) => board'))

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
         [ "WR"  "WN"  "WB"  "  "  "WK"  "WB"  "WN"  "WR" ]]
        black-castling-queenside ["e8" "c8"]
        game-state {:board board :turn \B :last-move ["b3" "c2"] :moves-cnt 0
                    :black-can-castle-qs true}]
    (fact "Engine considers black castling queenside"
      (find-available-moves game-state) =>
      (contains [black-castling-queenside]))
    (fact "Engine understands the result of black castling queenside"
      (:board (move game-state black-castling-queenside)) => board'))

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
         [ "WR"  "WN"  "WB"  "  "  "WK"  "WB"  "WN"  "WR" ]]
        black-castling-kingside ["e8" "g8"]
        game-state {:board board :turn \B :last-move ["b3" "c2"] :moves-cnt 0
                    :black-can-castle-ks true}]
    (fact "Engine considers black castling kingside"
      (find-available-moves game-state) => (contains [black-castling-kingside]))
    (fact "Engine understands the result of black castling kingside"
      (:board (move game-state black-castling-kingside)) => board')))

(facts "about pawn promotion"
  (let [board
        [[ "  "  "  "  "  "  "  "  "BK"  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "WP"  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "BP"  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "  "  "WP"  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "  "  "BP"  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "WK"  "  "  "  "  "  " ]]
        board'
        [[ "  "  "  "  "  "  "  "  "BK"  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "WP"  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "BP"  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "  "  "WP"  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "  "  "BQ"  "  "  "  "  "WK"  "  "  "  "  "  " ]]
        promotion-move ["b2" "b1Q"]
        game-state {:board board :turn \B :last-move ["a5" "a6"] :moves-cnt 0}]
    (fact "Engine promotes pawns to queens"
      (choose-best-move game-state) => promotion-move)
    (fact "Engine understands the result of pawn promotion to queen"
      (:board (move game-state promotion-move)) => board'))

  (let [board
        [[ "  "  "  "  "  "  "  "  "BK"  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "WP"  "  "  "  "  "  "  "  "  "  "  "  "  "BR" ]
         [ "WK"  "WP"  "BP"  "  "  "  "  "  "  "  "  "  " ]
         [ "WR"  "WN"  "  "  "  "  "  "  "  "  "  "  "  " ]]
        board'
        [[ "  "  "  "  "  "  "  "  "BK"  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "  "  "  "  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "WP"  "  "  "  "  "  "  "  "  "  "  "  "  "BR" ]
         [ "WK"  "WP"  "  "  "  "  "  "  "  "  "  "  "  " ]
         [ "WR"  "WN"  "BN"  "  "  "  "  "  "  "  "  "  " ]]
        promotion-move ["c2" "c1N"]
        game-state {:board board :turn \B :last-move ["a5" "a6"] :moves-cnt 0}]
    (fact "Engine promotes pawns to knights"
      (choose-best-move game-state) => promotion-move)
    (fact "Engine understands the result of pawn promotion to knight"
      (:board (move game-state promotion-move)) => board')))
