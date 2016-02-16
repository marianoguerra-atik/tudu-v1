(ns tudu.ui
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]))

(enable-console-print!)

(defonce state (atom {:tudu/count 0}))

(defn on-increment-click [component]
  (om/transact! component
                `[(tudu.counter/increment {:amount 1}) :tudu/count]))

(defui UI
  static om/IQuery
  (query [this]
         [:tudu/count])
  Object
  (render [this]
          (let [{:keys [tudu/count]} (om/props this)]
            (dom/div nil
                     (dom/span nil "Count: " count)
                     (dom/button #js {:onClick #(on-increment-click this)}
                                 "Increment")))))

(defmulti read om/dispatch)
(defmulti mutate om/dispatch)

(defmethod read :default [env key params]
  (if-let [[_ value] (find @state key)]
    {:value value}
    :not-found))

(defmethod mutate 'tudu.counter/increment
  [{:keys [state] :as env} _ {:keys [amount]}]
  {:action (fn [] (swap! state (fn [st]
                                 (update st :tudu/count #(+ % amount)))))})

(def reconciler (om/reconciler
                  {:state state
                   :parser (om/parser {:read read :mutate mutate})}))

(om/add-root! reconciler UI (gdom/getElement "main-app-area"))
