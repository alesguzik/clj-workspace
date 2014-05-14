(ns workspace.core
  (:require
   [clojure.string :as string]
   [org.httpkit.client :as http]
   [clojure.tools.reader.edn :as edn]
   [clojure.data.json :as json]
   [clojure.data.xml  :as xml ]
   [clojure.data.csv  :as csv ]
   [clojure.data.zip  :as zip ]
   [me.raynes.conch :as sh]

   [clj-ns-browser.sdoc :refer [sdoc]]))

(defn config []
  (edn/read-string (slurp "config.edn")))

(sdoc)
