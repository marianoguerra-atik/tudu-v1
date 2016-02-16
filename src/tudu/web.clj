(ns tudu.web
  (:require
    tudu.data
    [tudu.api :as api]
    om.next.server
    [com.stuartsierra.component :as component]
    [bidi.ring :refer [make-handler]]
    [ring.middleware.resource :refer [wrap-resource]]
    [immutant.web.middleware :refer [wrap-websocket]]
    [immutant.web :as web]))

(defn- wrap-add-component [app component]
  (fn [req]
    (app (assoc req :tudu/env component))))

(defn- start-component-web-api-handler [component host port path app]
  (let [new-handler (web/run (wrap-add-component app component)
                             {:host host :port port :path path})
        new-component (assoc component
                             :handler new-handler
                             :host host
                             :port port
                             :path path
                             :app app)]
    new-component))

(defn response-to-transit [{:keys [body headers] :as response}]
  (assoc response
         :body (tudu.data/to-string body)
         :headers (assoc headers "Content-Type" "application/transit+json")))

(defn call-handler [handler has-body {:keys [body] :as request}]
  (let [req-body (when has-body (tudu.data/parse body))]
    (try
      (handler {:body req-body :request request})
      (catch Throwable ex
        (println ex "Error calling handler")
        {:status 500 :body {:error "Internal Error"}}))))

(defn wrap-handler [handler has-body]
  (fn [request]
    (response-to-transit (call-handler handler has-body request))))

(def req-handlers {:query       (wrap-handler api/query true)
                   :not-found   (wrap-handler api/not-found false)})

(def routes ["/" {"query" {:post :query}
                  true :not-found}])

(defrecord WebAPI [host port base-path app db]
  component/Lifecycle
  (start [{:keys [handler] :as component}]
    (println "Starting Web API at" (str host ":" port base-path))
    (if (nil? handler)
      (let [app (-> (make-handler routes req-handlers)
                    (wrap-resource  "public"))]
        (start-component-web-api-handler component host port base-path app))

      (do
        (println "Existing Web API, Stoping and Starting New")
        (web/stop handler)
        (start-component-web-api-handler component host port base-path app))))

  (stop [{:keys [handler host port path] :as component}]
    (println "Stopping Web API at" (str host ":" port path))
    (if (nil? handler)
      (println "No Handler, No Op")

      (do
        (web/stop handler)
        ; dissoc one of a record's base fields, you get a plain map
        (assoc component :handler nil)))))

(defn new-web-api [config]
  (map->WebAPI config))
