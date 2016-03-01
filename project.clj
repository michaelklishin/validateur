(defproject com.novemberain/validateur "2.6.0-SNAPSHOT"
  :description "Functional validations inspired by Ruby's ActiveModel"
  :license { :name "Eclipse Public License" }
  :url "http://clojurevalidations.info"
  :min-lein-version "2.5.1"
  :dependencies [[org.clojure/clojure  "1.7.0"]]
  :jar-exclusions [#"\.swp|\.swo|\.DS_Store"]
  :profiles {:1.8 {:dependencies [[org.clojure/clojure "1.8.0"]]}
             :master {:dependencies [[org.clojure/clojure "1.9.0-master-SNAPSHOT"]]}
             :cljs {:hooks [leiningen.cljsbuild]}
             :dev {:dependencies [[org.clojure/clojurescript "1.7.228"]]
                   :plugins [[lein-codox "0.9.0"]
                             [lein-cljsbuild "1.1.2"]]
                   :cljsbuild {:test-commands {"phantom" ["phantomjs" "target/testable.js"]}
                               :builds [{:source-paths ["src" "test"]
                                         :compiler {:output-to "target/testable.js"
                                                    :main validateur.runner
                                                    :libs [""]
                                                    :source-map "target/testable.js.map"
                                                    :optimizations :advanced}}]}
                   :codox {:source-paths ["src"]}}}
  :aliases  {"all" ["with-profile" "+dev:dev,1.8:dev,master:dev,cljs"]}
  :repositories {"sonatype" {:url "http://oss.sonatype.org/content/repositories/releases"
                             :snapshots false
                             :releases {:checksum :fail :update :always}}
                 "sonatype-snapshots" {:url "http://oss.sonatype.org/content/repositories/snapshots"
                                       :snapshots true
                                       :releases {:checksum :fail :update :always}}}
  :source-paths ["src"]
  :test-paths ["test"]
  :test-selectors {:focus :focus}
  :codox {:only [validateur.validation]}
  :auto-clean false)
