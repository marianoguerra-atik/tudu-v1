(defproject tudu "0.1.0-SNAPSHOT"
  :description "An example project that builds an end to end todo list"
  :url "http://github.com/marianoguerra-atik/tudu"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.7.228"]
                 [org.clojure/core.async "0.2.374"]
                 [org.omcljs/om "1.0.0-alpha30"]
                 [cljs-http "0.1.38"]

                 [com.h2database/h2 "1.4.190"]
                 [org.clojure/java.jdbc "0.4.2"]
                 [com.stuartsierra/component "0.3.1"]
                 [org.immutant/web "2.1.2"]
                 [com.cognitect/transit-clj "0.8.285"]
                 [bidi "1.21.1"]]

  :main tudu.core
  :aot :all
  :plugins [[lein-cljsbuild "1.1.2"]
            [lein-figwheel "0.5.0-3"]]

  :source-paths ["src"]
  :clean-targets ^{:protect false} ["resources/public/js/compiled"
                                    "target"]

  :profiles {:uberjar {:prep-tasks ["compile" ["cljsbuild" "once" "prod"]]}}
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
