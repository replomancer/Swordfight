(ns swordfight.cecp
  (:use [swordfight.game-rules :only [move]]))


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

(defn eval-command [game-state game-settings cmd-vector]
  (let [cmd (get cmd-vector 0)
        cmd-fun (cond (= cmd "quit") quit
                      (= cmd "xboard") xboard
                      :else MOVE)]
    (cmd-fun game-state game-settings cmd-vector)))
