(ns swordfight.core
  (:gen-class)
  (:use [swordfight.cecp :only [eval-command]]
        [swordfight.game-rules :only [initial-board]]))


(def initial-settings {:xboard-mode false})


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
    (if-not (:xboard-mode game-settings)
      (print "> "))
    (flush)
    (let [[game-state' game-settings'] (->> (receive-command)
                                            (eval-command game-state game-settings)
                                            (send-command))]
      ;;(println game-state' game-settings')
      (if-not (:quitting game-state')
        (recur game-state' game-settings')))))
