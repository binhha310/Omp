(ns app.views.details
  (:require
   [clojure.spec.alpha :as s]
   ["react-native" :as rn]
   ["react-native-paper" :refer (Button)]
   [tick.alpha.interval :as t.i]
   [tick.core :as t]
   [app.views.themes
    :refer (dracula)
    :rename {dracula theme}]
   [re-frame.core :refer (dispatch-sync dispatch clear-subscription-cache! subscribe)]
   [app.calendar.event :refer (relevant?) :rename {relevant? event-relevant?}]
   [app.calendar.todo :refer (relevant?) :rename {relevant? todo-relevant?}]
   [app.calendar.decor-factory :refer (marking-repeat)]
   [app.utils :refer (interval->> date->> calendar-dates)]))

(def styles
  ^js (-> {:horizontal
           {:height 60
            :width "100%"}
           :container
           {:backgroundColor (:current_line theme)
            :color (:foreground theme)}
           :hour
           {:borderBottomStyle "solid"
            :borderWidth 1
            :backgroundColor (:background theme)
            :borderColor "rgba(98, 114, 164, 0.1)"
            :height 60
            :margin 0}
           :hourTitle
           {:color (:foreground theme)}
           :todo
           {:flex 1
            :padding 10
            :borderRadius 10
            :marginHorizontal 5
            :alignItems "center"
            :backgroundColor (:orange theme)
            :justifyContent "center"}
           :event
           {:flex 1
            :padding 10
            :borderRadius 10
            :marginHorizontal 5
            :alignItems "center"
            :backgroundColor (:cyan theme)
            :justifyContent "center"}
           :date
           {:justifyContent "center"
            :alignItems "center"}
           :dateTitle
           {:fontSize 20
            :color (:foreground theme)
            :fontWeight "bold"}
           :title
           {:fontSize 15
            :fontWeight "bold"}
           :markingView
           {:position "absolute"
            :flexDirection "row"
            :height "100%"
            :width "85%"
            :backgroundColor "rgba(68, 71, 90, 0.2)"
            :marginLeft "15%"}}
          (clj->js)
          (rn/StyleSheet.create)))

(s/def ::date t/date?)
(s/def ::interval t/interval?)

(defn relation [date interval]
  (when (and (s/valid? ::date date) (s/valid? ::interval interval))
    (t.i/relation date interval)))

(defn has-close-relation? [date time]
  (if-not (t/date? time)
    (#{:overlapped-by
       :starts
       :during
       :contains
       :finishes
       :overlaps}
     (relation date time))
    (#{:equals}
     (relation date time))))

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
  [navigation todo]
   (let [navigate #(.navigate navigation "TodoDetail", todo)]
     (fn []
       [:> rn/TouchableOpacity {:style (.-todo styles) :on-press navigate}
        [:> rn/Text {:style (.-title styles)} (:name todo)]])))

(defn- hour-view [num]
  (let [text (if (> 10 num) (str 0 num ":00") (str num ":00"))]
    [:> rn/View {:style (.-hour styles)}
     [:> rn/Text {:style (.-hourTitle styles)} text]]))

(defn- relate-intervals
  [events date]
  (let [month-dates (calendar-dates
                     (t/int (t/year date))
                     (t/int (t/month date)))
        first-day (first month-dates)
        last-day (last month-dates)
        this-month? (partial event-relevant? first-day last-day)
              relevant-to-date (partial has-close-relation? date)]
    (loop [events events
           result {}]
      (if-not (seq events)
        result
        (let [e (first events)
              intervals (->> e
                             (marking-repeat this-month? interval->>)
                             (filter relevant-to-date))]
          (recur (rest events)
                 (assoc-in result [(:id e)] intervals)))))))

(defn- relate-todos
  [todos date]
  (let [month-dates (calendar-dates
                     (t/int (t/year date))
                     (t/int (t/month date)))
        first-day (first month-dates)
        last-day (last month-dates)
        this-month? (partial todo-relevant? first-day last-day)
        relevant (partial has-close-relation? date)]
    (for [t todos
          :let [dates (->> t
                           (marking-repeat this-month? date->>)
                           (filter relevant))]
          :when (seq dates)]
      t)))

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
            relate-todos (relate-todos todos date)
            relate-intervals (relate-intervals events date)
            event-interval-pairs (pair! events relate-intervals)
            full-days (filter full? event-interval-pairs)
            partials (filter (complement full?) event-interval-pairs)]
        [:> rn/View {:style (.-container styles)}
         [day-view navigation date relate-todos full-days]
         [:> rn/ScrollView {:style (.-container styles)}
          (for [i (range 24)]
            [hour-view i])
          [:> rn/View {:style (.-markingView styles)}
           (when (seq partials)
             (for [[event i] partials
                   :let [[top bottom] (handle-relation date i)]]
               [event-view navigation top bottom event]))]]]))))
