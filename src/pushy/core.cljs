(ns pushy.core
  (:require [goog.events :as events])
  (:import goog.History
           goog.history.Html5History
           goog.history.Html5History.TokenTransformer
           goog.history.EventType
           goog.Uri))

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

(def transformer
  (-> (TokenTransformer.) set-retrieve-token! set-create-url!))

(def history
  (-> (Html5History. js/window transformer) update-history!))

(defn set-token!
  "Sets the history state"
  ([token]
     (. history (setToken token)))
  ([token title]
     (. history (setToken token title))))

(defn replace-token!
  "Replaces the current history state without affecting the rest of the history stack"
  ([token]
     (. history (replaceToken token)))
  ([token title]
     (. history (replaceToken token title))))

(defn get-token
  "Returns the current token"
  []
  (.getToken history))

(defn supported?
  "Returns whether Html5History is supported"
  [window]
  (.isSupported Html5History window))

(defn push-state!
  "Initializes push state using goog.history.Html5History

  Adds an event listener to all click events and dispatches `dispatch-fn`
  when the target element contains a href attribute that matches
  any of the routes returned by `match-fn`

  Takes in three functions:
    * dispatch-fn: the function that dispatches when a match is found
    * match-fn: the function used to check if a particular route exists
    * identity-fn: (optional) extract the route from value returned by match-fn"
  ([dispatch-fn match-fn]
     (push-state! dispatch-fn match-fn identity))

  ([dispatch-fn match-fn identity-fn]
     ;; We want to call `dispatch-fn` on any change to the location
     (events/listen history EventType.NAVIGATE
                    #(-> (.-token %) match-fn identity-fn dispatch-fn))

     ;; Dispatch on initialization
     (when-let [match (match-fn (get-token))]
       (-> match identity-fn dispatch-fn))

     ;; Setup event listener on all 'click' events
     (on-click
      (fn [e]
        (when-let [href (recur-href (-> e .-target))]
          (let [path (->> href  (.parse Uri) (.getPath))]
            ;; Proceed if `identity-fn` returns a value and
            ;; the user did not trigger the event via one of the
            ;; keys we should bypass
            (when (and (identity-fn (match-fn path))
                       ;; Bypass dispatch if any of these keys
                       (not (.-altKey e))
                       (not (.-ctrlKey e))
                       (not (.-metaKey e))
                       (not (.-shiftKey e))
                       ;; Bypass dispatch if middle click
                       (not= 1 (.-button e)))
              ;; Dispatch!
              (set-token! path (-> e .-target .-title))
              (.preventDefault e))))))))
