(ns swordfight.cecp
  (:use [swordfight.game-rules :only [initial-game-state move
                                      possible-moves-from-square
                                      update-castling-info]]
        [swordfight.board :only [empty-board put-piece]]
        [swordfight.ai :only [mexican-defense]]
        [swordfight.debug :only [show-game-state]]))

(def cecp-msg-finished "Finished.")
(defn cecp-msg-illegal [mv] (str "Illegal move:" mv))
(def cecp-msg-ok "")
(defn cmd-ignored-msg [cmd] (str "#\n#    COMMAND IGNORED: " cmd "\n#"))

(defn xboard [game-state game-settings _]
  [game-state
   (update game-settings :xboard-mode not)
   cecp-msg-ok])

(defn quit [game-state game-settings _]
  [(assoc game-state :quitting true) game-settings cecp-msg-finished])

(defn make-move [game-state game-settings cmd-vector]
  (let [algebraic-notation (first cmd-vector)
        pos1 (subs algebraic-notation 0 2) ;; FIXME: notation parsing
        pos2 (subs algebraic-notation 2)
        [game-state' output-str] (if (some #{pos2}
                                           (possible-moves-from-square game-state
                                                                       pos1))
                                   [(move game-state [pos1 pos2]) cecp-msg-ok]
                                   [game-state
                                    (cecp-msg-illegal algebraic-notation)])]
    (when (:debug-mode game-settings)
      (show-game-state game-state'))
    [game-state' game-settings output-str]))

(defn edit [game-state game-settings cmd-vector]
  [(assoc game-state :edit-mode true :edited true) game-settings cecp-msg-ok])

(defn eval-edit-command [game-state game-settings cmd-vector]
  (let [cmd (first cmd-vector)]
    (cond (= cmd "c") (let [flip-color (fn [col] (if (= col \W) \B \W))]
                        [game-state
                         (update game-settings :edition-current-color flip-color)
                         cecp-msg-ok])
          (= cmd "#") [(assoc game-state :board empty-board)
                       game-settings
                       cecp-msg-ok]
          (= cmd ".") [(assoc game-state :edit-mode false)
                       game-settings
                       cecp-msg-ok]
          :else (let [piece-type (.toUpperCase (subs cmd 0 1))
                      pos (subs cmd 1 3)]
                  [(update game-state :board
                           put-piece pos (str (:edition-current-color
                                               game-settings)
                                              piece-type))
                   game-settings
                   cecp-msg-ok]))))

(defn new [game-state game-settings _]
  [initial-game-state game-settings cecp-msg-ok])

(defn ignore [game-state game-settings cmd-vector]
  [game-state game-settings (cmd-ignored-msg cmd-vector)])

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
                                                                 make-move)
                      :else (get cmd-fun-mapping cmd ignore))]
    (cmd-fun game-state game-settings cmd-vector)))
