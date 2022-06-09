(ns app.views.event
  (:require
   [app.utils :refer (repeat-string)]
   ["react-native" :refer (StyleSheet View Text)]
   [tick.core :refer (format beginning end)]))

(def styles
  ^js (->> {:container
            {:flex 1}
            :entry
            {:flexDirection "row"}
            :h1
            {:fontSize 30
             :fontWeight "bold"}
            :h2
            {:fontSize 20
             :fontWeight "bold"}
            :text
            {:fontSize "inherit"
             :fontWeight "inherit"}}
           (clj->js)
           (.create StyleSheet)))

(defn event-view [{:keys [navigation route]}]
  (let [marking (.-params route)]
    (fn []
      (let [{:keys [name time repeat]} marking
            beginning (format (beginning time))
            end (format (end time))
            time (str beginning " - " end)
            repeat (repeat repeat-string)]
        [:> View {:style (.-container styles)}
         [:> View {:style (.-entry styles)}
          [:> Text {:style (.-h1 styles)}"Name: "]
          [:> Text {:style (.-h1 styles)} name]]
         [:> View {:style (.-entry styles)}
          [:> Text {:style (.-h2 styles)}"Time: "]
          [:> Text {:style (.-h2 styles)} time]]
         [:> View {:style (.-entry styles)}
          [:> Text {:style (.-h2 styles)}"Repeat: "]
          [:> Text {:style (.-h2 styles)} repeat]]]))))
