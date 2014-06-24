(ns workspace.persistence.postgres
  (:require
   [korma.db :refer :all]
   [korma.core :refer :all]
   [clojure.java.jdbc :as sql]
   ))

(def dbspec (postgres {:db "workspace"
                       :user "postgres"
                       ;; :password "dbpass"
                       }))

(defdb db dbspec)

(defn create-tables []
  (sql/create-table
   "factoid"
   [:id "IDENTITY" "NOT NULL" "PRIMARY KEY"]
   [:created_by "VARCHAR(255)"]
   [:fact "VARCHAR(255)"]
   [:answer "VARCHAR"]
   [:created_on "TIMESTAMP" "NOT NULL" "DEFAULT CURRENT_TIMESTAMP"])
  (sql/do-commands "CREATE INDEX FACTIDX ON factoid(fact)")))
