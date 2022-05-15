(ns app.main
  (:require
   [shadow.react-native :refer (render-root)]
   ["react-native" :as rn]
   [reagent.core :as r]
   ["react-native-calendars" :refer (Calendar)]
   ["react-native-paper" :refer (DarkTheme Appbar Provider List)]
   [tick.core :as t]
   [tick.alpha.interval :as t.i]
   ["buffer" :refer (Buffer)]))

;; must use defonce and must refresh full app so metro can fill these in
;; at live-reload time `require` does not exist and will cause errors
;; must use path relative to :output-dir

(defonce splash-img (js/require "../assets/cat.jpg"))
(def styles
  ^js (-> {:container
           {:flex 1
            :backgroundColor "#fff"}
           :title
           {:fontWeight "bold"
            :fontSize 24
            :color "blue"}
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

(defn counter []
  (let [count (r/atom 0)]
    (fn []
      [:> rn/View {:style (.-container styles)}
       [:> rn/Text "atom's current value: " @count ". "]
       [:> rn/Button {:on-press #(swap! count inc) :title "Increase"}]])))

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

(def some-bound (t.i/bounds (t/new-date 2019 01 01)))
(def some-date (t/date "2019-01-01"))
(def interval (t.i/new-interval
  (t/date-time "2022-05-15T12:00")
  (t/date-time "2022-05-17T12:00")))

;; (defn next-day [date]
;;   (let [a-day (t/new-period 1 :days)]
;;     (t/>> date a-day)))
;; (next-day (t/date "2019-01-01"))
;; (def dates (iterate next-day (t/date "2019-01-01")))
;; (take 10 dates)
(defn include? [interval date]
  (case (t.i/relation interval date)
    :preceded-by false
    :met-by false
    :meets false
    :precedes false
    true))

(def startingDay (t/date "2019-01-01"))
(def containDay (t/date "2019-01-15"))
(def endingDay (t/date "2019-02-01"))

(defn decorator [interval date]
  {:startingDay (case (t.i/relation interval date)
                  :overlapped-by true
                  :started-by true
                  false)
   :endingDay (case (t.i/relation interval date)
                 :overlaps true
                 :finished-by true
                 false)
   :color "#000000"})

(defn update-decor [decor-map interval date]
  (update-in decor-map [(keyword date) :periods] #(vec (conj % (decorator interval date)))))

(defn decor-map [calendar interval]
  (apply merge (map (partial update-decor {} interval) (map t/format calendar))))

(defn dates [year beginning end]
  (let [intvl (t.i/bounds (t/year year))]
  (t/range
    (t/new-date year beginning 1)
    (t/new-date year end 1)
    (t/new-period 1 :days))))

(defn decor-map-d [calendar interval]
  (->> calendar
       (filter (partial include? interval))
       (#(decor-map % interval))))

(defn get-key [date]
  (keyword (t/format date)))

(defn app []
  [:> rn/View {:style (.-container styles)}
   [:> rn/View {:style (.-containter styles)}
    [app-bar]]
   [:> Calendar {:on-day-press (fn [day] (println day))
                 :first-day 1
                 :marking-type "multi-period"
                 :on-month-change (fn [month] (println month))
                 ;; :marked-dates (clj->js {:2022-05-14 {:periods [{:startingDay false :endingDay true :color "#5f9ea0"}
                 ;;                                               {:startingDay false :endingDay true :color "#ffa500"}
                 ;;                                               {:startingDay true :endingDay false :color "#f0e68c"}]}
                 ;;                         :2022-05-15 {:periods [{:color "transparent"} {:color "transparent"} {:startingDay false :endingDay false :color "#f0e68c"}]}
                 ;;                         :2022-05-16 {:periods [{:startingDay true :endingDay false :color "#ffa500"}
                 ;;                                               {:color "transparent"}
                 ;;                                               {:startingDay false :endingDay false :color "#f0e68c"}]}})}]
                 :marked-dates (clj->js (decor-map-d (dates 2022 5 6) interval))}]
   ;;[calendar]
   [sub-list "List"]])
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
