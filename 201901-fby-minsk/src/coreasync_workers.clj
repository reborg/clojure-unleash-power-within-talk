(ns coreasync-workers
  (:require [clojure.core.async :refer [go go-loop chan >! <! <!! close!]]))

(defn- master [items in]
  (go
    (doseq [item items]
      (>! in item))
    (close! in)))

(defn- worker [out]
  (let [in (chan)]
    (go-loop []
       (if-some [item (<! in)]
         (do
           (>! out (str "*" item "*"))
           (recur))
         (close! out)))
    in))

(defn process [items]
  (let [out (chan)]
    (master items (worker out))
    (loop [res []]
      (if-some [item (<!! out)]
        (recur (conj res item))
        res))))

;; (require '[coreasync-workers :as w])
;; (w/process (range 100))
