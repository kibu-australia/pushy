var history, oldPushState, oldReplaceState, _base;

(_base = Function.prototype).bind || (_base.bind = function(thisp) {
  var fn;

  fn = this;
  return function() {
    return fn.apply(thisp, arguments);
  };
});

if (window.navigator.userAgent.match(/PhantomJS/)) {
  history = window.history;
  oldPushState = history.pushState.bind(history);
  oldReplaceState = history.replaceState.bind(history);
  history.pushState = function(state, title, url) {
    history.state = state;
    return oldPushState(state, title, url);
  };
  history.replaceState = function(state, title, url) {
    history.state = state;
    return oldReplaceState(state, title, url);
  };
}
