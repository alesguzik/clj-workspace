(ns workspace.vk.util
  (:require
   [workspace.vk.api :as api]
   [me.raynes.conch :as sh]
   ))

(defn save-post [prefix date text images]
  (when-not (empty? images)
    (sh/with-programs [wget mkdir]
      (let [dirname (str prefix "/" date)]
        (mkdir "-p" dirname)
        (spit (str dirname "/desc.txt") text)
        (doseq [i images] (wget "-P" dirname i))
        true))))

(defn extract-photo [item]
  (->> item
       :attachments
       (mapv (comp :photo-807 :photo))))

(defn save-group-photos-with-text [prefix group-id & {:keys [count] :or {count 20}}]
  (->> (api/make-api-call "wall.get" {:owner-id (- group-id) :count count})
       :response
       :items
       (map (juxt :date :text extract-photo))
       (map #(apply save-post prefix %))))

;; (save-group-photos-with-text "faunagoroda" 55903494)
