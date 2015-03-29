(ns swordfight.cecp
  (:use [swordfight.game-rules :only [empty-board move put-piece]]))

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


(defn ignore [game-state game-settings cmd-vector]
  [game-state game-settings (str "#\n#    COMMAND IGNORED: " cmd-vector "\n#")])


(defn eval-command [game-state game-settings cmd-vector]
  (if (:edit-mode game-state)
    (eval-edit-command game-state game-settings cmd-vector)
    (let [cmd (get cmd-vector 0)
          cmd-fun-mapping {"quit" quit
                           "xboard" xboard
                           "edit" edit}
          cmd-fun (if (and (= (count cmd) 4) ;; FIXME: notation parsing
                           (Character/isDigit (.charAt cmd 1)))
                    MOVE
                    (get cmd-fun-mapping cmd ignore))]
      (cmd-fun game-state game-settings cmd-vector))))
