(ns swordfight.cecp
  (:use [swordfight.game-rules :only [black? empty-board initial-game-state move promote
                                      put-piece legal-destination-indexes]]
        [swordfight.ai :only [mexican-defense]]))

(defn xboard [game-state game-settings _]
  [game-state
   (update game-settings :xboard-mode not)
   ""])


(defn quit [game-state game-settings _]
  [(assoc game-state :quitting true) game-settings "Finished."])


(defn MOVE [game-state game-settings cmd-vector]
  (let [algebraic-notation (first cmd-vector)
        pos1 (subs algebraic-notation 0 2) ;; FIXME: notation parsing
        pos2 (subs algebraic-notation 2 4)
        promoted-to (when (> (.length algebraic-notation) 4) (subs algebraic-notation 4 5))]
    [(let [shifted-piece-game-state (update game-state :board move pos1 pos2)]
       (if-not promoted-to
         shifted-piece-game-state
         (update shifted-piece-game-state :board promote pos2 promoted-to)))
     game-settings
     ""]))


(defn edit [game-state game-settings cmd-vector]
  [(assoc game-state :edit-mode true) game-settings ""])


(defn eval-edit-command [game-state game-settings cmd-vector]
  (let [cmd (first cmd-vector)]
    (cond (= cmd "c") (let [flip-color (fn [col] (if (= col \W) \B \W))]
                        [game-state
                         (update game-settings :edition-current-color flip-color)
                         ""])
          (= cmd "#") [(assoc game-state :board empty-board)
                       game-settings
                       ""]
          (= cmd ".") [(assoc game-state :edit-mode false)
                       game-settings
                       ""]
          :else (let [piece-type (.toUpperCase (subs cmd 0 1))
                      pos (subs cmd 1 3)]
                  [(update game-state :board
                           put-piece pos (str (:edition-current-color
                                               game-settings)
                                              piece-type))
                   game-settings
                   ""]))))

(defn new [game-state game-settings _]
  [initial-game-state game-settings ""])

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
          cmd-fun (if (and (>= (.length cmd) 4) ;; FIXME: notation parsing
                           (Character/isDigit (.charAt cmd 1)))
                    (comp #(apply mexican-defense %) MOVE)
                    (get cmd-fun-mapping cmd ignore))]
      (cmd-fun game-state game-settings cmd-vector))))
