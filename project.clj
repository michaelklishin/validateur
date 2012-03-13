(defproject com.novemberain/validateur "1.1.0-SNAPSHOT"
  :description "Functional validations inspired by Ruby's ActiveModel"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure "1.3.0"]]
  :profiles { :1.4 { :dependencies [[org.clojure/clojure "1.4.0-beta4"]] } }
  :aliases  { "all" ["with-profile" "dev:dev,1.4"] }
  :test-selectors {:focus (fn [v] (:focus v))})
