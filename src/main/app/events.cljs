(ns app.events
  (:require
   [app.db :refer [default-db save-local-database]]
   [app.utils :refer (year-month)]
   [re-frame.core :refer [reg-event-db reg-event-fx reg-fx inject-cofx path after dispatch]]
   [cljs.spec.alpha :as s]))

(defn check-and-throw
  "Throws an exception if `db` doesn't match the Spec `a-spec`."
  [a-spec db]
  (when-not (s/valid? a-spec db)
    (throw (ex-info (str "spec check failed: " (s/explain-str a-spec db)) {}))))

(def check-spec-interceptor (after (partial check-and-throw :app.db/db)))
(def ->local-store (after save-local-database))
(def data-interceptors [check-spec-interceptor
                        (path :data)
                        ->local-store])

(defn allocate-next-id
  []
  (str (random-uuid)))

(reg-fx
 :async-db
 (fn [p]
   (-> (s/conform :app.db/promise p)
       (.then (fn [value]
                (check-and-throw :app.db/data value)
                (swap! re-frame.db/app-db #(assoc % :data value)))))))

(reg-event-fx
 :initialise-db
 [(inject-cofx :local-store-data)
  check-spec-interceptor]
 (fn [{:keys [db local-store-data]} _]
   {:db default-db
    :async-db local-store-data}))

(reg-event-db
 :change-month
 [check-spec-interceptor]
 (fn [db [_ {:keys [year month]}]]
   (assoc db :month (year-month year month))))

(reg-event-db
 :add-data
 data-interceptors
 (fn [data [_ {:keys [type new]}]]
   (let [new (if-not (:id new)
               (->> (allocate-next-id)
                    (assoc-in new [:id]))
               new)]
     (update-in data [type] #(conj % new)))))

(reg-event-db
 :delete-data
 data-interceptors
 (fn [data [_ {:keys [type id]}]]
   (let [has-id? (fn [marking] (= id (:id marking)))
         drop (fn [markings] (filter (complement has-id?) markings))]
     (update-in data [type] drop))))

(reg-event-db
 :update-data
 data-interceptors
 (fn [data [_ {:keys [type marking]}]]
   (do
     (dispatch [:delete-data {:type type :id (:id marking)}])
     (dispatch [:add-data {:type type :new marking}])
     data)))
