(defproject kibu/pushy "0.3.6"
  :description "HTML5 pushState for Clojurescript"
  :url "https://github.com/kibu-australia/pushy"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.48"]]

  :aliases {"deploy" ["do" "clean," "deploy" "clojars"]
            "test" ["do" "clean," "with-profile" "dev" "cljsbuild" "test"]}

  :lein-release {:deploy-via :shell
                 :shell ["lein" "deploy"]}

  :profiles {:dev {:dependencies [[secretary "1.2.3"]]
                   :plugins [[lein-cljsbuild "1.1.5"]
                             [com.cemerick/clojurescript.test "0.3.3"]]

                   :cljsbuild
                   {:test-commands
                    {"unit" ["phantomjs" :runner
                             "target/unit-test.js"]}
                    :builds
                    {:test {:source-paths ["src" "test"]
                            :compiler {:output-to "target/unit-test.js"
                                       :optimizations :whitespace
                                       :pretty-print true}}}}}})
