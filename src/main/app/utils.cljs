(ns app.utils
  (:require
   [tick.core :as t]
   [tick.alpha.interval :as t.i]))

(defn year-dates [year]
  (let [year (if (number? year) (t/year year) year)
        one-day (t/new-period 1 :days)
        next-year (t/inc year)]
    (t/range
     (t/new-date year 1 1)
     (t/new-date next-year 1 1)
     one-day)))

(defn date-compare [f month date]
  (let [a (if (number? month) month (t/int month))
        b (t/int (t/month date))]
    (if (f b a) true false)))

(defn month-dates [year month]
  (letfn [(month?
            [month date]
            (let [next-month (if (number? month)
                               (inc month)
                               (inc (t/int month)))]
              (and (date-compare >= month date)
                   (date-compare < next-month date))))]
    (filter (partial month? month) (year-dates year))))

(defn include? [interval date]
  (case (t.i/relation interval date)
    :preceded-by false
    :met-by false
    :meets false
    :precedes false
    true))

(defn no-lap? [i1 i2]
  (case (t.i/relation i1 i2)
    :preceded-by true
    :met-by true
    :meets true
    :precedes true
    false))

(defn after-or-overlap? [d1 d2]
  (case (t.i/relation (t/end d1) (t/end d2))
    :finished-by true
    :overlaps true
    :meets true
    :precedes true
    false))

(defn interval-seq [i & is]
  (let [sort-latest (comparator (fn [x y] (after-or-overlap? x y)))
        sorted (sort sort-latest (list* i is))
        b (first sorted)
        e (last sorted)]

    (t/range
     (t/<< (t/beginning b) (t/new-duration 1 :days))
     (t/>> (t/end e) (t/new-duration 1 :days))
     (t/new-duration 1 :days))))

(defn count-day [i]
  (let [start (t/date (t/beginning i))
        end (t/date (t/end i))
        dates (iterate t/inc start)]
    (+ 1 (count (take-while (partial not= end) dates)))))
