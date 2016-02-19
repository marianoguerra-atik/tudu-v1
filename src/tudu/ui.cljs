(ns tudu.ui
  (:require
    [om.next :as om :refer-macros [defui]]
    [cognitect.transit :as transit]
    [om.dom :as dom])
  (:import [goog.net XhrIo]))

(enable-console-print!)

(def clean-item-editing {:title ""})

(def initial-state {:tudu.item/editing clean-item-editing
                    :tudu/items nil})

(defn close-item [c id]
  (om/transact! c `[(tudu.item/close {:id ~id}) :tudu/items]))

(defui TodoItemUI
  static om/IQuery
  (query [this]
         '[:id :status :title])
  static om/Ident
  (ident [this {:keys [id]}]
         [:tudu.items/by-id id])
  Object
  (render [this]
          (let [{:keys [id status title]} (om/props this)
                closed? (= status :close)
                class-name (str "todo-list-item-title status-" (name status))]
            (dom/div #js {:className "todo-list-item" :key (str "todo-item-" id)}
                     (dom/input #js {:type "checkbox" :disabled closed?
                                     :checked closed?
                                     :onClick #(close-item this id)})

                     (dom/span #js {:className class-name} title)))))

(def todo-item-ui (om/factory TodoItemUI {:keyfn :id}))

(defn status-to-order [{:keys [status]}]
  (if (= status :open) 0 1))

(defn compare-todo-items [{a-status :status a-id :id} {b-status :status b-id :id}]
  (let [status-compare (compare (status-to-order a-status)
                                (status-to-order b-status))]
    (if (zero? status-compare)
      (if (or (om.tempid/tempid? a-id) (om.tempid/tempid? b-id))
        0
        (compare a-id b-id))
      status-compare)))

(defui TodoListUI
  Object
  (render [this]
          (let [{:keys [tudu/items]} (om/props this)]
            (dom/div #js {:className "todo-list-items"}
                     (map todo-item-ui
                         (sort-by compare-todo-items items))))))

(def todo-list-ui (om/factory TodoListUI))

(defn on-change-cb [c callback]
  (fn [e]
    (let [target (.-target e)
          value (.-value target)]
      (callback c value))))

(defn task-title-change [c text]
  (om/transact! c `[(tudu.item.editing/set-title {:value ~text})
                            :tudu.item/editing]))

(defn create-task [c task]
  (let [full-task (assoc task :id (om/tempid) :status :open)]
    (om/transact! c `[(tudu.item/create {:value ~full-task})
                      :tudu.item/editing])))

(defui NewTodoItemUI
  static om/IQuery
  (query [this]
         '[:title])
  Object
  (render [this]
          (let [{:keys [title] :as editing} (om/props this)
                on-title-change (on-change-cb this task-title-change)]
            (dom/div #js {:className "new-todo-item-form"}
                      (dom/div #js {:className "form-group"}
                               (dom/label nil "Task")
                               (dom/input #js {:value title
                                               :onChange on-title-change}))
                      (dom/button #js {:className "new-todo-item-create"
                                       :onClick #(create-task this editing)}
                                  "Create")))))

(def new-todo-item-ui (om/factory NewTodoItemUI))

(defui UI
  static om/IQuery
  (query [this]
         [{:tudu.item/editing (om/get-query NewTodoItemUI)}
          {:tudu/items (om/get-query TodoItemUI)}])
  Object
  (render [this]
          (let [{:keys [tudu.item/editing] :as props} (om/props this)]
            (dom/div nil
                     (new-todo-item-ui editing)
                     (todo-list-ui props)))))

(defmulti read om/dispatch)
(defmulti mutate om/dispatch)

(defmethod read :default [{:keys [state]} key params]
  (if-let [[_ value] (find @state key)]
    {:value value}
    :not-found))

(defn get-items [state k]
  (let [st @state]
    (into [] (map #(get-in st %)) (get st k))))

(defmethod read :tudu/items [{:keys [state] :as env} k params]
  (let [value (get-items state k)]
    {:value value :api true}))

(defmethod mutate 'tudu.item/close [{:keys [state]} _ {:keys [id]}]
  {:api true
   :action #(swap! state assoc-in [:tudu.items/by-id id :status] :close)})

(defmethod mutate 'tudu.item.editing/set-title [{:keys [state]} _ {:keys [value]}]
  {:action #(swap! state assoc-in [:tudu.item/editing :title] value)})

(defmethod mutate 'tudu.item/create [{:keys [state]} _ {:keys [value]}]
  {:api true
   :action (fn []
             (swap! state
                    (let [{:keys [id]} value]
                      #(-> %
                           (assoc :tudu.item/editing clean-item-editing)
                           (assoc-in [:tudu.items/by-id id] value)
                           (update :tudu/items
                                   (fn [s] (conj s [:tudu.items/by-id id])))))))})

(def reader (om/reader))
(def writer (om/writer))

(defn parse-transit [data]
  (transit/read reader data))

(defn to-transit [data]
  (transit/write writer data))

(defn send-post [url data cb]
  (.send XhrIo url (fn [resp]
                     (let [target (.-target resp)
                           body-raw (.getResponse target)
                           body (parse-transit body-raw)
                           status (.getStatus target)]
                     (cb {:status status :body body})))
         "POST" (to-transit data)
         #js {"content-type" "application/transit+json"}))

(defn send-query [query cb]
  (send-post "/query" query cb))

(defn send-to-api [{:keys [api] :as remotes} cb]
  (send-query api (fn [{:keys [body status]}]
                    (when (= status 200)
                      (cb body)))))

(defn resolve-tempids [state tid->rid]
  (clojure.walk/prewalk #(if (om.tempid/tempid? %) (get tid->rid %) %) state))

(defn tempid-migrate [pure query tempids id-key]
  (if (empty? tempids)
    pure
    (resolve-tempids pure tempids)))

(defonce reconciler (om/reconciler
                      {:state initial-state
                       :send send-to-api
                       :remotes [:api]
                       :id-key :db/id
                       :migrate tempid-migrate
                       :parser (om/parser {:read read :mutate mutate})}))

(defonce root (atom nil))

(defn init []
  (if (nil? @root)
    (let [target (js/document.getElementById "main-app-area")]
      (om/add-root! reconciler UI target)
      (reset! root UI))
    (om/force-root-render! reconciler)))

(init)
