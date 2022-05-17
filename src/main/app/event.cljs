(ns app.event
  (:require
   [tick.core :as t]
   [tick.alpha.interval :as t.i]))

;; (defn year-dates [year]
;;   (let [year (if (number? year) (t/year year) year)
;;         one-day (t/new-period 1 :days)
;;         next-year (t/inc year)]
;;     (t/range
;;      (t/new-date year 1 1)
;;      (t/new-date next-year 1 1)
;;      one-day)))

;; (defn date-compare [f month date]
;;   (let [a (if (number? month) month (t/int month))
;;         b (t/int (t/month date))]
;;     (if (f b a) true false)))

;; (defn month-dates [year month]
;;   (letfn [(month?
;;             [month date]
;;             (let [next-month (if (number? month)
;;                                (inc month)
;;                                (inc (t/int month)))]
;;               (and (date-compare >= month date)
;;                    (date-compare < next-month date))))]
;;     (filter (partial month? month) (year-dates year))))

(defn include? [interval date]
  (case (t.i/relation interval date)
    :preceded-by false
    :met-by false
    :meets false
    :precedes false
    true))

;; (def i1 (t.i/new-interval
;;                (t/date-time "2022-05-15T12:00")
;;                (t/date-time "2022-05-17T12:00")))

;; (def i2 (t.i/new-interval
;;                (t/date-time "2022-05-15T12:00")
;;                (t/date-time "2022-05-18T13:00")))

(defn after-or-overlap? [d1 d2]
  (case (t.i/relation d1 d2)
    :finished-by true
    :overlaps true
    :meets true
    :precedes true
    false))

(defn interval-seq [i & is]
  (let [sort-latest (comparator (fn [x y] (after-or-overlap? x y)))
        sorted (sort sort-latest (apply vector i is))
        b (first sorted)
        e (last sorted)]

  (t/range
   (t/beginning b)
   (t/>> (t/end e) (t/new-duration 1 :days))
   (t/new-duration 1 :days))))

(defn period-decorator [interval date]
  {:startingDay (case (t.i/relation interval date)
                  :overlapped-by true
                  :started-by true
                  false)
   :endingDay (case (t.i/relation interval date)
                :overlaps true
                :finished-by true
                false)
   :color "#000000"})

(defn trans-decorator []
  {:color "transparent"})

(defn conj-vector [v pos value int-value]
  (let [in (- (+ 1 pos) (count v))]
    (if (> in 0)
      (conj (apply conj (vec v) (repeat (- in 1) int-value)) value)
      (assoc v pos value))))

(defn update-decor-date [decor-map pos interval date]
  (if (include? interval date)
    (update-in decor-map
               [(keyword date) :periods]
               #(conj-vector % pos (period-decorator interval date) (trans-decorator)))
    decor-map))

(defn update-decor-date-coll [m coll date]
  (let [coll (partition 2 (interleave (range (count coll)) coll))
        f #(update-decor-date % (first %2) (last %2) date)]
    (reduce f m coll)))

(defn update-decor-interval [m coll]
  (let [dates (interval-seq coll)]
    (->> (map t/date dates)
         (map t/format)
         (reduce #(update-decor-date-coll % coll %2) m))))

(defn count-day [i]
  (-> (t/range
       (t/beginning i)
       (t/end i)
       (t/new-duration 1 :days))
      count
      inc))

(def update-decor (memoize update-decor-interval))
(def sort-by-length #(sort-by count-day > %))
