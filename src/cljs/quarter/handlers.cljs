(ns quarter.handlers
  (:require [re-frame.core :as re-frame]
            [ajax.core :refer [GET POST PUT ajax-request json-request-format json-response-format]]
            [cljs-time.format :refer [formatter unparse]]
            [cljs-time.core :refer [today]]
            [goog.string :as gstring]
            [cognitect.transit :as t]
            [quarter.db :as db]))

(re-frame/register-handler
  :initialize-db
  (fn [_ _]
    db/default-db))

(re-frame/register-handler
  :set-active-panel
  (fn [db [_ panel]]
    (assoc db :active-panel panel)))

(defn now-str []
  (let [dt (js/Date.)]
    (str (.getFullYear dt) "-" (gstring/format "%02d" (inc (.getMonth dt))) "-" (gstring/format "%02d" (.getDate dt)))))

(re-frame/register-handler
  :update-login
  (fn [db [_]]
    (if-let [token (.-localStorage.userToken js/window)]
      (do
        (println "Have token" token)
        (re-frame/dispatch [:get-gear])
          (assoc db :user-token (db/json->LoginToken token) :activity "Getting gear" :active-panel :wait-activity))
      (dissoc db :user-token))))

(defn call-aws [params success-fn]
  (POST "https://6ii6u996ub.execute-api.us-east-1.amazonaws.com/prod"
        {:params          params
         :format          :json
         :response-format :json
         :keywords?       true
         :handler         (fn [resp]
                            (if (= "ok" (:status resp))
                              (re-frame/dispatch [:aws-success success-fn resp])
                              (re-frame/dispatch [:aws-fail (or (:errorMessage resp) "Server failure")])))
         :error-handler   (fn [{:keys [status status-text]}]
                            (.log js/console (str "Failed aws: " status " " status-text))
                            (re-frame/dispatch [:aws-fail status-text]))
         }
        ))


(re-frame/register-handler
  :aws-success
  (fn [db [_ success-fn resp]]
    (dissoc (success-fn db resp) :activity)))

(re-frame/register-handler
  :logout
  (fn [db [_]]
    (.removeItem (.-localStorage js/window) "userToken")
    (dissoc db :user-token )))

(re-frame/register-handler
  :aws-fail
  (fn [db [_ error]]
    (assoc db :activity (str "Error " error) :active-panel :wait-activity)))

(defn parse-sites [txt]
  (let [lines (clojure.string/split txt #"\n")
        matrix (map #(clojure.string/split % #"\t") lines)
        sites (apply map list matrix)                       ;; Transpose list
        ]
    (for [[name numbers & items] sites]
      (db/->ExpectedSite
        name
        (if (= "" numbers) "1" (clojure.string/split numbers #" "))
        (for [line items
              :while (or (first line) (second line))]
          (if (= "=" (first line))
            (db/map->ExpectedGear {:heading (subs line 1)})
            (let [[name qty] (clojure.string/split line #":")]
              (db/map->ExpectedGear {:gear-name name :quantity (if qty (js/parseInt qty) 1)}))))

        ))))

(defn save-token [db resp]
  (println "Saving token " resp)
  (.setItem (.-localStorage js/window) "userToken" (.stringify js/JSON (clj->js (:user resp))))
  (re-frame/dispatch [:get-gear])
  (assoc db :user-token (:user resp)))

(re-frame/register-handler
  :set-login
  (fn [db [_ token]]
    (println "set login xhr " token)
    (call-aws {:token token :operation "login"} save-token)
    (assoc db :activity "Logging in" :active-panel :wait-activity)
    ))

(re-frame/register-handler
  :get-gear
  (fn [db [_]]
    (call-aws {:operation "gear"
               :user      (:user-token db)
               }
              (fn [db resp] (assoc db :site-list (parse-sites (:expected-gear resp))))
              )
    (assoc db :activity "Getting gear list" :active-panel :wait-activity)
    ))


(defn shake-file [state]
  (str (:shake-date state) "/" (:site-name state) " " (:site-number state) "/" (:username state)))

(re-frame/register-handler
  :set-site
  (fn [db [_ site-info number]]
    (let [new-shakedown (db/map->Shakedown {:username      (get-in db [:user-token :name])
                                            :site-name     (:site-name site-info)
                                            :site-number   number
                                            :shake-date    (now-str)
                                            :gear-quantity {}
                                            :gear-notes    {}
                                            })]
      (call-aws {:operation "get"
                 :user      (:user-token db)
                 :file      (shake-file new-shakedown)
                 }
                (fn [db resp] (assoc db :shakedown (or (db/json->Shakedown (:shakedown resp)) new-shakedown) :active-panel :shakedown)))
      (assoc db :activity "Checking for existing shakedown"
                :expected-gear (:expected-gear site-info)
                :active-panel :wait-activity))))

(re-frame/register-handler
  :set-count
  (fn [db [_ gear-name actual expected]]
    (println "set-count " gear-name " " actual)
    (-> db
        (assoc-in [:shakedown :gear-quantity gear-name] (db/->ActualGear actual expected))
        (assoc :save-status :changed))))

(re-frame/register-handler
  :get-shakedowns
  (fn [db _]
    (call-aws {:operation "list"
               :user      (:user-token db)}
              (fn [db resp] (assoc db :shake-list (:shakedowns resp))))
    (assoc db :shake-list [] :activity "Listing" :active-panel :shake-list)))

(re-frame/register-handler
  :set-note
  (fn [db [_ gear-name note]]
    (-> db
        (assoc-in [:shakedown :gear-notes gear-name] note)
        (assoc :save-status :changed))))

(re-frame/register-handler
  :view-shakedown
  (fn [db [_ shake-path]]
    (call-aws {:operation "get"
               :user      (:user-token db)
               :file      shake-path}
              (fn [db resp] (assoc db :shakedown (db/json->Shakedown (:shakedown resp)) :active-panel :view-shakedown)))
    (assoc db :activity "Loading shakedown" :active-panel :wait-activity)))

(re-frame/register-handler
  :save-db
  (fn [db _]
    (println "saving shakedown " db)
    (let [state (:shakedown db)]
      (call-aws {:operation "store"
                 :user      (:user-token db)
                 :file      (shake-file state)
                 :body      (.stringify js/JSON (clj->js state))}
                (fn [db resp] (assoc db :save-status :saved)))
      (assoc db :activity "saving"))
    ))


