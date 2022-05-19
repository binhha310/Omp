(ns app.main
  (:require
   [shadow.react-native :refer (render-root)]
   ["react-native" :as rn]
   [reagent.core :as r]
   ["react-native-calendars" :refer (Calendar)]
   ["react-native-paper" :refer (DarkTheme Appbar Provider List FAB)]
   ["react-native-date-picker" :default DatePicker]
   [tick.core :as t]
   [tick.alpha.interval :as t.i]
   [app.event :as e]))

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
           :calendar
           {:height "100%"}
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

;; (defn counter []
;;   (let [count (r/atom 0)]
;;     (fn []
;;       [:> rn/View {:style (.-container styles)}
;;        [:> rn/Text "atom's current value: " @count ". "]
;;        [:> rn/Button {:on-press #(swap! count inc) :title "Increase"}]])))

(defn app-bar []
  [:> Appbar {:style (.-appbar styles)}])

(defn sub-list [title]
  (fn []
    [:> (.-Section List) {:style (.-list styles)}
     [:> (.-Subheader List) title]
     [:> (.-Item List) {:title "First item"}]
     [:> (.-Item List) {:title "First item"}]
     [:> (.-Item List) {:title "First item"}]
     [:> (.-Item List) {:title "First item"}]
     [:> (.-Item List) {:title "First item"}]]))

(defn calendar []
  [:> Calendar {:marked-dates (clj->js {:2022-05-16 {:marked true}})
                :on-day-press (fn [day] (println day))}])

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


(def calendar-theme {:stylesheet.calendar.main
                     {:dayContainer
                      {:flex 1
                       :margin 0
                       :alignItems "center"
                       :borderColor "#000000"
                       :borderWidth 1
                       :borderStyle "solid"}
                      :container
                      {:justifyContent "space-evenly"
                       :paddingLeft 5
                       :paddingBottom 5
                       :paddingRight 5}
                      :week
                      {
                       :flex 1
                       :flexDirection "row"
                       :justifyContent "space-evenly"}
                      :monthView
                      {:marginTop 0
                       :flex 1
                       :justifyContent "space-evenly"}}})

(defn app []
  (let [is [interval interval2 interval3 interval4]
        cm (t/month)
        year (t/year)
        decor-map (r/atom (e/update-decor {} is))]
    (fn []
      [:> rn/View {:style (.-container styles)}
       [:> Calendar {:style (.-calendar styles)
                     :theme calendar-theme
                     :on-day-press (fn [day] (println day))
                     :first-day 1
                     :marking-type "multi-period"
                     :on-month-change (fn [date]
                                        (let [{:strs [month year]} (js->clj date)]
                                          (swap! decor-map #(e/update-decor % is))))
                     :marked-dates (clj->js @decor-map)}]])))
       ;;[date-picker]
       ;;[fab]])))
       ;;[sub-list "List"]])))

;;[:> rn/Text {:style (.-title styles)} "Hello!"]
;;[:> rn/Image {:source splash-img :style {:width 200 :height 200}}]])

(defn root []
  [:> Provider
   [app]])

(defn start
  {:dev/after-load true}
  []
  (render-root "Omp" (r/as-element [root])))

(defn init []
  (start))
