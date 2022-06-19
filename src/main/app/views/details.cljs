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
  ^js (-> {:horizontal
           {:height 60
            :width "100%"}
           :hour
           {:borderBottomStyle "solid"
            :borderWidth 1
            :borderColor "rgba(0, 0, 0, 0.1)"
            :height 60
            :margin 0}
           :todo
           {:flex 1
            :padding 10
            :borderRadius 10
            :marginHorizontal 5
            :alignItems "center"
            :backgroundColor "#ffb86c"
            :justifyContent "center"}
           :event
           {:flex 1
            :padding 10
            :borderRadius 10
            :marginHorizontal 5
            :alignItems "center"
            :backgroundColor "#50fa7b"
            :justifyContent "center"}
           :date
           {:justifyContent "center"
            :alignItems "center"}
           :dateTitle
           {:fontSize 20
            :fontWeight "bold"}
           :title
           {:fontSize 15
            :fontWeight "bold"}
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


(defn- event-view
  ([navigation top bottom event]
   (let [marginTop top
         marginBottom (if (= 0 bottom) 0 (- 1440 bottom))
         navigate #(.navigate navigation "EventDetail", event)
         positionStyle (->
                        {:marginTop marginTop
                         :marginBottom marginBottom}
                        (clj->js)
                        (rn/StyleSheet.create))
         style (rn/StyleSheet.compose positionStyle (.-event styles))]
     (fn []
       [:> rn/TouchableOpacity {:style style :on-press navigate}
        [:> rn/Text {:style (.-title styles)} (:name event)]])))
  ([navigation event]
   (let [navigate #(.navigate navigation "EventDetail", event)]
     (fn []
       [:> rn/TouchableOpacity {:style (.-event styles) :on-press navigate}
        [:> rn/Text {:style (.-title styles)} (:name event)]]))))

(defn- todo-view
  [navigation event]
   (let [navigate #(.navigate navigation "EventDetail", event)]
     (fn []
       [:> rn/TouchableOpacity {:style (.-todo styles) :on-press navigate}
        [:> rn/Text {:style (.-title styles)} (:name event)]])))

(defn- hour-view [num]
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

;; Some thing like ((e1 i1) (e1 i2) (e2 i3) (e2 i4))
(defn- pair! [events map-of-vector]
  (when (seq map-of-vector)
    (let [m map-of-vector
          get-pair (partial partition 2)
          pairs-seq (for [[id intervals] m
                          :let [event (get-event events id)
                                pairs (get-pair (interleave (repeat event) intervals))]]
                      pairs)]
      (apply concat pairs-seq))))

(defn- day-view [navigation date todos events]
  (fn []
    [:> rn/ScrollView {:style (.-horizontal styles) :horizontal true}
     [:> rn/View {:style (.-date styles)}
      [:> rn/Text {:style (.-dateTitle styles)} (str date ": ")]]
     (for [todo todos]
       [todo-view navigation todo])
     (for [[event _] events]
       [event-view navigation event])]))

(defn details-view [{:keys [route navigation]}]
  (let [dateObj (:date (.-params route))
        date (t/date (.-dateString ^js dateObj))
        data (subscribe [:data])
        full? (fn [s]
                (let [[_ i] s]
                  (= [0 0] (handle-relation date i))))]
    (fn []
      (let [events (:events @data)
            todos (:todos @data)
            relate-intervals (relate-intervals events date)
            event-interval-pairs (pair! events relate-intervals)
            full-days (filter full? event-interval-pairs)
            partials (filter (complement full?) event-interval-pairs)]
        [:> rn/View {:style (.-container styles)}
         [day-view navigation date todos full-days]
         [:> rn/ScrollView {:style (.-container styles)}
          (for [i (range 24)]
            [hour-view i])
          [:> rn/View {:style (.-markingView styles)}
           (when (seq partials)
             (for [[event i] partials
                   :let [[top bottom] (handle-relation date i)]]
               [event-view navigation top bottom event]))]]]))))
