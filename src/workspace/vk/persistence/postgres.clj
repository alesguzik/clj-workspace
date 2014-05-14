(ns workspace.vk.persistence.postgres
  (:require
   [korma.db :refer :all]
   [korma.core :refer :all]
   ))

(defdb db (postgres {:db "workspace"
                     :user "postgres"
                     ;; :password "dbpass"
                     }))
