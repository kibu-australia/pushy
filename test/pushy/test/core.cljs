(ns pushy.test.core
  (:require-macros
   [cemerick.cljs.test :refer (is deftest done use-fixtures)])
  (:require
   [pushy.core :as pushy]
   [secretary.core :as secretary :include-macros true :refer [defroute]]
   [goog.events :as events]
   [cemerick.cljs.test :as t]))

(secretary/set-config! :prefix "/")

(def test-atom (atom false))

(defroute foo-route "/foo" []
  (reset! test-atom true))

(pushy/push-state! secretary/dispatch!
                   (fn [x] (when (secretary/locate-route x) x))
                   identity)

(deftest ^:async test-push-state
  (let [el (.createElement js/document "a")
        click-fn (fn [target]
                   (let [ev (.createEvent js/document "MouseEvents")]
                     (.initMouseEvent ev "click" true true (.-defaultView js/document))
                     (.dispatchEvent target ev)))]
    (.setAttribute el "href" "/foo")
    (.appendChild (.-body js/document) el)
    (click-fn el)
    (js/setTimeout
     (fn []
       (is @test-atom)
       (done))
     5000)))
