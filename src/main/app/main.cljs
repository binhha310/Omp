(ns app.main
  (:require
   [shadow.react-native :refer (render-root)]
   ["react-native" :as rn]
   [reagent.core :as r]
   [re-frame.core :refer (dispatch-sync dispatch clear-subscription-cache! subscribe)]
   ["react-native-calendars" :refer (Calendar)]
   ["react-native-paper" :refer (DarkTheme Provider FAB Button)]
   ["@react-navigation/native" :refer (NavigationContainer)]
   ["@react-navigation/native-stack" :refer (createNativeStackNavigator)]
   [app.views :refer (event-add-view todo-add-view)]
   [app.events]
   [app.subs]
   [tick.core :as t :refer (hour minute time)]
   [tick.alpha.interval :as t.i]
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
           :addFab
           {:position "absolute"
            :margin 16
            :right 0
            :bottom 0}
           :activityFab
           {:position "relative"
            :marginBottom 16
            :marginRight 16
            :right 0
            :bottom 0}
           :activity
           {:flex 1
            :height "100%"
            :flexDirection "column"
            :justifyContent "flex-end"
            :alignItems "flex-end"}
           :marking
           {:flexDirection "column"}
           :marking-name
           {:borderWidth 0}
           :hour
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

(defn fab [{:keys [style icon callback]}]
  [:> FAB {:icon icon
           :style style
           :on-press (fn [] (callback))}])

(defn main-view [{:keys [navigation]}]
  (let [to-activity #(.navigate navigation "Activity")]
    (fn []
      [:> rn/View {:style (.-container styles)}
       [calendar {:navigation navigation}]
       [fab {:style (.-addFab styles) :icon "plus" :callback to-activity}]])))

(defn activity-view [{:keys [navigation]}]
  (let [new-event #(.navigate navigation "NewEvent")
        new-todo #(.navigate navigation "NewTodo")]
    (fn []
      [:> rn/View {:style (.-activity styles)}
       [fab {:style (.-activityFab styles) :icon "format-list-checks" :callback new-todo}]
       [fab {:style (.-activityFab styles) :icon "calendar" :callback new-event}]])))

(defn hour-view [num]
  (let [text (if (> 10 num) (str 0 num ":00") (str num ":00"))]
    [:> rn/View {:style (.-hour styles)}
     [:> rn/Text text]]))

(defn button-view [beginning ending name]
  (let [pos #(+ (* 60 (hour %)) (minute %))
        top (pos beginning)
        bottom (- 1440 (pos ending))
        style (->
               {:width "auto"
                :justifyContent "center"
                :flex 1
                :textAlign "center"
                :marginTop top
                :alignItem "stre"
                :marginBottom bottom}
               (clj->js)
               (rn/StyleSheet.create))]
    [:> Button {:mode "contained" :style style}
     name]))

(def beginning (t/now))
(def ending (t/>> (t/now) (t/new-duration 10 :hours)))
(def ending2 (t/>> (t/now) (t/new-duration 5 :hours)))
(defn details-view [{:keys [route]}]
  (let [date (:date (.-params route))]
  (fn []
    [:> rn/ScrollView {:style (.-container styles)}
     (for [i (range 24)]
       [hour-view i])
     [:> rn/View {:style (.-markingView styles)}
      [button-view beginning ending "text"]
      [button-view beginning ending2 "second"]]])))

(dispatch-sync [:initialise-db])

(defn create-element
  ([re]
   (r/create-element (r/reactify-component re)))

  ([re props]
   (r/create-element
    (r/reactify-component re) (clj->js props))))

(defn app []
  (let [Stack (createNativeStackNavigator)
        Home (r/reactify-component main-view)
        Activity (r/reactify-component activity-view)
        NewEvent (r/reactify-component event-add-view)
        NewTodo (r/reactify-component todo-add-view)
        Details (r/reactify-component details-view)]
    (fn []
      [:> NavigationContainer
       [:> (.-Navigator Stack) {:screenOptions​ {:headerShown false}}
        [:> (.-Screen Stack) {:name "Home"
                              :component Home
                              :options (clj->js {:headerShown false})}]
        [:> (.-Screen Stack) {:name "Activity"
                              :component Activity
                              :options (clj->js {:headerShown false})}]
        [:> (.-Screen Stack) {:name "NewEvent"
                              :component NewEvent
                              :options #js{}}]
        [:> (.-Screen Stack) {:name "NewTodo"
                              :component NewTodo
                              :options #js{}}]
        [:> (.-Screen Stack) {:name "Details"
                              :component Details
                              :options #js{}}]]])))

(defn root []
  [:> Provider
   [app]])

(defn start
  {:dev/after-load true}
  []
  (clear-subscription-cache!)
  (render-root "Omp" (r/as-element [root])))

(defn init []
  (start))
