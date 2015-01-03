(ns pushy.core
  (:require [goog.events :as events]
            [cljs.core.async.impl.protocols :as proto]
            [cljs.core.async :as async :refer (chan <! >! put! close! mult tap)])
  (:require-macros [cljs.core.async.macros :refer (go-loop alt!)])
  (:import goog.History
           goog.history.Html5History
           goog.history.Html5History.TokenTransformer
           goog.history.EventType
           goog.Uri))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Private + util

(defn- on-click [funk]
  (events/listen js/document "click" funk))

(defn- recur-href
  "Recursively find a href value

  This hack is because a user might click on a <span> nested in an <a> element"
  [target]
  (if (.-href target)
    (.-href target)
    (when (.-parentNode target)
      (recur-href (.-parentNode target)))))

(defn- get-attribute [target]
  (.getAttribute target))

(defn- update-history! [h]
  (.setUseFragment h false)
  (.setPathPrefix h "")
  (.setEnabled h true)
  h)

(defn- set-retrieve-token! [t]
  (set! (.. t -retrieveToken)
        (fn [path-prefix location]
          (str (.-pathname location) (.-search location))))
  t)

(defn- set-create-url! [t]
  (set! (.. t -createUrl)
        (fn [token path-prefix location]
          (str path-prefix token)))
  t)

(defn new-transformer []
  (-> (TokenTransformer.) set-retrieve-token! set-create-url!))

(defn new-history []
  (-> (Html5History. js/window (new-transformer)) update-history!))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; Public API

(defn supported?
  "Returns whether Html5History is supported"
  ([] (supported? js/document))
  ([window] (.isSupported Html5History window)))

(defprotocol IToken
  (-set-token! [this token])
  (-set-token! [this token title])

  (-get-token [this])

  (-replace-token! [this token])
  (-replace-token! [this token title]))

(defprotocol IHook
  (start [this pushy-ch])
  (stop [this event]))

(defn on-click! [pushy-ch]
  (reify
    IHook
    (start [_]
      (on-click
       (fn [e]
         (when-let [target-href (recur-href (-> e .-target))]
           (let [path (->> target-href  (.parse Uri) (.getPath))]
             ;; Proceed if `identity-fn` returns a value and
             ;; the user did not trigger the event via one of the
             ;; keys we should bypass
             (when (and (not (.-altKey e))
                        (not (.-ctrlKey e))
                        (not (.-metaKey e))
                        (not (.-shiftKey e))
                        (not (= "_blank" (get-attribute target-href)))
                        (not= 1 (.-button e)))
               ;; Dispatch!
               (do (put! pushy-ch (-> target-href .-title))
                   (.preventDefault e))))))))

    (stop [_ event]
      (events/unlisten event))))

(defn history-chan [read-ch write-ch close-ch history]
  (reify
    proto/ReadPort
    (take! [_ fn-handler]
      (proto/take! read-ch fn-handler))

    proto/WritePort
    (put! [this val fn-handler]
      (do (proto/put! write-ch val fn-handler)))

    proto/Channel
    (close! [_]
      (do (proto/close! read-ch)
          (proto/close! write-ch)
          (proto/close! close-ch)
          (events/unlisten history EventType.NAVIGATE)))

    IToken
    (-set-token! [_ token]
      (. history (setToken token)))
    (-set-token! [_ token title]
      (. history (setToken token title)))

    (-replace-token! [_ token]
      (. history (replaceToken token)))
    (-replace-token! [_ token title]
      (. history (replaceToken token title)))

    (-get-token [this]
      (.getToken history))

    cljs.core/IDeref
    (-deref [this]
      (-get-token this))))

(defn pushy-chan
  [& [{:keys [read-xform write-xform hooks]
       :or {read-xform (filter identity) write-xform (filter identity)}}]]
  (let [read-ch (chan 10 (filter read-xform))
        write-ch (chan 10 (map write-xform))
        history (new-history)
        close-ch (chan)
        history-ch (history-chan read-ch write-ch close-ch history)
        hooks (map (fn [hook] (hook history-ch)) hooks)
        hook-events (map (fn [hook] (start hook)) hooks)]

    (let [m (mult write-ch)
          process-ch (chan)]
      (tap m process-ch)
      (go-loop []
        (alt!
          close-ch
          ([_]
             (doseq [[hook event] (interleave hooks hook-events)]
               (stop hook event)))

          process-ch
          ([x]
             (-set-token history-ch x)
             (recur)))))

    (events/listen history EventType.NAVIGATE
                   #(put! read-ch (.-token %)))

    (put! write-ch @history-ch)

    history-ch))
