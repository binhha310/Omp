(ns app.views.edit-views
  (:require
   ["react-native" :as rn]
   [reagent.core :as r]
   [tick.core :as t :refer (interval? beginning end)]
   [re-frame.core :refer (dispatch)]
   [app.views.activities :refer (fab)]
   [app.views.themes
    :refer (dracula)
    :rename {dracula theme}]
   [app.utils :refer (repeat-string set-time now get-date get-time tomorrow ->js-dt interval next-hour)]
   ["react-native-date-picker" :default DatePicker]
   ["react-native-paper" :refer (TextInput Divider List IconButton Switch Button Menu Banner Portal)]))

(def styles
  ^js (-> {:banner
           {:position "static"
            :top 0
            :color (:red theme)}
           :container
           {:flex 1}
           :datePicker
           {:alignSelf "flex-end"
            :color (:purple theme)}
           :deleteFab
           {:position "absolute"
            :margin 16
            :backgroundColor (:red theme)
            :marginBottom 48
            :bottom 0
            :right 0}
           :marking
           {:flex 1
            :backgroundColor (:background theme)
            :color (:foreground theme)
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
            :justifyContent "space-between"}}
          (clj->js)
          (rn/StyleSheet.create)))

(defn locale-format [date]
  (.toLocaleString date))

