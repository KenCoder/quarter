(ns quarter.server
  (:require
    [compojure.core :refer [GET defroutes PUT]]
    [quarter.handler :refer [handler html-handler]]
            [environ.core :refer [env]]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.middleware.json :refer [wrap-json-response wrap-json-params]]
            [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
            [taoensso.timbre :as timbre]
            [taoensso.timbre.appenders.core :as appenders]
            [prone.middleware :refer [wrap-exceptions]]
            [ring.middleware.reload :refer [wrap-reload]]
            [ring.util.response :refer [file-response]]
            )
  (:gen-class))
(timbre/refer-timbre)


(timbre/merge-config!
  {:appenders {:spit (appenders/spit-appender {:fname "server.log"})}})

(info "Timbre configured")

(def app
  handler)

(defn -main [& args]
  (let [port (Integer/parseInt (or (env :port) "3000"))]
    (run-jetty app {:port port :join? false})))
