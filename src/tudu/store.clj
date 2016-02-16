(ns tudu.store
  (:require
    [com.stuartsierra.component :as component]
    [clojure.java.jdbc :as j]))

(defprotocol Store
  (get-tasks [this])
  (create-task [this task])
  (close-task [this id]))

(defn create-tables [db]
  (j/with-db-connection [c db]
    (j/execute! c [(j/create-table-ddl "IF NOT EXISTS tudu_task"
                                     [:m_id "int AUTO_INCREMENT PRIMARY KEY"]
                                     [:m_title "varchar(256) NOT NULL"]
                                     [:m_status "varchar(32) NOT NULL"])])))

(defn query [db q]
  (j/with-db-connection [c db]
    (j/query c q)))

(defn insert [db table data]
  (j/with-db-connection [c db]
    (let [result (j/insert! db table data)
          id (get (first result) (keyword "scope_identity()"))]
      (if (nil? id)
        result
        {:id id}))))

(defn update-by-id [db table data]
  (println "updating on" table (:m_id data) " -> " data)
  (j/with-db-connection [c db]
    (let [id (:m_id data)
          result (j/update! db table data ["m_id = ?" id])]
      (println "update on" table id "updated" result " -> " data)
      {:result result :id id :data data})))

(defn row->task [{:keys [m_id m_title m_status]}]
  {:id m_id :title m_title :status (keyword m_status)})

(defn task->row [{:keys [id title status]}]
  {:m_id id
   :m_title title
   :m_status (when status (name status))})

(defrecord DB [classname subprotocol subname user password]
  component/Lifecycle
  (start [{:keys [] :as component}]
    (println "Starting DB" (str classname " " subprotocol " " subname " " user))
    (let [new-component (assoc component
                               :classname classname
                               :subprotocol subprotocol
                               :subname subname
                               :user user
                               :password password)]
      (try
        (create-tables new-component)
        (catch Exception e
          (prn "Error creating tables" e)))

      new-component))

  (stop [{:keys [classname subprotocol subname user] :as component}]
    (println "Stopping DB" (str classname " " subprotocol " " subname " " user))
    component)

  Store
  (get-tasks[this]
    (map row->task (query this ["SELECT * FROM tudu_task"])))
  (create-task [this task]
    (insert this :tudu_task (task->row (assoc task :status :open))))
  (close-task [this id]
    (update-by-id this :tudu_task {:m_id id :m_status "close"})))

(defn new-db [db-info]
  (map->DB db-info))
