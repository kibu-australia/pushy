(ns pushy.core
  (:require [goog.events :as events]
            [clojure.string])
  (:import goog.History
           goog.history.Html5History
           goog.history.Html5History.TokenTransformer
           goog.history.EventType
           goog.Uri))

(defn- on-click [funk]
  (events/listen js/document "click" funk))

(defn- recur-href
  "Traverses up the DOM tree and returns the first node that contains a href attr"
  [target]
  (if (.-href target)
    target
    (when (.-parentNode target)
      (recur-href (.-parentNode target)))))

(defn- update-history! [h {:keys [use-fragment path-prefix]}]
  (doto h
    (.setUseFragment use-fragment)
    (.setPathPrefix (or path-prefix ""))
    (.setEnabled true)))

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

(defn token-transformer []
  (-> (TokenTransformer.)
      set-retrieve-token!
      set-create-url!))

(defn new-history
  ([]
   (new-history {:token-transformer (token-transformer)}))
  ([{:keys [token-transformer] :as cfg}]
   (-> (Html5History. js/window token-transformer)
       (update-history! cfg))))

(defprotocol IHistory
  (set-token! [this token] [this token title])
  (replace-token! [this token] [this token title])
  (get-token [this])
  (start! [this])
  (stop! [this]))

(defn- processable-url? [uri]
  (and (not (clojure.string/blank? uri))                    ;; Blank URLs are not processable.
       (or (and (not (.hasScheme uri)) (not (.hasDomain uri))) ;; By default only process relative URLs + URLs matching window's origin
           (some? (re-matches (re-pattern (str "^" (.-origin js/location) ".*$"))
                              (str uri))))))

(defn- uri->token [uri]
  (let [path (.getPath uri)
        query (.getQuery uri)]
    ;; Include query string in token
    (if (empty? query) path (str path "?" query))))

(defn- uri-fragment->token [uri]
  (.getFragment uri))

(defn setup-fragment-config
  "When `use-fragment' is true, configure pushy for this mode."
  [{:keys [use-fragment] :as cfg}]
  (conj cfg (when use-fragment
              {:token-transformer nil
               :uri->token-fn uri-fragment->token})))

(defn pushy
  "Takes in three functions:
    * dispatch-fn: the function that dispatches when a match is found
    * match-fn: the function used to check if a particular route exists

    Additionaly, takes optional keyword arguments:

    :identity-fn - Extract the route from value returned by match-fn
    :use-fragment - When true, use fragments (hashes) for single-page
                    applications."
  [dispatch-fn match-fn &
   {:keys [processable-url? identity-fn uri->token-fn]
    :or {processable-url? processable-url?
         identity-fn identity

         use-fragment false
         path-prefix ""

         token-transformer (token-transformer)}
    :as cfg}]

  (let [{:keys [uri->token-fn] :as cfg} (setup-fragment-config cfg)
        history (new-history cfg)
        event-keys (atom nil)]
    (reify
      IHistory
      (set-token! [_ token]
        (. history (setToken token)))
      (set-token! [_ token title]
        (. history (setToken token title)))

      (replace-token! [_ token]
        (. history (replaceToken token)))
      (replace-token! [_ token title]
        (. history (replaceToken token title)))

      (get-token [_]
        (.getToken history))

      (start! [this]
        (stop! this)
        ;; We want to call `dispatch-fn` on any change to the location
        (swap! event-keys conj
               (events/listen history EventType.NAVIGATE
                              (fn [e]
                                (when-let [match (-> (.-token e) match-fn identity-fn)]
                                  (dispatch-fn match)))))

        ;; Dispatch on initialization
        (when-let [match (-> (get-token this) match-fn identity-fn)]
          (dispatch-fn match))

        (swap! event-keys conj
               (on-click
                (fn [e]
                  (when-let [el (recur-href (-> e .-target))]
                    (let [uri (.parse Uri (.-href el))]
                      ;; Proceed if `identity-fn` returns a value and
                      ;; the user did not trigger the event via one of the
                      ;; keys we should bypass
                      (when (and (processable-url? uri)
                                 ;; Bypass dispatch if any of these keys
                                 (not (.-altKey e))
                                 (not (.-ctrlKey e))
                                 (not (.-metaKey e))
                                 (not (.-shiftKey e))
                                 ;; Bypass if target = _blank
                                 (not (get #{"_blank" "_self"} (.getAttribute el "target")))
                                 ;; Bypass if explicitly instructed to ignore this element
                                 (or (not (.hasAttribute el "data-pushy-ignore"))
                                     (= (.getAttribute el "data-pushy-ignore") "false"))
                                 ;; Bypass dispatch if middle click
                                 (not= 1 (.-button e)))
                        (let [next-token (uri->token-fn uri)]
                          (when (identity-fn (match-fn next-token))
                            ;; Dispatch!
                            (if-let [title (-> el .-title)]
                              (set-token! this next-token title)
                              (set-token! this next-token))
                            (.preventDefault e)))))))))
        nil)

      (stop! [this]
        (doseq [key @event-keys]
          (events/unlistenByKey key))
        (reset! event-keys nil)))))

(defn supported?
  "Returns whether Html5History is supported"
  ([] (supported? js/window))
  ([window] (.isSupported Html5History window)))

;; Backwards compatibility with pushy <= 0.2.2
(defn push-state!
  ([dispatch-fn match-fn]
   (push-state! dispatch-fn match-fn identity))
  ([dispatch-fn match-fn identity-fn]
   (let [h (pushy dispatch-fn match-fn :identity-fn identity-fn)]
     (start! h)
     h)))
