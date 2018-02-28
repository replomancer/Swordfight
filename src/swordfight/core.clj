(ns swordfight.core
  (:gen-class)
  (:require [swordfight.cecp :refer [initial-communication
                                     read-command eval-command]]
            [swordfight.debug :refer [print-debug-output]]
            [swordfight.rules :refer [initial-game-state]]))

(def initial-game-settings {:xboard-mode false
                            :debug-mode true
                            :force-mode false})

(def game-state (atom initial-game-state))
(def game-settings (atom initial-game-settings))

(defn -main
  [& args]
  (initial-communication)
  (loop []
    (when (:debug-mode @game-settings)
      (print-debug-output @game-state @game-settings))
    (when-not (:xboard-mode @game-settings)
      (print "> "))
    (flush)
    (eval-command game-state game-settings (read-command))
    (if-not (:quitting @game-state)
      (recur)))
  (shutdown-agents))
