(ns sudoku
  (:refer-clojure :exclude [==])
  (:use clojure.core.logic)
  (:require [clojure.core.logic.fd :as fd]))

(defn get-square [rows x y]
  (for [x (range x (+ x 3))
        y (range y (+ y 3))] (get-in rows [x y])))

(defn init [vars hints]
  (if (seq vars)
    (let [hint (first hints)]
      (all (if-not (zero? hint)
          (== (first vars) hint)
          succeed)
        (init (next vars) (next hints)))) succeed))

(defn sudokufd [hints]
  (let [vars (repeatedly 81 lvar)
        rows (->> vars (partition 9) (map vec) (into []))
        cols (apply map vector rows)
        sqs  (for [x (range 0 9 3)
                   y (range 0 9 3)] (get-square rows x y))]
    (run 1 [q]
      (== q vars)
      (everyg #(fd/in % (fd/domain 1 2 3 4 5 6 7 8 9)) vars)
      (init vars hints)
      (everyg fd/distinct rows)
      (everyg fd/distinct cols)
      (everyg fd/distinct sqs))))

(clojure.pprint/pprint
  (partition 9
    (first (sudokufd
             [2 0 7 0 1 0 5 0 8
              0 0 0 6 7 8 0 0 0
              8 0 0 0 0 0 0 0 6
              0 7 0 9 0 6 0 5 0
              4 9 0 0 0 0 0 1 3
              0 3 0 4 0 1 0 2 0
              5 0 0 0 0 0 0 0 1
              0 0 0 2 9 4 0 0 0
              3 0 6 0 8 0 4 0 9]))))
