(ns app.event
  (:require
   [tick.core :as t]
   [tick.alpha.interval :as t.i]))

(defn- include? [interval date]
  (case (t.i/relation interval date)
    :preceded-by false
    :met-by false
    :meets false
    :precedes false
    true))

(defn- no-lap? [i1 i2]
  (case (t.i/relation i1 i2)
    :preceded-by true
    :met-by true
    :meets true
    :precedes true
    false))

(def i1 (t.i/new-interval
               (t/date-time "2022-05-15T12:00")
               (t/date-time "2022-05-17T12:00")))


(def i2 (t.i/new-interval
                (t/date-time "2022-05-13T12:00")
                (t/date-time "2022-05-18T12:00")))

(def i3 (t.i/new-interval
                (t/date-time "2022-05-12T12:00")
                (t/date-time "2022-05-14T12:00")))

(def i4 (t.i/new-interval
                (t/date-time "2022-05-11T12:00")
                (t/date-time "2022-05-19T12:00")))

(defn- decor-indexes [coll]
  (for [x (range (count coll))
    :let [val (nth coll x)
          lap? (complement (partial no-lap? val))
          prev (take x coll)]]
    (count (take-while lap? prev))))

(defn- after-or-overlap? [d1 d2]
  (case (t.i/relation (t/end d1) (t/end d2))
    :finished-by true
    :overlaps true
    :meets true
    :precedes true
    false))

(defn- interval-seq [i & is]
  (let [sort-latest (comparator (fn [x y] (after-or-overlap? x y)))
        sorted (sort sort-latest (list* i is))
        b (first sorted)
        e (last sorted)]

    (t/range
     (t/<< (t/beginning b) (t/new-duration 1 :days))
     (t/>> (t/end e) (t/new-duration 1 :days))
     (t/new-duration 1 :days))))

(defn- period-decorator [interval date]
  {:startingDay (case (t.i/relation interval date)
                  :overlapped-by true
                  :started-by true
                  false)
   :endingDay (case (t.i/relation interval date)
                :overlaps true
                :finished-by true
                false)
   :color "#000000"})

(defn- trans-decorator []
  {:color "transparent"})

(defn- conj-vector [v pos value int-value]
  (let [in (- (+ 1 pos) (count v))]
    (if (> in 0)
      (conj (apply conj
                   (vec v)
                   (repeat (- in 1) int-value))
            value)
      (assoc v pos value))))

(defn- update-decor-date [decor-map pos interval date]
  (if (include? interval date)
    (let [period (period-decorator interval date)
          trans (trans-decorator)]
      (update-in decor-map
                 [(keyword date) :periods]
                 #(conj-vector % pos period trans)))
      decor-map))

(defn- update-decor-date-coll [m coll indexes date]
  (let [coll (partition 2 (interleave indexes coll))
        f #(update-decor-date % (first %2) (last %2) date)]
    (reduce f m coll)))

(defn- count-day [i]
  (-> (t/range
       (t/beginning i)
       (t/end i)
       (t/new-duration 1 :days))
      count
      inc))

(def sort-by-length #(sort-by count-day > %))

(defn- update-decor-interval [m coll]
  (let [coll (sort-by-length coll)
        indexes (decor-indexes coll)
        dates (apply interval-seq coll)]
    (->> (map t/date dates)
         (map t/format)
         (reduce #(update-decor-date-coll % coll indexes %2) m))))

;;(decor-indexes (sort-by-length [i1 i2 i3 i4]))

(def update-decor (memoize update-decor-interval))
