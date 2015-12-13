(ns quarter.handler
  (:require [compojure.core :refer [GET defroutes PUT]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-params]]
            [ring.middleware.keyword-params :refer [wrap-keyword-params]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [quarter.db :as db]
            [clojure.data.json :as json]
            [clj-time.core :as t]
            [clj-time.format :as f]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.core :as appenders]
            [prone.middleware :refer [wrap-exceptions]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.util.response :refer [file-response]]
            )
  (:import (java.util Date)))
(timbre/refer-timbre)
(defn map-keywords [m]
  (into {} (for [[k v] m] [(keyword k) v])))

(defroutes
  json-handler
  (PUT "/shakedowns" [shakedown]
    (info "handling shakedown " shakedown)
    (let [{:keys [name site gear date]} shakedown]
      (if (and gear name site)
        (let [res (db/insert-shakedown<! {:gear (json/write-str gear) :name name :site site
                                :date (or date (f/unparse (f/formatter "yyyy-MM-dd") (t/now)))})]
          (info "Insert result is " res)
          res)
        {:status 400
         :body (str "Invalid response: must be "
                    (json/write-str {:shakedown {:name "scout name" :site "site" :gear {:gear-id {:count -1 :notes "gear notes"}}}}))
         }
      )))
  (GET "/shakedowns" [] (map #(update % :gear json/read-str) (db/get-shakedowns)))
  (PUT "/shakedowns/:id" [id shakedown]
    (let [new-shake (assoc (map-keywords shakedown) :id id)]
      (if (= 0 (db/update-shakedown! new-shake))
        (db/insert-shakedown<! (dissoc :id new-shake))
        new-shake)))
  )


(defroutes
  html-handler
  (GET "/" [] (file-response "index.html" {:root "resources/public"})))

(defroutes base-handler
           (-> json-handler wrap-keyword-params wrap-json-params wrap-json-response)
           html-handler)

(def handler (wrap-reload #'base-handler))

