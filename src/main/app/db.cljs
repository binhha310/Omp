(ns app.db
  (:require [clojure.spec.alpha :as s]))

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

(s/def ::db (s/keys :req-un [::events ::todos]))
(s/conform ::db app.main/data)
