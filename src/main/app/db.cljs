(ns app.db
  (:require
   [cljs.reader]
   [clojure.spec.alpha :as s]
   [re-frame.core :as rf]
   ["@react-native-async-storage/async-storage" :default AsyncStorage]))

(defn valid-date? [date]
  (every? #{"_year" "_month" "_day"} (.keys js/Object date)))

(s/def ::id number?)
(s/def ::name string?)
(s/def ::repeat keyword?)
(s/def ::common (s/keys :req-un [::id ::name ::repeat]))

(s/def ::interval (s/keys :req [:tick/beginning :tick/end]))
(s/def :event/time ::interval)
(s/def ::event (s/merge ::common
                        (s/keys :req-un [:event/time])))
(s/def ::events (s/coll-of ::event))

(s/def ::date valid-date?)
(s/def :todo/time ::date)
(s/def ::todo (s/merge ::common
                       (s/keys :req-un [:todo/time])))
(s/def ::todos (s/coll-of ::todo))

(s/def ::data (s/keys :req-un [::events ::todos]))
(s/def ::showing #(every? #{:all :events :todos} %))

(s/def ::db (s/keys :req-un [::data ::showing]))

(def default-db {:data
                 {:events []
                  :todos []}
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
