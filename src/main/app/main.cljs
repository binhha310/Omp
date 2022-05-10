(ns app.main
  (:require
    [shadow.react-native :refer (render-root)]
    ["react-native" :as rn]
    [reagent.core :as r]
    ["buffer" :refer (Buffer)]
    ["react-native-image-picker" :refer (launchImageLibrary)]
    ["jpeg-js" :as jpeg]))

;; must use defonce and must refresh full app so metro can fill these in
;; at live-reload time `require` does not exist and will cause errors
;; must use path relative to :output-dir

(defonce splash-img (js/require "../assets/cat.jpg"))
(def styles
  ^js (-> {:container
           {:flex 1
            :backgroundColor "#fff"
            :alignItems "center"
            :justifyContent "center"}
           :title
           {:fontWeight "bold"
            :fontSize 24
            :color "blue"}}
          (clj->js)
          (rn/StyleSheet.create)))

(defn counter []
  (let [count (r/atom 0)]
    (fn []
      [:> rn/View {:style (.-container styles)}
       [:> rn/Text "atom's current value: " @count ". "]
       [:> rn/Button {:on-press #(swap! count inc) :title "Increase"}]])))

(defn picker-callback [args]
  (let [a (js->clj args :keywordize-keys true)
        didCancel (:didCancel a)
        errorCode (:errorCode a)
        assets (:assets a)
        fileSize (:fileSize a)]
    (if (or (some? didCancel) errorCode (nil? assets) (= 0 fileSize))
      (println "Some error occur: " errorCode)
      (letfn [(extract-buffer [assets]
                (-> assets
                (nth 0)
                :base64
                (#(.from Buffer % "base64"))
                (#(.decode jpeg % #js {:useTArray true}))
                .-data))]
        (let [buffer (extract-buffer assets)]
          (println buffer)
          buffer)))))

(defn picker []
  (let [options #js {:selectionLimit 1
                     :mediaType "photo"
                     :includeBase64 true}]
    (launchImageLibrary options picker-callback)))

(defn root []
  [:> rn/View {:style (.-container styles)}
   [:> rn/Text {:style (.-title styles)} "Hello!"]
   [:> rn/Image {:source splash-img :style {:width 200 :height 200}}]])

(defn start
  {:dev/after-load true}
  []
  (render-root "Omp" (r/as-element [root])))

(defn init []
  (start))
