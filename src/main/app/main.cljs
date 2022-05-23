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
   [app.calendar-theme :as theme]))

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

;; (defn app-bar []
;;   [:> Appbar {:style (.-appbar styles)}])

;; (defn sub-list [title]
;;   (fn []
;;     [:> (.-Section List) {:style (.-list styles)}
;;      [:> (.-Subheader List) title]
;;      [:> (.-Item List) {:title "First item"}]
;;      [:> (.-Item List) {:title "First item"}]
;;      [:> (.-Item List) {:title "First item"}]
;;      [:> (.-Item List) {:title "First item"}]
;;      [:> (.-Item List) {:title "First item"}]]))

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

(def interval (t.i/new-interval
               (t/date-time "2022-05-15T12:00")
               (t/date-time "2022-05-17T12:00")))


(def interval2 (t.i/new-interval
                (t/date-time "2022-05-13T12:00")
                (t/date-time "2022-05-18T12:00")))

(def interval3 (t.i/new-interval
                (t/date-time "2022-05-12T12:00")
                (t/date-time "2022-05-14T12:00")))

(def interval4 (t.i/new-interval
                (t/date-time "2022-05-11T12:00")
                (t/date-time "2022-05-19T12:00")))

(def interval5 (t.i/new-interval
                (t/date-time "2022-05-25T12:00")
                (t/date-time "2022-06-05T12:00")))

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
   {:event [interval interval2 interval3 interval4]}))

;; (fn [date]
;;                                         (do
;;                                           (println @test)
;;                                           (swap! decor-map #(e/update-decor % is))))

(rf/reg-sub
 :event
 (fn [db _] (:event db)))

(defn app []
  (let [is [interval interval2 interval3 interval4]
        test (rf/subscribe [:event])
        cm (t/month)
        year (t/year)
        decor-map (r/atom (e/update-decor {} is))]
    (fn []
      [:> rn/View {:style (.-container styles)}
       [:> Calendar {:style (.-calendar styles)
                     :theme (merge theme/calendar-theme theme/calendar-main theme/marking theme/day-basic)
                     :on-day-press #(rf/dispatch [:day-press %])
                     :first-day 1
                     :marking-type "multi-period"
                     :on-month-change #(rf/dispatch [:month-change %])
                     :marked-dates (clj->js @decor-map)}]
       [pick-date]
       ])))

(defn root []
  [:> Provider
   [app]])

(defn start
  {:dev/after-load true}
  []
  (render-root "Omp" (r/as-element [root])))

(defn init []
  (start))
