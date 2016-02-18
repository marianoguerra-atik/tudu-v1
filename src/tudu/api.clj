(ns tudu.api
  (:require
    tudu.store
    [om.next.server :as om]))

(defmulti read om/dispatch)
(defmulti mutate om/dispatch)

(def parser (om/parser {:read read :mutate mutate}))

(defn query [{:keys [body request]}]
  (let [query-env {:app-env (:tudu/env request)}]
    {:status 200 :body (parser query-env body)}))

(defn not-found [_]
  {:status 404 :body {:error "Not Found"}})

(defmethod read :default [env key params]
  :not-found)

(defmethod read :tudu/items [{:keys [app-env]} key {:keys [id] :as params}]
  (let [{:keys [db]} app-env]
    {:value (vec (tudu.store/get-tasks db))}))

(defmethod mutate :default [_ key params]
  :notfound)

(defmethod mutate 'tudu.item/close [{:keys [app-env]} _ {:keys [id]}]
  (let [{:keys [db]} app-env]
    (tudu.store/close-task db id)
    {}))

(defmethod mutate 'tudu.item/create [{:keys [app-env]} _ {:keys [value]}]
  (let [{:keys [db]} app-env]
    (tudu.store/create-task db value)
    {}))

