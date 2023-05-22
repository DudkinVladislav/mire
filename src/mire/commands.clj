(ns mire.commands
  (:require [clojure.string :as str]
            [mire.rooms :as rooms]
            [mire.weapons :as weapons]
            [mire.player :as player]
            [mire.enemies :as enemies]
            [mire.ammos :as ammos]
            [mire.items :as items]
            ))

(defn- move-between-refs
  "Move one instance of obj between from and to. Must call in a transaction."
  [obj from to]
  (alter from disj obj)
  (alter to conj obj))

;; Command functions

(defn check
  "Look at your characteristics."
  []
  (str
    "Your health is "   @player/*health* "\n"
    "Your level is "   @player/*level* "\n"
    "Your magic power is "   @player/*magic_power* "\n"
    "Your damage without weapon is "   @player/*damage_without_weapon* "\n"
    "Your equipped weapon is " (str/join "\n" (seq @player/*weapon*)) "\n"
    "Your empty hands is " @player/*hands* "\n"
    "Your money is " @player/*money* "\n"
    )
  )




(defn look
  "Get a description of the surrounding environs and its contents."
  []
  (str (:desc @player/*current-room*)
       "\nExits: " (keys @(:exits @player/*current-room*)) "\n"
       (str/join "\n" (map #(str "There is " % " here.\n")
                           @(:items @player/*current-room*)))
       (str/join "\n" (map #(str "There is dangerous enemy " % " here.\n")
                           @(:enemy @player/*current-room*)))
       ))

(defn move
  "\" We're going to get out of this place... \" Give a direction."
  [direction]
  (dosync
   (let [target-name ((:exits @player/*current-room*) (keyword direction))
         target (@rooms/rooms target-name)]
     (if target
       (do
        (if
          (first @player/*kills*)
           (move-between-refs (first @player/*kills*) player/*kills* (:enemy @player/*current-room*) )
              (ref-set player/*hits* 0)
          )
         (move-between-refs player/*name*
                            (:inhabitants @player/*current-room*)
                            (:inhabitants target))
         (ref-set player/*current-room* target)
         (look))
       "You can't go that way.")

     )))

(defn grab
  "Pick something up."
  [thing]
  (dosync
   (if (rooms/room-contains? @player/*current-room* thing)
     (do (move-between-refs (keyword thing)
                            (:items @player/*current-room*)
                            player/*inventory*)
         (str "You picked up the " thing "."))
     (str "There isn't any " thing " here."))))

(defn discard
  "Put something down that you're carrying."
  [thing]
  (dosync
   (if (player/carrying? thing)
     (do (move-between-refs (keyword thing)
                            player/*inventory*
                            (:items @player/*current-room*))
         (str "You dropped the " thing "."))
     (str "You're not carrying a " thing "."))))

(defn equip
  "Choose your weapon."
  [weapon]
  (dosync (if (player/carrying? weapon)
            (if (<= (:wearing ((keyword weapon) @weapons/weapons )) @player/*hands* )
              (do
                (move-between-refs (keyword weapon) player/*inventory*  player/*weapon* )
                (alter player/*hands* - (:wearing ((keyword weapon) @weapons/weapons )))
                (alter player/*damage* + (:damage ((keyword weapon) @weapons/weapons )))
                (alter player/*range* + (:range ((keyword weapon) @weapons/weapons )))
                (str "You equipped " weapon))
              "You can't equip more weapons."
              )
    (str "You don't have " weapon))
          ))

(defn remove
  "Take off any equipped weapon."
  [weapon]
  (dosync
  (if (player/equipcur? weapon)
    (do
      (move-between-refs (keyword weapon) player/*weapon*  player/*inventory* )
      (alter player/*hands* + (:wearing ((keyword weapon) @weapons/weapons )))
      (alter player/*damage* - (:damage ((keyword weapon) @weapons/weapons )))
      (alter player/*range* - (:range ((keyword weapon) @weapons/weapons )))
      (str "You removed " weapon))
    (str "You don't wear " weapon))
  ))

(defn inventory
  "See what you've got."
  []
  (str "You are carrying:\n"
       (str/join "\n" (seq @player/*inventory*))))

(defn detect
  "If you have the detector, you can see which room an item is in."
  [item]
  (if (@player/*inventory* :detector)
    (if-let [room (first (filter #((:items %) (keyword item))
                                 (vals @rooms/rooms)))]
      (str item " is in " (:name room))
      (str item " is not in any room."))
    "You need to be carrying the detector for that."))

(defn analysis
  "If you have the analyser, you can see items statuses"
  [item]
  (if (@player/*inventory* :analyser)
    (if  (player/carrying? item)
      (if ((keyword item) @weapons/weapons)
        (str item "'s description is " (:desc ((keyword item) @weapons/weapons )) "\n" item "'s durability is " (:durability ((keyword item) @weapons/weapons ))
             ".\n" item "'s damage is " (:damage ((keyword item) @weapons/weapons )) ".\n" item "'s price is " (:price ((keyword item) @weapons/weapons ))
             ".\n" item "'s weight is " (:weight ((keyword item) @weapons/weapons )) ".\n You need " (:wearing ((keyword item) @weapons/weapons )) " hands for using it.")
        (if  ((keyword item) @ammos/ammos)
             (str item "'s description is " (:desc ((keyword item) @ammos/ammos )) "\n"  item "'s damage is " (:damage ((keyword item) @ammos/ammos )) ".\n"
                  item "'s price is " (:price ((keyword item) @ammos/ammos )) ".\n" item "'s weight is " (:weight ((keyword item) @ammos/ammos ))
                  ".\n You need " (:wearing ((keyword item) @ammos/ammos )) " hands for using it.\n" )
             (if ((keyword item) @items/items)
               (str item "'s description is " (:desc ((keyword item) @items/items )) "\n" item "'s usability is " (:usability ((keyword item) @items/items ))
                    ".\n" item "'s price is " (:price ((keyword item) @items/items )) ".\n" item "'s weight is " (:weight ((keyword item) @items/items )) ".\n"
                    item "'s quantity is " (:quantity ((keyword item) @items/items )))
               "This item doesn't exist"
               ))
        )
            (str "You don't carry " item)
             )
    "You need to be carrying the analyser for that."
        )
  )
(defn use
  "Use some items."
  [item]
(dosync
    (if (player/carrying? item)
      (if (= 0 (:heal ( (keyword item) @items/items)))
      (if (= (keyword item) :keys)
        (if (= (:name @player/*current-room*) :door_to_abyss)
          (do
          (move-between-refs player/*name*
                             (:inhabitants @player/*current-room*)
                             (:inhabitants (:abyss @rooms/rooms)))
          (ref-set player/*current-room* (:abyss @rooms/rooms))
          (look)
          )
          "They're just some keys. You can't use them here."
          )
        (if (= (keyword item) :eternity)
             (do
               (move-between-refs player/*name*
                                   (:inhabitants @player/*current-room*)
                                   (:inhabitants (:start @rooms/rooms)))
                (ref-set player/*current-room* (:start @rooms/rooms))
               (print "\n You win this game!!! \n")
                (look))
             (str "You can't use it anywhere. Try to sell it.")
        ))
        (if (> @player/*health* 100)
          "Your health is full"
          (do
            (alter player/*health* + (:heal ( (keyword item) @items/items)))
            (move-between-refs (keyword item)
                               player/*inventory*
                               (:items (:cemetery @rooms/rooms)))
            (str"You restore " (:heal ( (keyword item) @items/items))
             " health points.")
            (if (<= @player/*health* 0)
              (do
              (ref-set player/*current-room* (@rooms/rooms :cemetery ))
              (if (player/carrying? "keys" )
                (move-between-refs :keys
                                   player/*inventory*
                                   (:items (:demon_guard @enemies/enemies))))
              (str "You are dead.")))
            )
        ))
      (str "You don't carry " (keyword item))
  )
    )
  )
(defn hit
   "Fight with enemy by weapon."
   [enemy]
  (dosync
   (if (rooms/room-contains-enemy? @player/*current-room* enemy)
     (if (player/equip?)
         (if (< (:range ((keyword enemy) @enemies/enemies))
                @player/*range*)
           (do
             (alter player/*hits_with_weapon* + 1)
             (if (<= (- (:health ((keyword enemy) @enemies/enemies)) (* @player/*hits_with_weapon* (+ @player/*damage* @player/*damage_without_weapon*) ) (* @player/*hits*  @player/*damage_without_weapon*)) 0)
               (do

                   (move-between-refs (keyword enemy) (:enemy @player/*current-room*)  player/*kills*)
                   (ref-set player/*hits* 0)
                   (ref-set player/*hits_with_weapon* 0)
                   (alter player/*experience* + (:experience ((keyword enemy) @enemies/enemies)))
                   (player/level_up)
                   (print "You kill " enemy "\n")
                   (if-let [item (first @(:items ((keyword enemy) @enemies/enemies)))]
                     (do
                       (move-between-refs (keyword item) (:items ((keyword enemy) @enemies/enemies))  (:items @player/*current-room*) )
                       (str item " left after " (keyword enemy)))
                     (str "Nothing left after " (keyword enemy))
                     )
                   )
               (do
                 (alter player/*health* - (:power ((keyword enemy) @enemies/enemies )))
                 (if (<= @player/*health* 0)
                   (do
                     (ref-set player/*current-room* (@rooms/rooms :cemetery ))
                     (if (player/carrying? :keys) (
                                                 (move-between-refs :keys
                                                                    player/*inventory*
                                                                    (:items (:demon_guard @enemies/enemies)))
                                                  ))
                     (str "You were killed by " enemy)
                     )
                   (str
                     "You hit the enemy with weapon. It's health is "
                     (- (:health ((keyword enemy) @enemies/enemies)) (* @player/*hits_with_weapon* (+ @player/*damage* @player/*damage_without_weapon*) ) (* @player/*hits*  @player/*damage_without_weapon*))
                     " and " enemy " hit you too. Your health is " @player/*health*)
                   )
                 )
               )
             )

           (do
             (alter player/*hits_with_weapon* + 1)
             (alter player/*health* - (:power ((keyword enemy) @enemies/enemies )))
             (if (<= @player/*health* 0)
               (do
                 (ref-set player/*current-room* (@rooms/rooms :cemetery ))
                 (if (player/carrying? '(keys)) (
                                             (move-between-refs :keys
                                                                player/*inventory*
                                                                (:items (:demon_guard @enemies/enemies)))
                                             ))
                 (str "You were killed by " enemy)
                 )
               (do
                 (if (<= (- (:health ((keyword enemy) @enemies/enemies)) (* @player/*hits_with_weapon* (+ @player/*damage* @player/*damage_without_weapon*) ) (* @player/*hits*  @player/*damage_without_weapon*)) 0)
                   (do
                     (ref-set player/*hits* 0)
                     (ref-set player/*hits_with_weapon* 0)
                     (move-between-refs (keyword enemy) (:enemy @player/*current-room*)  player/*kills*)
                     (print "You kill " enemy ", but it could hit you. Your health is " @player/*health* "\n")
                     (alter player/*experience* + (:experience ((keyword enemy) @enemies/enemies)))
                     (if-let [item (first @(:items ((keyword enemy) @enemies/enemies)))]
                       (do

                         (move-between-refs (keyword item) (:items ((keyword enemy) @enemies/enemies))  (:items @player/*current-room*) )
                         (str item " left after " (keyword enemy)))
                       (str "Nothing left after " (keyword enemy))
                       ))

                 (str
                   "You hit the enemy with weapon. It's health is "
                   (- (:health ((keyword enemy) @enemies/enemies)) (* @player/*hits_with_weapon* (+ @player/*damage* @player/*damage_without_weapon*) ) (* @player/*hits*  @player/*damage_without_weapon*))
                   " and " enemy " hit you too. Your health is " @player/*health*)
                 )
               )
             )
         )
     )
         (do
           (alter player/*hits* + 1)
           (alter player/*health* - (:power ((keyword enemy) @enemies/enemies )))
           (if (<= @player/*health* 0)
             (do
               (ref-set player/*current-room* (@rooms/rooms :cemetery ))
               (if (player/carrying? "keys" )
               (move-between-refs :keys
               player/*inventory*
               (:items (:demon_guard @enemies/enemies))))
               (str "You were killed by " enemy))
             (do
               (if (<= (- (:health ((keyword enemy) @enemies/enemies)) (* @player/*hits_with_weapon* (+ @player/*damage* @player/*damage_without_weapon*) ) (* @player/*hits*  @player/*damage_without_weapon*)) 0)
                 (do
                   (ref-set player/*hits_with_weapon* 0)
                   (ref-set player/*hits* 0 )
                   (move-between-refs (keyword enemy) (:enemy @player/*current-room*)  player/*kills*)
                   (print "You kill " enemy ", but it could hit you. Your health is " @player/*health* "\n")
                   (alter player/*experience* + (:experience ((keyword enemy) @enemies/enemies)))
                   (if-let [item (first @(:items ((keyword enemy) @enemies/enemies)))]
                     (do

                       (move-between-refs (keyword item) @(:items ((keyword enemy) @enemies/enemies))  (:items @player/*current-room*) )
                       (str item " left after " (keyword enemy)))
                     (str "Nothing left after " (keyword enemy))
                     ))
                 (str
                   "You hit the enemy without weapon. It's health is "
                   (- (:health ((keyword enemy) @enemies/enemies)) (* @player/*hits_with_weapon* (+ @player/*damage* @player/*damage_without_weapon*) ) (* @player/*hits*  @player/*damage_without_weapon*))
                   " and " enemy " hit you too. Your health is " @player/*health*)
                 )
               )
             )
             )
           )
     (str "There isn't " enemy)
         )

     )
     )



(defn say
  "Say something out loud so everyone in the room can hear."
  [& words]
  (let [message (str/join " " words)]
    (doseq [inhabitant (disj @(:inhabitants @player/*current-room*)
                             player/*name*)]
      (binding [*out* (player/streams inhabitant)]
        (println message)
        (println player/prompt)))
    (str "You said " message)))

(defn help
  "Show available commands and what they do."
  []
  (str/join "\n" (map #(str (key %) ": " (:doc (meta (val %))))
                      (dissoc (ns-publics 'mire.commands)
                              'execute 'commands))))


;; Command data

(def commands {"move" move,
               "north" (fn [] (move :north)),
               "south" (fn [] (move :south)),
               "east" (fn [] (move :east)),
               "west" (fn [] (move :west)),
               "grab" grab
               "discard" discard
               "inventory" inventory
               "detect" detect
               "look" look
               "say" say
               "help" help
               "hit" hit
               "equip" equip
               "check" check
               "remove" remove
               "analysis" analysis
               "use" use})

;; Command handling

(defn execute
  "Execute a command that is passed to us."
  [input]
  (try (let [[command & args] (.split input " +")]
         (apply (commands command) args))
       (catch Exception e
         (.printStackTrace e (new java.io.PrintWriter *err*))
         "You can't do that!")))