(ns status-im.utils.random
  (:require [re-frame.core :as re-frame]
            [status-im.utils.datetime :as datetime]
            ["chance" :as Chance]))

(def chance (Chance.))

(defn guid []
  (.guid chance))

(defn id []
  (str (datetime/timestamp) "-" (.guid chance)))

(defn rand-gen
  [seed]
  (dependencies/Chance. seed))

(defn seeded-rand-int
  [gen n] (.integer gen #js {:min 0 :max (dec n)}))

(defn seeded-rand-nth
  [gen coll]
  (nth coll (seeded-rand-int gen (count coll))))

(re-frame/reg-cofx
 :random-guid-generator
 (fn [coeffects _]
   (assoc coeffects :random-guid-generator guid)))

(re-frame/reg-cofx
 :random-id-generator
 (fn [coeffects _]
   (assoc coeffects :random-id-generator id)))
