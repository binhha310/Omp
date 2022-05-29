(ns app.events
  (:require
   [app.db :refer [default-db save-local-database]]
   [re-frame.core :refer [reg-event-db reg-event-fx reg-fx inject-cofx path after]]
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

;; (defn print-and-return [data]
;;   (do
;;     (println data)
;;     data))

(defn initialise-db [{:keys [db local-storage-data]}]
  {:db (assoc default-db :data local-storage-data)})

(reg-fx
 :async-db
 (fn [promise]
   (-> promise
       (.then (fn [value]
                (reset! re-frame.db/app-db value))))))

(reg-event-fx
 :initialise-db
 [(inject-cofx :local-store-data)
  check-spec-interceptor]
 (fn [{:keys [db local-store-data]} _]
   {:db default-db
    :async-db local-store-data}))
