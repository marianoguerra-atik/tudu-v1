(ns tudu.core
  (:require
    tudu.store
    tudu.web
    [com.stuartsierra.component :as component]))

(defn system [{:keys [web-info db-info]}]
  (component/system-map
    :db (tudu.store/new-db db-info)
    :web-api (component/using (tudu.web/new-web-api web-info)
                              [:db])))

(defn new-system [host port base-path]
  (system {:web-info {:host host
                      :port port
                      :base-path base-path}
           :db-info {:classname   "org.h2.Driver"
                     :subprotocol "h2:file"
                     :subname     "./tudu"
                     :user        "tudu"
                     :password    ""
                     :naming {:keys clojure.string/lower-case
                              :fields clojure.string/upper-case}
                     :make-pool? true
                     :excess-timeout 99
                     :idle-timeout 88
                     :minimum-pool-size 5
                     :maximum-pool-size 20
                     :test-connection-on-checkout true
                     :test-connection-query "SELECT 1"}}))

(defn start [host port base-path]
  (component/start (new-system host port base-path)))

(defn stop [system]
  (component/stop system))

(defn -main [& args]
  (start "localhost" 8080 "/"))
