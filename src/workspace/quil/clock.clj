(ns workspace.quil.clock
  (:require
   [workspace.util :as u]
   [workspace.quil.util :as qu]
   [quil.core :as q]))

(defn setup-clock []
  (q/smooth)
  (q/frame-rate 60)
  (q/color-mode :hsb)
  (q/background 42))

(defn draw-clock []
  (q/background 42)
  (q/stroke 255)
  (q/stroke-weight 1)
  (let [w (q/width)
        h (q/height)
        center-x (/ w 2)
        center-y (/ h 2)
        diam (* (min h w) 2/3)
        date (java.util.Date.)
        seconds (.getSeconds date)
        minutes (.getMinutes date)
        hours (.getHours date)]
    (q/translate [center-x center-y])
    (qu/draw-circle [(mod (* (u/now) 1/20) 256) 255 128] [0 0] diam)
    (qu/draw-circle [42] [0 0] 42)
    (qu/draw-circle [0 255 255]   (-> seconds qu/time->angle (qu/polar->rect (* 3/6 diam))) 42)
    (qu/draw-circle [50 255 255]  (-> minutes qu/time->angle (qu/polar->rect (* 2/6 diam))) 50)
    (qu/draw-circle [100 255 128] (-> hours   qu/time->angle (qu/polar->rect (* 1/6 diam))) 100)))

(defonce sketch-clock3
  (q/defsketch clock
   :title "clock"
   :setup setup-clock
   :draw draw-clock
   :size [500 500]))

[(u/now)
(.getTime (java.util.Date.))]
