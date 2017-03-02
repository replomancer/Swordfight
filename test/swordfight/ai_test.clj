(ns swordfight.ai-test
  (:require [midje.sweet :refer [facts fact has-prefix]]
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
            [swordfight.ai :refer [midgame-move]]))

(facts "about en passant moves"
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
        game-state {:board board :turn black :last-move ["a2" "a4" P] :moves-cnt 0}]
    (fact "Engine makes an en passant move in an obvious scenario"
      (midgame-move game-state) => (has-prefix [en-passant-move])))

  (let [board
        [[ _ _ _ _ k _ _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ P p _ _ _ _ _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ _ _ _ _ _ _ _ _ ]
         [ _ _ _ _ K _ _ _ ]]
        forward-move ["b4" "b3"]
        game-state {:board board :turn black :last-move ["a3" "a4" P] :moves-cnt 0}]
    (fact "Engine makes an obvious best move when en passant not possible"
      (midgame-move game-state) => (has-prefix [forward-move]))))

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
        promotion-move ["b2" "b1q"]
        game-state {:board board :turn black :last-move ["a5" "a6" P] :moves-cnt 10}]
    (fact "Engine promotes pawns to queens"
      (midgame-move game-state) => (has-prefix [promotion-move])))

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
        promotion-move ["c2" "c1n"]
        game-state {:board board :turn black :last-move ["b3" "a2" K] :moves-cnt 10}]
    (fact "Engine promotes pawns to knights"
      (midgame-move game-state) => (has-prefix [promotion-move]))))

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
    (fact "Engine chooses to attack with pawn"
      (midgame-move game-state) => (has-prefix [regular-pawn-attack]))))
