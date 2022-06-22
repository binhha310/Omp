(ns app.components
  (:require
   ["react-native" :as rn]
   [reagent.core :as r]
   ["react-native-date-picker" :default DatePicker]
   [app.utils :refer (set-time get-time get-date now ->js-dt)]
   ["react-native-paper"
    :refer (Menu IconButton)
    :rename {Menu Menu_3rd}]
   [app.views.themes
    :refer (dracula)
    :rename {dracula theme}]))

(defn Text
  ([child] [Text {:style {}} child])
  ([{:keys [style]} child]
   (let [default {:color (:foreground theme)}]
     [:> rn/Text {:style (merge default style)} child])))

(defn- locale-format [date]
  (.toLocaleString date))

(defn- date-picker_3rd [mode date]
  (let [open (r/atom false)]
    (fn []
      (do
        (swap! date #(->js-dt %))
        [:> rn/View {:style {:alignSelf "flex-end"
                             :color (:purple theme)}}
         [:> rn/TouchableOpacity {:on-press #(swap! open not)}
          [Text {:style {:color (:purple theme)
                         :fontWeight "bold"
                         :margin 10
                         :fontSize 18}}
           (case mode
             "datetime" (str (locale-format @date))
             "date" (str (locale-format (get-date @date))))]]
         [:> DatePicker {:modal true
                         :date @date
                         :open @open
                         :on-confirm (fn [d] (do
                                               (swap! open not)
                                               (reset! date d)))
                         :on-cancel #(reset! open false)
                         :locale "vi"
                         :is24hour-source "locale"
                         :mode mode
                         :on-date-change #(reset! date %)}]]))))

(defn DateTimePicker
  ([{:keys [dateOnly]} datetime!]
   (let [datetime-picker (partial date-picker_3rd "datetime")
         date-picker (partial date-picker_3rd "date")
         set-night #(set-time % "00:00")
         set-now #(set-time % (-> (now) get-time))]
     (fn []
       (if-not @dateOnly
         (do
           (swap! datetime! set-now)
           [datetime-picker datetime!])
         (do
           (swap! datetime! set-night)
           [date-picker datetime!])))))
  ([date!]
   (date-picker_3rd "date" date!)))

(defn Icon [props]
  (let [default {:color (:foreground theme)
                 :size 10
                 :icon "text"}]
    [:> IconButton (merge default props)]))

(defn PickerMenu [props]
  (let [{:keys [value! menuShown value-map]} props
        item (fn [key value]
               [:> (.-Item Menu_3rd)
                {:titleStyle {:color (:purple theme)}
                 :title value
                 :style {:backgroundColor (:current_line theme)}
                 :on-press #(do
                              (swap! menuShown not)
                              (reset! value! key))}])
        anchor (fn [{:keys [title]}]
                 [:> rn/TouchableOpacity {:on-press #(swap! menuShown not)
                                          :style {:margin 5}}
                  [Text {:style {:color (:purple theme)
                                 :fontSize 20
                                 :fontWeight "bold"}} title]])]
    (fn []
      [:> Menu_3rd {:visible @menuShown
                    :contentStyle #js {:backgroundColor (:current_line theme)}
                    :on-dismiss #(swap! menuShown not)
                    :anchor (r/create-element
                             (r/reactify-component anchor)
                             #js {:title (@value! value-map)})}
       (for [[key value] value-map]
         ^{:key key}
         [item key value])])))
