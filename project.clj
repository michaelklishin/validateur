(defproject com.novemberain/validateur "2.7.0-SNAPSHOT"
  :description "Functional validations inspired by Ruby's ActiveModel"
  :license { :name "Eclipse Public License" }
  :url "https://github.com/michaelklishin/validateur"
  :min-lein-version "2.5.1"
  :dependencies [[org.clojure/clojure  "1.12.0"]]
  :jar-exclusions [#"\.swp|\.swo|\.DS_Store"]
  :profiles {:1.10 {:dependencies [[org.clojure/clojure "1.10.2"]]}
             :1.11 {:dependencies [[org.clojure/clojure "1.11.4"]]}
             :cljs {:hooks [leiningen.cljsbuild]}
             :dev {:dependencies [[org.clojure/clojurescript "1.11.132"]]
                   :plugins [[lein-codox "0.10.0"]
                             [lein-cljsbuild "1.1.7" :exclusions [org.clojure/clojure]]]
                   :cljsbuild {:test-commands {"phantom" ["phantomjs" "target/testable.js"]}
                               :builds [{:source-paths ["src" "test"]
                                         :compiler {:output-to "target/testable.js"
                                                    :main validateur.runner
                                                    :libs [""]
                                                    :source-map "target/testable.js.map"
                                                    :optimizations :advanced}}]}
                   :codox {:source-paths ["src"]}}}
  :aliases  {"all" ["with-profile" "+dev:dev,1.10:dev,1.11:dev,cljs"]}
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
