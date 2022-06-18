(ns app.views.activities
  (:require
   ["react-native" :refer (View) :as rn]
   ["react-native-paper" :refer (FAB)]
   [app.calendar :refer (calendar)]))

(def styles
  ^js (-> {:addFab
           {:position "absolute"
            :margin 16
            :right 0
            :bottom 0}
           :activityFab
           {:position "relative"
            :marginBottom 16
            :marginRight 16
            :right 0
            :bottom 0}}
          (clj->js)
          (rn/StyleSheet.create)))

(defn fab [{:keys [style icon callback]}]
  [:> FAB {:icon icon
           :style style
           :on-press (fn [] (callback))}])

(defn main-view [{:keys [navigation]}]
  (let [to-activity #(.navigate navigation "Activity")]
    (fn []
      [:> View {:style (.-container styles)}
       [calendar {:navigation navigation}]
       [fab {:style (.-addFab styles) :icon "plus" :callback to-activity}]])))

(defn activity-view [{:keys [navigation]}]
  (let [new-event #(.navigate navigation "NewEvent")
        new-todo #(.navigate navigation "NewTodo")]
    (fn []
      [:> View {:style (.-activity styles)}
       [fab {:style (.-activityFab styles) :icon "format-list-checks" :callback new-todo}]
       [fab {:style (.-activityFab styles) :icon "calendar" :callback new-event}]])))
