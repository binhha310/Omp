(ns app.subs
  (:require [re-frame.core :refer (subscribe reg-sub)]))

(reg-sub
 :data!
 (fn [db _]
   (:data db)))

(reg-sub
 :data
 (fn [query _]
   (subscribe [:data!]))
 (fn [data query _]
   data))
