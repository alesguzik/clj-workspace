(ns workspace.util)

(def ^:dynamic debug false)

(defmacro p [& body]
  `(let [result# ~body]
     (println result#)
     result#))

(defn debug-print [& args]
  (if debug
    (apply println args)))

(defmacro with-debug [& body]
  `(binding [debug true]
     ~@body))

(defn find-first [pred coll] (first (filter pred coll)))
(defn find-by [k v coll] (find-first #(= (k %) v) coll))
(defn map-hash [f a-map]
  (into {} (map (fn [[k v]] (f k v)) a-map)))

(defn filter-hash [f a-map]
  (into {} (filter (fn [[k v]] (f k v)) a-map)))

(defn swap-kv [a-map]
  (map-hash #(vector %2 %) a-map))

(defn map-vals [f hash]
  (into {} (map (fn [[k v]] [k (f v)]) hash)))

(defmacro catched [f & body]
  `(try ~body
        (catch clojure.lang.ExceptionInfo e
          (~f (.getData e)))))

(defn rnd [min max]
  (+ min
     (rand-int (- max min))))

(defn rand256 [] (rnd 0 255))

(defn now-ms []
  (System/currentTimeMillis))

(defn now []
  (java.util.Date.))
