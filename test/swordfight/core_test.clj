(ns swordfight.core-test
  (:require [midje.sweet :refer [facts fact contains just has-prefix
                                 truthy falsey]]
            [swordfight.core :refer [initial-settings]]
            [swordfight.game-rules :refer [initial-game-state move
                                           pseudolegal-moves
                                           legal-moves
                                           king-in-check?
                                           king-in-check-after-move?]]
            [swordfight.ai :refer [midgame-move]]))


(defn pseudolegal-moves-cnt-in-turn
  ([turn-nr] (pseudolegal-moves-cnt-in-turn turn-nr initial-game-state))
  ([turn-nr game-state]
   (if (= turn-nr 1)
     (count (pseudolegal-moves game-state))
     (apply +
            (for [piece-move (pseudolegal-moves game-state)]
              (pseudolegal-moves-cnt-in-turn (dec turn-nr)
                                             (move game-state piece-move)))))))


(facts "about numbers of moves"
  ;; "How many moves are generated. Compare against known values."
  (fact (pseudolegal-moves-cnt-in-turn 1) => 20)
  (fact (pseudolegal-moves-cnt-in-turn 2) => 400)
  (fact (pseudolegal-moves-cnt-in-turn 3) => 8902)
  (fact (pseudolegal-moves-cnt-in-turn 4) => 197742)
  ;; This test passes but it's too memory consuming for current Travis setup
  ;;(fact (legal-moves-cnt-in-turn 5) => 4897256)

  ;; TODO:
  ;; - Find the right value for turn 6
  ;; - Improve the test speed
  ;;(fact (legal-moves-cnt-in-turn 6) => 120000000)
)


