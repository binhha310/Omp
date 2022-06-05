(ns app.main
  (:require
   [shadow.react-native :refer (render-root)]
   ["react-native" :as rn]
   [reagent.core :as r]
   [re-frame.core :refer (dispatch-sync dispatch clear-subscription-cache! subscribe)]
   ["react-native-calendars" :refer (Calendar)]
   ["react-native-paper" :refer (DarkTheme Appbar Provider List FAB)]
   [app.views :refer (event-add-view)]
   [app.events]
   [app.subs]
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
           :marking
           {:flexDirection "column"}
           :marking-name
           {:borderWidth 0}
           :list
           {:flex 1
            :marginTop 50}}
          (clj->js)
          (rn/StyleSheet.create)))

(defn fab []
  [:> FAB {:style (.-fab styles)
           :icon "plus"
           :on-press (fn [] (println "pressed"))}])

(dispatch-sync [:initialise-db])

(defn app []
    (fn []
      [:> rn/View {:style (.-container styles)}
       [calendar]
       [fab]]))

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
