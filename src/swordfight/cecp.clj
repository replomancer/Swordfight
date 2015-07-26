(ns swordfight.cecp
  (:use [swordfight.game-rules :only [black? empty-board initial-game-state move promote
                                      put-piece legal-destination-indexes]]
        [swordfight.ai :only [mexican-defense]]
        [swordfight.debug :only [show-game-state]]))

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
        promoted-to (when (> (.length algebraic-notation) 4)
                      (subs algebraic-notation 4 5))
        game-state' (-> (update game-state :board move pos1 pos2)
                        (assoc :last-move [pos1 pos2])
                        (update :board promote pos2 promoted-to))]
        (when (:debug-mode game-settings)
          (show-game-state game-state'))
        [game-state' game-settings ""]))


(defn edit [game-state game-settings cmd-vector]
  [(assoc game-state :edit-mode true :edited true) game-settings ""])


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
  (let [cmd (get cmd-vector 0)
        cmd-fun-mapping {"quit" quit
                         "xboard" xboard
                         "edit" edit
                         "new" new}
        cmd-fun (cond (:edit-mode game-state) eval-edit-command
                      (and (>= (.length cmd) 4)
                           (Character/isDigit (.charAt cmd 1))) (comp
                                                                 #(apply mexican-defense %)
                                                                 MOVE)
                      :else (get cmd-fun-mapping cmd ignore))]
    (cmd-fun game-state game-settings cmd-vector)))
