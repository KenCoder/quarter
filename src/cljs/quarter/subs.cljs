(ns quarter.subs
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :as re-frame]))

(def standard-subs [:name :active-panel :gear :saved-state])
(doseq [id standard-subs]
  (re-frame/register-sub
    id
    (fn [db]
      (reaction (id @db)))))

(re-frame/register-sub
  :name-and-site
  (fn [db _]
    (reaction [(:name @db) (:site @db)])))
