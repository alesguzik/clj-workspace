(ns workspace.vk
  (:require
   [workspace.vk.api :as api]
   [me.raynes.conch :as sh]
   ))

(def me (api/make-api-call "users.get" {:user-id 3885655 :fields api/all-user-fields}))
