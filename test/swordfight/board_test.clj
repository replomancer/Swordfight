(ns swordfight.board-test
  (:require [midje.sweet :refer [facts fact has-prefix]]
            [swordfight.board :refer [black-king black-queen black-bishop
                                      black-knight black-rook black-pawn
                                      white-king white-queen white-bishop
                                      white-knight white-rook white-pawn
                                      empty-square white black
                                      initial-board ->board]
             :rename {black-king k black-queen q black-bishop b
                      black-knight n black-rook r black-pawn p
                      white-king K white-queen Q white-bishop B
                      white-knight N white-rook R white-pawn P
                      empty-square _}]
            [swordfight.ai :refer [midgame-move]]))

(facts "about board creation"
  (let [new-initial-board (->board [" r n b q k b n r "
                                    " p p p p p p p p "
                                    " . . . . . . . . "
                                    " . . . . . . . . "
                                    " . . . . . . . . "
                                    " . . . . . . . . "
                                    " P P P P P P P P "
                                    " R N B Q K B N R "])]
    (fact "->board function correctly (re)creates initial board"
      new-initial-board => initial-board))

  (let [board (->board [" . . . . k . . . "
                        " b N . . r . . . "
                        " . . . q . . . . "
                        " . . . . . . R . "
                        " P p . . . . . . "
                        " . . . . . Q . . "
                        " . B n . . . . . "
                        " . . . . . K . . "])
        board-representation [[ _ _ _ _ k _ _ _ ]
                              [ b N _ _ r _ _ _ ]
                              [ _ _ _ q _ _ _ _ ]
                              [ _ _ _ _ _ _ R _ ]
                              [ P p _ _ _ _ _ _ ]
                              [ _ _ _ _ _ Q _ _ ]
                              [ _ B n _ _ _ _ _ ]
                              [ _ _ _ _ _ K _ _ ]]]
    (fact "Freshly created board is represented correctly"
      board => board-representation)))
