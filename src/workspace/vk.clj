(ns workspace.vk
  (:require
   [workspace.core :as core]
   [workspace.vk.api :as api]
   ))

(def me (api/make-api-call "users.get" {:user-id 3885655 :fields api/all-user-fields}))
