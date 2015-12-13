(ns quarter.views
  (:require [re-frame.core :as re-frame]
            [quarter.gear :as gear]
            [reagent.core :as reagent]))


;; home

(def jquery (js* "$"))


(defn dialog [notes-holder note-gear-id]
  (reagent/create-class
    {:component-did-mount
   (fn [] (-> "#notesModal" jquery (.on "shown.bs.modal" (fn [] (-> "#message-text" jquery .focus)))))
   :reagent-render
   (fn []
     [:div#notesModal.modal.fade
      {:tabindex        "-1",
       :role            "dialog",
       :aria-labelledby "notesLabel"}
      [:div.modal-dialog
       {:role "document"}
       [:div.modal-content
        [:div.modal-header
         [:button.close
          {:type "button", :data-dismiss "modal", :aria-label "Close"}
          [:span {:aria-hidden "true"} "×"]]
         [:h4#notesLabel.modal-title "gear"]]
        [:div.modal-body
         [:form
          [:div.form-group
           [:label.control-label {:for "message-text"} "Notes:"]
           [:textarea#message-text.form-control {:value     @notes-holder
                                                 :on-change #(reset! notes-holder (-> % .-target .-value))
                                                 }]]]]
        [:div.modal-footer
         [:button.btn.btn-default
          {:type     "button"
           :on-click #(reset! notes-holder "")}
          "Clear"]
         [:button.btn.btn-primary {:type         "button"
                                   :data-dismiss "modal"
                                   :on-click     #(re-frame/dispatch [:set-note @note-gear-id @notes-holder])
                                   } "Save"]]]]]
     )}))

(defn do-shakedown []
  (let [shake-gear (re-frame/subscribe [:gear])
        saved-state (re-frame/subscribe [:saved-state])
        name-and-site (re-frame/subscribe [:name-and-site])
        notes-holder (reagent/atom nil)
        note-gear-id (reagent/atom nil)]
    (fn []
      (print "shakedown " @shake-gear)
      (let [[name site] @name-and-site
            site-type (gear/gear-sites site)
            required-gear (site-type gear/gear-types)]
        [:div
         [dialog notes-holder note-gear-id]
         [:h1 (str site " - " name)]
         [:button.btn.btn-xs {:type "button" :on-click #(re-frame/dispatch [:save-db])}
          [:span {:class (str "glyphicon glyphicon-" (condp = @saved-state  :saved "floppy-saved" :sent "floppy-save" "floppy-disk"))
                    :aria-hidden "true"}]]

         (doall (for [[category items] required-gear]
                  ^{:key category}
                  [:div
                   [:h2 category]
                   [:table.table.table-bordered
                    (doall (for [{:keys [:gear_id :description :count]} items
                                 :let [current-delta (get-in @shake-gear [gear_id :delta])
                                       note (get-in @shake-gear [gear_id :note])]]
                             ^{:key gear_id}
                             [:tr {:class (cond (= current-delta nil) ""
                                                (= current-delta 0) "success"
                                                (< current-delta 0) "danger"
                                                :else "warning")}
                              [:td
                               [:button.btn.btn-xs {:type "button" :on-click #(re-frame/dispatch [:set-count gear_id (inc (or current-delta 0))])}
                                [:span.glyphicon.glyphicon-plus {:aria-hidden "true"}]]]
                              [:td
                               [:button.btn.btn-xs {:type "button" :on-click #(re-frame/dispatch [:set-count gear_id
                                                                                                  (max 0 (dec (or current-delta 0)))])}
                                [:span.glyphicon.glyphicon-minus {:aria-hidden "true"}]]
                               ]
                              [:td
                               [:button {:type          "button"
                                         :data-toggle   "modal"
                                         :class         (str "btn btn-xs" (if (clojure.string/blank? note) "" " btn-info"))
                                         :data-target   "#notesModal"
                                         :data-whatever gear_id
                                         :on-click      (fn [e]
                                                          (reset! notes-holder note)
                                                          (reset! note-gear-id gear_id)
                                                          (-> (jquery "#notesLabel") (.text description)))
                                         }
                                [:span.glyphicon.glyphicon-pencil {:aria-hidden "true"}]]
                               ]
                              [:td
                               [:span (str (if (nil? current-delta) "" (str (+ count current-delta) " of ")) count " " description)]
                               ]
                              ]))]]))]))))


(defn prompt-name []
  (let [scout-name (reagent/atom nil)]
    (fn []
      [:div
       [:h1 "Select gear site"]
       [:form#select {:action "/shake"}
        (for [name gear/default-people] ^{:key name}
                                        [:button.btn.btn-default {:type "button" :on-click #(reset! scout-name name)} name])
        [:div#name_div.form-group
         [:label.control-label "Scout Name"]
         [:input.form-control {:type       "text"
                               :value      @scout-name
                               :auto-focus true
                               :name       "name_input"
                               :on-change  #(reset! scout-name (-> % .-target .-value))}]]

        [:div (for [[site _] gear/gear-sites]
                ^{:key site}
                [:button.btn.btn-default {:type     "button"
                                          :on-click #(re-frame/dispatch [:set-name-site @scout-name site])
                                          } site]
                )]
        ]])))



(defn home-panel []
  (let [info (re-frame/subscribe [:name-and-site])]
    (fn []
      (let [[name site] @info]
        (if (or (nil? name) (nil? site))
          [prompt-name]
          [do-shakedown])))))

;; about

(defn about-panel []
  (fn []
    [:div "This is the About Page."
     [:div [:a {:href "#/"} "go to Home Page"]]]))


;; main

(defmulti panels identity)
(defmethod panels :home-panel [] [home-panel])
(defmethod panels :about-panel [] [about-panel])
(defmethod panels :default [] [:div])

(defn main-panel []
  (let [active-panel (re-frame/subscribe [:active-panel])]
    (fn []
      (panels @active-panel))))
