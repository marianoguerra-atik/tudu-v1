(defproject tudu "0.1.0-SNAPSHOT"
  :description "An example project that builds an end to end todo list"
  :url "http://github.com/marianoguerra-atik/tudu"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.7.228"]
                 [org.omcljs/om "1.0.0-alpha30"]]

  :plugins [[lein-cljsbuild "1.1.2"]
            [lein-figwheel "0.5.0-3"]]

  :clean-targets ^{:protect false} ["resources/public/js/compiled"
                                    "target"]

  :cljsbuild {:builds [{:id "dev"
                        :source-paths ["src"]
                        :figwheel true
                        :compiler {:main       "tudu.ui"
                                   :asset-path "js/compiled/out"
                                   :output-to  "resources/public/js/compiled/tudu.js"
                                   :output-dir "resources/public/js/compiled/out"
                                   :source-map-timestamp true }}
                       {:id "prod"
                        :source-paths ["src"]
                        :compiler {:main       "tudu.ui"
                                   :asset-path "js/compiled/out"
                                   :output-to  "resources/public/js/compiled/tudu.js"
                                   :optimizations :advanced}}]}

  :figwheel { :css-dirs ["resources/public/css"] })
