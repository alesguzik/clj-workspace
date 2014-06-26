(ns workspace.vk
  (:require
   [workspace.vk.api :as api]
   [me.raynes.conch :as sh]
   ))

(defn get-user [user-id]
  (api/make-api-call "users.get" {:user-id user-id :fields api/all-user-fields}))

(defn get-groups [user-id]
  (-> (api/make-api-call "groups.get" {:user-id user-id})
      :response
      :items))

#_(def me (get-user 3885655))
#_(get-groups 3885655)
