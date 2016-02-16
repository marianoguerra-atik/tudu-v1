(ns tudu.data
  (:require
    [cognitect.transit :as transit]
    om.next.server)
  (:import [java.io ByteArrayInputStream ByteArrayOutputStream]))

(defn to-string [data]
  (let [out (ByteArrayOutputStream.)
        writer (om.next.server/writer out)]
    (transit/write writer data)
    (.toString out)))

(defn parse [data]
  (let [reader (om.next.server/reader data)]
    (transit/read reader)))
