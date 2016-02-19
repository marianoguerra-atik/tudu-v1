(ns tudu.api
  (:require
    tudu.store
    [om.next.server :as om]))

(defmulti readf om/dispatch)
(defmulti mutatef om/dispatch)

(def parser (om/parser {:read readf :mutate mutatef}))

(defn query [{:keys [body request]}]
  (let [query-env {:app-env (:tudu/env request)}
        body (parser query-env body)]
    {:status 200 :body body}))

(defn not-found [_]
  {:status 404 :body {:error "Not Found"}})

(defmethod readf :default [env key params]
  :not-found)

(defmethod readf :tudu/items [{:keys [app-env]} key {:keys [id] :as params}]
  (let [{:keys [db]} app-env]
    {:value (vec (tudu.store/get-tasks db))}))

(defmethod mutatef :default [_ key params]
  :notfound)

(defmethod mutatef 'tudu.item/close [{:keys [app-env]} _ {:keys [id]}]
  (let [{:keys [db]} app-env]
    (tudu.store/close-task db id)
    {}))

(defmethod mutatef 'tudu.item/create [{:keys [app-env]} _ {:keys [value]}]
  (let [{:keys [db]} app-env
        tempid (:id value)
        {:keys [id]} (tudu.store/create-task db (dissoc value :id))
        response (if (and id tempid)
                   {tempid id}
                   {})]
    {:value {:tempids response}}))

