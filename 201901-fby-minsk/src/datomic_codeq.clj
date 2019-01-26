(ns datomic_codeq)

'[:find
  ?var ?def ?n :where
  [?cn :code/name ?var]
  [?cq :clj/ns ?cn]
  [?cq :codeq/file ?f]
  [?n :node/object ?f]
  [?cq :codeq/code ?def]]

(def rules
  '[[(node-files ?n ?f) [?n :node/object ?f] [?f :git/type :blob]]
    [(node-files ?n ?f) [?n :node/object ?t] [?t :git/type :tree] [?t :tree/nodes ?n2] (node-files ?n2 ?f)]
    [(object-nodes ?o ?n) [?n :node/object ?o]] [(object-nodes ?o ?n) [?n2 :node/object ?o] [?t :tree/nodes ?n2] (object-nodes ?t ?n)]
    [(commit-files ?c ?f) [?c :commit/tree ?root] (node-files ?root ?f)]
    [(commit-codeqs ?c ?cq) (commit-files ?c ?f) [?cq :codeq/file ?f]]
    [(file-commits ?f ?c) (object-nodes ?f ?n) [?c :commit/tree ?n]]
    [(codeq-commits ?cq ?c) [?cq :codeq/file ?f] (file-commits ?f ?c)]])

;; all of the different definitions of the function datomic.codeq.core/commit,
;; and when they were first defined
#_(d/q '[:find ?src (min ?date)
       :in $ % ?name
       :where
       [?n :code/name ?name]
       [?cq :clj/def ?n]
       [?cq :codeq/code ?cs]
       [?cs :code/text ?src]
       [?cq :codeq/file ?f]
       (file-commits ?f ?c)
       (?c :commit/authoredAt ?date)]
     db rules "datomic.codeq.core/commit")
