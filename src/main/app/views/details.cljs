(ns app.views.details
  (:require
   [clojure.spec.alpha :as s]
   ["react-native" :as rn]
   ["react-native-paper" :refer (Button)]
   [tick.alpha.interval :as t.i]
   [tick.core :as t]
   [re-frame.core :refer (dispatch-sync dispatch clear-subscription-cache! subscribe)]
   [app.marking :refer (marking-loop event-relevant? todo-relevant?)]
   [app.utils :refer (interval->> calendar-dates)]))

(def styles
  ^js (-> {:hour
           {:borderBottomStyle "solid"
            :borderWidth 1
            :borderColor "rgba(0, 0, 0, 0.1)"
            :height 60
            :margin 0}
           :markingView
           {:position "absolute"
            :flexDirection "row"
            :height "100%"
            :width "85%"
            :backgroundColor "rgba(0, 0, 20, 0.2)"
            :marginLeft "15%"}}
          (clj->js)
          (rn/StyleSheet.create)))

(s/def ::date t/date?)
(s/def ::interval t/interval?)

(defn relation [date interval]
  (when (and (s/valid? ::date date) (s/valid? ::interval interval))
    (t.i/relation date interval)))

(defn has-close-relation? [date interval]
  (#{:overlapped-by
     :starts
     :during
     :contains
     :finishes
     :overlaps} (relation date interval)))

(defn- handle-relation [date interval]
  (let [relation (relation date interval)
        offset #(+ (* 60 (t/hour %)) (t/minute %))
        beginning (if (#{:overlaps :contains} relation)
                    (offset (:tick/beginning interval))
                    0)
        end (if (#{:overlapped-by :contains} relation)
              (offset (:tick/end interval))
              0)]
    [beginning end]))


(defn button-view [i name date]
  (let [[top bottom] (handle-relation date i)
        marginTop top
        marginBottom (if (= 0 bottom) 0 (- 1440 bottom))
        style (->
               {:justifyContent "center"
                :textAlign "center"
                :flex 1
                :marginTop marginTop
                :marginBottom marginBottom}
               (clj->js)
               (rn/StyleSheet.create))]
    (fn []
      [:> Button {:mode "contained" :style style}
       name])))

(defn hour-view [num]
  (let [text (if (> 10 num) (str 0 num ":00") (str num ":00"))]
    [:> rn/View {:style (.-hour styles)}
     [:> rn/Text text]]))

(defn relate-intervals
  [events date]
  (let [month-dates (calendar-dates
                     (t/int (t/year date))
                     (t/int (t/month date)))
        first-day (first month-dates)
        last-day (last month-dates)
        this-month? (partial event-relevant? first-day last-day)]
    (loop [events events
           result {}]
      (if-not (seq events)
        result
        (let [e (first events)
              relevant-to-date (partial has-close-relation? date)
              intervals (->> e
                             (marking-loop this-month? interval->>)
                             (filter relevant-to-date))]
          (recur (rest events)
                 (assoc-in result [(:id e)] intervals)))))))

(defn- get-event [events id]
  (first
   (for [e events
         :let [current (:id e)]
         :when (= id current)]
     e)))

(defn details-view [{:keys [route]}]
  (let [dateObj (:date (.-params route))
        date (t/date (.-dateString ^js dateObj))
        data (subscribe [:data])
        month (subscribe [:month])]
    (fn []
      (let [events (:events @data)
            relate-intervals (relate-intervals events date)]
        [:> rn/ScrollView {:style (.-container styles)}
         (for [i (range 24)]
           [hour-view i])
         [:> rn/View {:style (.-markingView styles)}
          (when (seq relate-intervals)
            (for [[id intervals] relate-intervals
                  :let [name (:name (get-event events id))]
                  :when (seq intervals)]
              (for [i intervals]
                [button-view i name date])))]]))))
