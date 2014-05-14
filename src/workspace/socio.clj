(ns workspace.socio
  (:require
   [workspace.core :as core]
   [org.httpkit.client :as http]
   [clojure.string :as str]
   [clojure.data.json :as json]
   [clojure.data.csv  :as csv]
   [clojure.tools.reader.edn :as edn]
   [net.cgrand.enlive-html :as html]
   [me.raynes.conch :as sh]
   ))

(defn pvtest-get-results [user]
  @(http/post "http://pvtest.wsocial.ru/apps/ierarhia/post.php"
              {:form-params {"q" "resultf"
                             "iduser" 3885655
                             "uid" user}}))
(defn pvtest-get-results-2 [user]
  @(http/post "http://pvtest.wsocial.ru/apps/ierarhia/post.php"
              {:form-params {"q" "testresult"
                             "uid" user}}))
(def res (-> )

(defn result-title [result]
  (-> result first :content first))

(defn result-stats [result]
  (-> result second :attrs :title))

(mapv #(->> % :content ((juxt result-title result-stats)))
 (-> (pvtest-get-results-2 4335128)
     :body
     (#(java.io.StringReader. %))
     html/html-resource
     (html/select [:.ierstr [:td (html/nth-child 3)]])))

(defn socio2-cookie []
  (-> (core/config) :socio2 :cookie))

(defn socio2 [city tim gender]
  ;; {:city 282 :stype "duma" :gender 1}
  (->> @(http/get "http://vk1.mysocio.ru/city/dual"
                  {:headers
                   {"Accept" "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
                    "Cookie" (socio2-cookie)
                    "Host" "vk1.mysocio.ru"}
                   :query-params {:city city :stype (name tim) :gender gender}})
       :body
       str/split-lines
       (filter #(.contains % "duals_queue"))
       first
       (#(str/replace-first % "var duals_queue = " ""))
       edn/read-string
       butlast
       (mapv first)))

(socio2 282 :duma 1)
