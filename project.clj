(defproject com.novemberain/validateur "1.1.0-beta1"
  :description "Functional validations inspired by Ruby's ActiveModel"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure  "1.3.0"]
                 [clojurewerkz/support "0.1.0-beta3"]]
  :profiles { :1.4 { :dependencies [[org.clojure/clojure "1.4.0-beta5"]] } }
  :aliases  { "all" ["with-profile" "dev:dev,1.4"] }
  :test-selectors {:focus (fn [v] (:focus v))})
