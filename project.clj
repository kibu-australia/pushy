(defproject kibu/pushy "0.2.1"
  :description "HTML5 pushState for Clojurescript"
  :url "https://github.com/kibu-australia/pushy"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/clojurescript "0.0-2356"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]]

  :aliases {"deploy" ["do" "clean," "deploy" "clojars"]
            "test" ["do" "clean," "with-profile" "dev" "cljsbuild" "test"]}

  :lein-release {:deploy-via :shell
                 :shell ["lein" "deploy"]}

  :profiles {:dev {:dependencies [[secretary "1.2.1"]]

                   :plugins [[lein-cljsbuild "1.0.3"]
                             [com.cemerick/clojurescript.test "0.3.1"]]

                   :cljsbuild
                   {:test-commands
                    {"unit" ["phantomjs" :runner
                             "window.literal_js_was_evaluated=true"
                             "test/vendor/es5-shim.js"
                             "test/vendor/es5-sham.js"
                             "test/vendor/history-shim.js"
                             "test/vendor/console-polyfill.js"
                             "target/unit-test.js"]}
                    :builds
                    {:test {:source-paths ["src" "test"]
                            :compiler {:output-to "target/unit-test.js"
                                       :optimizations :whitespace
                                       :pretty-print true}}}}}})
