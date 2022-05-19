(ns app.utils)

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
