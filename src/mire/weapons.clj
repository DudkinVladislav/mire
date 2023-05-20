(ns mire.weapons)

(def weapons (ref {}))

(defn load-weapon [weapons file]
  (let [weapon (read-string (slurp (.getAbsolutePath file)))]
    (conj weapons
          {(keyword (.getName file))
           {
            :desc (:desc weapon)
            :durability (:durability weapon)
            :damage (:damage weapon)
            :price (:price weapon )
            :weight (:weight weapon)
            :wearing (:wearing weapon)
            :range (:range weapon)
            :range_without_ammo (:range_without_ammo weapon)
            :damage_without_ammo (:damage_without_ammo weapon)
            :ammo (:ammo weapon)}})))


(defn load-weapons
  "Given a dir, return a map with an entry corresponding to each file
  in it. Files should be maps containing item data."
  [weapons dir]
  (dosync
    (reduce load-weapon weapons
            (.listFiles (java.io.File. dir)))))

(defn add-weapons
  "Look through all the files in a dir for files describing items and add
  them to the mire.items/items map."
  [dir]
  (dosync
    (alter weapons load-weapons dir)))
