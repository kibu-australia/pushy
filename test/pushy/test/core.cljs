(ns pushy.test.core
  (:require-macros
   [cemerick.cljs.test :refer (is deftest done use-fixtures)])
  (:require
   [pushy.core :as pushy]
   [secretary.core :as secretary :refer-macros [defroute]]
   [goog.events :as events]
   [cemerick.cljs.test :as t])
  (:import goog.history.Html5History
           goog.Uri))

(def test-val (atom :fail))

(def history
  (pushy/pushy secretary/dispatch!
               (fn [x] (when (secretary/locate-route x) x))
               identity))

(defroute foo-route "/foo" []
  (reset! test-val :foo))

(deftest constructing-history
  (is (instance? Html5History (pushy/new-history))))

(deftest constructing-pushy
  (is (satisfies? pushy/IHistory (pushy/pushy (constantly nil) (constantly nil)))))

(deftest supported-browser
  (is (pushy/supported?)))

(deftest fragment-to-token
  (is (= "foo" (pushy/uri-fragment->token (Uri. "https://github.com/kibu-australia/pushy#foo")))))

(deftest url-to-token
  (is (= "/kibu-australia/pushy" (pushy/uri->token (Uri. "https://github.com/kibu-australia/pushy"))))
  (is (= "/kibu-australia/pushy?foo=1" (pushy/uri->token (Uri. "https://github.com/kibu-australia/pushy?foo=1")))))

(deftest test-setup-fragment
  (let [non-frag {}
        frag {:use-fragment true}]
    (is (= non-frag (pushy/setup-fragment-config non-frag)))
    (is (nil? (:token-transformer (pushy/setup-fragment-config frag))))
    (is (:uri->token-fn (pushy/setup-fragment-config frag)))))

;; event listeners started = dispatch
(deftest ^:async push-state-foo-route
  (reset! test-val :fail)
  (secretary/set-config! :prefix "/")
  (pushy/start! history)
  (pushy/replace-token! history "/foo")
  (js/setTimeout
   (fn []
     (is (= :foo @test-val))
     (is (nil? (pushy/stop! history)))
     (is (= "/foo" (pushy/get-token history)))
     (done))
   5000))

;; no event listeners started = no dispatch
(deftest ^:async push-state-bar-route
  (reset! test-val :fail)
  (pushy/replace-token! history "/foo")
  (js/setTimeout
   (fn []
     (is (= :fail @test-val))
     (is (= "/foo" (pushy/get-token history)))
     (done))
   5000))
