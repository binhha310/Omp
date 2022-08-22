(ns app.calendar.marking
  (:require
   [app.utils :as u :refer (date->> interval->>)]
   [app.calendar.todo :refer (todo-decor)]
   [app.calendar.event :refer (event-decor)]
   [app.views.themes
    :refer (dracula)
    :rename {dracula theme}]
   [tick.alpha.interval :as t.i]))

(def decoration-color
  {:space "transparent"
   :events (:cyan theme)
   :todos (:orange theme)})

(defn decor-month [year month data]
  (let [month-seq (u/calendar-dates year month)
        matrix (-> month-seq
                   count
                   (take (repeat :space))
                   vector)
        first-day (first month-seq)
        last-day (last month-seq)]
    (->> matrix
         (event-decor first-day last-day (:events data))
         (todo-decor first-day last-day (:todos data)))))

;;(decor-month 2022 5 data)
(defn decorator [type]
  (let [color (type decoration-color)]
    {:space {:color color}
     :meets {:color "transparent"}
     :met-by {:color "transparent"}
     :overlapped-by {:startingDay true :endingDay false :color color}
     :started-by {:startingDay true :endingDay false :color color}
     :finished-by {:startingDay false :endingDay true :color color}
     :contains {:startingDay false :endingDay false :color color}
     :overlaps {:startingDay false :endingDay true :color color}
     :during {:startingDay true :endingDay true :color color}}))

(defn month-decoration [year month data]
  (let [transform (fn [data]
                    (if (= data :space)
                      (:space (decorator data))
                      ((:decor data) (decorator (:type data)))))
        row-trans #(map transform %)
        decored (decor-month year month data)]
    (map row-trans decored)))
