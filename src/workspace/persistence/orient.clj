(ns workspace.persistence.orient
  (:require
   [clj-orient
    [graph :as g]
    [core :as o]]))
;; (o/create-db! "memory:workspace")
;; (o/create-db! "plocal:workspace")
;; (o/create-db! "remote:workspace")

;; (def db
;;   (g/open-graph-db! "memory:workspace" "admin" "admin"))

;; (def db2
;;   (g/open-graph-db! "local:localhost/workspace" "admin" "admin"))
(g/open-graph-db! "remote:localhost/workspace" "admin" "admin")
