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
   [app.views.edit-views :refer (event-add-view todo-add-view event-update-view)]
   [app.events]
   [app.subs]
   [app.views.details :refer (details-view)]
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
            :alignItems "flex-end"}}
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
        Details (r/reactify-component details-view)
        EventDetail (r/reactify-component event-update-view)]
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
                              :options #js{}}]
        [:> (.-Screen Stack) {:name "EventDetail"
                              :component EventDetail
                              :options {:headerShown true
                                        :title "Detail"}}]]])))

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
