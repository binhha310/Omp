(ns app.event
  (:require
   [app.utils :as u]
   [tick.core :as t]
   [tick.alpha.interval :as t.i]))

;; (def i1 (t.i/new-interval
;;          (t/date-time "2022-05-15T12:00")
;;          (t/date-time "2022-05-17T12:00")))

;; (def i2 (t.i/new-interval
;;          (t/date-time "2022-05-13T12:00")
;;          (t/date-time "2022-05-18T12:00")))

;; (def i3 (t.i/new-interval
;;          (t/date-time "2022-05-12T12:00")
;;          (t/date-time "2022-05-14T12:00")))

;; (def i4 (t.i/new-interval
;;          (t/date-time "2022-05-11T12:00")
;;          (t/date-time "2022-05-19T12:00")))

(def decoration-color
  {:space "transparent"
   :event "#50fa7b"
   :todo "#ffb86c"})

(def periods
  {:no [0 :days]
   :day [1 :days]
   :week [7 :days]
   :month [1 :months]
   :year [1 :years]})

(defn- insert-vector [a start b]
  (let [length (count a)
        pos (- start 1)
        head (take pos a)
        tail (take-last (- length (count b) pos) a)]
    (into [] (take length (concat head b tail)))))

(defn- first-space [matrix start len space?]
  (let [matter? (complement space?)
        space-coll? (complement #(some matter? %))
        contain-space? (fn [row]
                         (->> row
                              (drop (- start 1))
                              (take len)
                              space-coll?))]
    (->> matrix
         (map contain-space?)
         (take-while false?)
         count)))

(defn gravity [matrix sub start space-val]
  (let [matrix (if (vector? matrix) matrix (vec matrix))
        column (count (peek matrix))
        len (count sub)
        edit-num (first-space matrix start len (partial = space-val))
        old-row (nth matrix edit-num (take column (repeat space-val)))
        new-row (insert-vector old-row start sub)]
    (if (< edit-num (count matrix))
      (assoc matrix edit-num new-row)
      (conj matrix new-row))))

(defn decor
  ([type] [{:type type :decor :during}])
  ([type interval] (decor type interval (t/date (t/beginning interval))))
  ([type interval date] (lazy-seq
                         (take-while
                          #(not= :precedes (:decor %))
                          (cons {:type type :decor (t.i/relation interval date)}
                                (decor type interval (t/inc date)))))))

(defn same-month? [year month date]
  (and (= year (-> date t/year t/int))
       (= month (-> date t/month t/int))))

(defn- date->> [date period]
  (t/>> date (apply t/new-period
                    (period periods))))

(defn- interval->> [interval period]
  (t.i/new-interval
   (date->> (t/beginning interval) period)
   (date->> (t/end interval) period)))

(defn repeat-handler [matrix decoration positions]
  (loop [matrix matrix
         positions positions]
    (if (seq positions)
      (let [pos (first positions)]
        (recur
         (gravity matrix decoration pos :space)
         (rest positions)))
      matrix)))

(defn- decor-month-factory [decor-type relevant? timeshift position matrix year month coll]
  (let [relevant? (partial relevant? year month)]
    (loop [matrix matrix
           coll coll]
      (if (seq coll)
        (let [item (first coll)
              time (:time item)
              decoration (if (= decor-type :todo)
                           (decor decor-type)
                           (decor decor-type time))
              repeat (:repeat item)
              next-time #(timeshift % repeat)
              positions (if (= repeat :no)
                          [(position time)]
                          (->> time
                               (iterate next-time)
                               (take-while relevant?)
                               (map position)))
              new-matrix (repeat-handler matrix decoration positions)]
          (recur new-matrix (rest coll)))
        matrix))))

;; (def data
;;   {:event
;;    [{:time i1 :name "i1" :repeat :no}
;;     {:time i2 :name "i2" :repeat :week}
;;     {:time i3 :name "i3" :repeat :no}
;;     {:time i4 :name "i4" :repeat :no}]
;;    :todo
;;    [{:time (t/date "2022-05-15") :name "t1" :repeat :no}
;;     {:time (t/date "2022-05-24") :name "t2" :repeat :day}]})

(def todo-decor-month
  (let [relevant? (fn [year month time]
                    (same-month? year month time))
        position t/day-of-month
        timeshift date->>
        decor-type :todo]
    (partial decor-month-factory decor-type relevant? timeshift position)))

(def event-decor-month
  (let [relevant? (fn [year month time]
                    (or (same-month? year month (t/beginning time))
                        (same-month? year month (t/end time))))
        position #(-> % t/beginning t/day-of-month)
        timeshift interval->>
        decor-type :event]
  (partial decor-month-factory decor-type relevant? timeshift position)))

(defn decor-month [year month data]
  (let [month-seq (u/month-dates year month)
        matrix (vector (take (count month-seq) (repeat :space)))]
    (-> matrix
        (event-decor-month year month (:event data))
        (todo-decor-month year month (:todo data)))))

(defn decorator [type]
  (let [color (type decoration-color)]
    {:space {:color color}
     :overlapped-by {:startingDay true :endingDay false :color color}
     :contains {:startingDay false :endingDay false :color color}
     :overlaps {:startingDay false :endingDay true :color color}
     :during {:startingDay true :endingDay true :color color}}))

(defn format [year month data]
  (let [transform (fn [data]
                    (if (= data :space)
                      (:space (decorator data))
                      ((:decor data) (decorator (:type data)))))
        row-trans #(map transform %)
        decored (decor-month year month data)]
    (map row-trans decored)))

(defn vendor-format [year month data]
  (let [transpose (fn [matrix] (apply map vector matrix))
        month-dates (map #(-> % t/format keyword)
                         (u/month-dates year month))
        data (map
              (partial assoc {} :periods)
              (transpose (format year month data)))]
    (apply assoc {} (interleave month-dates data))))
