(ns swordfight.core
  (:gen-class)
  (:use [swordfight.cecp :only [eval-command]]
        [swordfight.debug :only [show-board]]
        [swordfight.game-rules :only [initial-board]]))


(def initial-settings {:xboard-mode false
                       :debug-mode true})


(defn receive-command []
  (-> (read-line)
      (.toLowerCase)
      (clojure.string/split #" ")))


(defn send-command [[game-state game-settings cmd]]
  (println cmd)
  [game-state game-settings])


(defn -main
  [& args]
  (loop [game-state {:board initial-board :turn "white"}
         game-settings initial-settings]
    (if (:debug-mode game-settings)
      (do
        (println game-settings)
        (show-board (:board game-state))))
    (if-not (:xboard-mode game-settings)
      (print "> "))
    (flush)
    (let [[game-state' game-settings'] (->> (receive-command)
                                            (eval-command game-state game-settings)
                                            (send-command))]
      (if-not (:quitting game-state')
        (recur game-state' game-settings')))))
