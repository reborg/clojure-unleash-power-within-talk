(ns corelogic
  (:require [clojure.core.logic :as cl]))

;; a,b,c,d,e are encoded "facts"
(defn some-rules [{:keys [a b c d e]}]
  (and
    (= a 10)
    (= b 20)
    (= c 30)
    (= d 40)
    (= e 50)))

; (some-rules {:a 10 :b 20 :c 30 :d 40 :e 50})
;; true

; (some-rules {:a -2 :b 20 :c 30 :d 40 :e 50})
;; false

; (some-rules {:b 20 :c 30 :d 40 :e 50})
;; false

;; What input do I need to solve this problem?
(defn some-rules* []
  (cl/run*
    [q]
    (cl/fresh
      [a b c d e]
      (cl/== a 10)
      (cl/== b 20)
      (cl/== c 30)
      (cl/== d 40)
      (cl/== e 50)
      (cl/== q {:a a :b b :c c :d d :e e}))))

; (some-rules*)
;; ({:a 10, :b 20, :c 30, :d 40, :e 50})

;; What if I give you some of the input?
(defn some-rules* [{:keys [a b c d e] :as m}]
  (cl/run*
    [q]
    (cl/fresh
      [a' b' c' d' e']
      (cl/== (or a a') 10)
      (cl/== (or b b') 20)
      (cl/== (or c c') 30)
      (cl/== (or d d') 40)
      (cl/== (or e e') 50)
      (cl/== q (reduce-kv
                 (fn [m k v] (dissoc m k))
                 {:a a' :b b' :c c' :d d' :e e'}
                 m)))))

; (some-rules* {:b 20 :d 40})
;; ({:a 10, :c 30, :e 50})

; (some-rules* {:a -1})
;; () no solutions
