(ns app.main
  (:require
   [shadow.react-native :refer (render-root)]
   ["react-native" :as rn]
   [reagent.core :as r]
   ["react-native-paper" :refer (DarkTheme Appbar Provider List)]
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

(defn app []
  [:> rn/View {:style (.-container styles)}
   [:> rn/View {:style (.-containter styles)}
    [app-bar]]
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
