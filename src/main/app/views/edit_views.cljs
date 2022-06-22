(ns app.views.edit-views
  (:require
   ["react-native" :as rn]
   [app.components :refer (Text DateTimePicker Icon PickerMenu)]
   [reagent.core :as r]
   [tick.core :as t :refer (interval? beginning end)]
   [re-frame.core :refer (dispatch)]
   [app.views.activities :refer (fab)]
   [app.views.themes
    :refer (dracula)
    :rename {dracula theme}]
   [app.utils :refer (repeat-string now get-date tomorrow interval next-hour)]
   ["react-native-paper" :refer (TextInput Divider List Switch Button Banner Portal)]))

(def styles
  ^js (-> {:banner
           {:position "static"
            :top 0
            :color (:red theme)}
           :container
           {:flex 1}
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

(defn marking-description []
  (fn []
    [:> rn/View {:style (.-markingProperty styles)}
     [:> rn/View {:style (.-icon styles)}
      [Icon {:size 32 :icon "text"}]]
     [:> rn/View {:style (.-markingValue styles)}
      [:> TextInput {:label "Description"}]]]))

(defn event-time [disabled? time]
  (let [beginning (r/atom (beginning @time))
        end (r/atom (end @time))
        dateOnly (r/atom false)
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
           [Icon {:size 32 :icon "clock-outline"}]]
          [:> rn/View {:style (.-markingValue styles)}
           [:> rn/View {:style (.-property styles)}
            [Text "Entire day?"]
            [:> Switch {:value @dateOnly :onValueChange #(swap! dateOnly not)}]]
           [:> rn/View {:style (.-property styles)}
            [Text "Beginning: "]
            [DateTimePicker {:dateOnly dateOnly} beginning]]
           [:> rn/View {:style (.-property styles)}
            [Text "Ending: "]
            [DateTimePicker {:dateOnly dateOnly} end]]]]]))))

(defn todo-time [date]
  (let [date-time (r/atom (now))]
    (fn []
      (reset! date (get-date @date-time))
      [:> rn/View {:style (.-markingProperty styles)}
       [:> rn/View {:style (.-icon styles)}
        [Icon {:size 32 :icon "clock-outline"}]]
       [:> rn/View {:style (.-markingValue styles)}
        [:> rn/View {:style (.-property styles)}
         [Text "Date: "]
         [DateTimePicker date-time]]]])))

(defn- marking-repeat [value!]
  (let [menuShown (r/atom false)]
    (fn []
      [:> rn/View {:style (.-markingProperty styles)}
       [:> rn/View {:style (.-icon styles)}
        [Icon {:size 32 :icon "repeat"}]]
       [:> rn/View {:style (.-markingValue styles)}
        [:> rn/View {:style (.-property styles)}
         [Text "Repeat?"]
         [PickerMenu {:value! value! :menuShown menuShown :value-map repeat-string}]]]])))

(defn marking-name [mode name!]
  (let [name-label (str mode " name")]
    (fn []
      [:> rn/View {:style (.-markingProperty styles)}
       [:> rn/View {:style (.-icon styles)}
        [Icon {:size 32 :icon "calendar"}]]
       [:> rn/View {:style (.-markingValue styles)}
        [:> TextInput {:label name-label :value @name! :on-change-text #(reset! name! %)}]]])))

(defn save-button [{:keys [disabled navigation type new]}]
  (fn []
    [:> Button {:mode "contained"
                :disabled (when disabled @disabled)
                :color (:green theme)
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
                :color (:green theme)
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
        name (r/cursor new [:name])
        repeat (r/cursor new [:repeat])
        time (r/cursor new [:time])
        disabled? (r/atom false)]
    (fn [{:keys [navigation]}]
      [adding-view
       [marking-name "Event" name]
       [marking-description "Event"]
       [event-time disabled? time]
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
        main-view (view marking update-button)
        fab-style {:position "absolute"
                   :margin 16
                   :backgroundColor (:red theme)
                   :marginBottom 48
                   :bottom 0
                   :right 0}]
    [:> rn/View {:style {:flex 1}}
     [main-view {:navigation navigation}]
     (when (:id marking)
       [fab {:style fab-style
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
