(ns tudu.ui
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]))

(enable-console-print!)

(def initial-state {:tudu/items
                    [{:id 1 :status :close :title "Implement this Step"}
                     {:id 2 :status :open :title "Buy Food"}
                     {:id 3 :status :open :title "Implement next Step"}]})

(defn close-item [component id]
  (om/transact! component `[(tudu.item/close {:id ~id}) :tudu/items]))

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

(defui TodoListUI
  static om/IQuery
  (query [this]
         [{:tudu/items (om/get-query TodoItemUI)}])
  Object
  (render [this]
          (let [{:keys [tudu/items]} (om/props this)]
            (dom/div #js {:className "todo-list-items"}
                     (map todo-item-ui items)))))

(def todo-list-ui (om/factory TodoListUI))

(defui UI
  static om/IQuery
  (query [this]
         ; if I just return (om/get-query TodoListUI) I get
         ; Error: Assert failed: Query violation
         (vec (concat (om/get-query TodoListUI))))
  Object
  (render [this]
          (let [props (om/props this)]
            (todo-list-ui props))))

(defmulti read om/dispatch)
(defmulti mutate om/dispatch)

(defmethod read :default [{:keys [state]} key params]
  (if-let [[_ value] (find @state key)]
    {:value value}
    :not-found))

(defn get-items [state]
  (let [st @state]
    (into [] (map #(get-in st %)) (get st :tudu/items))))

(defmethod read :tudu/items [{:keys [state]} key params]
  {:value (get-items state)})

(defmethod mutate 'tudu.item/close [{:keys [state]} _ {:keys [id]}]
  {:action (fn []
             (swap! state
                    #(assoc-in % [:tudu.items/by-id id :status] :close)))})

(defonce reconciler (om/reconciler
                      {:state initial-state
                       :parser (om/parser {:read read :mutate mutate})}))

(om/add-root! reconciler UI (gdom/getElement "main-app-area"))
