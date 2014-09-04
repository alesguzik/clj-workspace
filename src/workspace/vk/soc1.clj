(ns workspace.vk.soc1
  (:require [clojure.data.csv :as csv]
            [workspace.socio :as s]
            [workspace.util :as u]
            [workspace.vk :as vk]
            [workspace.persistence.neo4j :as neo]))

(def soc1-data
  (->> "data/social_mbti.csv"
       slurp
       csv/read-csv
       rest
       (map #(->> %
                  (map read-string)
                  (zipmap [:vk_id :soc1_tim_id])))))

(defn stats [data]
  (->> data
      (map :soc1_tim_id)
      frequencies))

(defn save-mapping [vk_id soc1_tim_id]
  (neo/merge-rel :VkUser {:vk_id vk_id}
                 :Tim {:soc1_tim_id soc1_tim_id}
                 :SOC1_TYPED {}))

(defn import-groups [vk_id]
  (doseq [group_id (vk/get-groups vk_id)]
    (neo/merge-rel :VkUser {:vk_id vk_id}
                   :VkGroup {:group_id group_id}
                   :IN_GROUP {})))

(defn save-with-groups-import [{:keys [vk_id soc1_tim_id]}]
  (save-mapping vk_id soc1_tim_id)
  (import-groups vk_id))

(defn save-all [collection]
  (-> (mapv save-with-groups-import collection)
      count))

#_(save-mapping {:vk_id 3885655 :soc1_tim_id (:soc1_tim_id (s/types-map :don))})
#_(save-all (take 10000 soc1-data))
#_(stats soc1-data)
