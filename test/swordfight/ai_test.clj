(ns swordfight.ai-test
  (:require [midje.sweet :refer [facts fact has-prefix]]
            [swordfight.board :refer [white black ->board
                                      white-pawn black-pawn white-king]]
            [swordfight.ai :refer [midgame-move eval-board]]))

(facts "about en passant moves"
  (let [board (->board [" . . . . k . . . "
                        " . . . . . . . . "
                        " . . . . . . . . "
                        " . . . . . . . . "
                        " P p . . . . . . "
                        " . . . . . . . . "
                        " . . . . . . . . "
                        " . . . . K . . . "])
        en-passant-move ["b4" "a3"]
        game-state {:board board :turn black :last-move ["a2" "a4" white-pawn]
                    :moves-cnt 0 :board-value (eval-board board)}]
    (fact "Engine makes an en passant move in an obvious scenario"
      (midgame-move game-state) => (has-prefix [en-passant-move])))

  (let [board (->board [" . . . . k . . . "
                        " . . . . . . . . "
                        " . . . . . . . . "
                        " . . . . . . . . "
                        " P p . . . . . . "
                        " . . . . . . . . "
                        " . . . . . . . . "
                        " . . . . K . . . "])
        forward-move ["b4" "b3"]
        game-state {:board board :turn black :last-move ["a3" "a4" white-pawn]
                    :moves-cnt 0 :board-value (eval-board board)}]
    (fact "Engine makes an obvious best move when en passant not possible"
      (midgame-move game-state) => (has-prefix [forward-move]))))

(facts "about pawn promotion"
  (let [board (->board [" . . . . k . . . "
                        " . . . . . . . . "
                        " P . . . . . . . "
                        " . . . . p . . . "
                        " . . . . . P . . "
                        " . . . . . . . . "
                        " . p . . . . . . "
                        " . . . . K . . . "])
        board' (->board [" . . . . k . . . "
                         " . . . . . . . . "
                         " P . . . . . . . "
                         " . . . . p . . . "
                         " . . . . . P . . "
                         " . . . . . . . . "
                         " . . . . . . . . "
                         " . q . . K . . . "])
        promotion-move ["b2" "b1q"]
        game-state {:board board :turn black :last-move ["a5" "a6" white-pawn]
                    :moves-cnt 10 :board-value (eval-board board)}]
    (fact "Engine promotes pawns to queens"
      (midgame-move game-state) => (has-prefix [promotion-move])))

  (let [board (->board [" . . . . k . . . "
                        " . . . . . . . . "
                        " . . . . . . . . "
                        " . . . . . . . . "
                        " . . . . . . . . "
                        " P . . . . . . r "
                        " K P p . . . . . "
                        " R N . . . . . . "])
        board' (->board [" . . . . k . . . "
                         " . . . . . . . . "
                         " . . . . . . . . "
                         " . . . . . . . . "
                         " . . . . . . . . "
                         " P . . . . . . r "
                         " K P . . . . . . "
                         " R N n . . . . . "])
        promotion-move ["c2" "c1n"]
        game-state {:board board :turn black :last-move ["b3" "a2" white-king]
                    :moves-cnt 10 :board-value (eval-board board)}]
    (fact "Engine promotes pawns to knights"
      (midgame-move game-state) => (has-prefix [promotion-move]))))

(facts "about basic moves"
  (let [board (->board [" . . . . k . . . "
                        " . . . . . . . . "
                        " . . . . . . . . "
                        " . . . . . . . . "
                        " . . . . . . . . "
                        " P . . . . . . p "
                        " K P . . . . P P "
                        " R N . . . . . . "])
        board' (->board [" . . . . k . . . "
                         " . . . . . . . . "
                         " . . . . . . . . "
                         " . . . . . . . . "
                         " . . . . . . . . "
                         " P . . . . . . P "
                         " K P . . . . . P "
                         " R N . . . . . . "])
        regular-pawn-attack ["g2" "h3"]
        game-state {:board board :turn white :last-move ["h4" "h3" black-pawn]
                    :moves-cnt 10 :board-value (eval-board board)}]
    (fact "Engine chooses to attack with pawn"
      (midgame-move game-state) => (has-prefix [regular-pawn-attack]))))
