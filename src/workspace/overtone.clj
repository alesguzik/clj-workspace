(ns workspace.overtone
  (:use [overtone.live]
        [overtone.synth.stringed]
        [overtone.inst.sampled-piano])
  (:require [clojure.string :as str]
            [shadertone.tone :as t]))

(def server (osc-server 44100 "osc-clj"))

(def gtr (guitar))
(def snare (sample (freesound-path 26903)))
(def kick (sample (freesound-path 2086)))
(def close-hihat (sample (freesound-path 802)))
(def open-hihat (sample (freesound-path 26657)))

(defn try-parse-int [str]
  (try (Integer/parseInt str) (catch NumberFormatException e nil)))

(defn listener [msg]
  (println msg)
  (let [path (:path msg)
        args (:args msg)
        value (first args)
        active-note (try-parse-int (str/replace-first path "/1/push" ""))]
    (if (> value 0.5) (sampled-piano (+ (note :C#4) active-note)))))

(osc-listen server (fn [msg] (listener msg)) :debug)
#_(osc-rm-listener server :debug)



(doseq [n (chord :E4 :minor)]
  (sampled-piano n))
(sampled-piano 42)
(note :C#4)
(demo (gtr 40))
(guitar-strum gtr :Gsus4 :down 0.15)

(map #(do (guitar-strum gtr % :down 0.15) (Thread/sleep 1000))
     [:C :Em :F :G :G])

(snare)
(do
  (guitar-strum gtr :E :down 1.25)
  (guitar-strum gtr :A :up 0.25))
(guitar-strum gtr :G :down 0.5)
(guitar-strum gtr [-1 -1 -1 -1 -1 -1])

(ctl gtr
     :pre-amp 5.0 :distort 0.76
     :lp-freq 2000 :lp-rq 0.25
     :rvb-mix 0.5 :rvb-room 0.7 :rvb-damp 0.4)

(t/start "src/workspace/quasicrystal.glsl"
         :width 800 :height 800
         :title "Quasicrystal")

(demo 15 (mix (sin-osc [(mouse-x 20 20000 EXP)
                        (mouse-y 20 20000 EXP)])))

(kick)
(close-hihat)
(open-hihat)

42
