(ns quarter.views
  (:require [re-frame.core :as re-frame]
            [reagent.core :as reagent]
            [quarter.db :as db]))


;; home

(def jquery (js* "$"))
(def AUTH0_CLIENT_ID "kTGZCC3GfHMkXeHqC4lnI1fVV0ykmGEm")
(def AUTH0_DOMAIN "kencoder.auth0.com")
(def AUTH0_CALLBACK_URL (.-location.-href js/window))
(def auth0-lock (delay (js/Auth0Lock. AUTH0_CLIENT_ID AUTH0_DOMAIN)))

(defn dialog [notes-holder note-gear-id]
  (reagent/create-class
    {:component-did-mount
     (fn [] (-> "#notesModal" jquery (.on "shown.bs.modal" (fn [] (-> "#message-text" jquery .focus)))))
     :reagent-render
     (fn []
       [:div#notesModal.modal.fade
        {:tabIndex        "-1",
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

(defn echo [msg x]
  (println msg x)
  x)


(defn shake-list []
  (let [shakes (re-frame/subscribe [:shake-list])]
    (fn []
      (doall (into [:table.table.table-bordered
                    [:tr [:th "Date"] [:th "Site"] [:th "User"]]]
                   (for [shake @shakes
                         :let [[shake-date gear user] (echo "split string is " (clojure.string/split (echo "shake is " shake) #"/"))]]
                     (with-meta [:tr [:td shake-date] [:td [:a {:href "#" :on-click #(re-frame/dispatch [:view-shakedown shake])} gear]] [:td user]] {:key (str shake-date gear user)}))
                   )))))

(defn view-shakedown []
  (let [shakedown (re-frame/subscribe [:shakedown])
        site-list (re-frame/subscribe [:site-list])]
    (fn []
      (let [site-name (:site-name @shakedown)
            expected-gear (:expected-gear (first (filter #(= (:site-name %) site-name) @site-list)))
            known-gear-names (set (map :gear-name expected-gear))
            gear-quantity (:gear-quantity @shakedown)
            gear-notes (:gear-notes @shakedown)
            deleted-gear (sort (filter #(nil? (known-gear-names %)) (concat (keys gear-quantity) (keys gear-notes))))
            all-gear (if (empty? deleted-gear)
                       expected-gear
                       (concat expected-gear
                               [(db/->ExpectedGear "Items removed from required list" nil nil)]
                               (map #(db/->ExpectedGear nil % nil) deleted-gear)))]
        (println deleted-gear)
        [:div
         [:h2 (str site-name " - " (:site-number @shakedown))]
         [:h3 (:username @shakedown)]
         (into [:table.table.table-bordered
                [:tr [:th "Item"] [:th "Actual"] [:th "Expected"] [:th "Notes"]]]
               (doall (for [{:keys [heading gear-name quantity]} all-gear]
                        (with-meta
                          (if heading
                            [:tr [:td {:colSpan 4} [:h3 heading]]]
                            (let [actual-qty (:actual-quantity (gear-quantity gear-name))
                                  note (gear-notes gear-name)]
                              [:tr {:class (cond (nil? actual-qty) ""
                                                 (= actual-qty quantity) "success"
                                                 (< actual-qty quantity) "danger"
                                                 :else "warning")}
                               [:td gear-name]
                               [:td.number actual-qty]
                               [:td.number quantity]
                               [:td note]]))
                          {:key (str heading gear-name)}))))]))))

(defn do-shakedown []
  (let [expected-gear (re-frame/subscribe [:expected-gear])
        shakedown (re-frame/subscribe [:shakedown])
        notes-holder (reagent/atom nil)
        note-gear-id (reagent/atom nil)]
    (fn []
      (println "expected gera " @expected-gear)
      (let [{:keys [gear-notes gear-quantity site-name site-number]} @shakedown
            ]
        [:div
         [dialog notes-holder note-gear-id]
         [:h1 (str site-name " - " site-number)]
         [:table.table.table-bordered
          (doall (for [{:keys [heading gear-name quantity]} @expected-gear]
                   (with-meta
                     (if heading
                       [:tr [:td {:colSpan 4} [:h2 heading]]]
                       (let [actual-qty (:actual-quantity (gear-quantity gear-name))
                             note (gear-notes gear-name)]
                         [:tr {:class (cond (nil? actual-qty) ""
                                            (= actual-qty quantity) "success"
                                            (< actual-qty quantity) "danger"
                                            :else "warning")}
                          [:td
                           [:button.btn.btn-xs {:type "button" :on-click #(re-frame/dispatch [:set-count gear-name (inc (or actual-qty 0)) quantity])}
                            [:span.glyphicon.glyphicon-plus {:aria-hidden "true"}]]]
                          [:td
                           [:button.btn.btn-xs {:type "button" :on-click #(re-frame/dispatch [:set-count gear-name
                                                                                              (max 0 (dec (or actual-qty 0))) quantity])}

                            [:span.glyphicon.glyphicon-minus {:aria-hidden "true"}]]
                           ]
                          [:td
                           [:button {:type          "button"
                                     :data-toggle   "modal"
                                     :class         (str "btn btn-xs" (if (clojure.string/blank? note) "" " btn-info"))
                                     :data-target   "#notesModal"
                                     :data-whatever gear-name
                                     :on-click      (fn [e]
                                                      (reset! notes-holder note)
                                                      (reset! note-gear-id gear-name)
                                                      (-> (jquery "#notesLabel") (.text gear-name)))
                                     }
                            [:span.glyphicon.glyphicon-pencil {:aria-hidden "true"}]]
                           ]
                          [:td
                           [:span (str (if (nil? actual-qty) "" (str actual-qty " of ")) quantity ": " gear-name)]
                           ]
                          ]))
                     {:key (str heading gear-name)})))]]))))


(defn prompt-site [name]
  (let [site-list (re-frame/subscribe [:site-list])]
    (fn []
      [:div
       [:p "Welcome " name]
       [:h1 "Select gear site"]
       [:table.table.table-bordered
        (doall (for [site-info @site-list
                     :let [{:keys [site-name numbers]} site-info]]
                 ^{:key site-name}
                 [:tr
                  [:td site-name]
                  [:td
                   (doall (for [number numbers]
                            ^{:key (str site-name " " number)}
                            [:button.btn.btn-default {:type     "button"
                                                      :on-click (fn [] (re-frame/dispatch [:set-site site-info number]))
                                                      } number]))]]
                 ))]
       ])))


(defn login []
  (fn []
    [:div [:h1 "Quartermaster login"]
     [:button#mysignin.btn.btn-default
      {:type     "button"
       :on-click (fn []
                   (.show @auth0-lock (fn [err profile token]
                                        (println "Login complete " err profile token)
                                        (if err
                                          (do (println "Auth error " err)
                                              (.alert js/window "Login error"))
                                          (re-frame/dispatch [:set-login token])))))

       } "Login"]
     ])

  )

(defn home-panel []
  (re-frame/dispatch-sync [:update-login])
  (let [activity (re-frame/subscribe [:activity])
        active-panel (re-frame/subscribe [:active-panel])
        user-login (re-frame/subscribe [:user-token])
        save-status (re-frame/subscribe [:save-status])
        shakedown (re-frame/subscribe [:shakedown])
        ]
    (fn []
      [:div
       [:nav.navbar.navbar-default.navbar-fixed-top
        [:div.container
         [:div.navbar-header
          [:button.navbar-toggle.collapsed {:type          "button" :data-toggle "collapse"
                                            :data-target   "#nav-collapse"
                                            :aria-expanded false}
           [:span.sr-only "Toggle navigation"]
           [:span.icon-bar] [:span.icon-bar] [:span.icon-bar]]
          [:a.navbar-brand {:href "#"} "Troop125"]]
         [:div#nav-collapse.collapse.navbar-collapse
          [:ul.nav.navbar-nav
           [:li.dropdown
            [:a.dropdown-toggle {:href "#" :data-toggle "dropdown" :role "button" :aria-haspopup true :aria-expanded false}
             "Actions" [:span.caret]]
            [:ul.dropdown-menu
             [:li [:a {:href "#" :on-click #(re-frame/dispatch [:set-active-panel :new-shakedown])} "New Shakedown"]]
             [:li [:a {:href "#" :on-click #(re-frame/dispatch [:get-shakedowns])} "View Shakedowns"]]
             [:li [:a {:href "#" :on-click #(re-frame/dispatch [:set-active-panel :gear-finder])} "Gear Finder"]]
             [:li [:a {:href "#" :on-click #(re-frame/dispatch [:logout])} "Logout"]]
             ]]]
          [:ul.nav.navbar-nav.navbar-right
           (concat [(with-meta [:li @activity] {:key "activity"})]
                   (if (= @save-status :changed)
                     [[:li [:button.btn.btn-xs.glyphicon.glyphicon-floppy-disk {:type     "button"
                                                                                :on-click #(re-frame/dispatch [:save-db])}
                            [:span {:aria-hidden "true"}]]]]
                     []))
           ]]]]
       (cond
         (and @activity (= @active-panel :wait-activity)) [:h2 @activity]
         (nil? @user-login) [login]
         (= @active-panel :shakedown) [do-shakedown]
         (= @active-panel :shake-list) [shake-list]
         (= @active-panel :view-shakedown) [view-shakedown]
         :else [prompt-site (str (:name @user-login))])
       ]
      )))


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
  (fn []
    (panels :home-panel)))
