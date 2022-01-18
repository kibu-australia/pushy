(defproject ua.kasta/pushy "0.3.8-4"
  :description "HTML5 pushState for Clojurescript"
  :url "https://github.com/kasta-ua/pushy"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.10.3" :scope "provided"]
                 [org.clojure/clojurescript "1.11.4" :scope "provided"]]

  :aliases {"deploy" ["do" "clean," "deploy" "clojars"]
            "test" ["do" "clean," "with-profile" "dev" "cljsbuild" "test"]}

  :lein-release {:deploy-via :shell
                 :shell ["lein" "deploy"]}

  :source-paths ["src"]

  :profiles {:dev {:dependencies [[secretary "1.2.3"]]
                   :plugins [[lein-cljsbuild "1.1.8"]
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
