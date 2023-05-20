(ns mire.server
  (:require [clojure.java.io :as io]
            [server.socket :as socket]
            [mire.player :as player]
            [mire.commands :as commands]
            [mire.rooms :as rooms]
            [mire.weapons :as weapons]
            [mire.enemies :as enemies]
            [mire.ammos :as ammos]
            [mire.items :as items]))

(defn- cleanup []
  "Drop all inventory and remove player from room and player list."
  (dosync
   (doseq [item @player/*inventory*]
     (commands/discard item))
   (commute player/streams dissoc player/*name*)
   (commute (:inhabitants @player/*current-room*)
            disj player/*name*)))

(defn- get-unique-player-name [name]
  (if (@player/streams name)
    (do (print "That name is in use; try again: ")
        (flush)
        (recur (read-line)))
    name))

(defn- mire-handle-client [in out]
  (binding [*in* (io/reader in)
            *out* (io/writer out)
            *err* (io/writer System/err)]

    ;; We have to nest this in another binding call instead of using
    ;; the one above so *in* and *out* will be bound to the socket
    (print "\nYou wake up in a ditch near the village gates with big empty bag. A girl who looks very excited comes up to you and asks: \"Are you all right? What is your name?\" \n") (flush)
    (binding [player/*name* (get-unique-player-name (read-line))
              player/*current-room* (ref (@rooms/rooms :start))
              player/*health* (ref 10)
              player/*level* (ref 1)
              player/*magic_power* (ref 0)
              player/*damage_without_weapon* (ref 1)
              player/*inventory* (ref #{})
              player/*weapon* (ref #{})
              player/*spells* (ref #{})
              player/*hands* (ref 2)
              player/*damage* (ref 0)
              player/*experience* (ref 0)
              player/*money* (ref 1)
              player/*range* (ref 0)
              player/*hits* (ref 0)
              player/*hits_with_weapon* (ref 0)
              player/*kills* (ref #{})
              player/*new_experience* (ref 10)
              player/*time* (ref 1)]
      (dosync
       (commute (:inhabitants @player/*current-room*) conj player/*name*)
       (commute player/streams assoc player/*name* *out*))

      (println (commands/look)) (print player/prompt) (flush)

      (try (loop [input (read-line)]
             (when input
               (println (commands/execute input))
               (.flush *err*)
               (print player/prompt) (flush)
               (recur (read-line))))
           (finally (cleanup))))))

(defn -main
  ([port dir1 dir2 dir3 dir4 dir5]
     (rooms/add-rooms dir1)
     (weapons/add-weapons dir2)
     (ammos/add-ammos dir3)
     (enemies/add-enemies dir4)
     (items/add-items dir5)
     (defonce server (socket/create-server (Integer. port) mire-handle-client))
     (println "Launching Mire server on port" port))
  ([port] (-main port "resources/rooms" "resources/weapons" "resources/ammos" "resources/enemies" "resources/items"))
  ([] (-main 3333)))
