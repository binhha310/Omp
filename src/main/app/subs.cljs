(ns app.subs
  (:require
   [app.utils :refer (month year)]
   [app.calendar.marking :refer (month-decoration)]
   [re-frame.core :refer (subscribe reg-sub)]))

(reg-sub
 :data!
 (fn [db _]
   (:data db)))

(reg-sub
 :month
 (fn [db _]
   (:month db)))

(reg-sub
 :data
 (fn [query _]
   (subscribe [:data!]))
 (fn [data query _]
   data))

(reg-sub
 :marking
 (fn [query _]
   [(subscribe [:data!])
    (subscribe [:month])])
 (fn [[data year-month] query _]
   (month-decoration (year year-month) (month year-month) data)))
