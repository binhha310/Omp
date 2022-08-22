(ns app.calendar.todo
  (:require
   [app.calendar.decor-factory :refer (decor-factory)]
   [tick.core :refer (<= >=)]
   [app.utils :refer (date->> between)]))

(defn- relevant? [first last date]
  (and (<= date last)
       (>= date first)))

(def todo-decor
  (let [position (fn [start date] (between start date))
        timeshift date->>
        decor-type :todos]
    (decor-factory decor-type relevant? timeshift position)))
