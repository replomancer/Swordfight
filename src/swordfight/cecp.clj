(ns swordfight.cecp
  (:require [swordfight.rules :as rules]
            [swordfight.board :as board]
            [swordfight.ai :as ai]
            [swordfight.debug :as debug]))

(def engine-name "Swordfight")
(def cecp-msg-finished "# Good bye.")
(defn cecp-msg-illegal [mv] (str "Illegal move: " mv))
(defn cecp-msg-move [[from-pos to-pos]] (str "move " from-pos to-pos))
(defn cecp-msg-ignored [cmd-vec] (str "#\n# Command ignored: " cmd-vec "\n#"))
(defn cecp-msg-result [result] (condp = result
                                 [:checkmated board/white] "0-1 {Black mates}"
                                 [:checkmated board/black] "1-0 {White mates}"
                                 "1/2-1/2 {Stalemate}"))
(defn cecp-msg-engine-thinking [cmd-vec]
  (str "# Engine is thinking. " cmd-vec " was ignored.\n"
       "# Allowed commands: \"?\", \"quit\", \"force\", \"xboard\" and \"new\"."))
(def cecp-feature-myname (str "myname=\"" engine-name "\""))
(def cecp-feature-no-signals-please "sigint=0 sigterm=0")
(def cecp-feature-done "done=1")

(def thinking-mode (atom false))

(defn read-command []
  (clojure.string/split (read-line) #" "))

(defn ignore [game-state game-settings cmd-vector]
  (println (cecp-msg-ignored cmd-vector)))

(defn initial-communication []
  (println "feature" cecp-feature-myname cecp-feature-no-signals-please
           cecp-feature-done))

(defn engine-move [game-state game-settings]
  (reset! thinking-mode true)
  (reset! ai/hurried-move false)
  (future
    (let [[computed-move computed-move-val] (ai/computer-move @game-state)]
      (if (#{:checkmated :stalemated} (first computed-move))
        (println (cecp-msg-result computed-move))
        (do
          (when-not (= @ai/hurried-move :move-cancelled)
            (swap! game-state rules/move computed-move)
            (if (= @ai/hurried-move true)
              (println "# Hurried move."))
            (println (cecp-msg-move computed-move)))
          (when (:debug-mode @game-settings)
            (debug/show-game-state @game-state))
          (when-not (:xboard-mode @game-settings)
            (print "> "))
          (flush)))
      (reset! thinking-mode false))))

(defn xboard-mode [_ game-settings _]
  (swap! game-settings update :xboard-mode not))

(defn quit [game-state _ _]
  (swap! game-state assoc :quitting true)
  (reset! ai/hurried-move :move-cancelled)
  (println cecp-msg-finished))

(defn move-immediately [_ _ _]
  (reset! ai/hurried-move true))

(defn force-mode [_ game-settings _]
  (reset! ai/hurried-move :move-cancelled)
  (swap! game-settings assoc :force-mode true))

(defn opponent-move [game-state game-settings cmd-vector]
  (let [algebraic-notation (first cmd-vector)
        from-pos (subs algebraic-notation 0 2)
        to-pos (subs algebraic-notation 2)]
    (if (some #{[from-pos to-pos]} (rules/legal-moves @game-state))
      (do
        (swap! game-state rules/move [from-pos to-pos])
        (if-not (:force-mode @game-settings)
          (engine-move game-state game-settings)))
      (println (cecp-msg-illegal algebraic-notation)))))

(defn edit [game-state game-settings _]
  (swap! game-state assoc :edit-mode true :edited true)
  (swap! game-settings assoc :edition-current-color board/white))

(defn eval-edit-command [game-state game-settings cmd-vector]
  (let [cmd (first cmd-vector)
        piece-placement-regexp #"[xPRNBQK][a-h][1-8]"]
    (case cmd
      "quit" (quit game-state game-settings cmd-vector)
      "c" (swap! game-settings update :edition-current-color board/change-side)
      "#" (swap! game-state assoc :board board/empty-board)
      "." (swap! game-state assoc :edit-mode false)
      (if-not (re-matches piece-placement-regexp cmd)
        (ignore game-state game-settings cmd-vector)
        (let [piece-char (.charAt cmd 0)
              piece (if (= piece-char \x)
                      ;; x is for empty square; not used by xboard
                      board/empty-square
                      (board/->piece
                       (:edition-current-color @game-settings)
                       piece-char))
              pos (subs cmd 1 3)]
          (swap! game-state update :board board/put-piece pos piece))))))

(defn go [game-state game-settings _]
  (swap! game-settings assoc :force-mode false)
  (engine-move game-state game-settings))

(defn new [game-state game-settings _]
  (reset! ai/hurried-move :move-cancelled)
  (reset! game-state rules/initial-game-state))

(defn on-move [player game-state _ _]
  (swap! game-state assoc :turn player))

(defn eval-command [game-state game-settings cmd-vector]
  (let [cmd (first cmd-vector)
        move-notation-regexp #"[a-h][1-8][a-h][1-8][qnrb]?"
        cmd-fun-mapping {"quit" quit
                         "xboard" xboard-mode
                         "edit" edit
                         "new" new
                         "?" move-immediately
                         "force" force-mode
                         "go" go
                         "white" (partial on-move board/white)
                         "black" (partial on-move board/black)}
        allowed-when-thinking #{quit force-mode move-immediately xboard-mode new}
        cmd-fun (cond
                  (:edit-mode @game-state) eval-edit-command
                  (re-matches move-notation-regexp cmd) opponent-move
                  :else (get cmd-fun-mapping cmd ignore))]
    (if (and @thinking-mode (not (allowed-when-thinking cmd-fun)))
      (println (cecp-msg-engine-thinking cmd-vector))
      (cmd-fun game-state game-settings cmd-vector))))
