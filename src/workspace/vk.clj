(ns workspace.vk
  (:require
   [clojure.string :as str]
   [clojure.set :as set]
   [org.httpkit.client :as http]
   [clojure.data.json :as json]
   [me.raynes.conch :as sh]
   [workspace.core :as core]
   [clojure.java.browse :as browse]
   ))

(def my-app-id 4328936)

(def me (make-api-call "users.get" {:user-id 3885655 :fields all-user-fields}))
