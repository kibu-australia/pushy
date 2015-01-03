(ns pushy.core
  (:require [goog.events :as events])
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

(defn history-chan [read-ch write-ch history & [{:keys [on-close!]}]]
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
          (events/unlisten history EventType.NAVIGATE)
          (when on-close!
            (on-close!))))

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

    IDeref
    (-deref [this]
      (-get-token this))))

(defn click-event [write-ch]
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
          (do (put! write-ch (-> target-href .-title))
              (.preventDefault e)))))))

(defn pushy-chan [read-xform write-xform]
  (let [read-ch (chan (map read-xform))
        write-ch (chan (map write-xform))
        history (new-history)
        click-ev (on-click (click-event write-ch))
        history-ch (history-chan read-ch
                                 write-ch
                                 history
                                 {:on-close! #(events/unlisten click-ev)})]

    (go-loop []
      (let [x (<! write-ch)]
        (-set-token history-ch x)
        (recur)))

    (events/listen history EventType.NAVIGATE
                   #(put! read-ch (.-token %)))

    (put! write-ch (get-token history-ch))

    history-ch))
