(ns logic-fun.fast
  (:use [clojure.pprint :only [pprint]]))

(defn vec-last [v] (nth v (dec (count v))))
(defrecord lvarT [name])
(defn lvar? [x] (instance? lvarT x))
(defn lvar [name] (lvarT. name))
(defmethod print-method lvarT [x writer] (.write writer (str "<lvar:" (:name x) ">")))
(defrecord pairT [lhs rhs])
(defn pair [lhs rhs] (pairT. lhs rhs))

(defprotocol ISubstitutions
  (length [this])
  (ext [this x v])
  (lookup [this v]))

(defn lookup* [ss v]
  (loop [v v a (vec-last (ss v)) ss ss orig v]
    (cond
     (nil? a)  v
     (= a orig) :circular
     (lvar? a) (recur a (vec-last (ss a)) ss orig)
     :else a)))

(defrecord Substitutions [ss order]
  ISubstitutions
  (ext [this x v]
    (if (= (lookup* ss x) :circular)
      nil
      (Substitutions.
        (update-in ss [x] (fnil conj []) v)
        (conj order x))))
  (lookup [this v]
    (lookup* ss v)))

(defn empty-s [] (Substitutions. {} []))

(comment
 (let [x  (lvar 'x)
       y  (lvar 'y)
       z  (lvar 'z)
       ss (Substitutions. {x [1 y]
                           y [5]}
                          [x y x])
       ss (ext ss x z)]
   (println ss)))
