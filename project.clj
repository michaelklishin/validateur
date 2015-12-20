(defproject com.novemberain/validateur "2.6.0-SNAPSHOT"
  :description "Functional validations inspired by Ruby's ActiveModel"
  :license { :name "Eclipse Public License" }
  :url "http://clojurevalidations.info"
  :min-lein-version "2.5.1"
  :dependencies [[org.clojure/clojure  "1.7.0"]]
  :jar-exclusions [#"\.cljx|\.swp|\.swo|\.DS_Store"]
  :profiles {:1.8 {:dependencies [[org.clojure/clojure "1.8.0-RC4"]]}
             :master {:dependencies [[org.clojure/clojure "1.8.0-master-SNAPSHOT"]]}
             :dev {:dependencies [[org.clojure/clojurescript "0.0-2138"]]
                   :plugins [[lein-codox "0.9.0"]
                             [com.keminglabs/cljx "0.5.0" :exclusions [org.clojure/clojure]]
                             [lein-cljsbuild "1.0.2"]
                             [com.cemerick/clojurescript.test "0.2.1"]]
                   :cljx {:builds [{:source-paths ["src/cljx"]
                                    :output-path "target/classes"
                                    :rules :clj}
                                   {:source-paths ["src/cljx"]
                                    :output-path "target/classes"
                                    :rules :cljs}
                                   {:source-paths ["test"]
                                    :output-path "target/test-classes"
                                    :rules :clj}
                                   {:source-paths ["test"]
                                    :output-path "target/test-classes"
                                    :rules :cljs}]}
                   :cljsbuild {:test-commands {"phantom" ["phantomjs" :runner "target/testable.js"]}
                               :builds [{:source-paths ["target/classes" "target/test-classes"]
                                         :compiler {:output-to "target/testable.js"
                                                    :libs [""]
                                                    :source-map "target/testable.js.map"
                                                    :optimizations :advanced}}]}
                   :codox {:source-paths ["src/cljx" "target/classes"]}}}
  :aliases  {"all" ["with-profile" "+dev:dev,1.8:dev,master"]}
  :repositories {"sonatype" {:url "http://oss.sonatype.org/content/repositories/releases"
                             :snapshots false
                             :releases {:checksum :fail :update :always}}
                 "sonatype-snapshots" {:url "http://oss.sonatype.org/content/repositories/snapshots"
                                       :snapshots true
                                       :releases {:checksum :fail :update :always}}}
  :source-paths ["src/cljx" "target/classes"]
  :test-paths ["target/test-classes"]
  :test-selectors {:focus :focus}
  :codox {:only [validateur.validation]}
  :auto-clean false)
