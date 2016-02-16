(ns tudu.ui
  (:require [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]))

(enable-console-print!)

(defui UI
  Object
  (render [this]
          (dom/div nil "hello om.next")))

(def reconciler (om/reconciler {}))

(om/add-root! reconciler UI (gdom/getElement "main-app-area"))
