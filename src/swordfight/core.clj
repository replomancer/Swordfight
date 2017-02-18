(ns swordfight.core
  (:gen-class)
  (:use [swordfight.cecp :only [eval-command]]
        [swordfight.debug :only [print-debug-output]]
        [swordfight.game-rules :only [initial-game-state]]))

(def initial-settings {:xboard-mode false
                       :debug-mode true
                       :edition-current-color \W})

(defn receive-command []
  (-> (read-line)
      (.toLowerCase)
      (clojure.string/split #" ")))

(defn send-command [[game-state game-settings cmd]]
  (println cmd)
  [game-state game-settings])

(defn -main
  [& args]
  (loop [game-state initial-game-state
         game-settings initial-settings]
    (when (:debug-mode game-settings)
      (print-debug-output game-state game-settings))
    (when-not (:xboard-mode game-settings)
      (print "> "))
    (flush)
    (let [[game-state' game-settings'] (->> (receive-command)
                                            (eval-command game-state game-settings)
                                            (send-command))]
      (if-not (:quitting game-state')
        (recur game-state' game-settings'))))
  (shutdown-agents))