(defn- date-picker! [mode date]
  (let [open (r/atom false)]
    (fn []
      (do
        (swap! date #(->js-dt %))
        [:> rn/View {:style (.-datePicker styles)}
         [:> rn/TouchableOpacity {:on-press #(swap! open not)}
          [:> rn/Text {:style #js {:color (:purple theme)
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

(defn- date-picker
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
      [:> IconButton {:size 32 :icon "text" :color (:foreground theme)}]]
     [:> rn/View {:style (.-markingValue styles)}
      [:> TextInput {:label "Description"}]]]))

(defn event-time [disabled? allDay time]
  (let [beginning (r/atom (beginning @time))
        end (r/atom (end @time))
        validTime? (fn [] (t/> @end @beginning))
        valid (r/track! validTime?)]
    (fn []
      (do
        (reset! disabled? (not @valid))
        (when @valid (reset! time (interval @beginning @end)))
        [:> rn/View
         [:> Banner {:visible (not @valid)
                     :style (.-banner styles)
                     :actions (clj->js [{:label "OK"
                                         :onPress #(reset! end (next-hour @beginning))}])}
          "Invalid time, end datetime must be larger than beginning datetime"]
         [:> rn/View {:style (.-markingProperty styles)}
          [:> rn/View {:style (.-icon styles)}
           [:> IconButton {:size 32 :icon "clock-outline" :color (:foreground theme)}]]
          [:> rn/View {:style (.-markingValue styles)}
           [:> rn/View {:style (.-property styles)}
            [:> rn/Text {:style {:color (:foreground theme)}} "Entire day?"]
            [:> Switch {:value @allDay :onValueChange #(swap! allDay not)}]]
           [:> rn/View {:style (.-property styles)}
            [:> rn/Text {:style {:color (:foreground theme)}} "Beginning: "]
            [date-picker allDay beginning]]
           [:> rn/View {:style (.-property styles)}
            [:> rn/Text {:style {:color (:foreground theme)}} "Ending: "]
            [date-picker allDay end]]]]]))))

(defn todo-time [date]
  (let [date-time (r/atom (now))]
    (fn []
      (reset! date (get-date @date-time))
      [:> rn/View {:style (.-markingProperty styles)}
       [:> rn/View {:style (.-icon styles)}
        [:> IconButton {:size 32 :icon "clock-outline" :color (:foreground theme)}]]
       [:> rn/View {:style (.-markingValue styles)}
        [:> rn/View {:style (.-property styles)}
         [:> rn/Text {:style {:color (:foreground theme)}}"Date: "]
         [date-picker date-time]]]])))

(defn- marking-repeat [repeat!]
  (let [visible (r/atom false)
        repeat-item (fn [key value]
                      [:> (.-Item Menu) {:titleStyle #js {:color (:purple theme)}
                                         :style #js {:backgroundColor (:current_line theme)}
                                         :on-press (fn []
                                                     (do
                                                       (swap! visible not)
                                                       (reset! repeat! key)))
                                         :title value}])
        anchor (fn [props]
                 [:> rn/TouchableOpacity {:on-press #(swap! visible not)
                                          :style {:margin 5}}
                  [:> rn/Text {:style {:color (:purple theme)
                                       :fontSize 20
                                       :fontWeight "bold"}} (:title props)]])]
    (fn []
      [:> rn/View {:style (.-markingProperty styles)}
       [:> rn/View {:style (.-icon styles)}
        [:> IconButton {:size 32 :icon "repeat" :color (:foreground theme)}]]
       [:> rn/View {:style (.-markingValue styles)}
        [:> rn/View {:style (.-property styles)}
         [:> rn/Text {:style {:color (:foreground theme)}}"Repeat?"]
         [:> Menu {:visible @visible
                   :contentStyle #js {:backgroundColor (:current_line theme)}
                   :on-dismiss #(swap! visible not)
                   :anchor (r/create-element
                            (r/reactify-component anchor) #js{:title (@repeat! repeat-string)})}
          (for [[key value] repeat-string]
            ^{:key key}
            [repeat-item key value])]]]])))

(defn marking-name [mode name!]
  (let [name-label (str mode " name")]
    (fn []
      [:> rn/View {:style (.-markingProperty styles)}
       [:> rn/View {:style (.-icon styles)}
        [:> IconButton {:size 32 :icon "calendar" :color (:foreground theme)}]]
       [:> rn/View {:style (.-markingValue styles)}
        [:> TextInput {:label name-label :value @name! :on-change-text #(reset! name! %)}]]])))

(defn save-button [{:keys [disabled navigation type new]}]
  (fn []
    [:> Button {:mode "contained"
                :disabled (when disabled @disabled)
                :color (:pink theme)
                :on-press #(do
                             (dispatch [:add-data
                                        {:type type
                                         :new @new}])
                             (.popToTop navigation))}
     "Save"]))

(defn update-button [{:keys [disabled navigation type new]}]
  (fn []
    [:> Button {:mode "contained"
                :disabled (when disabled @disabled)
                :color (:pink theme)
                :on-press #(do
                             (dispatch [:update-data
                                        {:type type
                                         :marking @new}])
                             (.popToTop navigation))}
     "Update"]))

(defn adding-view [name description time repeat action]
  (fn []
    [:> rn/View {:style (.-marking styles)}
     [:> rn/View {:style (.-markingProperties styles)}
      name
      [:> Divider]
      description
      [:> Divider]
      time
      [:> Divider]
      repeat]
     action]))

(defn- delete [{:keys [navigation type id]}]
  (do
    (dispatch [:delete-data
               {:type type
                :id id}])
    (.popToTop navigation)))

(defn event-view
  [default action]
  (let [new (r/atom default)
        allDay (r/atom false)
        name (r/cursor new [:name])
        repeat (r/cursor new [:repeat])
        time (r/cursor new [:time])
        disabled? (r/atom false)]
    (fn [{:keys [navigation]}]
      [adding-view
       [marking-name "Event" name]
       [marking-description "Event"]
       [event-time disabled? allDay time]
       [marking-repeat repeat]
       [action {:disabled disabled?
                :navigation navigation
                :type :events
                :new new}]])))

;; use today for deault time
(defn todo-view [default action]
  (let [new (r/atom default)
        name (r/cursor new [:name])
        repeat (r/cursor new [:repeat])
        time (r/cursor new [:time])]
    (fn [{:keys [navigation]}]
      [adding-view
       [marking-name "Todo" name]
       [marking-description "Todo"]
       [todo-time time]
       [marking-repeat repeat]
       [action {:navigation navigation
                :type :todos
                :new new}]])))

(defn update-view [view type {:keys [navigation route]}]
  (let [marking (.-params route)
        main-view (view marking update-button)]
    [:> rn/View {:style {:flex 1}}
     [main-view {:navigation navigation}]
     (when (:id marking)
       [fab {:style (.-deleteFab styles)
             :icon "delete"
             :callback #(delete {:navigation navigation
                                 :type type
                                 :id (:id marking)})}])]))

(defonce event-default {:name ""
                        :time (interval (now) (tomorrow))
                        :repeat :no})

(def event-add-view (event-view event-default save-button))

(def event-update-view (partial update-view event-view :events))
(defonce todo-default {:name ""
                       :time (now)
                       :repeat :no})

(def todo-add-view (todo-view todo-default save-button))
(def todo-update-view (partial update-view todo-view :todos))
