(ns swordfight.cecp
  (:use [swordfight.game-rules :only [initial-game-state move
                                      possible-moves-from-square
                                      update-castling-info]]
        [swordfight.board :only [empty-board put-piece change-side white black
                                 empty-square ->piece]]
        [swordfight.ai :only [computer-move checkmated-val stalemated-val]]
        [swordfight.debug :only [show-game-state]]))

(def cecp-msg-finished "Good bye.")
(defn cecp-msg-illegal [mv] (str "Illegal move:" mv))
(def cecp-msg-ok "")
(defn cecp-msg-move [[from-pos to-pos]] (str "move " from-pos to-pos))
(defn cmd-msg-ignored [cmd] (str "#\n#    COMMAND IGNORED: " cmd "\n#"))
(defn cecp-msg-result [result] (condp = result
                                 [:checkmated white] "0-1 {Black mates}"
                                 [:checkmated black] "1-0 {White mates}"
                                 "1/2-1/2 {Stalemate}"))

(defn xboard [game-state game-settings _]
  [game-state
   (update game-settings :xboard-mode not)
   cecp-msg-ok])

(defn quit [game-state game-settings _]
  [(assoc game-state :quitting true) game-settings cecp-msg-finished])

(defn make-move [game-state game-settings cmd-vector]
  (let [algebraic-notation (first cmd-vector)
        from-pos (subs algebraic-notation 0 2)
        to-pos (subs algebraic-notation 2)
        [game-state' msg] (if (some #{to-pos}
                                    (possible-moves-from-square game-state
                                                                from-pos))
                            [(move game-state [from-pos to-pos]) cecp-msg-ok]
                            [game-state
                             (cecp-msg-illegal algebraic-notation)])]
    (when (:debug-mode game-settings)
      (show-game-state game-state'))
    [game-state' game-settings msg]))

(defn edit [game-state game-settings cmd-vector]
  [(assoc game-state :edit-mode true :edited true)
   (assoc game-settings :edition-current-color white)
   cecp-msg-ok])

(defn eval-edit-command [game-state game-settings cmd-vector]
  (let [cmd (first cmd-vector)]
    (condp = cmd
      "quit" (quit game-state game-settings cmd-vector)
      "c" [game-state
           (update game-settings :edition-current-color change-side)
           cecp-msg-ok]
      "#" [(assoc game-state :board empty-board)
           game-settings
           cecp-msg-ok]
      "." [(assoc game-state :edit-mode false)
           game-settings
           cecp-msg-ok]
      (let [piece-char (.charAt cmd 0)
            piece (if (= piece-char \x)
                    ;; x is for empty square; not used by xboard
                    empty-square
                    (->piece (:edition-current-color game-settings)
                             piece-char))
            pos (subs cmd 1 3)]
        [(update game-state :board
                 put-piece pos piece)
         game-settings
         cecp-msg-ok]))))

(defn new [game-state game-settings _]
  [initial-game-state game-settings cecp-msg-ok])

(defn ignore [game-state game-settings cmd-vector]
  [game-state game-settings (cmd-msg-ignored cmd-vector)])

(defn engine-move [game-state game-settings last-msg]
  (if (not= last-msg cecp-msg-ok)
    [game-state game-settings last-msg]
    (let [[computed-move computed-move-val] (computer-move game-state)
          [game-state' move-str]
          (if (#{:checkmated :stalemated} (first computed-move))
            [game-state (cecp-msg-result computed-move)]
            [(move game-state computed-move) (cecp-msg-move computed-move)])]
      [game-state' game-settings move-str])))

(defn eval-command [game-state game-settings cmd-vector]
  (let [cmd (get cmd-vector 0)
        cmd-fun-mapping {"quit" quit
                         "xboard" xboard
                         "edit" edit
                         "new" new}
        cmd-fun (cond (:edit-mode game-state) eval-edit-command
                      (re-matches
                       #"^[a-h][1-8][a-h][1-8]$" cmd) (fn [& args]
                                                        (->> args
                                                             (apply make-move)
                                                             (apply engine-move)))
                      :else (get cmd-fun-mapping cmd ignore))]
    (cmd-fun game-state game-settings cmd-vector)))
