(ns app.utils
  (:require
   [tick.core :as t]
   [tick.alpha.interval :as t.i]
   [clojure.spec.alpha :as s]))

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

(def periods
  {:no [0 :days]
   :day [1 :days]
   :week [7 :days]
   :month [1 :months]
   :year [1 :years]})

(defn date->> [date period]
  (t/>> date (apply t/new-period
                    (period periods))))

(defn interval->> [interval period]
  (t.i/new-interval
   (date->> (t/beginning interval) period)
   (date->> (t/end interval) period)))

(defn month-dates [year month]
  (letfn [(month?
            [month date]
            (let [next-month (if (number? month)
                               (inc month)
                               (inc (t/int month)))]
              (and (date-compare >= month date)
                   (date-compare < next-month date))))]
    (filter (partial month? month) (year-dates year))))

(comment (defn first-calview-monday [year month]
  (let [fst (t/new-date year month 1)
        weekday (->> fst
                     t/day-of-week
                     t/int)
        fst-monday (last (take weekday (iterate t/dec fst)))]
    fst-monday)))

(defn first-day-of-month [year month]
  (t/new-date year month 1))

(defn last-day-of-month [year month]
  (let [next-month (if (= month 12) 1 (+ month 1))
        f (if (= next-month 1) (t/new-date (+ year 1) 1 1) (t/new-date year next-month 1))]
    (t/<< f 1)))

(defn first-calview-monday [year month]
  (let [f (first-day-of-month year month)
        n (t/<< f (t/day-of-month f))
        monday (t/>> n (- 8 (t/int (t/day-of-week (t/>> n 7)))))]
    (if (> (t/day-of-month monday) 1) (t/<< monday 7) monday)))

(comment (defn last-calview-sunday [year month]
  (let [fst-next (t/dec (t/new-date year (inc month) 1))
        weekday (->> fst-next
                     t/day-of-week
                     t/int)
        lst-sunday (last (take (- 8 weekday) (iterate t/inc fst-next)))]
    lst-sunday)))

(defn last-calview-sunday [year month]
  (let [next-month (if (= month 12) 1 (+ month 1))
        monday (if (= next-month 1) (first-calview-monday (+ year 1) 1) (first-calview-monday year next-month))
        sunday (t/<< monday 1)]
    (if (< (t/day-of-month sunday) (t/day-of-month (last-day-of-month year month))) (t/>> sunday 7) sunday)))

;;Return month's sequence of dates
(defn calendar-dates [year month]
  (let [f (first-calview-monday year month)
        l (last-calview-sunday year month)]
    (take-while (partial t/>= l) (iterate t/inc f))))

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

(defn- intermediate [beginning end]
  (let [from-beginning (iterate t/inc beginning)
        different (count (take-while (partial > end) from-beginning))]
    different))

(defn between [beginning end]
  (let [beginning (t/date beginning)
        end (t/date end)]
    (if (t/> beginning end)
      (- (intermediate end beginning))
      (intermediate beginning end))))

(defn today []
  (t/today))

(defn tomorrow []
  (-> (t/now)
      (t/>> (t/new-duration 1 :days))))

(defn next-hour [datetime]
  (-> datetime
      (t/>> (t/new-duration 1 :hours))))

(defn ->js-dt [date-time]
  (-> date-time
      t/inst))

(def now t/now)
(def get-time t/time)

(defn set-time [datetime time]
  (-> (t/date datetime)
      (t/at time)))

(defn year-month
  ([] (t/new-year-month))
  ([year month] (t/new-year-month year month)))

(defn month
  ([] (t/int (t/month)))
  ([year-month] (-> year-month
                    t/month
                    t/int)))

(defn year
  ([] (t/int (t/year)))
  ([year-month] (-> year-month
                    t/year
                    t/int)))

(def get-date t/date)

(defn interval
  [starting ending]
  (t.i/new-interval
   (t/date-time starting)
   (t/date-time ending)))

(def repeat-string
  {:no "No"
   :day "Daily"
   :week "Weekly"
   :month "Monthly"
   :year "Annual"})
