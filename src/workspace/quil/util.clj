(ns workspace.quil.util
  (:require
    [workspace.util :as u]
    [quil.core :as q]))

(defn randcolor [] [(u/rand256) (u/rand256) (u/rand256)])

(defn draw-circle [color [x y] diam]
  (apply q/fill color)
  (q/ellipse x y diam diam))

(defn polar->rect [angle radius]
  (map #(* radius %)
    [(q/cos angle)
     (q/sin angle)]))

(defn time->angle [time]
  (+ (* time 2 Math/PI 1/60)
    (* 3/2 Math/PI)))

(defn draw-cell [x y size state]
  (q/push-style)
  (if state (q/fill 255) (q/fill 42))
  (q/rect (* x size) (* y size) size size)
  (q/pop-style))
