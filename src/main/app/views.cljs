(ns app.views
  (:require
   ["react-native" :as rn]
   [reagent.core :as r]
   [re-frame.core :refer (dispatch)]
   [app.utils :refer (set-time today now get-date get-time tomorrow ->js-dt interval)]
   ["react-native-date-picker" :default DatePicker]
   ["react-native-paper" :refer (TextInput Divider List IconButton Switch Button Menu)]))

(def styles
  ^js (-> {:marking
           {:flex 1
            :justifyContent "space-between"
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
            :justifyContent "space-between"}
           :save
           {:alignSelf "flex-end"}}
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
         [:> Button {:on-press #(swap! open not)} (case mode
                                                    "datetime" (str (locale-format @date))
                                                    "date" (str (locale-format (get-date @date))))]
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

(defn date-picker
  ([allDay! datetime!]
   (let [datetime-picker (partial date-picker! "datetime")
         date-picker (partial date-picker! "date")
         set-night #(set-time % "00:00")
         set-now #(set-time % (-> (now) get-time))]
     (if-not @allDay!
       (do
         (swap! datetime! set-now)
         [datetime-picker datetime!])
       (do
         (swap! datetime! set-night)
         [date-picker datetime!]))))
  ([date!]
   (date-picker! "date" date!)))

(defn marking-description []
  (fn []
    [:> rn/View {:style (.-markingProperty styles)}
     [:> rn/View {:style (.-icon styles)}
      [:> IconButton {:size 32 :icon "text"}]]
     [:> rn/View {:style (.-markingValue styles)}
      [:> TextInput {:label "Description"}]]]))

(defn event-time [allDay beginningTime endingTime]
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

(defn todo-time [date]
  (let [date-time (r/atom (now))]
    (fn []
      (reset! date (get-date @date-time))
      [:> rn/View {:style (.-markingProperty styles)}
       [:> rn/View {:style (.-icon styles)}
        [:> IconButton {:size 32 :icon "clock-outline"}]]
       [:> rn/View {:style (.-markingValue styles)}
        [:> rn/View {:style (.-property styles)}
         [:> rn/Text "Date: "]
         [date-picker date-time]]]])))

(def repeat-descript
  {:no "No loop"
   :day "Daily"
   :week "Weekly"
   :month "Monthly"
   :year "Annual"})

(defn marking-repeat [repeat!]
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

(defn marking-name [mode name!]
  (let [name-label (str mode " name")]
    (fn []
      [:> rn/View {:style (.-markingProperty styles)}
       [:> rn/View {:style (.-icon styles)}
        [:> IconButton {:size 32 :icon "calendar"}]]
       [:> rn/View {:style (.-markingValue styles)}
        [:> TextInput {:label name-label :on-change-text #(reset! name! %)}]]])))

(defn save [{:keys [navigation type time name repeat]}]
  (fn []
    [:> Button {:mode "contained"
                :on-press #(let [time (case type
                                        :events (interval
                                                 @(:beginning time)
                                                 @(:ending time))
                                        :todos @time)]
                             (dispatch [:add-data
                                        {:type type
                                         :time time
                                         :name @name
                                         :repeat @repeat}])
                             (.popToTop navigation))}
     "Save"]))

(defn adding-view [name description time repeat save]
  (fn []
    [:> rn/View {:style (.-marking styles)}
     [:> rn/View {:style (.-markingProperties styles)}
      [name]
      [:> Divider]
      [description]
      [:> Divider]
      [time]
      [:> Divider]
      [repeat]]
     [save]]))

(defn event-add-view [{:keys [navigation]}]
  (let [allDay (r/atom false)
        name (r/atom "")
        rep (r/atom :no)
        starting (r/atom (now))
        ending (r/atom (tomorrow))]
    (fn []
      [adding-view
       (fn [] [marking-name "event" name])
       (fn [] [marking-description "event"])
       (fn [] [event-time allDay starting ending])
       (fn [] [marking-repeat rep])
       (fn [] [save {:navigation navigation
                     :type :events
                     :time {:beginning starting :ending ending}
                     :name name
                     :repeat rep}])])))

(defn todo-add-view [{:keys [navigation]}]
  (let [name (r/atom "")
        rep (r/atom :no)
        time (r/atom (today))]
    (fn []
      [adding-view
       (fn [] [marking-name "todo" name])
       (fn [] [marking-description "todo"])
       (fn [] [todo-time time])
       (fn [] [marking-repeat rep])
       (fn [] [save {:navigation navigation
                     :type :todos
                     :time time
                     :name name
                     :repeat rep}])])))
