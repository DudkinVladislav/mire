(ns mire.enemies)

(def enemies (ref {}))

(defn load-enemy [enemies file]
  (let [enemy (read-string (slurp (.getAbsolutePath file)))]
    (conj enemies
          {(keyword (.getName file))
           {
            :desc (:desc enemy)
            :items (ref (or (:items enemy) #{}))
            :power (:power enemy)
            :health (:health enemy )
            :experience (:experience enemy)
            :range (:range enemy)
            }})))


(defn load-enemies
  "Given a dir, return a map with an entry corresponding to each file
  in it. Files should be maps containing item data."
  [enemies dir]
  (dosync
    (reduce load-enemy enemies
            (.listFiles (java.io.File. dir)))))

(defn add-enemies
  "Look through all the files in a dir for files describing items and add
  them to the mire.items/items map."
  [dir]
  (dosync
    (alter enemies load-enemies dir)))