(ns workspace.vk.persistence.neo4j
  (:require
   [clojure.math.combinatorics :as combo]
   [clojure.data.csv :as csv]
   [clojurewerkz.neocons.rest :as neo]
   [clojurewerkz.neocons.rest
    [nodes :as node]
    [relationships :as rel]
    [cypher :as cypher]
    [paths :as path]
    [labels :as label]]))

(def conn (neo/connect "http://localhost:7474/db/data/"))

(defn- find-first [pred coll] (first (filter pred coll)))
(defn- find-by [k v coll] (find-first #(= (k %) v) coll))
(defn- map-hash [f a-map]
  (into {} (map (fn [[k v]] (f k v)) a-map)))
(defn- swap-kv [a-map]
  (map-hash #(vector %2 %) a-map))

(def axes [:irrational :extravert :intuit :logic])
(def axis-complements
  (let [direct-mapping {:irrational :rational
                        :extravert  :introvert
                        :intuit     :sensoric
                        :logic      :ethic    }
        reverse-mapping (swap-kv direct-mapping)]
    (merge direct-mapping reverse-mapping)))

(defn axis-values [_axis-name] [true false])

(def types-raw
  (let [values (map axis-values axes)]
    (map #(zipmap axes %)
         (apply combo/cartesian-product values))))

(defn complementary-axes-data [tim]
  (->> tim
       (map (fn [[k v]] [(axis-complements k) (not v)]))
       (into {})))

(defn canonical-name [tim]
  (let [rat-part (if (:logic     tim) "Л" "Э")
        irr-part (if (:intuit    tim) "И" "С")
        vertion  (if (:extravert tim) "Э" "И")
        [first-part second-part] (if (:irrational tim)
                                   [irr-part rat-part]
                                   [rat-part irr-part])]
    (str first-part second-part vertion)))

(def additional-info
  {"ИЛЭ" {:name "Дон Кихот"      :soc1_tim_id 1  :sname "Дон"  :n :don   }
   "СЭИ" {:name "Дюма"           :soc1_tim_id 2  :sname "Дюма" :n :duma  }
   "ЭСЭ" {:name "Гюго"           :soc1_tim_id 3  :sname "Гюго" :n :gugo  }
   "ЛИИ" {:name "Робеспьер"      :soc1_tim_id 4  :sname "Роб"  :n :rob   }
   "ЭИЭ" {:name "Гамлет"         :soc1_tim_id 5  :sname "Гам"  :n :gam   }
   "ЛСИ" {:name "Максим Горький" :soc1_tim_id 6  :sname "Макс" :n :max   }
   "СЛЭ" {:name "Жуков"          :soc1_tim_id 7  :sname "Жук"  :n :zhuk  }
   "ИЭИ" {:name "Есенин"         :soc1_tim_id 8  :sname "Есь"  :n :es    }
   "СЭЭ" {:name "Наполеон"       :soc1_tim_id 9  :sname "Нап"  :n :nap   }
   "ИЛИ" {:name "Бальзак"        :soc1_tim_id 10 :sname "Баль" :n :bal   }
   "ЛИЭ" {:name "Джек Лондон"    :soc1_tim_id 11 :sname "Джек" :n :jack  }
   "ЭСИ" {:name "Драйзер"        :soc1_tim_id 12 :sname "Драй" :n :drei  }
   "ЛСЭ" {:name "Штирлиц"        :soc1_tim_id 13 :sname "Штир" :n :shtir }
   "ЭИИ" {:name "Достоевский"    :soc1_tim_id 14 :sname "Дост" :n :dost  }
   "ИЭЭ" {:name "Гексли"         :soc1_tim_id 15 :sname "Гек"  :n :gek   }
   "СЛИ" {:name "Габен"          :soc1_tim_id 16 :sname "Габ"  :n :gab   }})

(defn complete-type-data [tim]
  (let [cname (canonical-name tim)]
    (merge tim
           (complementary-axes-data tim)
           {:canonical_name cname}
           (additional-info cname))))

(def types (sort-by :soc1_tim_id (map complete-type-data types-raw)))
(def types-map (into {} (map #(vector (:n %) %) types)))

(defn type-data-table [tim]
  (map tim axes))

(defn diff-tims [& tims]
  (let [vals (map type-data-table tims)
        diff (apply map not= vals)]
    (zipmap axes diff)))

(defn map-vals [f hash]
  (into {} (map (fn [[k v]] [k (f v)]) hash)))

(defn same-tims [& tims]
  (map-vals not (apply diff-tims tims)))

(defn relationship [tim1 tim2]
  (let [{lo :logic in :intuit ex :extravert ir :irrational} (same-tims tim1 tim2)
        * true
        - false]
    (condp = [lo in ex ir]
      [ * * * * ] :tozhd
      [ - * * * ] :rodstv
      [ * - * * ] :del
      [ - - * * ] :superego
      [ * * - * ] :protivopolozh
      [ - * - * ] :mirazh
      [ * - - * ] :poludual
      [ - - - * ] :dual
      [ * * * - ] :kvazitozhd
      [ - * * - ] :zakazchik
      [ * - * - ] :podzakazn
      [ - - * - ] :activation
      [ * * - - ] :zerkal
      [ - * - - ] :podrevizn
      [ * - - - ] :revizor
      [ - - - - ] :conflict
      )))

(let [types-with-nodes (map #(assoc % :node (node/create conn %)) types)]
  (cypher/tquery conn "match (e:Tim) optional match (e)-[r]->() delete e,r")
  (doseq [node (map :node types-with-nodes)] (label/add conn node :Tim))
  (count (for [tim1 types-with-nodes tim2 types-with-nodes]
           (rel/create conn (:node tim1) (:node tim2) (relationship tim1 tim2))))
  (cypher/tquery conn "match (e:Tim) return e.name, e.canonical_name"))

(read-string "42")

(def soc1-data
  (->> "data/social_mbti.csv"
       slurp
       csv/read-csv
       rest
       (take 10)
       (map #(->> %
                  (map read-string)
                  (zipmap [:vk_id :soc1_tim])))))
