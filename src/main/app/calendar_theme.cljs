(ns app.calendar-theme)

(def calendar-theme {:backgroundColor "#282a36"
                     :calendarBackground "#282a36"
                     :dayTextColor "#f8f8f2"
                     :textDisabledColor "#6272a4"
                     :todayTextColor "#f1fa8c"
                     :monthTextColor "#ff79c6"
                     :textSectionTitleColor "#ff79c6"
                     :arrowColor "#8be9fd"})

(def calendar-main
  {:stylesheet.calendar.main
   {:dayContainer
    {:flex 1
     :margin 0
     :alignItems "center"
     :borderColor "#44475a"
     :borderWidth 1
     :borderStyle "solid"}
    :container
    {:justifyContent "space-evenly"
     :backgroundColor "#282a36"
     :paddingLeft 5
     :height "100%"
     :paddingBottom 5
     :paddingRight 5}
    :week
    {:flex 1
     :flexDirection "row"}
    :monthView
    {:marginTop 0
     :flex 1
     :backgroundColor "#282a36"
     :justifyContent "space-evenly"}}})

(def marking
  {:stylesheet.marking
   {:periods
    {:position "absolute"
     :width "100%"
     :top "30%"}}})

(def day-basic
  {:stylesheet.day.basic
   {:base
    {:width "100%"
     :height "100%"
     :alignItems "center"}
    :container
    {:margin 0
     :flex 1
     :alignSelf "stretch"
     :alignItems "center"}}})
