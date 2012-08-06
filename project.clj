(defproject com.novemberain/validateur "1.2.0-SNAPSHOT"
  :description "Functional validations inspired by Ruby's ActiveModel"
  :min-lein-version "2.0.0"
  :dependencies [[org.clojure/clojure  "1.3.0"]
                 [clojurewerkz/support "0.5.0"]]
  :profiles {:1.4 { :dependencies [[org.clojure/clojure "1.4.0"]]}
             :1.5 { :dependencies [[org.clojure/clojure "1.5.0-master-SNAPSHOT"]]}}
  :aliases  {"all" ["with-profile" "dev:dev,1.4:dev,1.5"]}
  :repositories {"sonatype" {:url "http://oss.sonatype.org/content/repositories/releases"
                             :snapshots false
                             :releases {:checksum :fail :update :always}}
                 "sonatype-snapshots" {:url "http://oss.sonatype.org/content/repositories/snapshots"
                                       :snapshots true
                                       :releases {:checksum :fail :update :always}}}
  :source-paths   ["src/clojure"]
  :test-selectors {:focus :focus})
