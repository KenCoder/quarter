(ns quarter.db
  (:require [yesql.core :refer [defqueries]]))

(def db-spec {:classname "org.postgresql.Driver"
              :subprotocol "postgresql"
              :subname "//localhost:5432/quarter"
              })

(defqueries "sql/shakedown.sql"
            {:connection db-spec})

