(ns app.db
  (:require
   [cljs.reader]
   [clojure.spec.alpha :as s]
   [re-frame.core :as rf]
   [app.utils :refer (year-month)]
   ["@react-native-async-storage/async-storage" :default AsyncStorage]))

(defn valid-date? [date]
  (every? #{"_year" "_month" "_day"} (.keys js/Object date)))

(defn promise? [p]
  (every? #{"_U" "_V" "_W" "_X"} (.keys js/Object p)))

(defn month? [m]
  (every? #{"_year" "_month"} (js/Object.keys m)))

(s/def ::id string?)
(s/def ::name string?)
(s/def ::repeat keyword?)
(s/def ::common (s/keys :req-un [::id ::name ::repeat]))

(s/def ::interval (s/keys :req [:tick/beginning :tick/end]))
(s/def :event/time ::interval)
(s/def ::event (s/merge ::common
                        (s/keys :req-un [:event/time])))
(s/def ::events (s/coll-of ::event))

(s/def ::date valid-date?)
(s/def ::month month?)

(s/def :todo/time ::date)
(s/def ::todo (s/merge ::common
                       (s/keys :req-un [:todo/time])))
(s/def ::todos (s/coll-of ::todo))

(s/def ::data (s/keys :req-un [::events ::todos]))
(s/def ::showing #(every? #{:all :events :todos} %))

(s/def ::db (s/keys :req-un [::data ::showing ::month]))

(s/def ::promise promise?)
(def default-db {:data
                 {:events []
                  :todos []}
                 :month (year-month)
                 :showing []})

(def ls-key "omp-database")

(defn save-local-database [db]
  (.setItem AsyncStorage ls-key (str db)))

(defn get-local-database+ []
  (try
    (some->
     (.getItem AsyncStorage ls-key)
     (.then cljs.reader/read-string))
    (catch js/Error err (js/console.log (ex-cause err)))))

(rf/reg-cofx
 :local-store-data
 (fn [cofx _]
   (assoc
    cofx :local-store-data (get-local-database+))))
