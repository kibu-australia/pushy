# pushy
[![Build Status](https://travis-ci.org/kibu-australia/pushy.svg?branch=master)](https://travis-ci.org/kibu-australia/pushy)

A Clojurescript library for quick and easy HTML5 pushState.

What it does:

* Initializes `goog.history.HTML5History`
* Adds an event watcher to the document's `click` event, which dispatches on all matched routes. Bypasses Alt, Shift, Meta, Ctrl keys as well as middle clicks.

## Install

[![Clojars Project](http://clojars.org/kibu/pushy/latest-version.svg)](http://clojars.org/kibu/pushy)

## Usage

### Setup

You can initialize pushState by calling the `push-state!` function.

This takes in two arguments:

* `dispatch` fn: gets called when there is a match
* `match` fn: checks if the path matches any routes defined.

Optionally, you can pass in an `identity` fn which parses and returns the route based on the result of the `match` fn


pushy should work with any routing library.


[Secretary](https://github.com/gf3/secretary)

```clojure
  (ns foo.core
    (:require [secretary.core :as secretary :include-macros true :refer [defroute]]
              [pushy.core :as pushy :refer [push-state!])

  (secretary/set-config! :prefix "/")

  (defroute index "/" []
    (.log js/console "Hi"))

  (push-state! secretary/dispatch!
               (fn [x] (when (secretary/locate-route x) x)))
```

[Bidi](https://github.com/juxt/bidi)

```clojure
  (ns foo.core
    (:require [bidi.bidi :as bidi]
              [pushy.core :as pushy :refer [push-state!])

  (def state (atom {}))

  (def app-routes
    ["/" {"foo" :foo}])

  (defn set-page! [match]
    (swap state assoc :page match))

  (push-state! set-page! (partial bidi/match-route app-routes))
```

Note: check out our fork of [bidi](https://github.com/kibu-australia/bidi) for an experimental cljx version.

### set-token!

It is also possible to set the history state by calling the `set-token!` function. This will also call the `dispatch` fn on a successfully matched path.

```clojure
(set-token! "/foo")
```

## License

Copyright © 2014

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
