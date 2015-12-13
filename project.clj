(defproject quarter "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.170"]
                 [environ "1.0.1"]
                 [ring "1.4.0"]
                 [ring-server "0.4.0"]
                 [ring/ring-defaults "0.1.5"]
                 [ring/ring-json "0.4.0"]
                 [com.taoensso/timbre "4.1.4"]
                 [clj-json "0.5.3"]
                 [compojure "1.4.0"]
                 [yesql "0.5.1"]
                 [prone "0.8.2"]
                 [org.clojure/java.jdbc "0.4.1"]
                 [org.postgresql/postgresql "9.4-1201-jdbc41"]
                 [reagent "0.5.1"]
                 [cljs-ajax "0.5.2"]
                 [re-frame "0.5.0"]
                 [secretary "1.2.3"]
                 ]

  ;; Necessary because figwheel doesn't report compile errors
  :ring {:handler quarter.handler/handler}

  :source-paths ["src/clj"]

  :plugins [[lein-cljsbuild "1.1.1"]
            [lein-ring "0.9.7"]
            [lein-figwheel "0.5.0-2"] ]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :figwheel {:css-dirs ["resources/public/css"]
             :ring-handler quarter.handler/handler}

  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src/cljs"]

                        :figwheel {:on-jsload "quarter.core/mount-root"}

                        :compiler {:main quarter.core
                                   :output-to "resources/public/js/compiled/app.js"
                                   :output-dir "resources/public/js/compiled/out"
                                   :asset-path "js/compiled/out"
                                   :source-map-timestamp true}}

                       {:id "min"
                        :source-paths ["src/cljs"]
                        :compiler {:main quarter.core
                                   :output-to "resources/public/js/compiled/app.js"
                                   :optimizations :advanced
                                   :closure-defines {goog.DEBUG false}
                                   :pretty-print false}}]})
