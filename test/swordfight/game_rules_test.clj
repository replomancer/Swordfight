(ns swordfight.game-rules-test
  (:require [midje.sweet :refer [facts fact contains just truthy falsey]]
            [swordfight.board :refer [black-king black-queen black-bishop
                                      black-knight black-rook black-pawn
                                      white-king white-queen white-bishop
                                      white-knight white-rook white-pawn
                                      empty-square white black]
             :rename {black-king k black-queen q black-bishop b
                      black-knight n black-rook r black-pawn p
                      white-king K white-queen Q white-bishop B
                      white-knight N white-rook R white-pawn P
                      empty-square _}]
            [swordfight.game-rules :refer [initial-game-state move
                                           pseudolegal-moves
                                           legal-moves
                                           king-in-check?
                                           king-in-check-after-move?]]))

(defn pseudolegal-moves-cnt-in-turn
  ([turn-nr] (pseudolegal-moves-cnt-in-turn turn-nr initial-game-state))
  ([turn-nr game-state]
   (if (= turn-nr 1)
     (count (pseudolegal-moves game-state))
     (reduce +
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
  ;; (fact (pseudolegal-moves-cnt-in-turn 5) => 4897256)

  ;; TODO:
  ;; - Find the right value for turn 6
  ;; - Improve the test speed
  ;;(fact (pseudolegal-moves-cnt-in-turn 6) => 120000000)
)

(facts "about en passant moves"
  (let [board
        [[ r n b q k b n r ]
         [ p _ p p p p p p ]
         [ _ _ _ _ _ _ _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ P p _ _ _ _ _ _ ]
         [ _ _ _ P _ N _ _ ]
         [ _ P P _ P P P P ]
         [ R N B Q K B _ R ]]
        board'
        [[ r n b q k b n r ]
         [ p _ p p p p p p ]
         [ _ _ _ _ _ _ _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ p _ _ P _ N _ _ ]
         [ _ P P _ P P P P ]
         [ R N B Q K B _ R ]]
        en-passant-move ["b4" "a3"]
        game-state {:board board :last-move ["a2" "a4" P] :turn black :moves-cnt 0}]
    (fact "Engine knows en passant moves to the left"
      (legal-moves game-state) => (contains [en-passant-move]))
    (fact "Engine understands results of en passant moves to the left"
      (:board (move game-state en-passant-move))  =>  board'))

  (let [board
        [[ r n b q k b n r ]
         [ p _ p p p p p p ]
         [ _ _ _ _ _ _ _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ _ p P _ _ _ _ _ ]
         [ _ _ _ P _ N _ _ ]
         [ P P _ _ P P P P ]
         [ R N B Q K B _ R ]]
        board'
        [[ r n b q k b n r ]
         [ p _ p p p p p p ]
         [ _ _ _ _ _ _ _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ _ _ p P _ N _ _ ]
         [ P P _ _ P P P P ]
         [ R N B Q K B _ R ]]
        en-passant-move ["b4" "c3"]
        game-state {:board board :last-move ["c2" "c4" P] :turn black :moves-cnt 0}]
    (fact "Engine knows en passant moves to the right"
      (legal-moves game-state) => (contains [en-passant-move]))
    (fact "Engine understands results of en passant moves to the right"
      (:board (move game-state en-passant-move)) => board'))

  (let [board
        [[ _ _ _ _ k _ _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ P p _ _ _ _ _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ _ _ _ _ K _ _ _ ]]
        en-passant-move ["b4" "a3"]
        forward-move ["b4" "b3"]
        game-state {:board board :turn black :last-move ["a3" "a4" P] :moves-cnt 0}]
    (fact "Engine checks the last move for en passant"
      (legal-moves game-state) =not=> (contains [en-passant-move]))))

(facts "about castling"
  (let [board
        [[ r _ b q k b n r ]
         [ _ _ _ p _ p p p ]
         [ n p p _ p _ _ _ ]
         [ p _ _ _ _ _ _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ _ P N P B _ _ _ ]
         [ P _ P Q P P P P ]
         [ R _ _ _ K B N R ]]
        board'
        [[ r _ b q k b n r ]
         [ _ _ _ p _ p p p ]
         [ n p p _ p _ _ _ ]
         [ p _ _ _ _ _ _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ _ P N P B _ _ _ ]
         [ P _ P Q P P P P ]
         [ _ _ K R _ B N R ]]
        game-state {:board board :turn white :last-move ["b7" "b6" p] :moves-cnt 0
                    :white-can-castle-qs true}
        white-castling-queenside ["e1" "c1"]]
    (fact "Engine considers white castling queenside"
      (legal-moves game-state) => (contains [white-castling-queenside]))
    (fact "Engine understands the result of white castling queenside"
      (:board (move game-state white-castling-queenside)) => board'))

  (let [board
        [[ r N b q k b n r ]
         [ _ _ _ p _ p p p ]
         [ p _ p _ p _ _ _ ]
         [ _ p _ _ _ _ _ _ ]
         [ _ _ _ _ _ _ P _ ]
         [ _ _ _ _ _ P _ N ]
         [ P P P P P _ B P ]
         [ R N B Q K _ _ R ]]
        board'
        [[ r N b q k b n r ]
         [ _ _ _ p _ p p p ]
         [ p _ p _ p _ _ _ ]
         [ _ p _ _ _ _ _ _ ]
         [ _ _ _ _ _ _ P _ ]
         [ _ _ _ _ _ P _ N ]
         [ P P P P P _ B P ]
         [ R N B Q _ R K _ ]]
        white-castling-kingside ["e1" "g1"]
        game-state {:board board :turn white :last-move ["a7" "a6" p] :moves-cnt 0
                    :white-can-castle-ks true}]
    (fact "Engine considers white castling kingside"
      (legal-moves game-state) => (contains [white-castling-kingside]))
    (fact "Engine understands the result of white castling kingside"
      (:board (move game-state white-castling-kingside)) => board'))

  (let [board
        [[ r _ _ _ k b n r ]
         [ p p q b p p p p ]
         [ N _ p p _ _ _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ _ _ _ _ _ _ P _ ]
         [ _ _ P _ _ P _ _ ]
         [ P P Q P P _ _ P ]
         [ R N B _ K B N R ]]
        board'
        [[ _ _ k r _ b n r ]
         [ p p q b p p p p ]
         [ N _ p p _ _ _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ _ _ _ _ _ _ P _ ]
         [ _ _ P _ _ P _ _ ]
         [ P P Q P P _ _ P ]
         [ R N B _ K B N R ]]
        black-castling-queenside ["e8" "c8"]
        game-state {:board board :turn black :last-move ["b3" "c2" Q] :moves-cnt 10
                    :black-can-castle-qs true}]
    (fact "Engine considers black castling queenside"
      (legal-moves game-state) => (contains [black-castling-queenside]))
    (fact "Engine understands the result of black castling queenside"
      (:board (move game-state black-castling-queenside)) => board'))

  (let [board
        [[ r N b q k _ _ r ]
         [ p p _ p b p p p ]
         [ _ _ p _ p n _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ _ _ _ _ _ _ P _ ]
         [ _ _ P _ _ P _ _ ]
         [ P P Q P P _ _ P ]
         [ R N B _ K B N R ]]
        board'
        [[ r N b q _ r k _ ]
         [ p p _ p b p p p ]
         [ _ _ p _ p n _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ _ _ _ _ _ _ P _ ]
         [ _ _ P _ _ P _ _ ]
         [ P P Q P P _ _ P ]
         [ R N B _ K B N R ]]
        black-castling-kingside ["e8" "g8"]
        game-state {:board board :turn black :last-move ["b3" "c2" Q] :moves-cnt 10
                    :black-can-castle-ks true}]
    (fact "Engine considers black castling kingside"
      (legal-moves game-state) => (contains [black-castling-kingside]))
    (fact "Engine understands the result of black castling kingside"
      (:board (move game-state black-castling-kingside)) => board')))

(facts "about pawn promotion"
  (let [board
        [[ _ _ _ _ k _ _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ P _ _ _ _ _ _ _ ]
         [ _ _ _ _ p _ _ _ ]
         [ _ _ _ _ _ P _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ _ p _ _ _ _ _ _ ]
         [ _ _ _ _ K _ _ _ ]]
        board'
        [[ _ _ _ _ k _ _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ P _ _ _ _ _ _ _ ]
         [ _ _ _ _ p _ _ _ ]
         [ _ _ _ _ _ P _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ _ q _ _ K _ _ _ ]]
        promotion-move ["b2" "b1Q"]
        game-state {:board board :turn black :last-move ["a5" "a6" P] :moves-cnt 10}]
    (fact "Engine understands the result of pawn promotion to queen"
      (:board (move game-state promotion-move)) => board'))

  (let [board
        [[ _ _ _ _ k _ _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ P _ _ _ _ _ _ r ]
         [ K P p _ _ _ _ _ ]
         [ R N _ _ _ _ _ _ ]]
        board'
        [[ _ _ _ _ k _ _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ P _ _ _ _ _ _ r ]
         [ K P _ _ _ _ _ _ ]
         [ R N n _ _ _ _ _ ]]
        promotion-move ["c2" "c1N"]
        game-state {:board board :turn black :last-move ["b3" "a2" K] :moves-cnt 10}]
    (fact "Engine understands the result of pawn promotion to knight"
      (:board (move game-state promotion-move)) => board')))

(facts "about basic moves"
  (let [board
        [[ _ _ _ _ k _ _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ P _ _ _ _ _ _ p ]
         [ K P _ _ _ _ P P ]
         [ R N _ _ _ _ _ _ ]]
        board'
        [[ _ _ _ _ k _ _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ P _ _ _ _ _ _ P ]
         [ K P _ _ _ _ _ P ]
         [ R N _ _ _ _ _ _ ]]
        regular-pawn-attack ["g2" "h3"]
        game-state {:board board :turn white :last-move ["h4" "h3" p] :moves-cnt 10}]
    (fact "Engine understands the result of pawn attack"
      (:board (move game-state regular-pawn-attack)) => board')))

(facts "about checking"
  (let [board
        [[ _ _ _ _ k _ _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ P _ _ _ _ _ _ r ]
         [ K P _ _ _ _ _ _ ]
         [ R N n _ _ _ _ _ ]]
        game-state {:board board :turn white :last-move ["c2" "c1N" n] :moves-cnt 10}]

    (fact "King in check detected"
      (king-in-check? game-state white) => truthy)
    (fact "King moving onto attacked square detection"
      (king-in-check-after-move? game-state ["a2" "b3"]) => truthy)
    (fact "King in check - sanity test"
      (king-in-check? game-state black) => falsey)
    (fact "When checkmated no legal moves are found"
      (legal-moves game-state) => empty?))

  (let [board
        [[ _ _ _ _ k _ _ _ ]
         [ P R _ _ _ _ _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ _ _ _ K _ _ _ r ]
         [ _ _ _ _ _ _ _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ _ N n _ _ _ _ _ ]]
        game-state {:board board :turn white :last-move ["h8" "h4" n] :moves-cnt 10}
        moves (legal-moves game-state)]
    (fact "A move cannot leave the king in check"
      moves => (just #{["d4" "c5"] ["d4" "d5"] ["d4" "e5"] ["d4" "c3"] ["d4" "e3"]}))))
