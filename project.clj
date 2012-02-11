(defproject com.novemberain/validateur "1.1.0-SNAPSHOT"
  :description "Functional validations inspired by Ruby's ActiveModel"
  :dependencies [[org.clojure/clojure "1.3.0"]]
  :test-selectors   {:focus          (fn [v] (:focus v)) })
