(ns app.main
  (:require
   [shadow.react-native :refer (render-root)]
   ["react-native" :as rn]
   [reagent.core :as r]
   [re-frame.core :as rf]
   ["react-native-calendars" :refer (Calendar)]
   ["react-native-paper" :refer (DarkTheme Appbar Provider List FAB)]
   ["react-native-date-picker" :default DatePicker]
   [tick.core :as t]
   [tick.alpha.interval :as t.i]
   [app.event :as e]
   [app.calendar :refer (calendar)]))

;; must use defonce and must refresh full app so metro can fill these in
;; at live-reload time `require` does not exist and will cause errors
;; must use path relative to :output-dir

(def device-dim (.. rn/Dimensions (get "window")))
(def height (.-height device-dim))

(defonce splash-img (js/require "../assets/cat.jpg"))
(def styles
  ^js (-> {:container
           {:flex 1
            :backgroundColor "#fff"}
           :title
           {:fontWeight "bold"
            :fontSize 24
            :color "blue"}
           :fab
           {:position "absolute"
            :margin 16
            :right 0
            :bottom 0}
           :appbar
           {:position "absolute"
            :left 0
            :right 0
            :top 0}
           :list
           {:flex 1
            :marginTop 50}}
          (clj->js)
          (rn/StyleSheet.create)))

(defn locale-format [date]
  (.toLocaleString date))

(defn fab []
  [:> FAB {:style (.-fab styles)
           :icon "plus"
           :on-press (fn [] (println "pressed"))}])

(defn date-picker []
  (let [open (r/atom false)
        date (r/atom (js/Date.))]
    (fn []
      [:> rn/View {:style (.-container styles)}
       [:> rn/Button {:title (locale-format @date) :on-press #(reset! open true)}]
       [:> DatePicker {:modal true
                       :date @date
                       :open @open
                       :on-confirm (fn [d] (do
                                             (reset! open false)
                                             (reset! date d)))
                       :on-cancel #(reset! open false)
                       :locale "vi"
                       :is24hour-source "locale"
                       :mode "datetime"
                       :on-date-change #(reset! date %)}]])))

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

(def i5 (t.i/new-interval
                (t/date-time "2022-05-01T12:00")
                (t/date-time "2022-05-02T12:00")))

(def data
  {:event
   [{:time i1 :name "i1" :repeat :no}
    {:time i2 :name "i2" :repeat :no}
    {:time i3 :name "i3" :repeat :no}
    {:time i4 :name "i4" :repeat :no}
    {:time i5 :name "i5" :repeat :week}]
   :todo
   [{:time (t/date "2022-05-15") :name "t1" :repeat :no}
    {:time (t/date "2022-05-24") :name "t2" :repeat :day}]})


(defn pick-date []
  [:> rn/Button {:title "Print date" :on-press #(rf/dispatch [:pickdate "some date"])}])

(rf/reg-event-fx :pickdate (fn [cofx event]
                             (let [date (second event)]
                               (println date))))
(rf/reg-event-fx :day-press (fn [cofx event]
                              (let [date (second event)]
                                (println date))))
(rf/reg-event-fx
 :month-change
 (fn [_ [_ value]]
   (println value)))

(rf/reg-event-db
 :initialize
 (fn [_ _]
   {:event [i1 i2 i3 i4]}))

(rf/reg-sub
 :event
 (fn [db _] (:event db)))

(defn app []
  (let []
    (fn []
      [:> rn/View {:style (.-container styles)}
       [calendar (t/year) (t/month) data]
       [pick-date]])))

(defn root []
  [:> Provider
   [app]])

(defn start
  {:dev/after-load true}
  []
  (render-root "Omp" (r/as-element [root])))

(defn init []
  (start))
