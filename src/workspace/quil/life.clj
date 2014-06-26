(ns workspace.quil.life
  (:require
    [workspace.util :as u]
    [workspace.quil.util :as qu]
    [quil.core :as q]))

(defn setup-life []
  (q/smooth)
  (q/frame-rate 5)
  (q/color-mode :hsb)
  (q/background 42))

(defn make-field []
  {})

(def glider
  [[1 2] [2 3] [3 1] [3 2] [3 3]])

(def shifts
  (for [dx [-1 0 1]
             dy [-1 0 1]
             :when (not (and (= dx 0)
                             (= dy 0)))]
         [dx dy]))

(defn neighbours [field]
  (frequencies
   (for [point field
         shift shifts]
     (map + point shift))))

(defn alive-map [field]
  (into {} (map #(vector % true) field)))

(defn next-generation [field]
  (let [alive (alive-map field)]
    (->> (neighbours field)
         (map (fn [[k v]]
                (case v
                  3 k
                  2 (if (alive k) k)
                  nil)))
       (filter identity))))

(defn draw-life []
  (q/background 42)
  (q/stroke 255)
  (q/stroke-weight 1)
  (let [field (nth (iterate next-generation glider)
                    (mod (/ (u/now-ms) 250) 30))]
    (doall
      (for [x (range 20)
            y (range 20)]
        (qu/draw-cell x y 42 nil)))
    (doseq [[x y] field]
      (qu/draw-cell x y 42 true))))

(defonce sketch-life2
  (q/defsketch life
   :title "demo42"
   :setup setup-life
   :draw draw-life
   :size [500 500]))