(facts "about en passant moves"
  (let [board
        [[ \r \n \b \q \k \b \n \r ]
         [ \p \. \p \p \p \p \p \p ]
         [ \. \. \. \. \. \. \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \P \p \. \. \. \. \. \. ]
         [ \. \. \. \P \. \N \. \. ]
         [ \. \P \P \. \P \P \P \P ]
         [ \R \N \B \Q \K \B \. \R ]]
        board'
        [[ \r \n \b \q \k \b \n \r ]
         [ \p \. \p \p \p \p \p \p ]
         [ \. \. \. \. \. \. \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \p \. \. \P \. \N \. \. ]
         [ \. \P \P \. \P \P \P \P ]
         [ \R \N \B \Q \K \B \. \R ]]
        en-passant-move ["b4" "a3"]
        game-state {:board board :last-move ["a2" "a4" \P] :turn \B :moves-cnt 0}]
    (fact "Engine knows en passant moves to the left"
      (legal-moves game-state) => (contains [en-passant-move]))
    (fact "Engine understands results of en passant moves to the left"
      (:board (move game-state en-passant-move))  =>  board'))


  (let [board
        [[ \r \n \b \q \k \b \n \r ]
         [ \p \. \p \p \p \p \p \p ]
         [ \. \. \. \. \. \. \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \. \p \P \. \. \. \. \. ]
         [ \. \. \. \P \. \N \. \. ]
         [ \P \P \. \. \P \P \P \P ]
         [ \R \N \B \Q \K \B \. \R ]]
        board'
        [[ \r \n \b \q \k \b \n \r ]
         [ \p \. \p \p \p \p \p \p ]
         [ \. \. \. \. \. \. \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \. \. \p \P \. \N \. \. ]
         [ \P \P \. \. \P \P \P \P ]
         [ \R \N \B \Q \K \B \. \R ]]
        en-passant-move ["b4" "c3"]
        game-state {:board board :last-move ["c2" "c4" \P] :turn \B :moves-cnt 0}]
    (fact "Engine knows en passant moves to the right"
      (legal-moves game-state) => (contains [en-passant-move]))
    (fact "Engine understands results of en passant moves to the right"
      (:board (move game-state en-passant-move)) => board'))


  (let [board
        [[ \. \. \. \. \k \. \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \P \p \. \. \. \. \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \. \. \. \. \K \. \. \. ]]
        en-passant-move ["b4" "a3"]
        game-state {:board board :turn \B :last-move ["a2" "a4" \P] :moves-cnt 0}]
    (fact "Engine makes en passant moves in an obvious scenario"
      (midgame-move game-state) => (has-prefix [en-passant-move])))


  (let [board
        [[ \. \. \. \. \k \. \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \P \p \. \. \. \. \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \. \. \. \. \K \. \. \. ]]
        en-passant-move ["b4" "a3"]
        forward-move ["b4" "b3"]
        game-state {:board board :turn \B :last-move ["a3" "a4" \P] :moves-cnt 0}]
    (fact "Engine checks the last move for en passant"
      (legal-moves game-state) =not=> (contains [en-passant-move]))
    (fact "Engine makes an obvious best move when en passant not possible"
      (midgame-move game-state) => (has-prefix [forward-move]))))


(facts "about castling"
  (let [board
        [[ \r \. \b \q \k \b \n \r ]
         [ \. \. \. \p \. \p \p \p ]
         [ \n \p \p \. \p \. \. \. ]
         [ \p \. \. \. \. \. \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \. \P \N \P \B \. \. \. ]
         [ \P \. \P \Q \P \P \P \P ]
         [ \R \. \. \. \K \B \N \R ]]
        board'
        [[ \r \. \b \q \k \b \n \r ]
         [ \. \. \. \p \. \p \p \p ]
         [ \n \p \p \. \p \. \. \. ]
         [ \p \. \. \. \. \. \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \. \P \N \P \B \. \. \. ]
         [ \P \. \P \Q \P \P \P \P ]
         [ \. \. \K \R \. \B \N \R ]]
        game-state {:board board :turn \W :last-move ["b7" "b6" \p] :moves-cnt 0
                    :white-can-castle-qs true}
        white-castling-queenside ["e1" "c1"]]
    (fact "Engine considers white castling queenside"
      (legal-moves game-state) =>
      (contains [white-castling-queenside]))
    (fact "Engine understands the result of white castling queenside"
      (:board (move game-state white-castling-queenside)) => board'))


  (let [board
        [[ \r \N \b \q \k \b \n \r ]
         [ \. \. \. \p \. \p \p \p ]
         [ \p \. \p \. \p \. \. \. ]
         [ \. \p \. \. \. \. \. \. ]
         [ \. \. \. \. \. \. \P \. ]
         [ \. \. \. \. \. \P \. \N ]
         [ \P \P \P \P \P \. \B \P ]
         [ \R \N \B \Q \K \. \. \R ]]
        board'
        [[ \r \N \b \q \k \b \n \r ]
         [ \. \. \. \p \. \p \p \p ]
         [ \p \. \p \. \p \. \. \. ]
         [ \. \p \. \. \. \. \. \. ]
         [ \. \. \. \. \. \. \P \. ]
         [ \. \. \. \. \. \P \. \N ]
         [ \P \P \P \P \P \. \B \P ]
         [ \R \N \B \Q \. \R \K \. ]]
        white-castling-kingside ["e1" "g1"]
        game-state {:board board :turn \W :last-move ["a7" "a6" \p] :moves-cnt 0
                    :white-can-castle-ks true}]
    (fact "Engine considers white castling kingside"
      (legal-moves game-state) =>
      (contains [white-castling-kingside]))
    (fact "Engine understands the result of white castling kingside"
      (:board (move game-state white-castling-kingside)) => board'))

  (let [board
        [[ \r \. \. \. \k \b \n \r ]
         [ \p \p \q \b \p \p \p \p ]
         [ \N \. \p \p \. \. \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \. \. \. \. \. \. \P \. ]
         [ \. \. \P \. \. \P \. \. ]
         [ \P \P \Q \P \P \. \. \P ]
         [ \R \N \B \. \K \B \N \R ]]
        board'
        [[ \. \. \k \r \. \b \n \r ]
         [ \p \p \q \b \p \p \p \p ]
         [ \N \. \p \p \. \. \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \. \. \. \. \. \. \P \. ]
         [ \. \. \P \. \. \P \. \. ]
         [ \P \P \Q \P \P \. \. \P ]
         [ \R \N \B \. \K \B \N \R ]]
        black-castling-queenside ["e8" "c8"]
        game-state {:board board :turn \B :last-move ["b3" "c2" \Q] :moves-cnt 10
                    :black-can-castle-qs true}]
    (fact "Engine considers black castling queenside"
      (legal-moves game-state) =>
      (contains [black-castling-queenside]))
    (fact "Engine understands the result of black castling queenside"
      (:board (move game-state black-castling-queenside)) => board'))

  (let [board
        [[ \r \N \b \q \k \. \. \r ]
         [ \p \p \. \p \b \p \p \p ]
         [ \. \. \p \. \p \n \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \. \. \. \. \. \. \P \. ]
         [ \. \. \P \. \. \P \. \. ]
         [ \P \P \Q \P \P \. \. \P ]
         [ \R \N \B \. \K \B \N \R ]]
        board'
        [[ \r \N \b \q \. \r \k \. ]
         [ \p \p \. \p \b \p \p \p ]
         [ \. \. \p \. \p \n \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \. \. \. \. \. \. \P \. ]
         [ \. \. \P \. \. \P \. \. ]
         [ \P \P \Q \P \P \. \. \P ]
         [ \R \N \B \. \K \B \N \R ]]
        black-castling-kingside ["e8" "g8"]
        game-state {:board board :turn \B :last-move ["b3" "c2" \Q] :moves-cnt 10
                    :black-can-castle-ks true}]
    (fact "Engine considers black castling kingside"
      (legal-moves game-state) => (contains [black-castling-kingside]))
    (fact "Engine understands the result of black castling kingside"
      (:board (move game-state black-castling-kingside)) => board')))

(facts "about pawn promotion"
  (let [board
        [[ \. \. \. \. \k \. \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \P \. \. \. \. \. \. \. ]
         [ \. \. \. \. \p \. \. \. ]
         [ \. \. \. \. \. \P \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \. \p \. \. \. \. \. \. ]
         [ \. \. \. \. \K \. \. \. ]]
        board'
        [[ \. \. \. \. \k \. \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \P \. \. \. \. \. \. \. ]
         [ \. \. \. \. \p \. \. \. ]
         [ \. \. \. \. \. \P \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \. \q \. \. \K \. \. \. ]]
        promotion-move ["b2" "b1Q"]
        game-state {:board board :turn \B :last-move ["a5" "a6" \P] :moves-cnt 10}]
    (fact "Engine promotes pawns to queens"
      (midgame-move game-state) => (has-prefix [promotion-move]))
    (fact "Engine understands the result of pawn promotion to queen"
      (:board (move game-state promotion-move)) => board'))

  (let [board
        [[ \. \. \. \. \k \. \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \P \. \. \. \. \. \. \r ]
         [ \K \P \p \. \. \. \. \. ]
         [ \R \N \. \. \. \. \. \. ]]
        board'
        [[ \. \. \. \. \k \. \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \P \. \. \. \. \. \. \r ]
         [ \K \P \. \. \. \. \. \. ]
         [ \R \N \n \. \. \. \. \. ]]
        promotion-move ["c2" "c1N"]
        game-state {:board board :turn \B :last-move ["b3" "a2" \K] :moves-cnt 10}]
    (fact "Engine promotes pawns to knights"
      (midgame-move game-state) => (has-prefix [promotion-move]))
    (fact "Engine understands the result of pawn promotion to knight"
      (:board (move game-state promotion-move)) => board')))


(facts "about basic moves"
  (let [board
        [[ \. \. \. \. \k \. \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \P \. \. \. \. \. \. \p ]
         [ \K \P \. \. \. \. \P \P ]
         [ \R \N \. \. \. \. \. \. ]]
        board'
        [[ \. \. \. \. \k \. \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \P \. \. \. \. \. \. \P ]
         [ \K \P \. \. \. \. \. \P ]
         [ \R \N \. \. \. \. \. \. ]]
        regular-pawn-attack ["g2" "h3"]
        game-state {:board board :turn \W :last-move ["h4" "h3" \p] :moves-cnt 10}]
    (fact "Engine chooses to attack with pawn"
      (midgame-move game-state) => (has-prefix [regular-pawn-attack]))
    (fact "Engine understands the result of pawn attack"
      (:board (move game-state regular-pawn-attack)) => board')))

(facts "about checking"
  (let [board
        [[ \. \. \. \. \k \. \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \P \. \. \. \. \. \. \r ]
         [ \K \P \. \. \. \. \. \. ]
         [ \R \N \n \. \. \. \. \. ]]
        game-state {:board board :turn \W :last-move ["c2" "c1N" \n] :moves-cnt 10}]

    (fact "King in check detected"
      (king-in-check? game-state \W) => truthy)
    (fact "King moving onto attacked square detection"
      (king-in-check-after-move? game-state ["a2" "b3"]) => truthy)
    (fact "King in check - sanity test"
      (king-in-check? game-state \B) => falsey)
    (fact "When checkmated no legal moves are found"
      (legal-moves game-state) => empty?))

  (let [board
        [[ \. \. \. \. \k \. \. \. ]
         [ \P \R \. \. \. \. \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \. \. \. \K \. \. \. \r ]
         [ \. \. \. \. \. \. \. \. ]
         [ \. \. \. \. \. \. \. \. ]
         [ \. \N \n \. \. \. \. \. ]]
        game-state {:board board :turn \W :last-move ["h8" "h4" \n] :moves-cnt 10}
        moves (legal-moves game-state)]
    (fact "A move cannot leave the king in check"
      moves => (just #{["d4" "c5"] ["d4" "d5"] ["d4" "e5"] ["d4" "c3"] ["d4" "e3"]}))))
