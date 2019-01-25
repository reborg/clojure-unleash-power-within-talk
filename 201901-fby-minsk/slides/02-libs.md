# 2008-2010: Origins.

* [Compojure](https://groups.google.com/d/msg/clojure/RIhlJU-b0pM/RDXh7NNfRP4J) (2008) and [Ring](https://groups.google.com/d/msg/clojure/u8n9RTZn2VM/TmOJst9t-p8J) (2009)
* [Leininghen](https://groups.google.com/d/msg/clojure/gBoCUXq37fM/841iHii--2IJ) (2009)
* [Cascalog](https://groups.google.com/d/msg/clojure/mLZoEY7Y00M/7RhlU_SkSagJ) (2010)
* -> [Core.Logic](https://groups.google.com/d/msg/clojure/CbgvzLWCikk/EtjCvMeMvlIJ) (2010)

# Compojure

```clojure
(defroutes app
  (GET "/:x/:y"
       [x y :<< #(Integer/parseInt %)]
    "<h1>Hello World</h1>")
  (route/not-found
    "<h1>Page not found</h1>"))
```

# Leiningen

```clojure
(defproject clojure-unleash-power-within-talk "0.1.0-SNAPSHOT"
  :description "Slides and examples."
  :url "github.com/reborg/clojure-unleash-power-within-talk"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :jvm-opts ["-Xmx1g" "-server"]
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/core.async "0.4.490"]
                 [org.clojure/core.logic "0.8.11"]
                 [criterium "0.4.4"]])
```

# Cascalog

```clojure
(?<- (stdout)
     [?word ?count]
     (sentence ?line)
     (tokenise ?line :> ?word)
     (c/count ?count))
```

# Core.Logic

* Logic programming implementation (miniKanren)
* Express programs in terms of "relations"
* Prolog-like approach to problem solving
* A tree walk of substitution chains

# Core.Logic Demo

* Start REPL
* Open `src/corelogic.clj`
* Evaluate snippets on the fly
* Bonus: `src/sudoku.clj`

# Core.Logic Resources

* [Mini Kanren](http://minikanren.org)
* [Core.Logic Tutorial](https://github.com/clojure/core.logic/wiki/A-Core.logic-Primer)
* [Logos Early Sketches](https://github.com/clojure/core.logic/blob/6584ca6323a5e52758ff3d162b93d01050eb9f44/src/logic_fun/fast.clj)

# 2011: The fun begins.

* [Midje](https://groups.google.com/d/msg/clojure/57vhbdeHrqk/tfLyKJIJrHcJ)
* [Storm](https://groups.google.com/d/msg/clojure/G5syTJHhG1I/a7-S6iH8oN4J)
* [Pallet](https://github.com/pallet/pallet)
* -> [ClojureScript](https://www.youtube.com/watch?v=tVooR-dF_Ag)

# Midje

```clojure
(facts "about migration"
  (fact "Migration produces a new left and right map"
    (migrate {:a 1} :a {})
    => {:new-left {} :clashes #{} :new-right {:a 1}})
  (fact "multiple keys can be moved at once"
    (migrate {:a 1, :b 2} :a :b {})
    => {:new-left {} :clashes #{} :new-right {:a 1 :b 2}}))
```

# Storm

```clojure
(topology
 {"1" (spout-spec sentence-spout)
  "2" (spout-spec (sentence-spout-parameterized
                   ["the cat jumped over the door"
                    "greetings from a faraway land"])
                   :p 2)}
 {"3" (bolt-spec {"1" :shuffle "2" :shuffle}
                 split-sentence
                 :p 5)
  "4" (bolt-spec {"3" ["word"]}
                 word-count
                 :p 6)})
```

# Pallet

```clojure
(defn tomcat-deploy
  "Tomcat deploy as ROOT application"
  [request path]
  (-> request
      (tomcat/settings {})
      (tomcat/deploy "ROOT" :local-file path :clear-existing true)))

(defn haproxy
  "haproxy server with app1 on port 80."
  [request]
  (-> request
      (haproxy/install-package)
      (haproxy/configure
       :listen {:app1 {:server-address "0.0.0.0:80"
                       :balance "roundrobin"}})))
```

# ClojureScript 1/2

* Initial concept late 2008
* Officially reworked in 2011
* A Clojure to JavaScript compiler
* Core functionality in ~350 LOC

# ClojureScript 2/2

* Standard parse-analyze-emit recursion
* The AST is Clojure data structures
* Heavy use of Clojure polymorphism
* The language becomes the DSL!

# ClojureScript Demo

* Open up REPL
* `(load-file "src/clojurescript.clj")`
* Follow examples at the bottom.

# ClojureScript Resources

* [Rationale and Design](https://github.com/clojure/clojurescript/blob/515900f9762102987bda7d53b919dafc0b6c0580/cljs.org)
* [Initial Implementation](https://github.com/clojure/clojurescript/blob/515900f9762102987bda7d53b919dafc0b6c0580/src/clj/clojure/cljs.clj)
* [Main Website](https://clojurescript.org)
* [ClojureScript Release Presentation](https://www.youtube.com/watch?v=tVooR-dF_Ag)

# 2012: Going enterprise.

* [Core.Typed](https://github.com/clojure/core.typed)
* [EDN](https://groups.google.com/d/msg/clojure/aRUEIlAHguU/35WX-ns-LkAJ)
* [Reducers](https://groups.google.com/d/msg/clojure/EJ9hOZ8yaos/edhhczUjzNwJ)
* -> [Codeq](http://blog.datomic.com/2012/10/codeq.html)
* -> [Datomic](https://groups.google.com/d/msg/clojure/y_33dMclUu4/b1magLvfFuMJ)

# Core.Typed

![](media/coretyped.gif)

# Extensible Data Notation

* JS: https://github.com/shaunxcode/jsedn
* Go: https://github.com/go-edn/edn
* C++: https://github.com/shaunxcode/edn-cpp
* Haskell: http://hackage.haskell.org/package/hedn
* Scala: https://github.com/martintrojer/edn-scala
* ....

# Reducers

```clojure
(defn count-occurrences [words]
  (r/fold
    (r/monoid #(merge-with + %1 %2) (constantly {}))
    (fn [m [k cnt]] (assoc m k (+ cnt (get m k 0))))
    (r/map #(vector % 1) words)))

(defn word-count [s] (count-occurrences (s/split s #"\s+")))
(def war-and-peace "http://www.gutenberg.org/files/2600/2600-0.txt")
(def book (slurp war-and-peace))
(def freqs (word-count book))
(freqs "Andrew")
```

# Datomic

* Distributed, transactional, time-aware DB
* Database as immutable "value"
* Datalog query interface
* RDF inspired relations
* Efficient B-Tree implementation

# Codeq

* Semantic analysis of source code
* Functional definitions as unit of change
* Based on Datomic

# Datomic Demo

* Run `src/datomic.sh` (done)
* Open up browser at http://localhost:9256/browse
* Open `src/datomic.clj`

# 2013: Annus Mirabilis

* [LightTable](https://www.chris-granger.com/2012/06/24/its-playtime/)
* [CinC](https://groups.google.com/d/msg/clojure/cC1yC9zrS1s/W0ducjm0uQYJ)
* [Om](https://groups.google.com/d/msg/clojure/4wFYpXRGbqw/6GQ7xVuXO20J)
* [Components](https://github.com/stuartsierra/component/commit/ab02501ab1d750906f1331d8b8ff7a3c5e2d3ce6)
* -> [Core.Async](https://github.com/clojure/core.async/commit/3ed968b132a2dacd76dcb0f2744eff48fcce58e4)

# LightTable

![](media/lighttable.mp4)

# CinC (aka reader/analyzer/jvm)

```clojure
(ast/nodes (analyze '[1 (+ 1 2)]))
({:op        :vector,
  :top-level true,
  :items
  [{:op   :const,
    :type :number,
    :val  1,
    ...}
   {:op     :static-call,
    :class  clojure.lang.Numbers,
    :method add,
    :form   (. clojure.lang.Numbers (add 1 2)),
    :args   ...,
   ...
```

# Om

```clojure
(defui Hello
  Object
  (render [this]
    (dom/h1 nil "Hello, world!")))

(def hello (om/factory Hello))

(.render js/ReactDOM (hello) (gdom/getElement "example"))
```

# Stuart Sierra Component

```clojure
(defrecord Database [host port connection]

  component/Lifecycle

  (start [component]
    (println ";; Starting database")
    (let [conn (connect-to-database host port)]
      (assoc component :connection conn)))

  (stop [component]
    (println ";; Stopping database")
    (.close connection)
    (assoc component :connection nil)))
```

# Core.Async 1/2

* Beginning of 2013
* Need for non-blocking "await" threads
* Inspired by F#, C# then Scala async/await
* Plus "Channels" from CSP
* Avoid ClojureScript/JS callback hell

# Core.Async 2/2

* Transform blocking calls into state assignment
* Parse Clojure into SSA (Static Single Assignment) form
* Create a state for each await/pause/channel write/read
* Run thread (from pool) with callback into state
* Clojure macrology in action!

# Core.Async Demo

* Open up REPL
* Open `src/coreasync.clj`
* Follow examples at the bottom.
* Open `src/coreasync_workers.clj`
* More examples at the bottom.

# Core.Async Resources

* [Initial version of async/go macro](https://github.com/clojure/core.async/blob/b1f18f9d5cc6c2ffd9fecba279086e6115694e16/src/clj/core/async/ioc_macros.clj)
* [C Sharp State Machine Writer](https://github.com/dotnet/roslyn/blob/master/src/Compilers/CSharp/Portable/Lowering/AsyncRewriter/AsyncRewriter.cs)
* [What is SSA?](https://en.wikipedia.org/wiki/Static_single_assignment_form)

# 2014-Today

* [ClojureCLR](https://github.com/clojure/clojure-clr) (day 1)
* [Arcadia](https://github.com/arcadia-unity/Arcadia/wiki/Using-Arcadia#clojure-clr) (2014)
* [Transducers](https://clojure.org/news/2014/08/06/transducers-are-coming) (2014)
* [Core.Spec](https://clojure.org/guides/spec) (2016)

# Arcadia

![](media/arcadia.mp4)
