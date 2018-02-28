(ns swordfight.cecp-test
  (:require [midje.sweet :refer [contains fact facts has-suffix namespace-state-changes]]
            [swordfight.ai :refer [hurried-move]]
            [swordfight.board :refer [->board]]
            [swordfight.core :refer [initial-game-settings]]
            [swordfight.rules :refer [initial-game-state]]
            [swordfight.cecp :refer [initial-communication
                                     eval-command
                                     thinking-mode]]))

(defn cecp-facts-teardown []
  (reset! hurried-move false)
  (reset! thinking-mode false))
(namespace-state-changes [(after :facts (cecp-facts-teardown))])

(facts "about engine start"
  (fact "Engine sets the name to \"Swordfight\" during initial communication."
    (with-out-str
      (initial-communication)) => (contains "myname=\"Swordfight\""))
  (fact "Engine requests for signal-free communication with the GUI."
    (with-out-str
      (initial-communication)) => (contains "sigint=0 sigterm=0"))
  (fact "Engine sends done=1 in features."
    (with-out-str (initial-communication)) => (has-suffix "done=1\n")))

(facts "about force mode"
  (let [game-state (atom initial-game-state)
        game-settings (atom initial-game-settings)]
    (fact "Engine in force mode accepts a series of legal moves."
      (let [board-after-all-moves
            (->board [" r n b q k b . r "
                      " . p p p p p p p "
                      " p . . . . . . n "
                      " . . . . . . . . "
                      " P . . . . . . . "
                      " . . N . . . . . "
                      " . P P P P P P P "
                      " R . B Q K B N R "])]
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
            (->board [" r n b q k b . r "
                      " . p p . . p p p "
                      " p . . . . . . n "
                      " . . . Q . . . . "
                      " P . . . Q p R . "
                      " . . N . . . . . "
                      " . P P P P P P P "
                      " R . B Q K B N R "])]
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
