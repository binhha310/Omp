(ns app.views
  (:require
   ["react-native" :as rn]
   [reagent.core :as r]
   [app.utils :refer (set-time now get-time tomorrow ->js-dt)]
   ["react-native-date-picker" :default DatePicker]
   ["react-native-paper" :refer (TextInput Divider List IconButton Switch Button Menu)]))

(def styles
  ^js (-> {:marking
           {:flex 1
            :flexDirection "column"}
           :markingName
           {:borderWidth 0}
           :markingProperties
           {:flex 1
            :flexDirection "column"}
           :markingProperty
           {:alignItems "center"
            :flexDirection "row"}
           :icon
           {:flex -1
            :flexDirection "column"}
           :markingValue
           {:flex 1
            :flexDirection "column"}
           :menu
           {:width "100%"
            :flex -1
            :flexDirection "row"
            :justifyContent "flex-end"}
           :property
           {:flex -1
            :flexDirection "row"
            :alignItems "center"
            :justifyContent "space-between"}}
          (clj->js)
          (rn/StyleSheet.create)))

(defn locale-format [date]
  (.toLocaleString date))

(defn date-picker! [mode default]
  (let [open (r/atom false)
        date default]
    (do
      (swap! date #(->js-dt %))
      (fn []
        [:> rn/View {:style (.-container styles)}
         [:> Button {:on-press #(swap! open not)} (str (locale-format @date))]
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

(defn date-picker [allDay! datetime!]
  (let [datetime-picker (partial date-picker! "datetime")
        date-picker (partial date-picker! "date")
        set-night #(set-time % "00:00")
        set-now #(set-time % (-> (now) get-time))]
    (if-not @allDay!
      (do
        (swap! datetime! set-now)
        [date-picker datetime!])
      (do
        (swap! datetime! set-night)
        [date-picker datetime!]))))

(defn event-description []
  (fn []
    [:> rn/View {:style (.-markingProperty styles)}
     [:> rn/View {:style (.-icon styles)}
      [:> IconButton {:size 32 :icon "text"}]]
     [:> rn/View {:style (.-markingValue styles)}
      [:> TextInput {:label "Description"}]]]))

(defn event-interval [allDay beginningTime endingTime]
  (fn []
    [:> rn/View {:style (.-markingProperty styles)}
     [:> rn/View {:style (.-icon styles)}
      [:> IconButton {:size 32 :icon "clock-outline"}]]
     [:> rn/View {:style (.-markingValue styles)}
      [:> rn/View {:style (.-property styles)}
       [:> rn/Text "Entire day?"]
       [:> Switch {:value @allDay :onValueChange #(swap! allDay not)}]]
      [:> rn/View {:style (.-property styles)}
       [:> rn/Text "Beginning: "]
       [date-picker allDay beginningTime]]
      [:> rn/View {:style (.-property styles)}
       [:> rn/Text "Ending: "]
       [date-picker allDay endingTime]]]]))

(def repeat-descript
  {:no "No loop"
   :day "Daily"
   :week "Weekly"
   :month "Monthly"
   :year "Annual"})

(defn event-repeat [repeat!]
  (let [visible (r/atom false)
        repeat-item (fn [key value]
                      [:> (.-Item Menu) {:on-press (fn []
                                                     (do
                                                       (swap! visible not)
                                                       (reset! repeat! key)))
                                         :title value}])
        anchor (fn [props]
                 [:> Button {:on-press #(swap! visible not)} (:title props)])]
    (fn []
      [:> rn/View {:style (.-markingProperty styles)}
       [:> rn/View {:style (.-icon styles)}
        [:> IconButton {:size 32 :icon "repeat"}]]
       [:> rn/View {:style (.-markingValue styles)}
        [:> rn/View {:style (.-property styles)}
         [:> rn/Text "Repeat?"]
          [:> Menu {:visible @visible
                    :on-dismiss #(swap! visible not)
                    :anchor (r/create-element
                             (r/reactify-component anchor) #js{:title (@repeat! repeat-descript)})}
           (for [[key value] repeat-descript]
             ^{:key key}
             [repeat-item key value])]]]])))

(defn event-name []
  (fn []
    [:> rn/View {:style (.-markingProperty styles)}
     [:> rn/View {:style (.-icon styles)}
      [:> IconButton {:size 32 :icon "calendar"}]]
     [:> rn/View {:style (.-markingValue styles)}
      [:> TextInput {:label "Event name"}]]]))

(defn event-add-view []
  (let [name (r/atom "")
        allDay (r/atom false)
        rep (r/atom :no)
        starting (r/atom (now))
        ending (r/atom (tomorrow))]
    (fn []
      [:> rn/View {:style (.-marking styles)}
       [:> rn/View {:style (.-markingProperties styles)}
        [event-name]
        [:> Divider]
        [event-description]
        [:> Divider]
        [event-interval allDay starting ending]
        [:> Divider]
        [event-repeat rep]]])))
