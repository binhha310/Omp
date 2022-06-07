(ns app.marking
  (:require
   [app.utils :as u]
   [tick.core :as t]
   [tick.alpha.interval :as t.i]))

(def decoration-color
  {:space "transparent"
   :events "#50fa7b"
   :todos "#ffb86c"})

(def periods
  {:no [0 :days]
   :day [1 :days]
   :week [7 :days]
   :month [1 :months]
   :year [1 :years]})

(defn- insert-vector [a start b]
  (let [length (count a)
        head (take start a)
        tail (take-last (- length (count b) start) a)]
    (into [] (take length (concat head b tail)))))

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

(defn gravity [matrix subvec start space-val]
  (let [matrix (if (vector? matrix) matrix (vec matrix))
        column (count (peek matrix))
        sub (if (> 0 start)
              (drop (- 1 start) subvec)
              subvec)
        edit-num (first-space matrix start (count sub) (partial = space-val))
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

(defn- decor-month-factory
  [decor-type relevant? timeshift position]
  (fn decoration
    ([year month coll]
     (let [month-seq (u/calendar-dates year month)
           matrix (-> month-seq
                      count
                      (take (repeat :space))
                      vector)]
       (decoration year month coll matrix)))
    ([year month coll matrix]
     (let [dates (u/calendar-dates year month)
           first-day (first dates)
           last-day (last dates)
           relevant? (partial relevant? first-day last-day)]
       (loop [matrix matrix
              coll coll]
         (if (seq coll)
           (let [item (first coll)
                 {:keys [time repeat]} item
                 decoration (if (= decor-type :todos)
                              (decor decor-type)
                              (decor decor-type time))
                 next-time #(timeshift % repeat)
                 position (partial position first-day)
                 positions (if (= repeat :no)
                             (when (relevant? time) [(position time)])
                             (->> time
                                  (iterate next-time)
                                  (drop-while (complement relevant?))
                                  (take-while relevant?)
                                  (map position)))
                 new-matrix (repeat-handler matrix decoration positions)]
             (recur new-matrix (rest coll)))
           matrix))))))

(defn- todo-relevant? [first last date]
  (and (t/<= date last)
       (t/>= date first)))

(def todo-decor-month
  (let [relevant? todo-relevant?
        position (fn [start date] (u/between start date))
        timeshift date->>
        decor-type :todos]
    (partial decor-month-factory decor-type relevant? timeshift position)))

(defn- event-relevant? [first last interval]
  (let [beginning (-> interval t/beginning t/date)
        end (-> interval t/end t/date)]
    (or (and (t/<= beginning last)
             (t/>= beginning first))
        (and (t/<= end last)
             (t/>= end first)))))

(def event-decor-month
  (let [relevant? event-relevant?
        position (fn [start interval] (u/between start (t/beginning interval)))
        timeshift interval->>
        decor-type :events]
    (partial decor-month-factory decor-type relevant? timeshift position)))

(defn decor-month [year month data]
  (->> (event-decor-month year month (:events data))
       (todo-decor-month year month (:todos data))))

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
