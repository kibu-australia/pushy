(ns pushy.test.hash-based
  (:require-macros [cemerick.cljs.test :refer (is deftest done use-fixtures)])
  (:require [pushy.core :as pushy]
            [secretary.core :as secretary :refer-macros [defroute]]
            [goog.events :as events]
            [cemerick.cljs.test :as t])
  (:import goog.history.Html5History))

(def test-val (atom :fail))

(defn match [x]
  (when (secretary/locate-route x) x))

(def history
  (pushy/pushy secretary/dispatch! match identity
               :use-fragment true))

(defroute baz-hash-route "baz" []
  (println "->baz-hash-route")
  (reset! test-val :baz))

(deftest ^:async push-state-hash-baz-route
  (reset! test-val :fail)
  (secretary/set-config! :prefix "#")
  (pushy/start! history)
  (pushy/replace-token! history "baz")
  (js/setTimeout
   (fn []
     (is (= :baz @test-val))
     (is (nil? (pushy/stop! history)))
     (is (= "baz" (pushy/get-token history)))
     (done))
   5000))

