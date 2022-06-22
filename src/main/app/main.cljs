(ns app.main
  (:require
   [shadow.react-native :refer (render-root)]
   ["react-native" :as rn]
   [reagent.core :as r]
   [re-frame.core :refer (dispatch-sync clear-subscription-cache!)]
   ["react-native-paper" :refer (Provider)]
   ["@react-navigation/native" :refer (NavigationContainer)]
   ["@react-navigation/native-stack" :refer (createNativeStackNavigator)]
   [app.views.edit-views :refer (event-add-view todo-add-view event-update-view todo-update-view)]
   [app.events]
   [app.subs]
   [app.views.details :refer (details-view)]
   [app.views.activities :refer (main-view activity-view)]))

;; must use defonce and must refresh full app so metro can fill these in
;; at live-reload time `require` does not exist and will cause errors
;; must use path relative to :output-dir

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
        EventDetail (r/reactify-component event-update-view)
        TodoDetail (r/reactify-component todo-update-view)]
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
                              :options {:headerShown false}}]
        [:> (.-Screen Stack) {:name "EventDetail"
                              :component EventDetail
                              :options {:headerShown true
                                        :title "Detail"}}]
        [:> (.-Screen Stack) {:name "TodoDetail"
                              :component TodoDetail
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
