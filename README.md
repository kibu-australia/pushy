# pushy
[![Build Status](https://travis-ci.org/kibu-australia/pushy.svg?branch=master)](https://travis-ci.org/kibu-australia/pushy)

A Clojurescript library for quick and easy HTML5 pushState.

## Install

[![Clojars Project](http://clojars.org/kibu/pushy/latest-version.svg)](http://clojars.org/kibu/pushy)

## Usage

### Setup

You can construct a new instance by calling the `pushy` function.

`pushy` takes in two arguments:

* `dispatch` fn: gets called when there is a match
* `match` fn: checks if the path matches any routes defined.

Optionally, you can specify an `:identity-fn` which parses and returns the route based on the result of the `match` fn.

### Event listeners

You can start the event listeners with the `start!` method.

This adds an event listener to all `click` events, which dispatches on all matched routes.
Bypasses on Alt, Shift, Meta, Ctrl keys and middle clicks.

The `stop!` method will tear down all event listeners of the pushy instance.

### Routing libraries

pushy should work with any routing library:

[Secretary](https://github.com/gf3/secretary)

```clojure
(ns foo.core
  (:require [secretary.core :as secretary :include-macros true :refer-macros [defroute]]
            [pushy.core :as pushy]))

(secretary/set-config! :prefix "/")

(defroute index "/foo" []
  (.log js/console "Hi"))

(def history (pushy/pushy secretary/dispatch!
                          (fn [x] (when (secretary/locate-route x) x))))

;; Start event listeners
(pushy/start! history)
```

[Bidi](https://github.com/juxt/bidi)

```clojure
(ns foo.core
  (:require [bidi.bidi :as bidi]
            [pushy.core :as pushy]))

(def state (atom {}))

(def app-routes
  ["/" {"foo" :foo}])

(defn set-page! [match]
  (swap! state assoc :page match))

(def history
  (pushy/pushy set-page! (partial bidi/match-route app-routes)))

(pushy/start! history)
```

[Silk](https://github.com/DomKM/silk)

```clojure
(ns foo.core
  (:require [domkm.silk :as silk]
            [pushy.core :as pushy]))

(def state (atom {}))

(def app-routes
  (silk/routes [[:foo [["/foo"]]]]))

(defn set-page! [match]
  (swap! state assoc :page match))

(def history
  (pushy/pushy set-page! (partial silk/arrive app-routes)))

(pushy/start! history)
```

[Router](https://github.com/darkleaf/router)

```clojure
(ns foo.core
  (:require [pushy.core :as pushy]
            [darkleaf.router :as r]))

(r/defcontroller root-controller
  (show [_req]
    :root))

(r/defcontroller pages-controller
  (index [_req]
    :pages-index))

(def routing
  (r/group
    (r/resource :root root-controller, :segment false)
    (r/resources :pages :page pages-controller)))

(def handler (r/make-handler routing))

(def history (pushy/pushy (fn [response] (prn response))
                          (fn [uri] (handler {:uri uri, :request-method :get}))))

(pushy/start! history)

#_(pushy/set-token! history "/")
#_(pushy/set-token! history "/pages")
```

[Sibiro](https://github.com/aroemers/sibiro)

```clojure
(ns foo.core
  (:require [sibiro.core :as sibiro]
            [pushy.core :as pushy]))

(def state (atom {}))

(defn set-page! [match]
  (assoc state :page match))

(def routes
  {[:get "/home" :home]})

(defn match-uri [uri]
  (:route-handler (sibiro/match-uri (sibiro/compile-routes routes) uri :get)))

(def history (pushy/pushy set-page! match-uri))

(pushy/start! history)
```

### URL handling

By default pushy will dispatch on all relative URLs and absolute URLs that match the window's origin. This means that all external links will be bypassed.

It is possible to specify which URLs are processable to pushy by specifying a custom predicate function `:processable-url?` in the constructor. This function is passed a single argument which is an instance of `goog.URI`.

### goog.history.HTML5History methods

You can set the history state manually by calling the `set-token!` method. This will call the `dispatch` fn on a successfully matched path.

Example:

```clojure
(set-token! history "/foo")

(get-token history)
;; => /foo
```

Likewise, you can call `replace-token!` which will also call the `dispatch` fn and replace the current history state without affecting the rest of the history stack.

## License

Copyright © 2017

Distributed under the Eclipse Public License either version 1.0
