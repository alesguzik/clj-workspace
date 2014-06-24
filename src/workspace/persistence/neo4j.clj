(ns workspace.persistence.neo4j
  (:require
   [clojure.string :as str]
   [workspace.util :as u]
   [clojurewerkz.neocons.rest :as neo]
   [clojurewerkz.neocons.rest
    [nodes :as node]
    [relationships :as rel]
    [cypher :as cypher]
    [paths :as path]
    [labels :as label]]))

(def conn (neo/connect "http://localhost:7474/db/data/"))

(defn property-key [obj]
  (str/replace (name obj) "-" "_"))

(defn property-value [obj]
  (pr-str (if (keyword? obj) (name obj) obj)))

(defn property-str [hash]
  (let [pairs (for [[k v] hash]
                (str (property-key k) ":" (property-value v)))
        inner (reduce #(str % ", " %2) pairs)]
    (str "{" inner "}")))

(defmacro with-bad-query [& body]
  `(u/catched #(-> % :object :body println) ~@body))

(defn root-for-type [type]
  (let [node-and-return
        (str "(r:Root" (property-str {:type type}) ") return r")]
    (if (empty? (cypher/tquery conn (str "match " node-and-return)))
                (cypher/tquery conn (str "create" node-and-return)))))

(defn merge-node [node-type matched-params set-params]
  (let [query (str "merge (n:" (name node-type) (property-str matched-params) ")\n"
                   "set n = {set_params}\n"
                   "return n\n")]
    (u/debug-print query)
    (cypher/tquery conn query {:set_params set-params})))

(defn merge-rel [n1-type n1-data n2-type n2-data rel-type rel-data]
  (let [query (str
               "merge (n1:" (name n1-type) (property-str n1-data) ")\n"
               "merge (n2:" (name n2-type) (property-str n2-data) ")\n"
               "merge (n1)-[rel:" (name rel-type)"]->(n2)\n"
               "set rel = {rel_data}\n"
               "return n1, rel, n2\n")]
    (u/debug-print query)
    (cypher/tquery conn query {:rel_data rel-data})))
