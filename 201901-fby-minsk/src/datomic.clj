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
