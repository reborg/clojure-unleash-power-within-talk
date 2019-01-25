(ns datomic)

;; Email of all authors
[:find ?email
 :where
 [_ :commit/author ?e]
 [?u :email/address ?email]]

;; First commit date
[:find (min ?date)
 :where
 [_ :commit/committedAt ?date]]

;; Most commits per author
[:find ?email (count ?commit)
 :where
 [?commit :commit/author ?author]
 [?author :email/address ?email]]

; [:find ?email (min ?date) (max ?date)
;  :in $ ?email
;  :where
;  [?e :commit/committedAt ?date]
;  [?e :commit/author ?u]
;  [?u :email/address ?email]]

; [:find ?e ?msg
;  :in $ ?text
;  :where
;  [(fulltext $ :commit/message ?text) [[?e]]]
;  [?e :commit/message ?msg]]
