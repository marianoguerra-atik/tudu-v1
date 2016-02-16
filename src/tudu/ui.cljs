(ns tudu.ui
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]))

(enable-console-print!)

(defonce state (atom {:tudu/items
                      [{:id 1 :status :close :title "Implement this Step"}
                       {:id 2 :status :open :title "Buy Food"}
                       {:id 3 :status :open :title "Implement next Step"}]}))

(defn on-increment-click [component]
  (om/transact! component
                `[(tudu.counter/increment {:amount 1}) :tudu/count]))

(defn todo-item-ui [{:keys [id status title]}]
  (dom/div #js {:className "todo-list-item" :key (str "todo-item-" id)}
           (dom/span #js {:className (str "todo-list-item-title status-" (name status))}
                     title)))

(defui TodoListUI
  static om/IQuery
  (query [this]
         [:tudu/items])
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

(defmethod read :default [env key params]
  (if-let [[_ value] (find @state key)]
    {:value value}
    :not-found))

(def reconciler (om/reconciler
                  {:state state
                   :parser (om/parser {:read read :mutate mutate})}))

(om/add-root! reconciler UI (gdom/getElement "main-app-area"))
