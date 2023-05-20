(ns mire.player)

(def ^:dynamic *current-room*)
(def ^:dynamic *inventory*)
(def ^:dynamic *name*)
(def ^:dynamic *weapon*)
(def ^:dynamic *level*)
(def ^:dynamic *health*)
(def ^:dynamic *magic_power*)
(def ^:dynamic *damage_without_weapon*)
(def ^:dynamic *spells*)
(def ^:dynamic *hands*)
(def ^:dynamic *damage*)
(def ^:dynamic *experience*)
(def ^:dynamic *money*)
(def ^:dynamic *range*)
(def ^:dynamic *hits_with_weapon*)
(def ^:dynamic *hits*)
(def ^:dynamic *kills*)
(def ^:dynamic *new_experience*)
(def ^:dynamic *time*)
(def prompt "> ")
(def streams (ref {}))

(defn carrying? [thing]
  (some #{(keyword thing)} @*inventory*))

(defn equipcur? [weapon]
  (some #{(keyword weapon)} @*weapon*))

(defn equip? []
  (some?  (vals @*weapon*) ))

(defn level_up []
  (if (>= @*experience* @*new_experience*)
    (do
  (alter *level* + 1)
  (alter *health* + (* 10 @*level*))
  (alter *magic_power* + (* 2 (+ @*level* 1)))
  (alter *damage_without_weapon* + (* 2 (+ @*level* 1)))
  (if (= @*time* 1)
    (do
      (ref-set *time* 2)
      (alter *new_experience* * 5)
      )
    (do
      (ref-set *time* 1)
      (alter *new_experience* * 2)
      )
    )
  )
  )
  )
