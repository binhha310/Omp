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
                              (drop start)
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
        old-row (nth matrix edit-num (take column (repeat :space)))
        new-row (insert-vector old-row start sub)]
    (if (< edit-num (count matrix))
      (assoc matrix edit-num new-row)
      (conj matrix new-row))))

(defn count-day! [i]
  (let [start (t/date (t/beginning i))
        end (t/date (t/end i))
        dates (iterate t/inc start)]
    (+ 1 (count (take-while (partial not= end) dates)))))

(defn event-decor
  ([interval] (event-decor interval (t/date (t/beginning interval))))
  ([interval date] (lazy-seq
                    (cons (t.i/relation interval date) (event-decor interval (t/inc date))))))

(defn same-month? [year month date]
  (and (= year (-> date t/year t/int))
       (= month (-> date t/month t/int))))

(defn todo-decor-month [matrix year month coll]
  (let [same-month? (partial same-month? year month)
        decoration [:during]
        repeat-todo (fn [matrix positions]
                      (if (seq positions)
                        (let [pos (first positions)]
                          (recur
                           (gravity matrix decoration pos :space)
                           (rest positions)))
                        matrix))
        period {:no [0 :days]
                :day [1 :days]
                :week [7 :days]
                :month [1 :months]
                :year [1 :years]}]
    (loop [matrix matrix
           coll coll]
      (if (seq coll)
        (let [todo (first coll)
              i (:time todo)
              re (:repeat todo)
              get-period #(t/>> % (apply t/new-period (re period)))
              positions (if (= re :no)
                          [(t/day-of-month i)]
                          (map t/day-of-month
                               (take-while same-month? (iterate get-period i))))
              new-matrix (repeat-todo matrix positions)]
          (recur new-matrix (rest coll)))
        matrix))))

(defn event-decor-month [matrix coll]
  (if (seq coll)
    (let [i (:time (first coll))
          decoration (take-while (partial not= :precedes) (event-decor i))
          position (-> i t/beginning t/day-of-month)
          new-matrix (gravity matrix decoration position :space)]
      (recur new-matrix (rest coll)))
    matrix))

(def data
  {:event
   [{:time i1 :name "i1"}
    {:time i2 :name "i2"}
    {:time i3 :name "i3"}
    {:time i4 :name "i4"}]
   :todo
   [{:time (t/date "2022-05-15") :name "t1" :repeat :no}
    {:time (t/date "2022-05-24") :name "t2" :repeat :week}]})

(let [month (u/month-dates 2022 5)
      matrix (vector (take (count month) (repeat :space)))]
  (-> matrix
      (event-decor-month (:event data))
      (todo-decor-month 2022 5 (:todo data))))
