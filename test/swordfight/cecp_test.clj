(ns swordfight.cecp-test
  (:require [midje.sweet :refer [facts fact namespace-state-changes]]
            [swordfight.ai :refer [hurried-move]]
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
            [swordfight.core :refer [initial-game-settings]]
            [swordfight.game-rules :refer [initial-game-state]]
            [swordfight.cecp :refer [cecp-msg-myname
                                     initial-communication
                                     eval-command
                                     thinking-mode]]))

(defn cecp-facts-teardown []
  (reset! hurried-move false)
  (reset! thinking-mode false))
(namespace-state-changes [(after :facts (cecp-facts-teardown))])

(facts "about engine start"
  (fact "Engine sets the name during initial communication."
    (with-out-str
      (initial-communication)) => (str cecp-msg-myname "\n"))
  (fact "Engine sets name to \"Swordfight\" in a cecp-compatible way."
    cecp-msg-myname => "feature myname=\"Swordfight\""))

(facts "about force mode"
  (let [game-state (atom initial-game-state)
        game-settings (atom initial-game-settings)]
    (fact "Engine in force mode accepts a series of legal moves."
      (let [board-after-all-moves
            [[ r n b q k b _ r ]
             [ _ p p p p p p p ]
             [ p _ _ _ _ _ _ n ]
             [ _ _ _ _ _ _ _ _ ]
             [ P _ _ _ _ _ _ _ ]
             [ _ _ N _ _ _ _ _ ]
             [ _ P P P P P P P ]
             [ R _ B Q K B N R ]]]
        (doseq [cmd ["force" "a2a4" "a7a6" "b1c3" "g8h6"]]
          (eval-command game-state game-settings [cmd]))
        (:board @game-state) => board-after-all-moves))
    (fact "Engine in force mode rejects illegal moves."
      (with-out-str
        (eval-command game-state game-settings ["a1a8"]))
      => "Illegal move: a1a8\n")))

(facts "about board edition"
  (let [game-state (atom initial-game-state)
        game-settings (atom initial-game-settings)]
    (fact "Engine enters edition mode after edit command"
      (eval-command game-state game-settings ["edit"])
      (:edit-mode @game-state) => true)
    (fact "Edition commands alter the board correctly"
      (let [board-after-edition
            [[ r n b q k b _ r ]
             [ _ p p _ _ p p p ]
             [ p _ _ _ _ _ _ n ]
             [ _ _ _ Q _ _ _ _ ]
             [ P _ _ _ Q p R _ ]
             [ _ _ N _ _ _ _ _ ]
             [ _ P P P P P P P ]
             [ R _ B Q K B N R ]]]
        (doseq [cmd ["#"
                     "Ra1" "Bc1" "Qd1" "Ke1" "Bf1" "Ng1" "Rh1"
                     "Pb2" "Pc2" "Pd2" "Pe2" "Pf2" "Pg2" "Ph2"
                     "Nc3" "Pa4"
                     "c"
                     "Ra8" "Nb8" "Bc8" "Qd8" "Ke8" "Bf8" "Rh8"
                     "Pb7" "Pc7" "Pf7" "Pg7" "Ph7"
                     "Pa6" "Nh6"
                     "c"
                     "Qd5" "Qe4" "Rg4"
                     "c"
                     "Pf4"]]
          (eval-command game-state game-settings [cmd]))
        (:board @game-state) => board-after-edition))
    (fact "Engine leaves edition mode after . command"
      (with-out-str
        (eval-command game-state game-settings ["."]))
      (:edit-mode @game-state) => false)))
