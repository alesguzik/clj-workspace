(ns workspace
  (:require
   [me.raynes.conch :as sh]
   [clj-ns-browser.sdoc :refer [sdoc]]))

(defn -main [& args]
  (println "Hello world")
  (sdoc))
