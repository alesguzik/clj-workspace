(ns workspace.vk.soc1
  (:require [clojure.data.csv :as csv]
            [workspace.socio :as s]
            [workspace.util :as u]
            [workspace.persistence.neo4j :as neo]))

(def soc1-data
  (->> "data/social_mbti.csv"
       slurp
       csv/read-csv
       rest
       (map #(->> %
                  (map read-string)
                  (zipmap [:vk_id :soc1_tim_id])))
       ))

(defn stats [data]
  (->> data
      (map :soc1_tim_id)
      frequencies))

(defn save-mapping [{:keys [vk_id soc1_tim_id]}]
  (neo/merge-rel :VkUser {:vk_id vk_id}
                 :Tim {:soc1_tim_id soc1_tim_id}
                 :SOC1_TYPED {}))

(defn save-all [collection]
  (-> (mapv save-mapping collection)
      count))

#_(save-mapping {:vk_id 3885655 :soc1_tim_id (:soc1_tim_id (s/types-map :don))})
#_(save-all (take 10000 soc1-data))
#_(stats soc1-data)
