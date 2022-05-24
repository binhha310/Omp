(ns app.new-event
  (:require
   [app.utils :as u]
   [tick.core :as t]
   [tick.alpha.interval :as t.i]))

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

(defn beginning [event]
  (t/beginning (:event event)))

(defn end [event]
  (t/end (:event event)))

;; (defn- relation [i1 i2]
;;   (let [i1 (:event i1)
;;         i2 (:event i2)]
;;     (t.i/relation i1 i2)))

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

(defn- after-or-overlap? [d1 d2]
  (case (t.i/relation (t/end d1) (t/end d2))
    :finished-by true
    :overlaps true
    :meets true
    :precedes true
    false))

(defn- decor-indexes [coll]
  (for [x (range (count coll))
        :let [val (nth coll x)
              lap? (complement (partial no-lap? (:event val)))
              prev (map :event (take x coll))]]
    (count (take-while lap? prev))))

(defn- interval-seq [i & is]
  (let [sort-latest (comparator (fn [x y] (after-or-overlap? x y)))
        sorted (sort sort-latest (list* i is))
        b (first sorted)
        e (last sorted)]

    (t/range
     (t/<< (t/beginning b) (t/new-duration 1 :days))
     (t/>> (t/end e) (t/new-duration 1 :days))
     (t/new-duration 1 :days))))

(def decoration-color
  {:trans "transparent"
   :event "#50fa7b"
   :todo "#ffb86c"})

(defn- event-decorator [interval date]
  {:startingDay (case (t.i/relation interval date)
                  :overlapped-by true
                  :started-by true
                  false)
   :endingDay (case (t.i/relation interval date)
                :overlaps true
                :finished-by true
                false)
   :color (:event decoration-color)})

(defn- todo-decorator [date]
  {:startingDay true
   :endingDay true
   :color (:todo decoration-color)})

(defn- trans-decorator []
  {:color (:trans decoration-color)})

(defn- conj-vector [v pos value int-value]
  (let [in (- (+ 1 pos) (count v))]
    (if (> in 0)
      (conj (apply conj
                   (vec v)
                   (repeat (- in 1) int-value))
            value)
      (assoc v pos value))))

(defn- count-day [i]
  (-> (t/range
       (beginning i)
       (end i)
       (t/new-duration 1 :days))
      count
      inc))

(defn- update-decor-date [decor-map pos interval date]
  (let [interval (:event interval)]
    (if (include? interval date)
      (let [period (event-decorator interval date)
            trans (trans-decorator)]
        (-> decor-map
            (update-in [(keyword date) :periods] #(conj-vector % pos period trans))
            (assoc-in [(keyword date) :accessibilityLabel] "test")))
      decor-map)))

(defn- update-decor-date-coll [m coll indexes date]
  (let [coll (partition 2 (interleave indexes coll))
        f #(update-decor-date % (first %2) (last %2) date)
        result (reduce f m coll)]
    result))

(def sort-by-length #(sort-by count-day > %))

(defn- update-decor-interval [m coll]
  (let [coll (sort-by-length coll)
        indexes (decor-indexes coll)
        dates (apply interval-seq (map :event coll))]
    (->> (map t/date dates)
         (map t/format)
         (reduce #(update-decor-date-coll % coll indexes %2) m))))

(def coll [{:event i1 :name "i1"}
           {:event i2 :name "i2"}
           {:event i3 :name "i3"}
           {:event i4 :name "i4"}])

;; take-while (partial not= :precedes) (decor i1))
;; (def i5 (t.i/new-interval
;;          (t/date-time "2022-05-15T01:00")
;;          (t/date-time "2022-05-15T23:00")))
;; (take-while (partial not= :precedes) (decor i5))
;; (update-decor-interval {} coll)
;; ;; (decor-indexes (sort-by-length coll))

(def matrix [[2 0 1]
             [0 0 0]
             [4 5 0]])

(def matrix2 '('(0 1 1)
               '(4 1 0)
               '(0 1 0)))

(defn- insert-vector [a start b]
  (let [length (count a)
        pos (- start 1)
        head (take pos a)
        tail (take-last (- length (count b) pos) a)]
    (into [] (take length (concat head b tail)))))

(defn- print-and-return [value]
  (do
    (println value)
    value))

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

(defn count-day! [i]
  (let [start (t/date (t/beginning i))
        end (t/date (t/end i))
        dates (iterate t/inc start)]
    (+ 1 (count (take-while (partial not= end) dates)))))

(defn decor
  ([] [:during])
  ([interval] (decor interval (t/date (t/beginning interval))))
  ([interval date] (lazy-seq
                    (take-while (partial not= :precedes)
                    (cons (t.i/relation interval date)
                          (decor interval (t/inc date)))))))

(def periods
  {:no [0 :days]
   :day [1 :days]
   :week [7 :days]
   :month [1 :months]
   :year [1 :years]})

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

(defn decor-month [decor-type relevant? timeshift position matrix year month coll]
  (let [relevant? (partial relevant? year month)]
    (loop [matrix matrix
           coll coll]
      (if (seq coll)
        (let [item (first coll)
              time (:time item)
              decoration (if (= decor-type :todo) (decor) (decor time))
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

(def data
  {:event
   [{:time i1 :name "i1" :repeat :no}
    {:time i2 :name "i2" :repeat :week}
    {:time i3 :name "i3" :repeat :no}
    {:time i4 :name "i4" :repeat :no}]
   :todo
   [{:time (t/date "2022-05-15") :name "t1" :repeat :no}
    {:time (t/date "2022-05-24") :name "t2" :repeat :day}]})

(def todo-decor-month!
  (let [relevant? (fn [year month time]
                    (same-month? year month time))
        position t/day-of-month
        timeshift date->>
        decor-type :todo]
    (partial decor-month decor-type relevant? timeshift position)))

(def event-decor-month!
  (let [relevant? (fn [year month time]
                    (or (same-month? year month (t/beginning time))
                        (same-month? year month (t/end time))))
        position #(-> % t/beginning t/day-of-month)
        timeshift interval->>
        decor-type :event]
  (partial decor-month decor-type relevant? timeshift position)))

(let [month (u/month-dates 2022 5)
      matrix (vector (take (count month) (repeat :space)))]
  (-> matrix
      (event-decor-month! 2022 5 (:event data))
      (todo-decor-month! 2022 5 (:todo data))
      ))
