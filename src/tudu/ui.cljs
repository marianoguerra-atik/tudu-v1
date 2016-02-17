(ns tudu.ui
  (:require
    [cljs-http.client :as http]
    [goog.dom :as gdom]
    [om.next :as om :refer-macros [defui]]
    [cognitect.transit :as transit]
    [cljs.core.async :refer [<!]]
    [om.dom :as dom])
  (:require-macros
    [cljs.core.async.macros :refer [go]]))

(enable-console-print!)
(declare reconciler)

(def clean-item-editing {:title ""})

(def initial-state {:tudu.item/editing clean-item-editing
                    :tudu/items nil})

(defn close-item [c-or-r id]
  (om/transact! c-or-r `[(tudu.item/close {:id ~id}) :tudu/items]))

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
                {:keys [close-item]} (om/get-computed this)
                closed? (= status :close)
                class-name (str "todo-list-item-title status-" (name status))]
            (dom/div #js {:className "todo-list-item" :key (str "todo-item-" id)}
                     (dom/input #js {:type "checkbox" :disabled closed?
                                     :checked closed?
                                     :onClick #(close-item id)})

                     (dom/span #js {:className class-name} title)))))

(def todo-item-ui (om/factory TodoItemUI {:keyfn :id}))

(defui TodoListUI
  static om/IQuery
  (query [this]
         [{:tudu/items (om/get-query TodoItemUI)}])
  Object
  (render [this]
          (let [{:keys [tudu/items]} (om/props this)
                tui-cprops {:close-item #(close-item reconciler %)}]
            (dom/div #js {:className "todo-list-items"}
                     (map #(todo-item-ui (om/computed % tui-cprops)) items)))))

(def todo-list-ui (om/factory TodoListUI))

(defn on-change-cb [c-or-r callback]
  (fn [e]
    (let [target (.-target e)
          value (.-value target)]
      (callback c-or-r value))))

(defn task-title-change [c-or-r text]
  (om/transact! c-or-r `[(tudu.item.editing/set-title {:value ~text})
                            :tudu.item/editing]))

(defn create-task [c-or-r task]
  (om/transact! c-or-r `[(tudu.item/create {:value ~task})
                            :tudu.item/editing :tudu/items]))

(defui NewTodoItemUI
  static om/IQuery
  (query [this]
         [:tudu.item/editing])
  Object
  (render [this]
          (let [{:keys [tudu.item/editing]} (om/props this)
                {:keys [title]} editing
                on-change (on-change-cb reconciler task-title-change)]
            (dom/div #js {:className "new-todo-item-form"}
                      (dom/div #js {:className "form-group"}
                               (dom/label nil "Task")
                               (dom/input #js {:value title
                                               :onChange on-change}))
                      ; NOTE: I have to pass the reconciler here and not the
                      ; component otherwise I get:
                      ; Error: No queries exist for component path (tudu.ui/UI tudu.ui/NewTodoItemUI)
                      (dom/button #js {:onClick #(create-task reconciler editing)}
                                  "Create")))))

(def new-todo-item-ui (om/factory NewTodoItemUI))

(defui UI
  static om/IQuery
  (query [this]
         (vec (concat (om/get-query TodoListUI)
                      (om/get-query NewTodoItemUI))))
  Object
  (render [this]
          (let [props (om/props this)]
            (dom/div nil
                     (todo-list-ui props)
                     (new-todo-item-ui props)))))

(defmulti read om/dispatch)
(defmulti mutate om/dispatch)

(defmethod read :default [{:keys [state]} key params]
  (if-let [[_ value] (find @state key)]
    {:value value}
    :not-found))

(defn get-items [state k]
  (let [st @state]
    (into [] (map #(get-in st %)) (get st k))))

(defmethod read :tudu/items [{:keys [state ast] :as env} k params]
  (let [value (get-items state k)]
    {:value value :api ast}))

(defmethod mutate 'tudu.item/close [{:keys [state ast]} _ {:keys [id]}]
  {:remote true :api ast
   :action #(swap! state assoc-in [:tudu.items/by-id id :status] :close)})

(defmethod mutate 'tudu.item.editing/set-title [{:keys [state]} _ {:keys [value]}]
  {:action #(swap! state assoc-in [:tudu.item/editing :title] value)})

(defmethod mutate 'tudu.item/create [{:keys [state ast]} _ {:keys [value]}]
  {:remote true :api ast
   :action (fn []
             (let [id (om/tempid)
                   full-task (assoc value :id id :status :open)]
               (swap! state
                      #(-> %
                           (assoc :tudu.item/editing clean-item-editing)
                           (assoc-in [:tudu.items/by-id id] full-task)
                           (update :tudu/items
                                   (fn [s] (conj s [:tudu.items/by-id id])))))))})

(def reader (om/reader))
(def writer (om/writer))

(defn parse-transit [data]
  (transit/read reader data))

(defn to-transit [data]
  (transit/write writer data))

(defn send-post [path query cb]
  (let [req (http/post path {:headers {"content-type" "application/transit+json"}
                             :body (to-transit query)})]
    (go (cb (<! req)))))

(defn send-query [query cb]
  (send-post "/query" query cb))

(defn send-to-api [{:keys [api] :as remotes} cb]
  (send-query api (fn [{:keys [body status]}]
                    (when (= status 200)
                      (cb body)))))

(defonce reconciler (om/reconciler
                      {:state initial-state
                       :send send-to-api
                       :remotes [:api]
                       :parser (om/parser {:read read :mutate mutate})}))

(om/add-root! reconciler UI (gdom/getElement "main-app-area"))
