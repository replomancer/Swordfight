(ns swordfight.cecp)


(defn xboard [game-state game-settings _]
  [game-state
   (assoc game-settings :xboard-mode
          (not (:xboard-mode game-settings)))
   ""])


(defn quit [game-state game-settings _]
  [(assoc game-state :quitting true) game-settings "Finished."])


(defn eval-command [game-state game-settings cmd-vector]
  (let [cmd (get cmd-vector 0)
        cmd-fun (cond (= cmd "quit") quit
                      (= cmd "xboard") xboard
                      :else
                      (fn [_ _ _] [game-state game-settings ""]))]
    (cmd-fun game-state game-settings cmd-vector)))
