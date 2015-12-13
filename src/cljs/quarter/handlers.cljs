(ns quarter.handlers
    (:require [re-frame.core :as re-frame]
              [ajax.core :refer [GET PUT]]
              [quarter.db :as db]))

(re-frame/register-handler
 :initialize-db
 (fn  [_ _]
   db/default-db))

(re-frame/register-handler
 :set-active-panel
 (fn [db [_ active-panel]]
   (assoc db :active-panel active-panel)))

(re-frame/register-handler
  :set-name-site
  (fn [db [_ scout-name site]]
    (assoc db :name scout-name :site site :save-status :changed)))

(re-frame/register-handler
  :set-count
  (fn [db [_ gear-id new-value]]
    (-> db
        (assoc-in [:gear gear-id :delta] new-value)
        (assoc :save-status :changed))))

(re-frame/register-handler
  :set-note
  (fn [db [_ gear-id note]]
    (-> db
        (assoc-in [:gear gear-id :note] note)
        (assoc :save-status :changed))))

(re-frame/register-handler
  :save-db
  (fn [db _]
    (println "saving shakedown " db)
    (PUT (str "/shakedowns")
         {:params {:shakedown (into {} (map (fn [id] [id (id db)]) [:site :name :gear]))}
         :handler #(re-frame/dispatch [:save-success %])
         :error-handler #(re-frame/dispatch [:save-fail %])
         :format :json
          }
    )
    (assoc db :save-status :sent)
    db
    ))

(re-frame/register-handler
  :save-success
  (fn [db [_ result]]
    (println "Put response " result)
    (if (= (:save-status :sent)) (assoc db :save-status :saved) db)
    ))

(re-frame/register-handler
  :save-fail
  (fn [db [_ {:keys [status status-text]}]]
    (println "saved failed " status " " status-text)
    (assoc db :save-status (str "Save failed " status " " status-text))))
