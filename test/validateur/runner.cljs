(ns validateur.runner
  (:require [cljs.test :refer-macros [run-tests]]
            validateur.test.validation-test))


(enable-console-print!)

(defn main []
  (println "Using ClojureScript version" *clojurescript-version*)
  (run-tests 'validateur.test.validation-test))

(let [result (main)]
  (if (exists? js/phantom)
    ((aget js/phantom "exit") result)))
