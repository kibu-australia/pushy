(ns pushy.test.core
  (:require-macros
   [cemerick.cljs.test :refer (is deftest done use-fixtures)])
  (:require
   [pushy.core :as pushy]
   [secretary.core :as secretary :refer-macros [defroute]]
   [goog.events :as events]
   [cemerick.cljs.test :as t])
  (:import goog.history.Html5History))

(secretary/set-config! :prefix "/")
(def test-val (atom false))

(def history
  (pushy/pushy secretary/dispatch!
               (fn [x] (when (secretary/locate-route x) x))
               identity))

(defroute foo-route "/foo" []
  (reset! test-val true))

(defroute bar-route "/bar" []
  (reset! test-val true))

(deftest constructing-history
  (is (instance? Html5History (pushy/new-history))))

(deftest constructing-pushy
  (is (satisfies? pushy/IHistory (pushy/pushy (constantly nil) (constantly nil)))))

(deftest supported-browser
  (is (pushy/supported?)))

;; event listeners started = dispatch
(deftest ^:async push-state-foo-route
  (reset! test-val false)
  (pushy/start! history)
  (pushy/replace-token! history "/foo")
  (js/setTimeout
   (fn []
     (is @test-val)
     (is (nil? (pushy/stop! history)))
     (is (= "/foo" (pushy/get-token history)))
     (done))
   5000))

;; no event listeners started = no dispatch
(deftest ^:async push-state-bar-route
  (reset! test-val false)
  (pushy/replace-token! history "/bar")
  (js/setTimeout
   (fn []
     (is (false? @test-val))
     (is (= "/bar" (pushy/get-token history)))
     (done))
   5000))
