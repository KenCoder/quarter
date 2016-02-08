(ns quarter.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame]))

(def standard-subs [:name :activity :site-list :user-token :active-panel :expected-gear :save-status :shake-list :shakedown])
(doseq [id standard-subs]
  (re-frame/register-sub
    id
    (fn [db]
      (reaction (id @db)))))


