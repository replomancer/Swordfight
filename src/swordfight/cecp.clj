(ns swordfight.cecp
  (:use [swordfight.game-rules :only [black? empty-board initial-game-state move pos2idx put-piece]]))

(defn xboard [game-state game-settings _]
  [game-state
   (assoc game-settings :xboard-mode
          (not (:xboard-mode game-settings)))
   ""])


(defn quit [game-state game-settings _]
  [(assoc game-state :quitting true) game-settings "Finished."])


(defn MOVE [game-state game-settings cmd-vector]
  (let [algebraic-notation (first cmd-vector)
        pos1 (subs algebraic-notation 0 2) ;; FIXME: notation parsing
        pos2 (subs algebraic-notation 2 4)]
    [(assoc game-state :board
            (move (:board game-state) pos1 pos2))
     game-settings
     ""]))


(defn edit [game-state game-settings cmd-vector]
  [(assoc game-state :edit-mode true) game-settings ""])


(defn eval-edit-command [game-state game-settings cmd-vector]
  (let [cmd (first cmd-vector)]
    (cond (= cmd "c") (let [flip-color (fn [col] (if (= col \W) \B \W))]
                        [game-state
                         (assoc game-settings :edition-current-color
                                (flip-color (:edition-current-color game-settings)))
                         ""])
          (= cmd "#") [(assoc game-state :board empty-board)
                       game-settings
                       ""]
          (= cmd ".") [(assoc game-state :edit-mode false)
                       game-settings
                       ""]
          :else (let [piece-type (.toUpperCase (subs cmd 0 1))
                      pos (subs cmd 1 3)]
                  [(assoc game-state :board (put-piece (:board game-state)
                                                       pos
                                                       (str
                                                        (:edition-current-color game-settings)
                                                        piece-type)))
                   game-settings
                   ""]))))

(defn new [game-state game-settings _]
  [initial-game-state game-settings ""])


(defn mexican-defense [game-state game-settings _]
  (if (black? ((:board game-state) (pos2idx "b8")))
    [(assoc game-state :board (move (:board game-state) "b8" "c6"))
     game-settings
     "move b8c6"]
    (if (black? ((:board game-state) (pos2idx "g8")))
      [(assoc game-state :board (move (:board game-state) "g8" "f6"))
       game-settings
       "move g8f6"]
      [game-state
       game-settings
       "tellopponent Good Game! I give up.\nresign"])))


(defn ignore [game-state game-settings cmd-vector]
  [game-state game-settings (str "#\n#    COMMAND IGNORED: " cmd-vector "\n#")])


(defn eval-command [game-state game-settings cmd-vector]
  (if (:edit-mode game-state)
    (eval-edit-command game-state game-settings cmd-vector)
    (let [cmd (get cmd-vector 0)
          cmd-fun-mapping {"quit" quit
                           "xboard" xboard
                           "edit" edit
                           "new" new}
          cmd-fun (if (and (= (count cmd) 4) ;; FIXME: notation parsing
                           (Character/isDigit (.charAt cmd 1)))
                    (comp #(apply mexican-defense %)  MOVE)
                    (get cmd-fun-mapping cmd ignore))]
      (cmd-fun game-state game-settings cmd-vector))))
