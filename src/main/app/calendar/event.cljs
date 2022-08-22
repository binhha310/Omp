(ns app.calendar.event
  (:require
   [app.calendar.decor-factory :refer (decor-factory)]
   [tick.core :refer (<= >=) :as t]
   [app.utils :refer (interval->> between)]))

(defn relevant? [first last interval]
  (let [beginning (-> interval t/beginning t/date)
        end (-> interval t/end t/date)]
    (or (and (t/<= beginning last)
             (t/>= beginning first))
        (and (t/<= end last)
             (t/>= end first)))))

(def event-decor
  (let [position (fn [start interval] (between start (t/beginning interval)))
        timeshift interval->>
        decor-type :events]
    (decor-factory decor-type relevant? timeshift position)))
