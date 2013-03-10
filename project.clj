(defproject com.novemberain/validateur "1.4.0-SNAPSHOT"
  :description "Functional validations inspired by Ruby's ActiveModel"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure  "1.5.1"]
                 [clojurewerkz/support "0.14.0"]]
  :profiles {:1.3 {:dependencies [[org.clojure/clojure "1.3.0"]]}
             :1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0-master-SNAPSHOT"]]}
             :master {:dependencies [[org.clojure/clojure "1.6.0-master-SNAPSHOT"]]}
             :dev {:plugins [[codox "0.6.4"]]
                   :codox {:sources ["src/clojure"]
                           :output-dir "doc/api"}}}
  :aliases  {"all" ["with-profile" "dev:dev,1.3:dev,1.4:dev,1.6:dev,master"]}
  :repositories {"sonatype" {:url "http://oss.sonatype.org/content/repositories/releases"
                             :snapshots false
                             :releases {:checksum :fail :update :always}}
                 "sonatype-snapshots" {:url "http://oss.sonatype.org/content/repositories/snapshots"
                                       :snapshots true
                                       :releases {:checksum :fail :update :always}}}
  :source-paths   ["src/clojure"]
  :test-selectors {:focus :focus}
  :codox {:only [validateur.validation]})
