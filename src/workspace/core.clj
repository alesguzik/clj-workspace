(ns workspace.core
  (:require
   [clojure.string :as str]
   [org.httpkit.client :as http]
   [clojure.tools.reader.edn :as edn]
   [clojure.data.json :as json]
   [clojure.data.xml  :as xml ]
   [clojure.data.csv  :as csv ]
   [clojure.data.zip  :as zip ]
   [me.raynes.conch :as sh]
))

(defn config []
  (edn/read-string (slurp "config.edn")))
