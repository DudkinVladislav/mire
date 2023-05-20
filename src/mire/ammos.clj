(ns mire.ammos)
(def ammos (ref {}))

(defn load-ammo [ammos file]
  (let [ammo (read-string (slurp (.getAbsolutePath file)))]
    (conj ammos
          {(keyword (.getName file))
           {
            :desc (:desc ammo)
            :damage (:damage ammo)
            :price (:price ammo )
            :weight (:weight ammo)
            :wearing (:wearing ammo)
            :range (:range ammo)
            :range_without_ammo (:range_without_ammo ammo)
            :damage_without_ammo (:damage_without_ammo ammo)
            :ammo (:ammo ammo)
            :quantity (:quantity ammo)}})))


(defn load-ammos
  "Given a dir, return a map with an entry corresponding to each file
  in it. Files should be maps containing item data."
  [ammos dir]
  (dosync
    (reduce load-ammo ammos
            (.listFiles (java.io.File. dir)))))

(defn add-ammos
  "Look through all the files in a dir for files describing items and add
  them to the mire.items/items map."
  [dir]
  (dosync
    (alter ammos load-ammos dir)))
