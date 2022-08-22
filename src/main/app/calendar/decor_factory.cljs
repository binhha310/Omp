(ns app.calendar.decor-factory
  (:require
   [tick.core :as t]
   [tick.alpha.interval :as t.i]))

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

(defn- gravity [matrix subvec start space-val]
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

(defn marking-repeat [relevant? timeshift item]
  (let [not-relevant? (complement relevant?)
        {:keys [time repeat]} item
        next-time #(timeshift % repeat)]
    (if (= repeat :no)
      (when (relevant? time) [time])
      (->> time
           (iterate next-time)
           (drop-while not-relevant?)
           (take-while relevant?)))))

(defn- repeat-handler [matrix decoration positions]
  (loop [matrix matrix
         positions positions]
    (if (seq positions)
      (let [pos (first positions)]
        (recur
         (gravity matrix decoration pos :space)
         (rest positions)))
      matrix)))

(defn- decor
  ([type] [{:type type :decor :during}])
  ([type interval] (decor type interval (t/date (t/beginning interval))))
  ([type interval date] (lazy-seq
                         (take-while
                          #(not= :precedes (:decor %))
                          (cons {:type type :decor (t.i/relation interval date)}
                                (decor type interval (t/inc date)))))))

(defn decor-factory
  [decor-type relevant? timeshift position]
  (fn [first-day last-day coll matrix]
    (let [this-month? (partial relevant? first-day last-day)
          position (partial position first-day)
          markings (partial marking-repeat this-month? timeshift)]
      (loop [matrix matrix
             coll coll]
        (if (seq coll)
          (let [item (first coll)
                time (:time item)
                decoration (if (= decor-type :todos)
                             (decor decor-type)
                             (decor decor-type time))
                positions (map position (markings item))
                new-matrix (repeat-handler matrix decoration positions)]
            (recur new-matrix (rest coll)))
          matrix)))))
