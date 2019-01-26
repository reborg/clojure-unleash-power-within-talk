;   Copyright (c) Rich Hickey. All rights reserved.
;   The use and distribution terms for this software are covered by the
;   Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;   which can be found in the file epl-v10.html at the root of this distribution.
;   By using this software in any fashion, you are agreeing to be bound by
;   the terms of this license.
;   You must not remove this notice, or any other, from this software.

(ns clojurescript)

(defonce namespaces (atom {}))

(def bootjs "
cljs = {}
cljs.user = {}
cljs.lang = {}
cljs.lang.truth = function(x){return x != null && x !== false;}
cljs.lang.fnOf = function(f){return (f instanceof Function?f:f.cljs$lang$Fn$invoke);}")

(defn- resolve-var [env sym]
  (let [s (str sym)
        lb (-> env :locals sym)
        nm
        (cond
         lb (:name lb)

         ;;todo - resolve ns aliases when we have them
         (namespace sym)
         (symbol (str (namespace sym) "." (name sym)))

         (.contains s ".")
         (let [idx (.indexOf s ".")
               prefix (symbol (subs s 0 idx))
               suffix (subs s idx)
               lb (-> env :locals prefix)]
           (if lb
             (symbol (str (:name lb) suffix))
             sym))

         :else
         (symbol (str (:ns env) "." (name sym))))]
    {:name nm}))

(defmulti emit-constant class)
(defmethod emit-constant nil [x] (print "null"))
(defmethod emit-constant Long [x] (print x))
(defmethod emit-constant Double [x] (print x))
(defmethod emit-constant String [x] (pr x))
(defmethod emit-constant Boolean [x] (print (if x "true" "false")))

(defmulti emit :op)

(defn emits [expr]
  (with-out-str (emit expr)))

(defn emit-block
  [context statements ret]
  (if statements
    (let [body (str "\t" (apply str (interpose "\t" (map emits statements)))
                    "\t" (emits ret))]
      (print body))
    (emit ret)))

(defmacro emit-wrap [env & body]
  `(let [env# ~env]
     (when (= :return (:context env#)) (print "return "))
     ~@body
     (when-not (= :expr (:context env#)) (print ";\n"))))

(defmethod emit :var
  [{:keys [info env] :as arg}]
  (emit-wrap env (print (:name info))))

(defmethod emit :constant
  [{:keys [form env]}]
  (emit-wrap env (emit-constant form)))

(defmethod emit :if
  [{:keys [test then else env]}]
  (let [context (:context env)]
    (if (= :expr context)
      (print (str "(cljs.lang.truth(" (emits test) ")?" (emits then) ":" (emits else) ")"))
      (print (str "if(cljs.lang.truth(" (emits test) "))\n\t" (emits then) " else\n\t" (emits else) "\n")))))

(defmethod emit :def
  [{:keys [name init env]}]
  (when init
    (print name)
    (print (str " = " (emits init)))
    (when-not (= :expr (:context env)) (print ";\n"))))

(defmethod emit :fn
  [{:keys [name params statements ret env recurs]}]
  ;;fn statements get erased, serve no purpose and can pollute scope if named
  (when-not (= :statement (:context env))
    (emit-wrap env
               (print (str "(function " name "(" (apply str (interpose "," params)) "){\n"))
               (when recurs (print "while(true){\n"))
               (emit-block :return statements ret)
               (when recurs (print "break;\n}\n"))
               (print "})"))))

(defmethod emit :do
  [{:keys [statements ret env]}]
  (let [context (:context env)]
    (when (and statements (= :expr context)) (print "(function ()"))
    (when statements (print "{\n"))
    (emit-block context statements ret)
    (when statements (print "}"))
    (when (and statements (= :expr context)) (print ")()"))))

(defmethod emit :let
  [{:keys [bindings statements ret env loop]}]
  (let [context (:context env)
        bs (map (fn [{:keys [name init]}]
                  (str "var " name " = " (emits init) ";\n"))
                bindings)]
    (when (= :expr context) (print "(function ()"))
    (print (str "{\n" (apply str bs) "\n"))
    (when loop (print "while(true){\n"))
    (emit-block (if (= :expr context) :return context) statements ret)
    (when loop (print "break;\n}\n"))
    (print "}")
    (when (= :expr context) (print ")()"))))

(defmethod emit :recur
  [{:keys [frame exprs env]}]
  (let [temps (vec (take (count exprs) (repeatedly gensym)))
        names (:names frame)]
    (print "{\n")
    (dotimes [i (count exprs)]
      (print (str "var " (temps i) " = " (emits (exprs i)) ";\n")))
    (dotimes [i (count exprs)]
      (print (str (names i) " = " (temps i) ";\n")))
    (print "continue;\n")
    (print "}\n")))

(defmethod emit :invoke
  [{:keys [f args env]}]
  (emit-wrap env
             (print (str "cljs.lang.fnOf(" (emits f) ")("
                         (apply str (interpose "," (map emits args)))
                         ")"))))

(defmethod emit :new
  [{:keys [ctor args env]}]
  (emit-wrap env
             (print (str "new " (emits ctor) "("
                         (apply str (interpose "," (map emits args)))
                         ")"))))

(defmethod emit :set!
  [{:keys [target val env]}]
  (emit-wrap env (print (str (emits target) " = "(emits val)))))

(defmethod emit :ns
  [{:keys [name requires macros env]}]
  (println (str "//goog.provide('" name "');"))
  (doseq [lib (vals requires)]
    (println (str "//goog.require('" lib "');"))))

(declare analyze analyze-symbol)

(def specials '#{if def fn* do let* loop recur new set! ns})

(def ^:dynamic *recur-frame* nil)

(defmacro disallowing-recur [& body]
  `(binding [*recur-frame* nil] ~@body))

(defn analyze-block
  "returns {:statements .. :ret .. :children ..}"
  [env exprs]
  (let [statements (disallowing-recur
                     (seq (map #(analyze (assoc env :context :statement) %) (butlast exprs))))
        ret (if (<= (count exprs) 1)
              (analyze env (first exprs))
              (analyze (assoc env :context (if (= :statement (:context env)) :statement :return)) (last exprs)))]
    {:statements statements :ret ret :children (vec (cons ret statements))}))

(defmulti parse (fn [op & rest] op))

(defmethod parse 'if
  [op env [_ test then else :as form] name]
  (let [test-expr (disallowing-recur (analyze (assoc env :context :expr) test))
        then-expr (analyze env then)
        else-expr (analyze env else)]
    {:env env :op :if :form form
     :test test-expr :then then-expr :else else-expr
     :children [test-expr then-expr else-expr]}))

(defmethod parse 'def
  [op env form name]
  (let [pfn (fn ([_ sym] {:sym sym})
              ([_ sym init] {:sym sym :init init})
              ([_ sym doc init] {:sym sym :doc doc :init init}))
        args (apply pfn form)
        sym (:sym args)]
    (assert (not (namespace sym)) "Can't def ns-qualified name")
    (let [name (:name (resolve-var (dissoc env :locals) sym))
          init-expr (when (contains? args :init)
                      (disallowing-recur
                        (analyze (assoc env :context :expr) (:init args) sym)))]
      (merge {:env env :op :def :form form
              :name name :doc (:doc args) :init init-expr}
             (when init-expr {:children [init-expr]})))))

(defmethod parse 'fn*
  [op env [_ & args] name]
  (let [name (if (symbol? (first args))
               (first args)
               name)
        meths (if (symbol? (first args))
               (next args)
               args)
        ;;turn (fn [] ...) into (fn ([]...))
        meths (if (vector? (first meths)) (list meths) meths)
        ;;todo, merge meths, switch on arguments.length
        meth (first meths)
        params (first meth)
        ;;todo, variadics
        params (remove '#{&} params)
        body (next meth)
        locals (reduce (fn [m name] (assoc m name {:name name})) (:locals env) params)
        recur-frame {:names (vec params) :flag (atom nil)}
        block (binding [*recur-frame* recur-frame]
                (analyze-block (assoc env :context :return :locals locals) body))]
    (assert (= 1 (count meths)) "Arity overloading not yet supported")
    (merge {:env env :op :fn :name name :meths meths :params params :recurs @(:flag recur-frame)} block)))

(defmethod parse 'do
  [op env [_ & exprs] _]
  (merge {:env env :op :do} (analyze-block env exprs)))

(defn analyze-let
  [encl-env [_ bindings & exprs :as form] is-loop]
  (assert (and (vector? bindings) (even? (count bindings))) "bindings must be vector of even number of elements")
  (let [context (:context encl-env)
        [bes env]
        (disallowing-recur
         (loop [bes []
                env (assoc encl-env :context :expr)
                bindings (seq (partition 2 bindings))]
           (if-let [[name init] (first bindings)]
             (do
               (assert (not (or (namespace name) (.contains (str name) "."))) (str "Invalid local name: " name))
               (let [init-expr (analyze env init)
                     be {:name (gensym (str name "__")) :init init-expr}]
                 (recur (conj bes be)
                        (assoc-in env [:locals name] be)
                        (next bindings))))
             [bes env])))
        recur-frame (when is-loop {:names (vec (map :name bes)) :flag (atom nil)})
        {:keys [statements ret children]}
        (binding [*recur-frame* (or recur-frame *recur-frame*)]
          (analyze-block (assoc env :context (if (= :expr context) :return context)) exprs))]
    {:env encl-env :op :let :loop is-loop
     :bindings bes :statements statements :ret ret :form form :children (into [children] (map :init bes))}))

(defmethod parse 'let*
  [op encl-env form _]
  (analyze-let encl-env form false))

(defmethod parse 'loop
  [op encl-env form _]
  (analyze-let encl-env form true))

(defmethod parse 'recur
  [op env [_ & exprs] _]
  (let [context (:context env)]
    (assert *recur-frame* "Can't recur here")
    (assert (= (count exprs) (count (:names *recur-frame*))) "recur argument count mismatch")
    (reset! (:flag *recur-frame*) true)
    (assoc {:env env :op :recur}
      :frame *recur-frame*
      :exprs (disallowing-recur (vec (map #(analyze (assoc env :context :expr) %) exprs))))))

(defmethod parse 'new
  [_ env [_ ctor & args] _]
  (disallowing-recur
   (let [enve (assoc env :context :expr)
         ctorexpr (analyze enve ctor)
         argexprs (vec (map #(analyze enve %) args))]
     {:env env :op :new :ctor ctorexpr :args argexprs :children (conj argexprs ctorexpr)})))

(defmethod parse 'set!
  [_ env [_ target val] _]
  (assert (symbol? target) "set! target must be a symbol naming var")
  (assert (nil? (-> env :locals target)) "Can't set! local var")
  (disallowing-recur
   (let [enve (assoc env :context :expr)
         targetexpr (analyze-symbol enve target)
         valexpr (analyze enve val)]
     {:env env :op :set! :target targetexpr :val valexpr :children [targetexpr valexpr]})))

(defmethod parse 'ns
  [_ env [_ name & {:keys [requires macros] :as params}] _]
  (doseq [nsym (vals macros)]
    (require nsym))
  (let [deps (into requires (map (fn [[alias nsym]]
                                   [alias (find-ns nsym)])
                                 macros))]
    (swap! namespaces #(-> % (assoc-in [name :name] name)
                           (assoc-in [name :deps] deps))))
  (merge {:env env :op :ns :name name} params))

(defn parse-invoke
  [env [f & args]]
  (disallowing-recur
   (let [enve (assoc env :context :expr)
         fexpr (analyze enve f)
         argexprs (vec (map #(analyze enve %) args))]
     {:env env :op :invoke :f fexpr :args argexprs :children (conj argexprs fexpr)})))

(defn analyze-symbol
  "Finds the var associated with sym"
  [env sym]
  (let [ret {:env env :form sym}
        lb (-> env :locals sym)]
    (if lb
      (assoc ret :op :var :info lb)
      (assoc ret :op :var :info (resolve-var env sym)))))

(defn get-expander [sym env]
  (when-not (-> env :locals sym)
    ))

(defn analyze-seq
  [env form name]
  (let [op (first form)]
    (assert (not (nil? op)) "Can't call nil")
    (if (specials op)
      (parse op env form name)
      (if-let [mac (and (symbol? op) (get-expander op env))]
        (analyze (apply mac (rest form)))
        (parse-invoke env form)))))

(defn analyze
  "Given an environment, a map containing {:locals (mapping of names to bindings), :context
  (one of :statement, :expr, :return), :ns (a symbol naming the
  compilation ns)}, and form, returns an expression object (a map
  containing at least :form, :op and :env keys). If expr has any (immediately)
  nested exprs, must have :children [exprs...] entry. This will
  facilitate code walking without knowing the details of the op set."
  ([env form] (analyze env form nil))
  ([env form name]
     (let [form (if (instance? clojure.lang.LazySeq form)
                  (or (seq form) ())
                  form)]
       (cond
        (symbol? form) (analyze-symbol env form)
        (and (seq? form) (seq form)) (analyze-seq env form name)
        :else {:op :constant :env env :form form}))))

;; (load-file "src/clojurescript.clj")
(comment
(in-ns 'clojurescript)
(require '[clojure.pprint :as pp])
(import '[javax.script ScriptEngineManager])
(def jse (-> (ScriptEngineManager.) (.getEngineByName "JavaScript")))
(.eval jse bootjs)
(def envx {:ns 'test.ns :context :return :locals '{ethel {:name ethel__123 :init nil}}})

(println
  (with-out-str
    (pp/write
      (analyze
        (assoc envx :context :statement)
        '(if test then "fooelse"))))) ;; :183

(emit
  (analyze
    (assoc envx :context :statement)
    '(if test then "fooelse"))) ;; :78
)
