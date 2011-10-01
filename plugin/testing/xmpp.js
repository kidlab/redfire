
/*  Prototype JavaScript framework, version 1.5.1
 *  (c) 2005-2007 Sam Stephenson
 *
 *  Prototype is freely distributable under the terms of an MIT-style license.
 *  For details, see the Prototype web site: http://www.prototypejs.org/
 *
/*--------------------------------------------------------------------------*/

var Prototype = {
  Version: '1.5.1',

  Browser: {
    IE:     !!(window.attachEvent && !window.opera),
    Opera:  !!window.opera,
    WebKit: navigator.userAgent.indexOf('AppleWebKit/') > -1,
    Gecko:  navigator.userAgent.indexOf('Gecko') > -1 && navigator.userAgent.indexOf('KHTML') == -1
  },

  BrowserFeatures: {
    XPath: !!document.evaluate,
    ElementExtensions: !!window.HTMLElement,
    SpecificElementExtensions:
      (document.createElement('div').__proto__ !==
       document.createElement('form').__proto__)
  },

  ScriptFragment: '<script[^>]*>([\u0001-\uFFFF]*?)</script>',
  JSONFilter: /^\/\*-secure-\s*(.*)\s*\*\/\s*$/,

  emptyFunction: function() { },
  K: function(x) { return x }
}

var Class = {
  create: function() {
    return function() {
      this.initialize.apply(this, arguments);
    }
  }
}

var Abstract = new Object();

Object.extend = function(destination, source) {
  for (var property in source) {
    destination[property] = source[property];
  }
  return destination;
}

Object.extend(Object, {
  inspect: function(object) {
    try {
      if (object === undefined) return 'undefined';
      if (object === null) return 'null';
      return object.inspect ? object.inspect() : object.toString();
    } catch (e) {
      if (e instanceof RangeError) return '...';
      throw e;
    }
  },

  toJSON: function(object) {
    var type = typeof object;
    switch(type) {
      case 'undefined':
      case 'function':
      case 'unknown': return;
      case 'boolean': return object.toString();
    }
    if (object === null) return 'null';
    if (object.toJSON) return object.toJSON();
    if (object.ownerDocument === document) return;
    var results = [];
    for (var property in object) {
      var value = Object.toJSON(object[property]);
      if (value !== undefined)
        results.push(property.toJSON() + ': ' + value);
    }
    return '{' + results.join(', ') + '}';
  },

  keys: function(object) {
    var keys = [];
    for (var property in object)
      keys.push(property);
    return keys;
  },

  values: function(object) {
    var values = [];
    for (var property in object)
      values.push(object[property]);
    return values;
  },

  clone: function(object) {
    return Object.extend({}, object);
  }
});

Function.prototype.bind = function() {
  var __method = this, args = $A(arguments), object = args.shift();
  return function() {
    return __method.apply(object, args.concat($A(arguments)));
  }
}

Function.prototype.bindAsEventListener = function(object) {
  var __method = this, args = $A(arguments), object = args.shift();
  return function(event) {
    return __method.apply(object, [event || window.event].concat(args));
  }
}

Object.extend(Number.prototype, {
  toColorPart: function() {
    return this.toPaddedString(2, 16);
  },

  succ: function() {
    return this + 1;
  },

  times: function(iterator) {
    $R(0, this, true).each(iterator);
    return this;
  },

  toPaddedString: function(length, radix) {
    var string = this.toString(radix || 10);
    return '0'.times(length - string.length) + string;
  },

  toJSON: function() {
    return isFinite(this) ? this.toString() : 'null';
  }
});

Date.prototype.toJSON = function() {
  return '"' + this.getFullYear() + '-' +
    (this.getMonth() + 1).toPaddedString(2) + '-' +
    this.getDate().toPaddedString(2) + 'T' +
    this.getHours().toPaddedString(2) + ':' +
    this.getMinutes().toPaddedString(2) + ':' +
    this.getSeconds().toPaddedString(2) + '"';
};

var Try = {
  these: function() {
    var returnValue;

    for (var i = 0, length = arguments.length; i < length; i++) {
      var lambda = arguments[i];
      try {
        returnValue = lambda();
        break;
      } catch (e) {}
    }

    return returnValue;
  }
}

/*--------------------------------------------------------------------------*/

var PeriodicalExecuter = Class.create();
PeriodicalExecuter.prototype = {
  initialize: function(callback, frequency) {
    this.callback = callback;
    this.frequency = frequency;
    this.currentlyExecuting = false;

    this.registerCallback();
  },

  registerCallback: function() {
    this.timer = setInterval(this.onTimerEvent.bind(this), this.frequency * 1000);
  },

  stop: function() {
    if (!this.timer) return;
    clearInterval(this.timer);
    this.timer = null;
  },

  onTimerEvent: function() {
    if (!this.currentlyExecuting) {
      try {
        this.currentlyExecuting = true;
        this.callback(this);
      } finally {
        this.currentlyExecuting = false;
      }
    }
  }
}
Object.extend(String, {
  interpret: function(value) {
    return value == null ? '' : String(value);
  },
  specialChar: {
    '\b': '\\b',
    '\t': '\\t',
    '\n': '\\n',
    '\f': '\\f',
    '\r': '\\r',
    '\\': '\\\\'
  }
});

Object.extend(String.prototype, {
  gsub: function(pattern, replacement) {
    var result = '', source = this, match;
    replacement = arguments.callee.prepareReplacement(replacement);

    while (source.length > 0) {
      if (match = source.match(pattern)) {
        result += source.slice(0, match.index);
        result += String.interpret(replacement(match));
        source  = source.slice(match.index + match[0].length);
      } else {
        result += source, source = '';
      }
    }
    return result;
  },

  sub: function(pattern, replacement, count) {
    replacement = this.gsub.prepareReplacement(replacement);
    count = count === undefined ? 1 : count;

    return this.gsub(pattern, function(match) {
      if (--count < 0) return match[0];
      return replacement(match);
    });
  },

  scan: function(pattern, iterator) {
    this.gsub(pattern, iterator);
    return this;
  },

  truncate: function(length, truncation) {
    length = length || 30;
    truncation = truncation === undefined ? '...' : truncation;
    return this.length > length ?
      this.slice(0, length - truncation.length) + truncation : this;
  },

  strip: function() {
    return this.replace(/^\s+/, '').replace(/\s+$/, '');
  },

  stripTags: function() {
    return this.replace(/<\/?[^>]+>/gi, '');
  },

  stripScripts: function() {
    return this.replace(new RegExp(Prototype.ScriptFragment, 'img'), '');
  },

  extractScripts: function() {
    var matchAll = new RegExp(Prototype.ScriptFragment, 'img');
    var matchOne = new RegExp(Prototype.ScriptFragment, 'im');
    return (this.match(matchAll) || []).map(function(scriptTag) {
      return (scriptTag.match(matchOne) || ['', ''])[1];
    });
  },

  evalScripts: function() {
    return this.extractScripts().map(function(script) { return eval(script) });
  },

  escapeHTML: function() {
    var self = arguments.callee;
    self.text.data = this;
    return self.div.innerHTML;
  },

  unescapeHTML: function() {
    var div = document.createElement('div');
    div.innerHTML = this.stripTags();
    return div.childNodes[0] ? (div.childNodes.length > 1 ?
      $A(div.childNodes).inject('', function(memo, node) { return memo+node.nodeValue }) :
      div.childNodes[0].nodeValue) : '';
  },

  toQueryParams: function(separator) {
    var match = this.strip().match(/([^?#]*)(#.*)?$/);
    if (!match) return {};

    return match[1].split(separator || '&').inject({}, function(hash, pair) {
      if ((pair = pair.split('='))[0]) {
        var key = decodeURIComponent(pair.shift());
        var value = pair.length > 1 ? pair.join('=') : pair[0];
        if (value != undefined) value = decodeURIComponent(value);

        if (key in hash) {
          if (hash[key].constructor != Array) hash[key] = [hash[key]];
          hash[key].push(value);
        }
        else hash[key] = value;
      }
      return hash;
    });
  },

  toArray: function() {
    return this.split('');
  },

  succ: function() {
    return this.slice(0, this.length - 1) +
      String.fromCharCode(this.charCodeAt(this.length - 1) + 1);
  },

  times: function(count) {
    var result = '';
    for (var i = 0; i < count; i++) result += this;
    return result;
  },

  camelize: function() {
    var parts = this.split('-'), len = parts.length;
    if (len == 1) return parts[0];

    var camelized = this.charAt(0) == '-'
      ? parts[0].charAt(0).toUpperCase() + parts[0].substring(1)
      : parts[0];

    for (var i = 1; i < len; i++)
      camelized += parts[i].charAt(0).toUpperCase() + parts[i].substring(1);

    return camelized;
  },

  capitalize: function() {
    return this.charAt(0).toUpperCase() + this.substring(1).toLowerCase();
  },

  underscore: function() {
    return this.gsub(/::/, '/').gsub(/([A-Z]+)([A-Z][a-z])/,'#{1}_#{2}').gsub(/([a-z\d])([A-Z])/,'#{1}_#{2}').gsub(/-/,'_').toLowerCase();
  },

  dasherize: function() {
    return this.gsub(/_/,'-');
  },

  inspect: function(useDoubleQuotes) {
    var escapedString = this.gsub(/[\x00-\x1f\\]/, function(match) {
      var character = String.specialChar[match[0]];
      return character ? character : '\\u00' + match[0].charCodeAt().toPaddedString(2, 16);
    });
    if (useDoubleQuotes) return '"' + escapedString.replace(/"/g, '\\"') + '"';
    return "'" + escapedString.replace(/'/g, '\\\'') + "'";
  },

  toJSON: function() {
    return this.inspect(true);
  },

  unfilterJSON: function(filter) {
    return this.sub(filter || Prototype.JSONFilter, '#{1}');
  },

  evalJSON: function(sanitize) {
    var json = this.unfilterJSON();
    try {
      if (!sanitize || (/^("(\\.|[^"\\\n\r])*?"|[,:{}\[\]0-9.\-+Eaeflnr-u \n\r\t])+?$/.test(json)))
        return eval('(' + json + ')');
    } catch (e) { }
    throw new SyntaxError('Badly formed JSON string: ' + this.inspect());
  },

  include: function(pattern) {
    return this.indexOf(pattern) > -1;
  },

  startsWith: function(pattern) {
    return this.indexOf(pattern) === 0;
  },

  endsWith: function(pattern) {
    var d = this.length - pattern.length;
    return d >= 0 && this.lastIndexOf(pattern) === d;
  },

  empty: function() {
    return this == '';
  },

  blank: function() {
    return /^\s*$/.test(this);
  }
});

if (Prototype.Browser.WebKit || Prototype.Browser.IE) Object.extend(String.prototype, {
  escapeHTML: function() {
    return this.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;');
  },
  unescapeHTML: function() {
    return this.replace(/&amp;/g,'&').replace(/&lt;/g,'<').replace(/&gt;/g,'>');
  }
});

String.prototype.gsub.prepareReplacement = function(replacement) {
  if (typeof replacement == 'function') return replacement;
  var template = new Template(replacement);
  return function(match) { return template.evaluate(match) };
}

String.prototype.parseQuery = String.prototype.toQueryParams;

Object.extend(String.prototype.escapeHTML, {
  div:  document.createElement('div'),
  text: document.createTextNode('')
});

with (String.prototype.escapeHTML) div.appendChild(text);

var Template = Class.create();
Template.Pattern = /(^|.|\r|\n)(#\{(.*?)\})/;
Template.prototype = {
  initialize: function(template, pattern) {
    this.template = template.toString();
    this.pattern  = pattern || Template.Pattern;
  },

  evaluate: function(object) {
    return this.template.gsub(this.pattern, function(match) {
      var before = match[1];
      if (before == '\\') return match[2];
      return before + String.interpret(object[match[3]]);
    });
  }
}

var $break = {}, $continue = new Error('"throw $continue" is deprecated, use "return" instead');

var Enumerable = {
  each: function(iterator) {
    var index = 0;
    try {
      this._each(function(value) {
        iterator(value, index++);
      });
    } catch (e) {
      if (e != $break) throw e;
    }
    return this;
  },

  eachSlice: function(number, iterator) {
    var index = -number, slices = [], array = this.toArray();
    while ((index += number) < array.length)
      slices.push(array.slice(index, index+number));
    return slices.map(iterator);
  },

  all: function(iterator) {
    var result = true;
    this.each(function(value, index) {
      result = result && !!(iterator || Prototype.K)(value, index);
      if (!result) throw $break;
    });
    return result;
  },

  any: function(iterator) {
    var result = false;
    this.each(function(value, index) {
      if (result = !!(iterator || Prototype.K)(value, index))
        throw $break;
    });
    return result;
  },

  collect: function(iterator) {
    var results = [];
    this.each(function(value, index) {
      results.push((iterator || Prototype.K)(value, index));
    });
    return results;
  },

  detect: function(iterator) {
    var result;
    this.each(function(value, index) {
      if (iterator(value, index)) {
        result = value;
        throw $break;
      }
    });
    return result;
  },

  findAll: function(iterator) {
    var results = [];
    this.each(function(value, index) {
      if (iterator(value, index))
        results.push(value);
    });
    return results;
  },

  grep: function(pattern, iterator) {
    var results = [];
    this.each(function(value, index) {
      var stringValue = value.toString();
      if (stringValue.match(pattern))
        results.push((iterator || Prototype.K)(value, index));
    })
    return results;
  },

  include: function(object) {
    var found = false;
    this.each(function(value) {
      if (value == object) {
        found = true;
        throw $break;
      }
    });
    return found;
  },

  inGroupsOf: function(number, fillWith) {
    fillWith = fillWith === undefined ? null : fillWith;
    return this.eachSlice(number, function(slice) {
      while(slice.length < number) slice.push(fillWith);
      return slice;
    });
  },

  inject: function(memo, iterator) {
    this.each(function(value, index) {
      memo = iterator(memo, value, index);
    });
    return memo;
  },

  invoke: function(method) {
    var args = $A(arguments).slice(1);
    return this.map(function(value) {
      return value[method].apply(value, args);
    });
  },

  max: function(iterator) {
    var result;
    this.each(function(value, index) {
      value = (iterator || Prototype.K)(value, index);
      if (result == undefined || value >= result)
        result = value;
    });
    return result;
  },

  min: function(iterator) {
    var result;
    this.each(function(value, index) {
      value = (iterator || Prototype.K)(value, index);
      if (result == undefined || value < result)
        result = value;
    });
    return result;
  },

  partition: function(iterator) {
    var trues = [], falses = [];
    this.each(function(value, index) {
      ((iterator || Prototype.K)(value, index) ?
        trues : falses).push(value);
    });
    return [trues, falses];
  },

  pluck: function(property) {
    var results = [];
    this.each(function(value, index) {
      results.push(value[property]);
    });
    return results;
  },

  reject: function(iterator) {
    var results = [];
    this.each(function(value, index) {
      if (!iterator(value, index))
        results.push(value);
    });
    return results;
  },

  sortBy: function(iterator) {
    return this.map(function(value, index) {
      return {value: value, criteria: iterator(value, index)};
    }).sort(function(left, right) {
      var a = left.criteria, b = right.criteria;
      return a < b ? -1 : a > b ? 1 : 0;
    }).pluck('value');
  },

  toArray: function() {
    return this.map();
  },

  zip: function() {
    var iterator = Prototype.K, args = $A(arguments);
    if (typeof args.last() == 'function')
      iterator = args.pop();

    var collections = [this].concat(args).map($A);
    return this.map(function(value, index) {
      return iterator(collections.pluck(index));
    });
  },

  size: function() {
    return this.toArray().length;
  },

  inspect: function() {
    return '#<Enumerable:' + this.toArray().inspect() + '>';
  }
}

Object.extend(Enumerable, {
  map:     Enumerable.collect,
  find:    Enumerable.detect,
  select:  Enumerable.findAll,
  member:  Enumerable.include,
  entries: Enumerable.toArray
});
var $A = Array.from = function(iterable) {
  if (!iterable) return [];
  if (iterable.toArray) {
    return iterable.toArray();
  } else {
    var results = [];
    for (var i = 0, length = iterable.length; i < length; i++)
      results.push(iterable[i]);
    return results;
  }
}

if (Prototype.Browser.WebKit) {
  $A = Array.from = function(iterable) {
    if (!iterable) return [];
    if (!(typeof iterable == 'function' && iterable == '[object NodeList]') &&
      iterable.toArray) {
      return iterable.toArray();
    } else {
      var results = [];
      for (var i = 0, length = iterable.length; i < length; i++)
        results.push(iterable[i]);
      return results;
    }
  }
}

Object.extend(Array.prototype, Enumerable);

if (!Array.prototype._reverse)
  Array.prototype._reverse = Array.prototype.reverse;

Object.extend(Array.prototype, {
  _each: function(iterator) {
    for (var i = 0, length = this.length; i < length; i++)
      iterator(this[i]);
  },

  clear: function() {
    this.length = 0;
    return this;
  },

  first: function() {
    return this[0];
  },

  last: function() {
    return this[this.length - 1];
  },

  compact: function() {
    return this.select(function(value) {
      return value != null;
    });
  },

  flatten: function() {
    return this.inject([], function(array, value) {
      return array.concat(value && value.constructor == Array ?
        value.flatten() : [value]);
    });
  },

  without: function() {
    var values = $A(arguments);
    return this.select(function(value) {
      return !values.include(value);
    });
  },

  indexOf: function(object) {
    for (var i = 0, length = this.length; i < length; i++)
      if (this[i] == object) return i;
    return -1;
  },

  reverse: function(inline) {
    return (inline !== false ? this : this.toArray())._reverse();
  },

  reduce: function() {
    return this.length > 1 ? this : this[0];
  },

  uniq: function(sorted) {
    return this.inject([], function(array, value, index) {
      if (0 == index || (sorted ? array.last() != value : !array.include(value)))
        array.push(value);
      return array;
    });
  },

  clone: function() {
    return [].concat(this);
  },

  size: function() {
    return this.length;
  },

  inspect: function() {
    return '[' + this.map(Object.inspect).join(', ') + ']';
  },

  toJSON: function() {
    var results = [];
    this.each(function(object) {
      var value = Object.toJSON(object);
      if (value !== undefined) results.push(value);
    });
    return '[' + results.join(', ') + ']';
  }
});

Array.prototype.toArray = Array.prototype.clone;

function $w(string) {
  string = string.strip();
  return string ? string.split(/\s+/) : [];
}

if (Prototype.Browser.Opera){
  Array.prototype.concat = function() {
    var array = [];
    for (var i = 0, length = this.length; i < length; i++) array.push(this[i]);
    for (var i = 0, length = arguments.length; i < length; i++) {
      if (arguments[i].constructor == Array) {
        for (var j = 0, arrayLength = arguments[i].length; j < arrayLength; j++)
          array.push(arguments[i][j]);
      } else {
        array.push(arguments[i]);
      }
    }
    return array;
  }
}
var Hash = function(object) {
  if (object instanceof Hash) this.merge(object);
  else Object.extend(this, object || {});
};

Object.extend(Hash, {
  toQueryString: function(obj) {
    var parts = [];
    parts.add = arguments.callee.addPair;

    this.prototype._each.call(obj, function(pair) {
      if (!pair.key) return;
      var value = pair.value;

      if (value && typeof value == 'object') {
        if (value.constructor == Array) value.each(function(value) {
          parts.add(pair.key, value);
        });
        return;
      }
      parts.add(pair.key, value);
    });

    return parts.join('&');
  },

  toJSON: function(object) {
    var results = [];
    this.prototype._each.call(object, function(pair) {
      var value = Object.toJSON(pair.value);
      if (value !== undefined) results.push(pair.key.toJSON() + ': ' + value);
    });
    return '{' + results.join(', ') + '}';
  }
});

Hash.toQueryString.addPair = function(key, value, prefix) {
  key = encodeURIComponent(key);
  if (value === undefined) this.push(key);
  else this.push(key + '=' + (value == null ? '' : encodeURIComponent(value)));
}

Object.extend(Hash.prototype, Enumerable);
Object.extend(Hash.prototype, {
  _each: function(iterator) {
    for (var key in this) {
      var value = this[key];
      if (value && value == Hash.prototype[key]) continue;

      var pair = [key, value];
      pair.key = key;
      pair.value = value;
      iterator(pair);
    }
  },

  keys: function() {
    return this.pluck('key');
  },

  values: function() {
    return this.pluck('value');
  },

  merge: function(hash) {
    return $H(hash).inject(this, function(mergedHash, pair) {
      mergedHash[pair.key] = pair.value;
      return mergedHash;
    });
  },

  remove: function() {
    var result;
    for(var i = 0, length = arguments.length; i < length; i++) {
      var value = this[arguments[i]];
      if (value !== undefined){
        if (result === undefined) result = value;
        else {
          if (result.constructor != Array) result = [result];
          result.push(value)
        }
      }
      delete this[arguments[i]];
    }
    return result;
  },

  toQueryString: function() {
    return Hash.toQueryString(this);
  },

  inspect: function() {
    return '#<Hash:{' + this.map(function(pair) {
      return pair.map(Object.inspect).join(': ');
    }).join(', ') + '}>';
  },

  toJSON: function() {
    return Hash.toJSON(this);
  }
});

function $H(object) {
  if (object instanceof Hash) return object;
  return new Hash(object);
};

// Safari iterates over shadowed properties
if (function() {
  var i = 0, Test = function(value) { this.key = value };
  Test.prototype.key = 'foo';
  for (var property in new Test('bar')) i++;
  return i > 1;
}()) Hash.prototype._each = function(iterator) {
  var cache = [];
  for (var key in this) {
    var value = this[key];
    if ((value && value == Hash.prototype[key]) || cache.include(key)) continue;
    cache.push(key);
    var pair = [key, value];
    pair.key = key;
    pair.value = value;
    iterator(pair);
  }
};
ObjectRange = Class.create();
Object.extend(ObjectRange.prototype, Enumerable);
Object.extend(ObjectRange.prototype, {
  initialize: function(start, end, exclusive) {
    this.start = start;
    this.end = end;
    this.exclusive = exclusive;
  },

  _each: function(iterator) {
    var value = this.start;
    while (this.include(value)) {
      iterator(value);
      value = value.succ();
    }
  },

  include: function(value) {
    if (value < this.start)
      return false;
    if (this.exclusive)
      return value < this.end;
    return value <= this.end;
  }
});

var $R = function(start, end, exclusive) {
  return new ObjectRange(start, end, exclusive);
}

var Ajax = {
  getTransport: function() {
    return Try.these(
      function() {return new XMLHttpRequest()},
      function() {return new ActiveXObject('Msxml2.XMLHTTP')},
      function() {return new ActiveXObject('Microsoft.XMLHTTP')}
    ) || false;
  },

  activeRequestCount: 0
}

Ajax.Responders = {
  responders: [],

  _each: function(iterator) {
    this.responders._each(iterator);
  },

  register: function(responder) {
    if (!this.include(responder))
      this.responders.push(responder);
  },

  unregister: function(responder) {
    this.responders = this.responders.without(responder);
  },

  dispatch: function(callback, request, transport, json) {
    this.each(function(responder) {
      if (typeof responder[callback] == 'function') {
        try {
          responder[callback].apply(responder, [request, transport, json]);
        } catch (e) {}
      }
    });
  }
};

Object.extend(Ajax.Responders, Enumerable);

Ajax.Responders.register({
  onCreate: function() {
    Ajax.activeRequestCount++;
  },
  onComplete: function() {
    Ajax.activeRequestCount--;
  }
});

Ajax.Base = function() {};
Ajax.Base.prototype = {
  setOptions: function(options) {
    this.options = {
      method:       'post',
      asynchronous: true,
      contentType:  'application/x-www-form-urlencoded',
      encoding:     'UTF-8',
      parameters:   ''
    }
    Object.extend(this.options, options || {});

    this.options.method = this.options.method.toLowerCase();
    if (typeof this.options.parameters == 'string')
      this.options.parameters = this.options.parameters.toQueryParams();
  }
}

Ajax.Request = Class.create();
Ajax.Request.Events =
  ['Uninitialized', 'Loading', 'Loaded', 'Interactive', 'Complete'];

Ajax.Request.prototype = Object.extend(new Ajax.Base(), {
  _complete: false,

  initialize: function(url, options) {
    this.transport = Ajax.getTransport();
    this.setOptions(options);
    this.request(url);
  },

  request: function(url) {
    this.url = url;
    this.method = this.options.method;
    var params = Object.clone(this.options.parameters);

    if (!['get', 'post'].include(this.method)) {
      // simulate other verbs over post
      params['_method'] = this.method;
      this.method = 'post';
    }

    this.parameters = params;

    if (params = Hash.toQueryString(params)) {
      // when GET, append parameters to URL
      if (this.method == 'get')
        this.url += (this.url.include('?') ? '&' : '?') + params;
      else if (/Konqueror|Safari|KHTML/.test(navigator.userAgent))
        params += '&_=';
    }

    try {
      if (this.options.onCreate) this.options.onCreate(this.transport);
      Ajax.Responders.dispatch('onCreate', this, this.transport);

      this.transport.open(this.method.toUpperCase(), this.url,
        this.options.asynchronous);

      if (this.options.asynchronous)
        setTimeout(function() { this.respondToReadyState(1) }.bind(this), 10);

      this.transport.onreadystatechange = this.onStateChange.bind(this);
      this.setRequestHeaders();

      this.body = this.method == 'post' ? (this.options.postBody || params) : null;
      this.transport.send(this.body);

      /* Force Firefox to handle ready state 4 for synchronous requests */
      if (!this.options.asynchronous && this.transport.overrideMimeType)
        this.onStateChange();

    }
    catch (e) {
      this.dispatchException(e);
    }
  },

  onStateChange: function() {
    var readyState = this.transport.readyState;
    if (readyState > 1 && !((readyState == 4) && this._complete))
      this.respondToReadyState(this.transport.readyState);
  },

  setRequestHeaders: function() {
    var headers = {
      'X-Requested-With': 'XMLHttpRequest',
      'X-Prototype-Version': Prototype.Version,
      'Accept': 'text/javascript, text/html, application/xml, text/xml, */*'
    };

    if (this.method == 'post') {
      headers['Content-type'] = this.options.contentType +
        (this.options.encoding ? '; charset=' + this.options.encoding : '');

      /* Force "Connection: close" for older Mozilla browsers to work
       * around a bug where XMLHttpRequest sends an incorrect
       * Content-length header. See Mozilla Bugzilla #246651.
       */
      if (this.transport.overrideMimeType &&
          (navigator.userAgent.match(/Gecko\/(\d{4})/) || [0,2005])[1] < 2005)
            headers['Connection'] = 'close';
    }

    // user-defined headers
    if (typeof this.options.requestHeaders == 'object') {
      var extras = this.options.requestHeaders;

      if (typeof extras.push == 'function')
        for (var i = 0, length = extras.length; i < length; i += 2)
          headers[extras[i]] = extras[i+1];
      else
        $H(extras).each(function(pair) { headers[pair.key] = pair.value });
    }

    for (var name in headers)
      this.transport.setRequestHeader(name, headers[name]);
  },

  success: function() {
    return !this.transport.status
        || (this.transport.status >= 200 && this.transport.status < 300);
  },

  respondToReadyState: function(readyState) {
    var state = Ajax.Request.Events[readyState];
    var transport = this.transport, json = this.evalJSON();

    if (state == 'Complete') {
      try {
        this._complete = true;
        (this.options['on' + this.transport.status]
         || this.options['on' + (this.success() ? 'Success' : 'Failure')]
         || Prototype.emptyFunction)(transport, json);
      } catch (e) {
        this.dispatchException(e);
      }

      var contentType = this.getHeader('Content-type');
      if (contentType && contentType.strip().
        match(/^(text|application)\/(x-)?(java|ecma)script(;.*)?$/i))
          this.evalResponse();
    }

    try {
      (this.options['on' + state] || Prototype.emptyFunction)(transport, json);
      Ajax.Responders.dispatch('on' + state, this, transport, json);
    } catch (e) {
      this.dispatchException(e);
    }

    if (state == 'Complete') {
      // avoid memory leak in MSIE: clean up
      this.transport.onreadystatechange = Prototype.emptyFunction;
    }
  },

  getHeader: function(name) {
    try {
      return this.transport.getResponseHeader(name);
    } catch (e) { return null }
  },

  evalJSON: function() {
    try {
      var json = this.getHeader('X-JSON');
      return json ? json.evalJSON() : null;
    } catch (e) { return null }
  },

  evalResponse: function() {
    try {
      return eval((this.transport.responseText || '').unfilterJSON());
    } catch (e) {
      this.dispatchException(e);
    }
  },

  dispatchException: function(exception) {
    (this.options.onException || Prototype.emptyFunction)(this, exception);
    Ajax.Responders.dispatch('onException', this, exception);
  }
});

Ajax.Updater = Class.create();

Object.extend(Object.extend(Ajax.Updater.prototype, Ajax.Request.prototype), {
  initialize: function(container, url, options) {
    this.container = {
      success: (container.success || container),
      failure: (container.failure || (container.success ? null : container))
    }

    this.transport = Ajax.getTransport();
    this.setOptions(options);

    var onComplete = this.options.onComplete || Prototype.emptyFunction;
    this.options.onComplete = (function(transport, param) {
      this.updateContent();
      onComplete(transport, param);
    }).bind(this);

    this.request(url);
  },

  updateContent: function() {
    var receiver = this.container[this.success() ? 'success' : 'failure'];
    var response = this.transport.responseText;

    if (!this.options.evalScripts) response = response.stripScripts();

    if (receiver = $(receiver)) {
      if (this.options.insertion)
        new this.options.insertion(receiver, response);
      else
        receiver.update(response);
    }

    if (this.success()) {
      if (this.onComplete)
        setTimeout(this.onComplete.bind(this), 10);
    }
  }
});

Ajax.PeriodicalUpdater = Class.create();
Ajax.PeriodicalUpdater.prototype = Object.extend(new Ajax.Base(), {
  initialize: function(container, url, options) {
    this.setOptions(options);
    this.onComplete = this.options.onComplete;

    this.frequency = (this.options.frequency || 2);
    this.decay = (this.options.decay || 1);

    this.updater = {};
    this.container = container;
    this.url = url;

    this.start();
  },

  start: function() {
    this.options.onComplete = this.updateComplete.bind(this);
    this.onTimerEvent();
  },

  stop: function() {
    this.updater.options.onComplete = undefined;
    clearTimeout(this.timer);
    (this.onComplete || Prototype.emptyFunction).apply(this, arguments);
  },

  updateComplete: function(request) {
    if (this.options.decay) {
      this.decay = (request.responseText == this.lastText ?
        this.decay * this.options.decay : 1);

      this.lastText = request.responseText;
    }
    this.timer = setTimeout(this.onTimerEvent.bind(this),
      this.decay * this.frequency * 1000);
  },

  onTimerEvent: function() {
    this.updater = new Ajax.Updater(this.container, this.url, this.options);
  }
});
function $(element) {
  if (arguments.length > 1) {
    for (var i = 0, elements = [], length = arguments.length; i < length; i++)
      elements.push($(arguments[i]));
    return elements;
  }
  if (typeof element == 'string')
    element = document.getElementById(element);
  return Element.extend(element);
}

if (Prototype.BrowserFeatures.XPath) {
  document._getElementsByXPath = function(expression, parentElement) {
    var results = [];
    var query = document.evaluate(expression, $(parentElement) || document,
      null, XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null);
    for (var i = 0, length = query.snapshotLength; i < length; i++)
      results.push(query.snapshotItem(i));
    return results;
  };

  document.getElementsByClassName = function(className, parentElement) {
    var q = ".//*[contains(concat(' ', @class, ' '), ' " + className + " ')]";
    return document._getElementsByXPath(q, parentElement);
  }

} else document.getElementsByClassName = function(className, parentElement) {
  var children = ($(parentElement) || document.body).getElementsByTagName('*');
  var elements = [], child;
  for (var i = 0, length = children.length; i < length; i++) {
    child = children[i];
    if (Element.hasClassName(child, className))
      elements.push(Element.extend(child));
  }
  return elements;
};

/*--------------------------------------------------------------------------*/

if (!window.Element) var Element = {};

Element.extend = function(element) {
  var F = Prototype.BrowserFeatures;
  if (!element || !element.tagName || element.nodeType == 3 ||
   element._extended || F.SpecificElementExtensions || element == window)
    return element;

  var methods = {}, tagName = element.tagName, cache = Element.extend.cache,
   T = Element.Methods.ByTag;

  // extend methods for all tags (Safari doesn't need this)
  if (!F.ElementExtensions) {
    Object.extend(methods, Element.Methods),
    Object.extend(methods, Element.Methods.Simulated);
  }

  // extend methods for specific tags
  if (T[tagName]) Object.extend(methods, T[tagName]);

  for (var property in methods) {
    var value = methods[property];
    if (typeof value == 'function' && !(property in element))
      element[property] = cache.findOrStore(value);
  }

  element._extended = Prototype.emptyFunction;
  return element;
};

Element.extend.cache = {
  findOrStore: function(value) {
    return this[value] = this[value] || function() {
      return value.apply(null, [this].concat($A(arguments)));
    }
  }
};

Element.Methods = {
  visible: function(element) {
    return $(element).style.display != 'none';
  },

  toggle: function(element) {
    element = $(element);
    Element[Element.visible(element) ? 'hide' : 'show'](element);
    return element;
  },

  hide: function(element) {
    $(element).style.display = 'none';
    return element;
  },

  show: function(element) {
    $(element).style.display = '';
    return element;
  },

  remove: function(element) {
    element = $(element);
    element.parentNode.removeChild(element);
    return element;
  },

  update: function(element, html) {
    html = typeof html == 'undefined' ? '' : html.toString();
    $(element).innerHTML = html.stripScripts();
    setTimeout(function() {html.evalScripts()}, 10);
    return element;
  },

  replace: function(element, html) {
    element = $(element);
    html = typeof html == 'undefined' ? '' : html.toString();
    if (element.outerHTML) {
      element.outerHTML = html.stripScripts();
    } else {
      var range = element.ownerDocument.createRange();
      range.selectNodeContents(element);
      element.parentNode.replaceChild(
        range.createContextualFragment(html.stripScripts()), element);
    }
    setTimeout(function() {html.evalScripts()}, 10);
    return element;
  },

  inspect: function(element) {
    element = $(element);
    var result = '<' + element.tagName.toLowerCase();
    $H({'id': 'id', 'className': 'class'}).each(function(pair) {
      var property = pair.first(), attribute = pair.last();
      var value = (element[property] || '').toString();
      if (value) result += ' ' + attribute + '=' + value.inspect(true);
    });
    return result + '>';
  },

  recursivelyCollect: function(element, property) {
    element = $(element);
    var elements = [];
    while (element = element[property])
      if (element.nodeType == 1)
        elements.push(Element.extend(element));
    return elements;
  },

  ancestors: function(element) {
    return $(element).recursivelyCollect('parentNode');
  },

  descendants: function(element) {
    return $A($(element).getElementsByTagName('*')).each(Element.extend);
  },

  firstDescendant: function(element) {
    element = $(element).firstChild;
    while (element && element.nodeType != 1) element = element.nextSibling;
    return $(element);
  },

  immediateDescendants: function(element) {
    if (!(element = $(element).firstChild)) return [];
    while (element && element.nodeType != 1) element = element.nextSibling;
    if (element) return [element].concat($(element).nextSiblings());
    return [];
  },

  previousSiblings: function(element) {
    return $(element).recursivelyCollect('previousSibling');
  },

  nextSiblings: function(element) {
    return $(element).recursivelyCollect('nextSibling');
  },

  siblings: function(element) {
    element = $(element);
    return element.previousSiblings().reverse().concat(element.nextSiblings());
  },

  match: function(element, selector) {
    if (typeof selector == 'string')
      selector = new Selector(selector);
    return selector.match($(element));
  },

  up: function(element, expression, index) {
    element = $(element);
    if (arguments.length == 1) return $(element.parentNode);
    var ancestors = element.ancestors();
    return expression ? Selector.findElement(ancestors, expression, index) :
      ancestors[index || 0];
  },

  down: function(element, expression, index) {
    element = $(element);
    if (arguments.length == 1) return element.firstDescendant();
    var descendants = element.descendants();
    return expression ? Selector.findElement(descendants, expression, index) :
      descendants[index || 0];
  },

  previous: function(element, expression, index) {
    element = $(element);
    if (arguments.length == 1) return $(Selector.handlers.previousElementSibling(element));
    var previousSiblings = element.previousSiblings();
    return expression ? Selector.findElement(previousSiblings, expression, index) :
      previousSiblings[index || 0];
  },

  next: function(element, expression, index) {
    element = $(element);
    if (arguments.length == 1) return $(Selector.handlers.nextElementSibling(element));
    var nextSiblings = element.nextSiblings();
    return expression ? Selector.findElement(nextSiblings, expression, index) :
      nextSiblings[index || 0];
  },

  getElementsBySelector: function() {
    var args = $A(arguments), element = $(args.shift());
    return Selector.findChildElements(element, args);
  },

  getElementsByClassName: function(element, className) {
    return document.getElementsByClassName(className, element);
  },

  readAttribute: function(element, name) {
    element = $(element);
    if (Prototype.Browser.IE) {
      if (!element.attributes) return null;
      var t = Element._attributeTranslations;
      if (t.values[name]) return t.values[name](element, name);
      if (t.names[name])  name = t.names[name];
      var attribute = element.attributes[name];
      return attribute ? attribute.nodeValue : null;
    }
    return element.getAttribute(name);
  },

  getHeight: function(element) {
    return $(element).getDimensions().height;
  },

  getWidth: function(element) {
    return $(element).getDimensions().width;
  },

  classNames: function(element) {
    return new Element.ClassNames(element);
  },

  hasClassName: function(element, className) {
    if (!(element = $(element))) return;
    var elementClassName = element.className;
    if (elementClassName.length == 0) return false;
    if (elementClassName == className ||
        elementClassName.match(new RegExp("(^|\\s)" + className + "(\\s|$)")))
      return true;
    return false;
  },

  addClassName: function(element, className) {
    if (!(element = $(element))) return;
    Element.classNames(element).add(className);
    return element;
  },

  removeClassName: function(element, className) {
    if (!(element = $(element))) return;
    Element.classNames(element).remove(className);
    return element;
  },

  toggleClassName: function(element, className) {
    if (!(element = $(element))) return;
    Element.classNames(element)[element.hasClassName(className) ? 'remove' : 'add'](className);
    return element;
  },

  observe: function() {
    Event.observe.apply(Event, arguments);
    return $A(arguments).first();
  },

  stopObserving: function() {
    Event.stopObserving.apply(Event, arguments);
    return $A(arguments).first();
  },

  // removes whitespace-only text node children
  cleanWhitespace: function(element) {
    element = $(element);
    var node = element.firstChild;
    while (node) {
      var nextNode = node.nextSibling;
      if (node.nodeType == 3 && !/\S/.test(node.nodeValue))
        element.removeChild(node);
      node = nextNode;
    }
    return element;
  },

  empty: function(element) {
    return $(element).innerHTML.blank();
  },

  descendantOf: function(element, ancestor) {
    element = $(element), ancestor = $(ancestor);
    while (element = element.parentNode)
      if (element == ancestor) return true;
    return false;
  },

  scrollTo: function(element) {
    element = $(element);
    var pos = Position.cumulativeOffset(element);
    window.scrollTo(pos[0], pos[1]);
    return element;
  },

  getStyle: function(element, style) {
    element = $(element);
    style = style == 'float' ? 'cssFloat' : style.camelize();
    var value = element.style[style];
    if (!value) {
      var css = document.defaultView.getComputedStyle(element, null);
      value = css ? css[style] : null;
    }
    if (style == 'opacity') return value ? parseFloat(value) : 1.0;
    return value == 'auto' ? null : value;
  },

  getOpacity: function(element) {
    return $(element).getStyle('opacity');
  },

  setStyle: function(element, styles, camelized) {
    element = $(element);
    var elementStyle = element.style;

    for (var property in styles)
      if (property == 'opacity') element.setOpacity(styles[property])
      else
        elementStyle[(property == 'float' || property == 'cssFloat') ?
          (elementStyle.styleFloat === undefined ? 'cssFloat' : 'styleFloat') :
          (camelized ? property : property.camelize())] = styles[property];

    return element;
  },

  setOpacity: function(element, value) {
    element = $(element);
    element.style.opacity = (value == 1 || value === '') ? '' :
      (value < 0.00001) ? 0 : value;
    return element;
  },

  getDimensions: function(element) {
    element = $(element);
    var display = $(element).getStyle('display');
    if (display != 'none' && display != null) // Safari bug
      return {width: element.offsetWidth, height: element.offsetHeight};

    // All *Width and *Height properties give 0 on elements with display none,
    // so enable the element temporarily
    var els = element.style;
    var originalVisibility = els.visibility;
    var originalPosition = els.position;
    var originalDisplay = els.display;
    els.visibility = 'hidden';
    els.position = 'absolute';
    els.display = 'block';
    var originalWidth = element.clientWidth;
    var originalHeight = element.clientHeight;
    els.display = originalDisplay;
    els.position = originalPosition;
    els.visibility = originalVisibility;
    return {width: originalWidth, height: originalHeight};
  },

  makePositioned: function(element) {
    element = $(element);
    var pos = Element.getStyle(element, 'position');
    if (pos == 'static' || !pos) {
      element._madePositioned = true;
      element.style.position = 'relative';
      // Opera returns the offset relative to the positioning context, when an
      // element is position relative but top and left have not been defined
      if (window.opera) {
        element.style.top = 0;
        element.style.left = 0;
      }
    }
    return element;
  },

  undoPositioned: function(element) {
    element = $(element);
    if (element._madePositioned) {
      element._madePositioned = undefined;
      element.style.position =
        element.style.top =
        element.style.left =
        element.style.bottom =
        element.style.right = '';
    }
    return element;
  },

  makeClipping: function(element) {
    element = $(element);
    if (element._overflow) return element;
    element._overflow = element.style.overflow || 'auto';
    if ((Element.getStyle(element, 'overflow') || 'visible') != 'hidden')
      element.style.overflow = 'hidden';
    return element;
  },

  undoClipping: function(element) {
    element = $(element);
    if (!element._overflow) return element;
    element.style.overflow = element._overflow == 'auto' ? '' : element._overflow;
    element._overflow = null;
    return element;
  }
};

Object.extend(Element.Methods, {
  childOf: Element.Methods.descendantOf,
  childElements: Element.Methods.immediateDescendants
});

if (Prototype.Browser.Opera) {
  Element.Methods._getStyle = Element.Methods.getStyle;
  Element.Methods.getStyle = function(element, style) {
    switch(style) {
      case 'left':
      case 'top':
      case 'right':
      case 'bottom':
        if (Element._getStyle(element, 'position') == 'static') return null;
      default: return Element._getStyle(element, style);
    }
  };
}
else if (Prototype.Browser.IE) {
  Element.Methods.getStyle = function(element, style) {
    element = $(element);
    style = (style == 'float' || style == 'cssFloat') ? 'styleFloat' : style.camelize();
    var value = element.style[style];
    if (!value && element.currentStyle) value = element.currentStyle[style];

    if (style == 'opacity') {
      if (value = (element.getStyle('filter') || '').match(/alpha\(opacity=(.*)\)/))
        if (value[1]) return parseFloat(value[1]) / 100;
      return 1.0;
    }

    if (value == 'auto') {
      if ((style == 'width' || style == 'height') && (element.getStyle('display') != 'none'))
        return element['offset'+style.capitalize()] + 'px';
      return null;
    }
    return value;
  };

  Element.Methods.setOpacity = function(element, value) {
    element = $(element);
    var filter = element.getStyle('filter'), style = element.style;
    if (value == 1 || value === '') {
      style.filter = filter.replace(/alpha\([^\)]*\)/gi,'');
      return element;
    } else if (value < 0.00001) value = 0;
    style.filter = filter.replace(/alpha\([^\)]*\)/gi, '') +
      'alpha(opacity=' + (value * 100) + ')';
    return element;
  };

  // IE is missing .innerHTML support for TABLE-related elements
  Element.Methods.update = function(element, html) {
    element = $(element);
    html = typeof html == 'undefined' ? '' : html.toString();
    var tagName = element.tagName.toUpperCase();
    if (['THEAD','TBODY','TR','TD'].include(tagName)) {
      var div = document.createElement('div');
      switch (tagName) {
        case 'THEAD':
        case 'TBODY':
          div.innerHTML = '<table><tbody>' +  html.stripScripts() + '</tbody></table>';
          depth = 2;
          break;
        case 'TR':
          div.innerHTML = '<table><tbody><tr>' +  html.stripScripts() + '</tr></tbody></table>';
          depth = 3;
          break;
        case 'TD':
          div.innerHTML = '<table><tbody><tr><td>' +  html.stripScripts() + '</td></tr></tbody></table>';
          depth = 4;
      }
      $A(element.childNodes).each(function(node) { element.removeChild(node) });
      depth.times(function() { div = div.firstChild });
      $A(div.childNodes).each(function(node) { element.appendChild(node) });
    } else {
      element.innerHTML = html.stripScripts();
    }
    setTimeout(function() { html.evalScripts() }, 10);
    return element;
  }
}
else if (Prototype.Browser.Gecko) {
  Element.Methods.setOpacity = function(element, value) {
    element = $(element);
    element.style.opacity = (value == 1) ? 0.999999 :
      (value === '') ? '' : (value < 0.00001) ? 0 : value;
    return element;
  };
}

Element._attributeTranslations = {
  names: {
    colspan:   "colSpan",
    rowspan:   "rowSpan",
    valign:    "vAlign",
    datetime:  "dateTime",
    accesskey: "accessKey",
    tabindex:  "tabIndex",
    enctype:   "encType",
    maxlength: "maxLength",
    readonly:  "readOnly",
    longdesc:  "longDesc"
  },
  values: {
    _getAttr: function(element, attribute) {
      return element.getAttribute(attribute, 2);
    },
    _flag: function(element, attribute) {
      return $(element).hasAttribute(attribute) ? attribute : null;
    },
    style: function(element) {
      return element.style.cssText.toLowerCase();
    },
    title: function(element) {
      var node = element.getAttributeNode('title');
      return node.specified ? node.nodeValue : null;
    }
  }
};

(function() {
  Object.extend(this, {
    href: this._getAttr,
    src:  this._getAttr,
    type: this._getAttr,
    disabled: this._flag,
    checked:  this._flag,
    readonly: this._flag,
    multiple: this._flag
  });
}).call(Element._attributeTranslations.values);

Element.Methods.Simulated = {
  hasAttribute: function(element, attribute) {
    var t = Element._attributeTranslations, node;
    attribute = t.names[attribute] || attribute;
    node = $(element).getAttributeNode(attribute);
    return node && node.specified;
  }
};

Element.Methods.ByTag = {};

Object.extend(Element, Element.Methods);

if (!Prototype.BrowserFeatures.ElementExtensions &&
 document.createElement('div').__proto__) {
  window.HTMLElement = {};
  window.HTMLElement.prototype = document.createElement('div').__proto__;
  Prototype.BrowserFeatures.ElementExtensions = true;
}

Element.hasAttribute = function(element, attribute) {
  if (element.hasAttribute) return element.hasAttribute(attribute);
  return Element.Methods.Simulated.hasAttribute(element, attribute);
};

Element.addMethods = function(methods) {
  var F = Prototype.BrowserFeatures, T = Element.Methods.ByTag;

  if (!methods) {
    Object.extend(Form, Form.Methods);
    Object.extend(Form.Element, Form.Element.Methods);
    Object.extend(Element.Methods.ByTag, {
      "FORM":     Object.clone(Form.Methods),
      "INPUT":    Object.clone(Form.Element.Methods),
      "SELECT":   Object.clone(Form.Element.Methods),
      "TEXTAREA": Object.clone(Form.Element.Methods)
    });
  }

  if (arguments.length == 2) {
    var tagName = methods;
    methods = arguments[1];
  }

  if (!tagName) Object.extend(Element.Methods, methods || {});
  else {
    if (tagName.constructor == Array) tagName.each(extend);
    else extend(tagName);
  }

  function extend(tagName) {
    tagName = tagName.toUpperCase();
    if (!Element.Methods.ByTag[tagName])
      Element.Methods.ByTag[tagName] = {};
    Object.extend(Element.Methods.ByTag[tagName], methods);
  }

  function copy(methods, destination, onlyIfAbsent) {
    onlyIfAbsent = onlyIfAbsent || false;
    var cache = Element.extend.cache;
    for (var property in methods) {
      var value = methods[property];
      if (!onlyIfAbsent || !(property in destination))
        destination[property] = cache.findOrStore(value);
    }
  }

  function findDOMClass(tagName) {
    var klass;
    var trans = {
      "OPTGROUP": "OptGroup", "TEXTAREA": "TextArea", "P": "Paragraph",
      "FIELDSET": "FieldSet", "UL": "UList", "OL": "OList", "DL": "DList",
      "DIR": "Directory", "H1": "Heading", "H2": "Heading", "H3": "Heading",
      "H4": "Heading", "H5": "Heading", "H6": "Heading", "Q": "Quote",
      "INS": "Mod", "DEL": "Mod", "A": "Anchor", "IMG": "Image", "CAPTION":
      "TableCaption", "COL": "TableCol", "COLGROUP": "TableCol", "THEAD":
      "TableSection", "TFOOT": "TableSection", "TBODY": "TableSection", "TR":
      "TableRow", "TH": "TableCell", "TD": "TableCell", "FRAMESET":
      "FrameSet", "IFRAME": "IFrame"
    };
    if (trans[tagName]) klass = 'HTML' + trans[tagName] + 'Element';
    if (window[klass]) return window[klass];
    klass = 'HTML' + tagName + 'Element';
    if (window[klass]) return window[klass];
    klass = 'HTML' + tagName.capitalize() + 'Element';
    if (window[klass]) return window[klass];

    window[klass] = {};
    window[klass].prototype = document.createElement(tagName).__proto__;
    return window[klass];
  }

  if (F.ElementExtensions) {
    copy(Element.Methods, HTMLElement.prototype);
    copy(Element.Methods.Simulated, HTMLElement.prototype, true);
  }

  if (F.SpecificElementExtensions) {
    for (var tag in Element.Methods.ByTag) {
      var klass = findDOMClass(tag);
      if (typeof klass == "undefined") continue;
      copy(T[tag], klass.prototype);
    }
  }

  Object.extend(Element, Element.Methods);
  delete Element.ByTag;
};

var Toggle = { display: Element.toggle };

/*--------------------------------------------------------------------------*/

Abstract.Insertion = function(adjacency) {
  this.adjacency = adjacency;
}

Abstract.Insertion.prototype = {
  initialize: function(element, content) {
    this.element = $(element);
    this.content = content.stripScripts();

    if (this.adjacency && this.element.insertAdjacentHTML) {
      try {
        this.element.insertAdjacentHTML(this.adjacency, this.content);
      } catch (e) {
        var tagName = this.element.tagName.toUpperCase();
        if (['TBODY', 'TR'].include(tagName)) {
          this.insertContent(this.contentFromAnonymousTable());
        } else {
          throw e;
        }
      }
    } else {
      this.range = this.element.ownerDocument.createRange();
      if (this.initializeRange) this.initializeRange();
      this.insertContent([this.range.createContextualFragment(this.content)]);
    }

    setTimeout(function() {content.evalScripts()}, 10);
  },

  contentFromAnonymousTable: function() {
    var div = document.createElement('div');
    div.innerHTML = '<table><tbody>' + this.content + '</tbody></table>';
    return $A(div.childNodes[0].childNodes[0].childNodes);
  }
}

var Insertion = new Object();

Insertion.Before = Class.create();
Insertion.Before.prototype = Object.extend(new Abstract.Insertion('beforeBegin'), {
  initializeRange: function() {
    this.range.setStartBefore(this.element);
  },

  insertContent: function(fragments) {
    fragments.each((function(fragment) {
      this.element.parentNode.insertBefore(fragment, this.element);
    }).bind(this));
  }
});

Insertion.Top = Class.create();
Insertion.Top.prototype = Object.extend(new Abstract.Insertion('afterBegin'), {
  initializeRange: function() {
    this.range.selectNodeContents(this.element);
    this.range.collapse(true);
  },

  insertContent: function(fragments) {
    fragments.reverse(false).each((function(fragment) {
      this.element.insertBefore(fragment, this.element.firstChild);
    }).bind(this));
  }
});

Insertion.Bottom = Class.create();
Insertion.Bottom.prototype = Object.extend(new Abstract.Insertion('beforeEnd'), {
  initializeRange: function() {
    this.range.selectNodeContents(this.element);
    this.range.collapse(this.element);
  },

  insertContent: function(fragments) {
    fragments.each((function(fragment) {
      this.element.appendChild(fragment);
    }).bind(this));
  }
});

Insertion.After = Class.create();
Insertion.After.prototype = Object.extend(new Abstract.Insertion('afterEnd'), {
  initializeRange: function() {
    this.range.setStartAfter(this.element);
  },

  insertContent: function(fragments) {
    fragments.each((function(fragment) {
      this.element.parentNode.insertBefore(fragment,
        this.element.nextSibling);
    }).bind(this));
  }
});

/*--------------------------------------------------------------------------*/

Element.ClassNames = Class.create();
Element.ClassNames.prototype = {
  initialize: function(element) {
    this.element = $(element);
  },

  _each: function(iterator) {
    this.element.className.split(/\s+/).select(function(name) {
      return name.length > 0;
    })._each(iterator);
  },

  set: function(className) {
    this.element.className = className;
  },

  add: function(classNameToAdd) {
    if (this.include(classNameToAdd)) return;
    this.set($A(this).concat(classNameToAdd).join(' '));
  },

  remove: function(classNameToRemove) {
    if (!this.include(classNameToRemove)) return;
    this.set($A(this).without(classNameToRemove).join(' '));
  },

  toString: function() {
    return $A(this).join(' ');
  }
};

Object.extend(Element.ClassNames.prototype, Enumerable);
/* Portions of the Selector class are derived from Jack Slocum’s DomQuery,
 * part of YUI-Ext version 0.40, distributed under the terms of an MIT-style
 * license.  Please see http://www.yui-ext.com/ for more information. */

var Selector = Class.create();

Selector.prototype = {
  initialize: function(expression) {
    this.expression = expression.strip();
    this.compileMatcher();
  },

  compileMatcher: function() {
    // Selectors with namespaced attributes can't use the XPath version
    if (Prototype.BrowserFeatures.XPath && !(/\[[\w-]*?:/).test(this.expression))
      return this.compileXPathMatcher();

    var e = this.expression, ps = Selector.patterns, h = Selector.handlers,
        c = Selector.criteria, le, p, m;

    if (Selector._cache[e]) {
      this.matcher = Selector._cache[e]; return;
    }
    this.matcher = ["this.matcher = function(root) {",
                    "var r = root, h = Selector.handlers, c = false, n;"];

    while (e && le != e && (/\S/).test(e)) {
      le = e;
      for (var i in ps) {
        p = ps[i];
        if (m = e.match(p)) {
          this.matcher.push(typeof c[i] == 'function' ? c[i](m) :
    	      new Template(c[i]).evaluate(m));
          e = e.replace(m[0], '');
          break;
        }
      }
    }

    this.matcher.push("return h.unique(n);\n}");
    eval(this.matcher.join('\n'));
    Selector._cache[this.expression] = this.matcher;
  },

  compileXPathMatcher: function() {
    var e = this.expression, ps = Selector.patterns,
        x = Selector.xpath, le,  m;

    if (Selector._cache[e]) {
      this.xpath = Selector._cache[e]; return;
    }

    this.matcher = ['.//*'];
    while (e && le != e && (/\S/).test(e)) {
      le = e;
      for (var i in ps) {
        if (m = e.match(ps[i])) {
          this.matcher.push(typeof x[i] == 'function' ? x[i](m) :
            new Template(x[i]).evaluate(m));
          e = e.replace(m[0], '');
          break;
        }
      }
    }

    this.xpath = this.matcher.join('');
    Selector._cache[this.expression] = this.xpath;
  },

  findElements: function(root) {
    root = root || document;
    if (this.xpath) return document._getElementsByXPath(this.xpath, root);
    return this.matcher(root);
  },

  match: function(element) {
    return this.findElements(document).include(element);
  },

  toString: function() {
    return this.expression;
  },

  inspect: function() {
    return "#<Selector:" + this.expression.inspect() + ">";
  }
};

Object.extend(Selector, {
  _cache: {},

  xpath: {
    descendant:   "//*",
    child:        "/*",
    adjacent:     "/following-sibling::*[1]",
    laterSibling: '/following-sibling::*',
    tagName:      function(m) {
      if (m[1] == '*') return '';
      return "[local-name()='" + m[1].toLowerCase() +
             "' or local-name()='" + m[1].toUpperCase() + "']";
    },
    className:    "[contains(concat(' ', @class, ' '), ' #{1} ')]",
    id:           "[@id='#{1}']",
    attrPresence: "[@#{1}]",
    attr: function(m) {
      m[3] = m[5] || m[6];
      return new Template(Selector.xpath.operators[m[2]]).evaluate(m);
    },
    pseudo: function(m) {
      var h = Selector.xpath.pseudos[m[1]];
      if (!h) return '';
      if (typeof h === 'function') return h(m);
      return new Template(Selector.xpath.pseudos[m[1]]).evaluate(m);
    },
    operators: {
      '=':  "[@#{1}='#{3}']",
      '!=': "[@#{1}!='#{3}']",
      '^=': "[starts-with(@#{1}, '#{3}')]",
      '$=': "[substring(@#{1}, (string-length(@#{1}) - string-length('#{3}') + 1))='#{3}']",
      '*=': "[contains(@#{1}, '#{3}')]",
      '~=': "[contains(concat(' ', @#{1}, ' '), ' #{3} ')]",
      '|=': "[contains(concat('-', @#{1}, '-'), '-#{3}-')]"
    },
    pseudos: {
      'first-child': '[not(preceding-sibling::*)]',
      'last-child':  '[not(following-sibling::*)]',
      'only-child':  '[not(preceding-sibling::* or following-sibling::*)]',
      'empty':       "[count(*) = 0 and (count(text()) = 0 or translate(text(), ' \t\r\n', '') = '')]",
      'checked':     "[@checked]",
      'disabled':    "[@disabled]",
      'enabled':     "[not(@disabled)]",
      'not': function(m) {
        var e = m[6], p = Selector.patterns,
            x = Selector.xpath, le, m, v;

        var exclusion = [];
        while (e && le != e && (/\S/).test(e)) {
          le = e;
          for (var i in p) {
            if (m = e.match(p[i])) {
              v = typeof x[i] == 'function' ? x[i](m) : new Template(x[i]).evaluate(m);
              exclusion.push("(" + v.substring(1, v.length - 1) + ")");
              e = e.replace(m[0], '');
              break;
            }
          }
        }
        return "[not(" + exclusion.join(" and ") + ")]";
      },
      'nth-child':      function(m) {
        return Selector.xpath.pseudos.nth("(count(./preceding-sibling::*) + 1) ", m);
      },
      'nth-last-child': function(m) {
        return Selector.xpath.pseudos.nth("(count(./following-sibling::*) + 1) ", m);
      },
      'nth-of-type':    function(m) {
        return Selector.xpath.pseudos.nth("position() ", m);
      },
      'nth-last-of-type': function(m) {
        return Selector.xpath.pseudos.nth("(last() + 1 - position()) ", m);
      },
      'first-of-type':  function(m) {
        m[6] = "1"; return Selector.xpath.pseudos['nth-of-type'](m);
      },
      'last-of-type':   function(m) {
        m[6] = "1"; return Selector.xpath.pseudos['nth-last-of-type'](m);
      },
      'only-of-type':   function(m) {
        var p = Selector.xpath.pseudos; return p['first-of-type'](m) + p['last-of-type'](m);
      },
      nth: function(fragment, m) {
        var mm, formula = m[6], predicate;
        if (formula == 'even') formula = '2n+0';
        if (formula == 'odd')  formula = '2n+1';
        if (mm = formula.match(/^(\d+)$/)) // digit only
          return '[' + fragment + "= " + mm[1] + ']';
        if (mm = formula.match(/^(-?\d*)?n(([+-])(\d+))?/)) { // an+b
          if (mm[1] == "-") mm[1] = -1;
          var a = mm[1] ? Number(mm[1]) : 1;
          var b = mm[2] ? Number(mm[2]) : 0;
          predicate = "[((#{fragment} - #{b}) mod #{a} = 0) and " +
          "((#{fragment} - #{b}) div #{a} >= 0)]";
          return new Template(predicate).evaluate({
            fragment: fragment, a: a, b: b });
        }
      }
    }
  },

  criteria: {
    tagName:      'n = h.tagName(n, r, "#{1}", c);   c = false;',
    className:    'n = h.className(n, r, "#{1}", c); c = false;',
    id:           'n = h.id(n, r, "#{1}", c);        c = false;',
    attrPresence: 'n = h.attrPresence(n, r, "#{1}"); c = false;',
    attr: function(m) {
      m[3] = (m[5] || m[6]);
      return new Template('n = h.attr(n, r, "#{1}", "#{3}", "#{2}"); c = false;').evaluate(m);
    },
    pseudo:       function(m) {
      if (m[6]) m[6] = m[6].replace(/"/g, '\\"');
      return new Template('n = h.pseudo(n, "#{1}", "#{6}", r, c); c = false;').evaluate(m);
    },
    descendant:   'c = "descendant";',
    child:        'c = "child";',
    adjacent:     'c = "adjacent";',
    laterSibling: 'c = "laterSibling";'
  },

  patterns: {
    // combinators must be listed first
    // (and descendant needs to be last combinator)
    laterSibling: /^\s*~\s*/,
    child:        /^\s*>\s*/,
    adjacent:     /^\s*\+\s*/,
    descendant:   /^\s/,

    // selectors follow
    tagName:      /^\s*(\*|[\w\-]+)(\b|$)?/,
    id:           /^#([\w\-\*]+)(\b|$)/,
    className:    /^\.([\w\-\*]+)(\b|$)/,
    pseudo:       /^:((first|last|nth|nth-last|only)(-child|-of-type)|empty|checked|(en|dis)abled|not)(\((.*?)\))?(\b|$|\s|(?=:))/,
    attrPresence: /^\[([\w]+)\]/,
    attr:         /\[((?:[\w-]*:)?[\w-]+)\s*(?:([!^$*~|]?=)\s*((['"])([^\]]*?)\4|([^'"][^\]]*?)))?\]/
  },

  handlers: {
    // UTILITY FUNCTIONS
    // joins two collections
    concat: function(a, b) {
      for (var i = 0, node; node = b[i]; i++)
        a.push(node);
      return a;
    },

    // marks an array of nodes for counting
    mark: function(nodes) {
      for (var i = 0, node; node = nodes[i]; i++)
        node._counted = true;
      return nodes;
    },

    unmark: function(nodes) {
      for (var i = 0, node; node = nodes[i]; i++)
        node._counted = undefined;
      return nodes;
    },

    // mark each child node with its position (for nth calls)
    // "ofType" flag indicates whether we're indexing for nth-of-type
    // rather than nth-child
    index: function(parentNode, reverse, ofType) {
      parentNode._counted = true;
      if (reverse) {
        for (var nodes = parentNode.childNodes, i = nodes.length - 1, j = 1; i >= 0; i--) {
          node = nodes[i];
          if (node.nodeType == 1 && (!ofType || node._counted)) node.nodeIndex = j++;
        }
      } else {
        for (var i = 0, j = 1, nodes = parentNode.childNodes; node = nodes[i]; i++)
          if (node.nodeType == 1 && (!ofType || node._counted)) node.nodeIndex = j++;
      }
    },

    // filters out duplicates and extends all nodes
    unique: function(nodes) {
      if (nodes.length == 0) return nodes;
      var results = [], n;
      for (var i = 0, l = nodes.length; i < l; i++)
        if (!(n = nodes[i])._counted) {
          n._counted = true;
          results.push(Element.extend(n));
        }
      return Selector.handlers.unmark(results);
    },

    // COMBINATOR FUNCTIONS
    descendant: function(nodes) {
      var h = Selector.handlers;
      for (var i = 0, results = [], node; node = nodes[i]; i++)
        h.concat(results, node.getElementsByTagName('*'));
      return results;
    },

    child: function(nodes) {
      var h = Selector.handlers;
      for (var i = 0, results = [], node; node = nodes[i]; i++) {
        for (var j = 0, children = [], child; child = node.childNodes[j]; j++)
          if (child.nodeType == 1 && child.tagName != '!') results.push(child);
      }
      return results;
    },

    adjacent: function(nodes) {
      for (var i = 0, results = [], node; node = nodes[i]; i++) {
        var next = this.nextElementSibling(node);
        if (next) results.push(next);
      }
      return results;
    },

    laterSibling: function(nodes) {
      var h = Selector.handlers;
      for (var i = 0, results = [], node; node = nodes[i]; i++)
        h.concat(results, Element.nextSiblings(node));
      return results;
    },

    nextElementSibling: function(node) {
      while (node = node.nextSibling)
	      if (node.nodeType == 1) return node;
      return null;
    },

    previousElementSibling: function(node) {
      while (node = node.previousSibling)
        if (node.nodeType == 1) return node;
      return null;
    },

    // TOKEN FUNCTIONS
    tagName: function(nodes, root, tagName, combinator) {
      tagName = tagName.toUpperCase();
      var results = [], h = Selector.handlers;
      if (nodes) {
        if (combinator) {
          // fastlane for ordinary descendant combinators
          if (combinator == "descendant") {
            for (var i = 0, node; node = nodes[i]; i++)
              h.concat(results, node.getElementsByTagName(tagName));
            return results;
          } else nodes = this[combinator](nodes);
          if (tagName == "*") return nodes;
        }
        for (var i = 0, node; node = nodes[i]; i++)
          if (node.tagName.toUpperCase() == tagName) results.push(node);
        return results;
      } else return root.getElementsByTagName(tagName);
    },

    id: function(nodes, root, id, combinator) {
      var targetNode = $(id), h = Selector.handlers;
      if (!nodes && root == document) return targetNode ? [targetNode] : [];
      if (nodes) {
        if (combinator) {
          if (combinator == 'child') {
            for (var i = 0, node; node = nodes[i]; i++)
              if (targetNode.parentNode == node) return [targetNode];
          } else if (combinator == 'descendant') {
            for (var i = 0, node; node = nodes[i]; i++)
              if (Element.descendantOf(targetNode, node)) return [targetNode];
          } else if (combinator == 'adjacent') {
            for (var i = 0, node; node = nodes[i]; i++)
              if (Selector.handlers.previousElementSibling(targetNode) == node)
                return [targetNode];
          } else nodes = h[combinator](nodes);
        }
        for (var i = 0, node; node = nodes[i]; i++)
          if (node == targetNode) return [targetNode];
        return [];
      }
      return (targetNode && Element.descendantOf(targetNode, root)) ? [targetNode] : [];
    },

    className: function(nodes, root, className, combinator) {
      if (nodes && combinator) nodes = this[combinator](nodes);
      return Selector.handlers.byClassName(nodes, root, className);
    },

    byClassName: function(nodes, root, className) {
      if (!nodes) nodes = Selector.handlers.descendant([root]);
      var needle = ' ' + className + ' ';
      for (var i = 0, results = [], node, nodeClassName; node = nodes[i]; i++) {
        nodeClassName = node.className;
        if (nodeClassName.length == 0) continue;
        if (nodeClassName == className || (' ' + nodeClassName + ' ').include(needle))
          results.push(node);
      }
      return results;
    },

    attrPresence: function(nodes, root, attr) {
      var results = [];
      for (var i = 0, node; node = nodes[i]; i++)
        if (Element.hasAttribute(node, attr)) results.push(node);
      return results;
    },

    attr: function(nodes, root, attr, value, operator) {
      if (!nodes) nodes = root.getElementsByTagName("*");
      var handler = Selector.operators[operator], results = [];
      for (var i = 0, node; node = nodes[i]; i++) {
        var nodeValue = Element.readAttribute(node, attr);
        if (nodeValue === null) continue;
        if (handler(nodeValue, value)) results.push(node);
      }
      return results;
    },

    pseudo: function(nodes, name, value, root, combinator) {
      if (nodes && combinator) nodes = this[combinator](nodes);
      if (!nodes) nodes = root.getElementsByTagName("*");
      return Selector.pseudos[name](nodes, value, root);
    }
  },

  pseudos: {
    'first-child': function(nodes, value, root) {
      for (var i = 0, results = [], node; node = nodes[i]; i++) {
        if (Selector.handlers.previousElementSibling(node)) continue;
          results.push(node);
      }
      return results;
    },
    'last-child': function(nodes, value, root) {
      for (var i = 0, results = [], node; node = nodes[i]; i++) {
        if (Selector.handlers.nextElementSibling(node)) continue;
          results.push(node);
      }
      return results;
    },
    'only-child': function(nodes, value, root) {
      var h = Selector.handlers;
      for (var i = 0, results = [], node; node = nodes[i]; i++)
        if (!h.previousElementSibling(node) && !h.nextElementSibling(node))
          results.push(node);
      return results;
    },
    'nth-child':        function(nodes, formula, root) {
      return Selector.pseudos.nth(nodes, formula, root);
    },
    'nth-last-child':   function(nodes, formula, root) {
      return Selector.pseudos.nth(nodes, formula, root, true);
    },
    'nth-of-type':      function(nodes, formula, root) {
      return Selector.pseudos.nth(nodes, formula, root, false, true);
    },
    'nth-last-of-type': function(nodes, formula, root) {
      return Selector.pseudos.nth(nodes, formula, root, true, true);
    },
    'first-of-type':    function(nodes, formula, root) {
      return Selector.pseudos.nth(nodes, "1", root, false, true);
    },
    'last-of-type':     function(nodes, formula, root) {
      return Selector.pseudos.nth(nodes, "1", root, true, true);
    },
    'only-of-type':     function(nodes, formula, root) {
      var p = Selector.pseudos;
      return p['last-of-type'](p['first-of-type'](nodes, formula, root), formula, root);
    },

    // handles the an+b logic
    getIndices: function(a, b, total) {
      if (a == 0) return b > 0 ? [b] : [];
      return $R(1, total).inject([], function(memo, i) {
        if (0 == (i - b) % a && (i - b) / a >= 0) memo.push(i);
        return memo;
      });
    },

    // handles nth(-last)-child, nth(-last)-of-type, and (first|last)-of-type
    nth: function(nodes, formula, root, reverse, ofType) {
      if (nodes.length == 0) return [];
      if (formula == 'even') formula = '2n+0';
      if (formula == 'odd')  formula = '2n+1';
      var h = Selector.handlers, results = [], indexed = [], m;
      h.mark(nodes);
      for (var i = 0, node; node = nodes[i]; i++) {
        if (!node.parentNode._counted) {
          h.index(node.parentNode, reverse, ofType);
          indexed.push(node.parentNode);
        }
      }
      if (formula.match(/^\d+$/)) { // just a number
        formula = Number(formula);
        for (var i = 0, node; node = nodes[i]; i++)
          if (node.nodeIndex == formula) results.push(node);
      } else if (m = formula.match(/^(-?\d*)?n(([+-])(\d+))?/)) { // an+b
        if (m[1] == "-") m[1] = -1;
        var a = m[1] ? Number(m[1]) : 1;
        var b = m[2] ? Number(m[2]) : 0;
        var indices = Selector.pseudos.getIndices(a, b, nodes.length);
        for (var i = 0, node, l = indices.length; node = nodes[i]; i++) {
          for (var j = 0; j < l; j++)
            if (node.nodeIndex == indices[j]) results.push(node);
        }
      }
      h.unmark(nodes);
      h.unmark(indexed);
      return results;
    },

    'empty': function(nodes, value, root) {
      for (var i = 0, results = [], node; node = nodes[i]; i++) {
        // IE treats comments as element nodes
        if (node.tagName == '!' || (node.firstChild && !node.innerHTML.match(/^\s*$/))) continue;
        results.push(node);
      }
      return results;
    },

    'not': function(nodes, selector, root) {
      var h = Selector.handlers, selectorType, m;
      var exclusions = new Selector(selector).findElements(root);
      h.mark(exclusions);
      for (var i = 0, results = [], node; node = nodes[i]; i++)
        if (!node._counted) results.push(node);
      h.unmark(exclusions);
      return results;
    },

    'enabled': function(nodes, value, root) {
      for (var i = 0, results = [], node; node = nodes[i]; i++)
        if (!node.disabled) results.push(node);
      return results;
    },

    'disabled': function(nodes, value, root) {
      for (var i = 0, results = [], node; node = nodes[i]; i++)
        if (node.disabled) results.push(node);
      return results;
    },

    'checked': function(nodes, value, root) {
      for (var i = 0, results = [], node; node = nodes[i]; i++)
        if (node.checked) results.push(node);
      return results;
    }
  },

  operators: {
    '=':  function(nv, v) { return nv == v; },
    '!=': function(nv, v) { return nv != v; },
    '^=': function(nv, v) { return nv.startsWith(v); },
    '$=': function(nv, v) { return nv.endsWith(v); },
    '*=': function(nv, v) { return nv.include(v); },
    '~=': function(nv, v) { return (' ' + nv + ' ').include(' ' + v + ' '); },
    '|=': function(nv, v) { return ('-' + nv.toUpperCase() + '-').include('-' + v.toUpperCase() + '-'); }
  },

  matchElements: function(elements, expression) {
    var matches = new Selector(expression).findElements(), h = Selector.handlers;
    h.mark(matches);
    for (var i = 0, results = [], element; element = elements[i]; i++)
      if (element._counted) results.push(element);
    h.unmark(matches);
    return results;
  },

  findElement: function(elements, expression, index) {
    if (typeof expression == 'number') {
      index = expression; expression = false;
    }
    return Selector.matchElements(elements, expression || '*')[index || 0];
  },

  findChildElements: function(element, expressions) {
    var exprs = expressions.join(','), expressions = [];
    exprs.scan(/(([\w#:.~>+()\s-]+|\*|\[.*?\])+)\s*(,|$)/, function(m) {
      expressions.push(m[1].strip());
    });
    var results = [], h = Selector.handlers;
    for (var i = 0, l = expressions.length, selector; i < l; i++) {
      selector = new Selector(expressions[i].strip());
      h.concat(results, selector.findElements(element));
    }
    return (l > 1) ? h.unique(results) : results;
  }
});

function $$() {
  return Selector.findChildElements(document, $A(arguments));
}
var Form = {
  reset: function(form) {
    $(form).reset();
    return form;
  },

  serializeElements: function(elements, getHash) {
    var data = elements.inject({}, function(result, element) {
      if (!element.disabled && element.name) {
        var key = element.name, value = $(element).getValue();
        if (value != null) {
         	if (key in result) {
            if (result[key].constructor != Array) result[key] = [result[key]];
            result[key].push(value);
          }
          else result[key] = value;
        }
      }
      return result;
    });

    return getHash ? data : Hash.toQueryString(data);
  }
};

Form.Methods = {
  serialize: function(form, getHash) {
    return Form.serializeElements(Form.getElements(form), getHash);
  },

  getElements: function(form) {
    return $A($(form).getElementsByTagName('*')).inject([],
      function(elements, child) {
        if (Form.Element.Serializers[child.tagName.toLowerCase()])
          elements.push(Element.extend(child));
        return elements;
      }
    );
  },

  getInputs: function(form, typeName, name) {
    form = $(form);
    var inputs = form.getElementsByTagName('input');

    if (!typeName && !name) return $A(inputs).map(Element.extend);

    for (var i = 0, matchingInputs = [], length = inputs.length; i < length; i++) {
      var input = inputs[i];
      if ((typeName && input.type != typeName) || (name && input.name != name))
        continue;
      matchingInputs.push(Element.extend(input));
    }

    return matchingInputs;
  },

  disable: function(form) {
    form = $(form);
    Form.getElements(form).invoke('disable');
    return form;
  },

  enable: function(form) {
    form = $(form);
    Form.getElements(form).invoke('enable');
    return form;
  },

  findFirstElement: function(form) {
    return $(form).getElements().find(function(element) {
      return element.type != 'hidden' && !element.disabled &&
        ['input', 'select', 'textarea'].include(element.tagName.toLowerCase());
    });
  },

  focusFirstElement: function(form) {
    form = $(form);
    form.findFirstElement().activate();
    return form;
  },

  request: function(form, options) {
    form = $(form), options = Object.clone(options || {});

    var params = options.parameters;
    options.parameters = form.serialize(true);

    if (params) {
      if (typeof params == 'string') params = params.toQueryParams();
      Object.extend(options.parameters, params);
    }

    if (form.hasAttribute('method') && !options.method)
      options.method = form.method;

    return new Ajax.Request(form.readAttribute('action'), options);
  }
}

/*--------------------------------------------------------------------------*/

Form.Element = {
  focus: function(element) {
    $(element).focus();
    return element;
  },

  select: function(element) {
    $(element).select();
    return element;
  }
}

Form.Element.Methods = {
  serialize: function(element) {
    element = $(element);
    if (!element.disabled && element.name) {
      var value = element.getValue();
      if (value != undefined) {
        var pair = {};
        pair[element.name] = value;
        return Hash.toQueryString(pair);
      }
    }
    return '';
  },

  getValue: function(element) {
    element = $(element);
    var method = element.tagName.toLowerCase();
    return Form.Element.Serializers[method](element);
  },

  clear: function(element) {
    $(element).value = '';
    return element;
  },

  present: function(element) {
    return $(element).value != '';
  },

  activate: function(element) {
    element = $(element);
    try {
      element.focus();
      if (element.select && (element.tagName.toLowerCase() != 'input' ||
        !['button', 'reset', 'submit'].include(element.type)))
        element.select();
    } catch (e) {}
    return element;
  },

  disable: function(element) {
    element = $(element);
    element.blur();
    element.disabled = true;
    return element;
  },

  enable: function(element) {
    element = $(element);
    element.disabled = false;
    return element;
  }
}

/*--------------------------------------------------------------------------*/

var Field = Form.Element;
var $F = Form.Element.Methods.getValue;

/*--------------------------------------------------------------------------*/

Form.Element.Serializers = {
  input: function(element) {
    switch (element.type.toLowerCase()) {
      case 'checkbox':
      case 'radio':
        return Form.Element.Serializers.inputSelector(element);
      default:
        return Form.Element.Serializers.textarea(element);
    }
  },

  inputSelector: function(element) {
    return element.checked ? element.value : null;
  },

  textarea: function(element) {
    return element.value;
  },

  select: function(element) {
    return this[element.type == 'select-one' ?
      'selectOne' : 'selectMany'](element);
  },

  selectOne: function(element) {
    var index = element.selectedIndex;
    return index >= 0 ? this.optionValue(element.options[index]) : null;
  },

  selectMany: function(element) {
    var values, length = element.length;
    if (!length) return null;

    for (var i = 0, values = []; i < length; i++) {
      var opt = element.options[i];
      if (opt.selected) values.push(this.optionValue(opt));
    }
    return values;
  },

  optionValue: function(opt) {
    // extend element because hasAttribute may not be native
    return Element.extend(opt).hasAttribute('value') ? opt.value : opt.text;
  }
}

/*--------------------------------------------------------------------------*/

Abstract.TimedObserver = function() {}
Abstract.TimedObserver.prototype = {
  initialize: function(element, frequency, callback) {
    this.frequency = frequency;
    this.element   = $(element);
    this.callback  = callback;

    this.lastValue = this.getValue();
    this.registerCallback();
  },

  registerCallback: function() {
    setInterval(this.onTimerEvent.bind(this), this.frequency * 1000);
  },

  onTimerEvent: function() {
    var value = this.getValue();
    var changed = ('string' == typeof this.lastValue && 'string' == typeof value
      ? this.lastValue != value : String(this.lastValue) != String(value));
    if (changed) {
      this.callback(this.element, value);
      this.lastValue = value;
    }
  }
}

Form.Element.Observer = Class.create();
Form.Element.Observer.prototype = Object.extend(new Abstract.TimedObserver(), {
  getValue: function() {
    return Form.Element.getValue(this.element);
  }
});

Form.Observer = Class.create();
Form.Observer.prototype = Object.extend(new Abstract.TimedObserver(), {
  getValue: function() {
    return Form.serialize(this.element);
  }
});

/*--------------------------------------------------------------------------*/

Abstract.EventObserver = function() {}
Abstract.EventObserver.prototype = {
  initialize: function(element, callback) {
    this.element  = $(element);
    this.callback = callback;

    this.lastValue = this.getValue();
    if (this.element.tagName.toLowerCase() == 'form')
      this.registerFormCallbacks();
    else
      this.registerCallback(this.element);
  },

  onElementEvent: function() {
    var value = this.getValue();
    if (this.lastValue != value) {
      this.callback(this.element, value);
      this.lastValue = value;
    }
  },

  registerFormCallbacks: function() {
    Form.getElements(this.element).each(this.registerCallback.bind(this));
  },

  registerCallback: function(element) {
    if (element.type) {
      switch (element.type.toLowerCase()) {
        case 'checkbox':
        case 'radio':
          Event.observe(element, 'click', this.onElementEvent.bind(this));
          break;
        default:
          Event.observe(element, 'change', this.onElementEvent.bind(this));
          break;
      }
    }
  }
}

Form.Element.EventObserver = Class.create();
Form.Element.EventObserver.prototype = Object.extend(new Abstract.EventObserver(), {
  getValue: function() {
    return Form.Element.getValue(this.element);
  }
});

Form.EventObserver = Class.create();
Form.EventObserver.prototype = Object.extend(new Abstract.EventObserver(), {
  getValue: function() {
    return Form.serialize(this.element);
  }
});
if (!window.Event) {
  var Event = new Object();
}

Object.extend(Event, {
  KEY_BACKSPACE: 8,
  KEY_TAB:       9,
  KEY_RETURN:   13,
  KEY_ESC:      27,
  KEY_LEFT:     37,
  KEY_UP:       38,
  KEY_RIGHT:    39,
  KEY_DOWN:     40,
  KEY_DELETE:   46,
  KEY_HOME:     36,
  KEY_END:      35,
  KEY_PAGEUP:   33,
  KEY_PAGEDOWN: 34,

  element: function(event) {
    return $(event.target || event.srcElement);
  },

  isLeftClick: function(event) {
    return (((event.which) && (event.which == 1)) ||
            ((event.button) && (event.button == 1)));
  },

  pointerX: function(event) {
    return event.pageX || (event.clientX +
      (document.documentElement.scrollLeft || document.body.scrollLeft));
  },

  pointerY: function(event) {
    return event.pageY || (event.clientY +
      (document.documentElement.scrollTop || document.body.scrollTop));
  },

  stop: function(event) {
    if (event.preventDefault) {
      event.preventDefault();
      event.stopPropagation();
    } else {
      event.returnValue = false;
      event.cancelBubble = true;
    }
  },

  // find the first node with the given tagName, starting from the
  // node the event was triggered on; traverses the DOM upwards
  findElement: function(event, tagName) {
    var element = Event.element(event);
    while (element.parentNode && (!element.tagName ||
        (element.tagName.toUpperCase() != tagName.toUpperCase())))
      element = element.parentNode;
    return element;
  },

  observers: false,

  _observeAndCache: function(element, name, observer, useCapture) {
    if (!this.observers) this.observers = [];
    if (element.addEventListener) {
      this.observers.push([element, name, observer, useCapture]);
      element.addEventListener(name, observer, useCapture);
    } else if (element.attachEvent) {
      this.observers.push([element, name, observer, useCapture]);
      element.attachEvent('on' + name, observer);
    }
  },

  unloadCache: function() {
    if (!Event.observers) return;
    for (var i = 0, length = Event.observers.length; i < length; i++) {
      Event.stopObserving.apply(this, Event.observers[i]);
      Event.observers[i][0] = null;
    }
    Event.observers = false;
  },

  observe: function(element, name, observer, useCapture) {
    element = $(element);
    useCapture = useCapture || false;

    if (name == 'keypress' &&
      (Prototype.Browser.WebKit || element.attachEvent))
      name = 'keydown';

    Event._observeAndCache(element, name, observer, useCapture);
  },

  stopObserving: function(element, name, observer, useCapture) {
    element = $(element);
    useCapture = useCapture || false;

    if (name == 'keypress' &&
        (Prototype.Browser.WebKit || element.attachEvent))
      name = 'keydown';

    if (element.removeEventListener) {
      element.removeEventListener(name, observer, useCapture);
    } else if (element.detachEvent) {
      try {
        element.detachEvent('on' + name, observer);
      } catch (e) {}
    }
  }
});

/* prevent memory leaks in IE */
if (Prototype.Browser.IE)
  Event.observe(window, 'unload', Event.unloadCache, false);
var Position = {
  // set to true if needed, warning: firefox performance problems
  // NOT neeeded for page scrolling, only if draggable contained in
  // scrollable elements
  includeScrollOffsets: false,

  // must be called before calling withinIncludingScrolloffset, every time the
  // page is scrolled
  prepare: function() {
    this.deltaX =  window.pageXOffset
                || document.documentElement.scrollLeft
                || document.body.scrollLeft
                || 0;
    this.deltaY =  window.pageYOffset
                || document.documentElement.scrollTop
                || document.body.scrollTop
                || 0;
  },

  realOffset: function(element) {
    var valueT = 0, valueL = 0;
    do {
      valueT += element.scrollTop  || 0;
      valueL += element.scrollLeft || 0;
      element = element.parentNode;
    } while (element);
    return [valueL, valueT];
  },

  cumulativeOffset: function(element) {
    var valueT = 0, valueL = 0;
    do {
      valueT += element.offsetTop  || 0;
      valueL += element.offsetLeft || 0;
      element = element.offsetParent;
    } while (element);
    return [valueL, valueT];
  },

  positionedOffset: function(element) {
    var valueT = 0, valueL = 0;
    do {
      valueT += element.offsetTop  || 0;
      valueL += element.offsetLeft || 0;
      element = element.offsetParent;
      if (element) {
        if(element.tagName=='BODY') break;
        var p = Element.getStyle(element, 'position');
        if (p == 'relative' || p == 'absolute') break;
      }
    } while (element);
    return [valueL, valueT];
  },

  offsetParent: function(element) {
    if (element.offsetParent) return element.offsetParent;
    if (element == document.body) return element;

    while ((element = element.parentNode) && element != document.body)
      if (Element.getStyle(element, 'position') != 'static')
        return element;

    return document.body;
  },

  // caches x/y coordinate pair to use with overlap
  within: function(element, x, y) {
    if (this.includeScrollOffsets)
      return this.withinIncludingScrolloffsets(element, x, y);
    this.xcomp = x;
    this.ycomp = y;
    this.offset = this.cumulativeOffset(element);

    return (y >= this.offset[1] &&
            y <  this.offset[1] + element.offsetHeight &&
            x >= this.offset[0] &&
            x <  this.offset[0] + element.offsetWidth);
  },

  withinIncludingScrolloffsets: function(element, x, y) {
    var offsetcache = this.realOffset(element);

    this.xcomp = x + offsetcache[0] - this.deltaX;
    this.ycomp = y + offsetcache[1] - this.deltaY;
    this.offset = this.cumulativeOffset(element);

    return (this.ycomp >= this.offset[1] &&
            this.ycomp <  this.offset[1] + element.offsetHeight &&
            this.xcomp >= this.offset[0] &&
            this.xcomp <  this.offset[0] + element.offsetWidth);
  },

  // within must be called directly before
  overlap: function(mode, element) {
    if (!mode) return 0;
    if (mode == 'vertical')
      return ((this.offset[1] + element.offsetHeight) - this.ycomp) /
        element.offsetHeight;
    if (mode == 'horizontal')
      return ((this.offset[0] + element.offsetWidth) - this.xcomp) /
        element.offsetWidth;
  },

  page: function(forElement) {
    var valueT = 0, valueL = 0;

    var element = forElement;
    do {
      valueT += element.offsetTop  || 0;
      valueL += element.offsetLeft || 0;

      // Safari fix
      if (element.offsetParent == document.body)
        if (Element.getStyle(element,'position')=='absolute') break;

    } while (element = element.offsetParent);

    element = forElement;
    do {
      if (!window.opera || element.tagName=='BODY') {
        valueT -= element.scrollTop  || 0;
        valueL -= element.scrollLeft || 0;
      }
    } while (element = element.parentNode);

    return [valueL, valueT];
  },

  clone: function(source, target) {
    var options = Object.extend({
      setLeft:    true,
      setTop:     true,
      setWidth:   true,
      setHeight:  true,
      offsetTop:  0,
      offsetLeft: 0
    }, arguments[2] || {})

    // find page position of source
    source = $(source);
    var p = Position.page(source);

    // find coordinate system to use
    target = $(target);
    var delta = [0, 0];
    var parent = null;
    // delta [0,0] will do fine with position: fixed elements,
    // position:absolute needs offsetParent deltas
    if (Element.getStyle(target,'position') == 'absolute') {
      parent = Position.offsetParent(target);
      delta = Position.page(parent);
    }

    // correct by body offsets (fixes Safari)
    if (parent == document.body) {
      delta[0] -= document.body.offsetLeft;
      delta[1] -= document.body.offsetTop;
    }

    // set position
    if(options.setLeft)   target.style.left  = (p[0] - delta[0] + options.offsetLeft) + 'px';
    if(options.setTop)    target.style.top   = (p[1] - delta[1] + options.offsetTop) + 'px';
    if(options.setWidth)  target.style.width = source.offsetWidth + 'px';
    if(options.setHeight) target.style.height = source.offsetHeight + 'px';
  },

  absolutize: function(element) {
    element = $(element);
    if (element.style.position == 'absolute') return;
    Position.prepare();

    var offsets = Position.positionedOffset(element);
    var top     = offsets[1];
    var left    = offsets[0];
    var width   = element.clientWidth;
    var height  = element.clientHeight;

    element._originalLeft   = left - parseFloat(element.style.left  || 0);
    element._originalTop    = top  - parseFloat(element.style.top || 0);
    element._originalWidth  = element.style.width;
    element._originalHeight = element.style.height;

    element.style.position = 'absolute';
    element.style.top    = top + 'px';
    element.style.left   = left + 'px';
    element.style.width  = width + 'px';
    element.style.height = height + 'px';
  },

  relativize: function(element) {
    element = $(element);
    if (element.style.position == 'relative') return;
    Position.prepare();

    element.style.position = 'relative';
    var top  = parseFloat(element.style.top  || 0) - (element._originalTop || 0);
    var left = parseFloat(element.style.left || 0) - (element._originalLeft || 0);

    element.style.top    = top + 'px';
    element.style.left   = left + 'px';
    element.style.height = element._originalHeight;
    element.style.width  = element._originalWidth;
  }
}

// Safari returns margins on body which is incorrect if the child is absolutely
// positioned.  For performance reasons, redefine Position.cumulativeOffset for
// KHTML/WebKit only.
if (Prototype.Browser.WebKit) {
  Position.cumulativeOffset = function(element) {
    var valueT = 0, valueL = 0;
    do {
      valueT += element.offsetTop  || 0;
      valueL += element.offsetLeft || 0;
      if (element.offsetParent == document.body)
        if (Element.getStyle(element, 'position') == 'absolute') break;

      element = element.offsetParent;
    } while (element);

    return [valueL, valueT];
  }
}

Element.addMethods();


/**
 * ====================================================================
 * About
 * ====================================================================
 * Sarissa is an ECMAScript library acting as a cross-browser wrapper for native XML APIs.
 * The library supports Gecko based browsers like Mozilla and Firefox,
 * Internet Explorer (5.5+ with MSXML3.0+), Konqueror, Safari and a little of Opera
 * @version ${project.version}
 * @author: Manos Batsis, mailto: mbatsis at users full stop sourceforge full stop net
 * ====================================================================
 * Licence
 * ====================================================================
 * Sarissa is free software distributed under the GNU GPL version 2 (see <a href="gpl.txt">gpl.txt</a>) or higher, 
 * GNU LGPL version 2.1 (see <a href="lgpl.txt">lgpl.txt</a>) or higher and Apache Software License 2.0 or higher 
 * (see <a href="asl.txt">asl.txt</a>). This means you can choose one of the three and use that if you like. If 
 * you make modifications under the ASL, i would appreciate it if you submitted those.
 * In case your copy of Sarissa does not include the license texts, you may find
 * them online in various formats at <a href="http://www.gnu.org">http://www.gnu.org</a> and 
 * <a href="http://www.apache.org">http://www.apache.org</a>.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY 
 * KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE 
 * WARRANTIES OF MERCHANTABILITY,FITNESS FOR A PARTICULAR PURPOSE 
 * AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR 
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR 
 * OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE 
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
/**
 * <p>Sarissa is a utility class. Provides "static" methods for DOMDocument, 
 * DOM Node serialization to XML strings and other utility goodies.</p>
 * @constructor
 */
function Sarissa(){};
Sarissa.VERSION = "${project.version}";
Sarissa.PARSED_OK = "Document contains no parsing errors";
Sarissa.PARSED_EMPTY = "Document is empty";
Sarissa.PARSED_UNKNOWN_ERROR = "Not well-formed or other error";
Sarissa.IS_ENABLED_TRANSFORM_NODE = false;
var _sarissa_iNsCounter = 0;
var _SARISSA_IEPREFIX4XSLPARAM = "";
var _SARISSA_HAS_DOM_IMPLEMENTATION = document.implementation && true;
var _SARISSA_HAS_DOM_CREATE_DOCUMENT = _SARISSA_HAS_DOM_IMPLEMENTATION && document.implementation.createDocument;
var _SARISSA_HAS_DOM_FEATURE = _SARISSA_HAS_DOM_IMPLEMENTATION && document.implementation.hasFeature;
var _SARISSA_IS_MOZ = _SARISSA_HAS_DOM_CREATE_DOCUMENT && _SARISSA_HAS_DOM_FEATURE;
var _SARISSA_IS_SAFARI = (navigator.userAgent && navigator.vendor && (navigator.userAgent.toLowerCase().indexOf("applewebkit") != -1 || navigator.vendor.indexOf("Apple") != -1));
var _SARISSA_IS_IE = document.all && window.ActiveXObject && navigator.userAgent.toLowerCase().indexOf("msie") > -1  && navigator.userAgent.toLowerCase().indexOf("opera") == -1;
if(!window.Node || !Node.ELEMENT_NODE){
    Node = {ELEMENT_NODE: 1, ATTRIBUTE_NODE: 2, TEXT_NODE: 3, CDATA_SECTION_NODE: 4, ENTITY_REFERENCE_NODE: 5,  ENTITY_NODE: 6, PROCESSING_INSTRUCTION_NODE: 7, COMMENT_NODE: 8, DOCUMENT_NODE: 9, DOCUMENT_TYPE_NODE: 10, DOCUMENT_FRAGMENT_NODE: 11, NOTATION_NODE: 12};
};

if(typeof XMLDocument == "undefined" && typeof Document !="undefined"){ XMLDocument = Document; } 

// IE initialization
if(_SARISSA_IS_IE){
    // for XSLT parameter names, prefix needed by IE
    _SARISSA_IEPREFIX4XSLPARAM = "xsl:";
    // used to store the most recent ProgID available out of the above
    var _SARISSA_DOM_PROGID = "";
    var _SARISSA_XMLHTTP_PROGID = "";
    var _SARISSA_DOM_XMLWRITER = "";
    /**
     * Called when the Sarissa_xx.js file is parsed, to pick most recent
     * ProgIDs for IE, then gets destroyed.
     * @private
     * @param idList an array of MSXML PROGIDs from which the most recent will be picked for a given object
     * @param enabledList an array of arrays where each array has two items; the index of the PROGID for which a certain feature is enabled
     */
    Sarissa.pickRecentProgID = function (idList){
        // found progID flag
        var bFound = false;
        for(var i=0; i < idList.length && !bFound; i++){
            try{
                var oDoc = new ActiveXObject(idList[i]);
                o2Store = idList[i];
                bFound = true;
            }catch (objException){
                // trap; try next progID
            };
        };
        if (!bFound) {
            throw "Could not retreive a valid progID of Class: " + idList[idList.length-1]+". (original exception: "+e+")";
        };
        idList = null;
        return o2Store;
    };
    // pick best available MSXML progIDs
    _SARISSA_DOM_PROGID = null;
    _SARISSA_THREADEDDOM_PROGID = null;
    _SARISSA_XSLTEMPLATE_PROGID = null;
    _SARISSA_XMLHTTP_PROGID = null;
    if(!window.XMLHttpRequest){
        /**
         * Emulate XMLHttpRequest
         * @constructor
         */
        XMLHttpRequest = function() {
            if(!_SARISSA_XMLHTTP_PROGID){
                _SARISSA_XMLHTTP_PROGID = Sarissa.pickRecentProgID(["MSXML2.XMLHTTP.3.0", "MSXML2.XMLHTTP", "Microsoft.XMLHTTP"]);
            };
            return new ActiveXObject(_SARISSA_XMLHTTP_PROGID);
        };
    };
    // we dont need this anymore
    //============================================
    // Factory methods (IE)
    //============================================
    // see non-IE version
    Sarissa.getDomDocument = function(sUri, sName){
        if(!_SARISSA_DOM_PROGID){
            _SARISSA_DOM_PROGID = Sarissa.pickRecentProgID(["Msxml2.DOMDocument.3.0", "MSXML2.DOMDocument", "MSXML.DOMDocument", "Microsoft.XMLDOM"]);
        };
        var oDoc = new ActiveXObject(_SARISSA_DOM_PROGID);
        // if a root tag name was provided, we need to load it in the DOM object
        if (sName){
            // create an artifical namespace prefix 
            // or reuse existing prefix if applicable
            var prefix = "";
            if(sUri){
                if(sName.indexOf(":") > 1){
                    prefix = sName.substring(0, sName.indexOf(":"));
                    sName = sName.substring(sName.indexOf(":")+1); 
                }else{
                    prefix = "a" + (_sarissa_iNsCounter++);
                };
            };
            // use namespaces if a namespace URI exists
            if(sUri){
                oDoc.loadXML('<' + prefix+':'+sName + " xmlns:" + prefix + "=\"" + sUri + "\"" + " />");
            } else {
                oDoc.loadXML('<' + sName + " />");
            };
        };
        return oDoc;
    };
    // see non-IE version   
    Sarissa.getParseErrorText = function (oDoc) {
        var parseErrorText = Sarissa.PARSED_OK;
        if(oDoc && oDoc.parseError && oDoc.parseError.errorCode && oDoc.parseError.errorCode != 0){
            parseErrorText = "XML Parsing Error: " + oDoc.parseError.reason + 
                "\nLocation: " + oDoc.parseError.url + 
                "\nLine Number " + oDoc.parseError.line + ", Column " + 
                oDoc.parseError.linepos + 
                ":\n" + oDoc.parseError.srcText +
                "\n";
            for(var i = 0;  i < oDoc.parseError.linepos;i++){
                parseErrorText += "-";
            };
            parseErrorText +=  "^\n";
        }
        else if(oDoc.documentElement == null){
            parseErrorText = Sarissa.PARSED_EMPTY;
        };
        return parseErrorText;
    };
    // see non-IE version
    Sarissa.setXpathNamespaces = function(oDoc, sNsSet) {
        oDoc.setProperty("SelectionLanguage", "XPath");
        oDoc.setProperty("SelectionNamespaces", sNsSet);
    };   
    /**
     * Basic implementation of Mozilla's XSLTProcessor for IE. 
     * Reuses the same XSLT stylesheet for multiple transforms
     * @constructor
     */
    XSLTProcessor = function(){
        if(!_SARISSA_XSLTEMPLATE_PROGID){
            _SARISSA_XSLTEMPLATE_PROGID = Sarissa.pickRecentProgID(["MSXML2.XSLTemplate.3.0"]);
        };
        this.template = new ActiveXObject(_SARISSA_XSLTEMPLATE_PROGID);
        this.processor = null;
    };
    /**
     * Imports the given XSLT DOM and compiles it to a reusable transform
     * <b>Note:</b> If the stylesheet was loaded from a URL and contains xsl:import or xsl:include elements,it will be reloaded to resolve those
     * @argument xslDoc The XSLT DOMDocument to import
     */
    XSLTProcessor.prototype.importStylesheet = function(xslDoc){
        if(!_SARISSA_THREADEDDOM_PROGID){
            _SARISSA_THREADEDDOM_PROGID = Sarissa.pickRecentProgID(["MSXML2.FreeThreadedDOMDocument.3.0"]);
        };
        xslDoc.setProperty("SelectionLanguage", "XPath");
        xslDoc.setProperty("SelectionNamespaces", "xmlns:xsl='http://www.w3.org/1999/XSL/Transform'");
        // convert stylesheet to free threaded
        var converted = new ActiveXObject(_SARISSA_THREADEDDOM_PROGID);
        // make included/imported stylesheets work if exist and xsl was originally loaded from url
        if(xslDoc.url && xslDoc.selectSingleNode("//xsl:*[local-name() = 'import' or local-name() = 'include']") != null){
            converted.async = false;
            if (_SARISSA_THREADEDDOM_PROGID == "MSXML2.FreeThreadedDOMDocument.6.0") { 
                converted.setProperty("AllowDocumentFunction", true); 
                converted.resolveExternals = true; 
            }
            converted.load(xslDoc.url);
        } else {
            converted.loadXML(xslDoc.xml);
        };
        converted.setProperty("SelectionNamespaces", "xmlns:xsl='http://www.w3.org/1999/XSL/Transform'");
        var output = converted.selectSingleNode("//xsl:output");
        this.outputMethod = output ? output.getAttribute("method") : "html";
        this.template.stylesheet = converted;
        this.processor = this.template.createProcessor();
        // for getParameter and clearParameters
        this.paramsSet = new Array();
    };

    /**
     * Transform the given XML DOM and return the transformation result as a new DOM document
     * @argument sourceDoc The XML DOMDocument to transform
     * @return The transformation result as a DOM Document
     */
    XSLTProcessor.prototype.transformToDocument = function(sourceDoc){
        // fix for bug 1549749
        if(_SARISSA_THREADEDDOM_PROGID){
            this.processor.input=sourceDoc;
            var outDoc=new ActiveXObject(_SARISSA_DOM_PROGID);
            this.processor.output=outDoc;
            this.processor.transform();
            return outDoc;
        }
        else{
            if(!_SARISSA_DOM_XMLWRITER){
                _SARISSA_DOM_XMLWRITER = Sarissa.pickRecentProgID(["Msxml2.MXXMLWriter.3.0", "MSXML2.MXXMLWriter", "MSXML.MXXMLWriter", "Microsoft.XMLDOM"]);
            };
            this.processor.input = sourceDoc;
            var outDoc = new ActiveXObject(_SARISSA_DOM_XMLWRITER);
            this.processor.output = outDoc; 
            this.processor.transform();
            var oDoc = new ActiveXObject(_SARISSA_DOM_PROGID);
            oDoc.loadXML(outDoc.output+"");
            return oDoc;
        };
    };
    
    /**
     * Transform the given XML DOM and return the transformation result as a new DOM fragment.
     * <b>Note</b>: The xsl:output method must match the nature of the owner document (XML/HTML).
     * @argument sourceDoc The XML DOMDocument to transform
     * @argument ownerDoc The owner of the result fragment
     * @return The transformation result as a DOM Document
     */
    XSLTProcessor.prototype.transformToFragment = function (sourceDoc, ownerDoc) {
        this.processor.input = sourceDoc;
        this.processor.transform();
        var s = this.processor.output;
        var f = ownerDoc.createDocumentFragment();
        if (this.outputMethod == 'text') {
            f.appendChild(ownerDoc.createTextNode(s));
        } else if (ownerDoc.body && ownerDoc.body.innerHTML) {
            var container = ownerDoc.createElement('div');
            container.innerHTML = s;
            while (container.hasChildNodes()) {
                f.appendChild(container.firstChild);
            }
        }
        else {
            var oDoc = new ActiveXObject(_SARISSA_DOM_PROGID);
            if (s.substring(0, 5) == '<?xml') {
                s = s.substring(s.indexOf('?>') + 2);
            }
            var xml = ''.concat('<my>', s, '</my>');
            oDoc.loadXML(xml);
            var container = oDoc.documentElement;
            while (container.hasChildNodes()) {
                f.appendChild(container.firstChild);
            }
        }
        return f;
    };
    
    /**
     * Set global XSLT parameter of the imported stylesheet
     * @argument nsURI The parameter namespace URI
     * @argument name The parameter base name
     * @argument value The new parameter value
     */
    XSLTProcessor.prototype.setParameter = function(nsURI, name, value){
        // make value a zero length string if null to allow clearing
        value = value ? value : "";
        // nsURI is optional but cannot be null 
        if(nsURI){
            this.processor.addParameter(name, value, nsURI);
        }else{
            this.processor.addParameter(name, value);
        };
        // update updated params for getParameter 
        if(!this.paramsSet[""+nsURI]){
            this.paramsSet[""+nsURI] = new Array();
        };
        this.paramsSet[""+nsURI][name] = value;
    };
    /**
     * Gets a parameter if previously set by setParameter. Returns null
     * otherwise
     * @argument name The parameter base name
     * @argument value The new parameter value
     * @return The parameter value if reviously set by setParameter, null otherwise
     */
    XSLTProcessor.prototype.getParameter = function(nsURI, name){
        nsURI = "" + nsURI;
        if(this.paramsSet[nsURI] && this.paramsSet[nsURI][name]){
            return this.paramsSet[nsURI][name];
        }else{
            return null;
        };
    };
    /**
     * Clear parameters (set them to default values as defined in the stylesheet itself)
     */
    XSLTProcessor.prototype.clearParameters = function(){
        for(var nsURI in this.paramsSet){
            for(var name in this.paramsSet[nsURI]){
                if(nsURI){
                    this.processor.addParameter(name, "", nsURI);
                }else{
                    this.processor.addParameter(name, "");
                };
            };
        };
        this.paramsSet = new Array();
    };
}else{ /* end IE initialization, try to deal with real browsers now ;-) */
    if(_SARISSA_HAS_DOM_CREATE_DOCUMENT){
        /**
         * <p>Ensures the document was loaded correctly, otherwise sets the
         * parseError to -1 to indicate something went wrong. Internal use</p>
         * @private
         */
        Sarissa.__handleLoad__ = function(oDoc){
            Sarissa.__setReadyState__(oDoc, 4);
        };
        /**
        * <p>Attached by an event handler to the load event. Internal use.</p>
        * @private
        */
        _sarissa_XMLDocument_onload = function(){
            Sarissa.__handleLoad__(this);
        };
        /**
         * <p>Sets the readyState property of the given DOM Document object.
         * Internal use.</p>
         * @private
         * @argument oDoc the DOM Document object to fire the
         *          readystatechange event
         * @argument iReadyState the number to change the readystate property to
         */
        Sarissa.__setReadyState__ = function(oDoc, iReadyState){
            oDoc.readyState = iReadyState;
            oDoc.readystate = iReadyState;
            if (oDoc.onreadystatechange != null && typeof oDoc.onreadystatechange == "function")
                oDoc.onreadystatechange();
        };
        Sarissa.getDomDocument = function(sUri, sName){
            var oDoc = document.implementation.createDocument(sUri?sUri:null, sName?sName:null, null);
            if(!oDoc.onreadystatechange){
            
                /**
                * <p>Emulate IE's onreadystatechange attribute</p>
                */
                oDoc.onreadystatechange = null;
            };
            if(!oDoc.readyState){
                /**
                * <p>Emulates IE's readyState property, which always gives an integer from 0 to 4:</p>
                * <ul><li>1 == LOADING,</li>
                * <li>2 == LOADED,</li>
                * <li>3 == INTERACTIVE,</li>
                * <li>4 == COMPLETED</li></ul>
                */
                oDoc.readyState = 0;
            };
            oDoc.addEventListener("load", _sarissa_XMLDocument_onload, false);
            return oDoc;
        };
        if(window.XMLDocument){
            // do nothing
        }// TODO: check if the new document has content before trying to copynodes, check  for error handling in DOM 3 LS
        else if(_SARISSA_HAS_DOM_FEATURE && window.Document && !Document.prototype.load && document.implementation.hasFeature('LS', '3.0')){
            //Opera 9 may get the XPath branch which gives creates XMLDocument, therefore it doesn't reach here which is good
            /**
            * <p>Factory method to obtain a new DOM Document object</p>
            * @argument sUri the namespace of the root node (if any)
            * @argument sUri the local name of the root node (if any)
            * @returns a new DOM Document
            */
            Sarissa.getDomDocument = function(sUri, sName){
                var oDoc = document.implementation.createDocument(sUri?sUri:null, sName?sName:null, null);
                return oDoc;
            };
        }
        else {
            Sarissa.getDomDocument = function(sUri, sName){
                var oDoc = document.implementation.createDocument(sUri?sUri:null, sName?sName:null, null);
                // looks like safari does not create the root element for some unknown reason
                if(oDoc && (sUri || sName) && !oDoc.documentElement){
                    oDoc.appendChild(oDoc.createElementNS(sUri, sName));
                };
                return oDoc;
            };
        };
    };//if(_SARISSA_HAS_DOM_CREATE_DOCUMENT)
};
//==========================================
// Common stuff
//==========================================
if(!window.DOMParser){
    if(_SARISSA_IS_SAFARI){
        /*
         * DOMParser is a utility class, used to construct DOMDocuments from XML strings
         * @constructor
         */
        DOMParser = function() { };
        /** 
        * Construct a new DOM Document from the given XMLstring
        * @param sXml the given XML string
        * @param contentType the content type of the document the given string represents (one of text/xml, application/xml, application/xhtml+xml). 
        * @return a new DOM Document from the given XML string
        */
        DOMParser.prototype.parseFromString = function(sXml, contentType){
            var xmlhttp = new XMLHttpRequest();
            xmlhttp.open("GET", "data:text/xml;charset=utf-8," + encodeURIComponent(sXml), false);
            xmlhttp.send(null);
            return xmlhttp.responseXML;
        };
    }else if(Sarissa.getDomDocument && Sarissa.getDomDocument() && Sarissa.getDomDocument(null, "bar").xml){
        DOMParser = function() { };
        DOMParser.prototype.parseFromString = function(sXml, contentType){
            var doc = Sarissa.getDomDocument();
            doc.loadXML(sXml);
            return doc;
        };
    };
};

if((typeof(document.importNode) == "undefined") && _SARISSA_IS_IE){
    try{
        /**
        * Implementation of importNode for the context window document in IE.
        * If <code>oNode</code> is a TextNode, <code>bChildren</code> is ignored.
        * @param oNode the Node to import
        * @param bChildren whether to include the children of oNode
        * @returns the imported node for further use
        */
        document.importNode = function(oNode, bChildren){
            var tmp;
            if (oNode.nodeName=='#text') {
                return document.createTextElement(oNode.data);
            }
            else {
                if(oNode.nodeName == "tbody" || oNode.nodeName == "tr"){
                    tmp = document.createElement("table");
                }
                else if(oNode.nodeName == "td"){
                    tmp = document.createElement("tr");
                }
                else if(oNode.nodeName == "option"){
                    tmp = document.createElement("select");
                }
                else{
                    tmp = document.createElement("div");
                };
                if(bChildren){
                    tmp.innerHTML = oNode.xml ? oNode.xml : oNode.outerHTML;
                }else{
                    tmp.innerHTML = oNode.xml ? oNode.cloneNode(false).xml : oNode.cloneNode(false).outerHTML;
                };
                return tmp.getElementsByTagName("*")[0];
            };
            
        };
    }catch(e){ };
};
if(!Sarissa.getParseErrorText){
    /**
     * <p>Returns a human readable description of the parsing error. Usefull
     * for debugging. Tip: append the returned error string in a &lt;pre&gt;
     * element if you want to render it.</p>
     * <p>Many thanks to Christian Stocker for the initial patch.</p>
     * @argument oDoc The target DOM document
     * @returns The parsing error description of the target Document in
     *          human readable form (preformated text)
     */
    Sarissa.getParseErrorText = function (oDoc){
        var parseErrorText = Sarissa.PARSED_OK;
        if(!oDoc.documentElement){
            parseErrorText = Sarissa.PARSED_EMPTY;
        } else if(oDoc.documentElement.tagName == "parsererror"){
            parseErrorText = oDoc.documentElement.firstChild.data;
            parseErrorText += "\n" +  oDoc.documentElement.firstChild.nextSibling.firstChild.data;
        } else if(oDoc.getElementsByTagName("parsererror").length > 0){
            var parsererror = oDoc.getElementsByTagName("parsererror")[0];
            parseErrorText = Sarissa.getText(parsererror, true)+"\n";
        } else if(oDoc.parseError && oDoc.parseError.errorCode != 0){
            parseErrorText = Sarissa.PARSED_UNKNOWN_ERROR;
        };
        return parseErrorText;
    };
};
Sarissa.getText = function(oNode, deep){
    var s = "";
    var nodes = oNode.childNodes;
    for(var i=0; i < nodes.length; i++){
        var node = nodes[i];
        var nodeType = node.nodeType;
        if(nodeType == Node.TEXT_NODE || nodeType == Node.CDATA_SECTION_NODE){
            s += node.data;
        } else if(deep == true
                    && (nodeType == Node.ELEMENT_NODE
                        || nodeType == Node.DOCUMENT_NODE
                        || nodeType == Node.DOCUMENT_FRAGMENT_NODE)){
            s += Sarissa.getText(node, true);
        };
    };
    return s;
};
if(!window.XMLSerializer 
    && Sarissa.getDomDocument 
    && Sarissa.getDomDocument("","foo", null).xml){
    /**
     * Utility class to serialize DOM Node objects to XML strings
     * @constructor
     */
    XMLSerializer = function(){};
    /**
     * Serialize the given DOM Node to an XML string
     * @param oNode the DOM Node to serialize
     */
    XMLSerializer.prototype.serializeToString = function(oNode) {
        return oNode.xml;
    };
};

/**
 * strips tags from a markup string
 */
Sarissa.stripTags = function (s) {
    return s.replace(/<[^>]+>/g,"");
};
/**
 * <p>Deletes all child nodes of the given node</p>
 * @argument oNode the Node to empty
 */
Sarissa.clearChildNodes = function(oNode) {
    // need to check for firstChild due to opera 8 bug with hasChildNodes
    while(oNode.firstChild) {
        oNode.removeChild(oNode.firstChild);
    };
};
/**
 * <p> Copies the childNodes of nodeFrom to nodeTo</p>
 * <p> <b>Note:</b> The second object's original content is deleted before 
 * the copy operation, unless you supply a true third parameter</p>
 * @argument nodeFrom the Node to copy the childNodes from
 * @argument nodeTo the Node to copy the childNodes to
 * @argument bPreserveExisting whether to preserve the original content of nodeTo, default is false
 */
Sarissa.copyChildNodes = function(nodeFrom, nodeTo, bPreserveExisting) {
    if((!nodeFrom) || (!nodeTo)){
        throw "Both source and destination nodes must be provided";
    };
    if(!bPreserveExisting){
        Sarissa.clearChildNodes(nodeTo);
    };
    var ownerDoc = nodeTo.nodeType == Node.DOCUMENT_NODE ? nodeTo : nodeTo.ownerDocument;
    var nodes = nodeFrom.childNodes;
    if(typeof(ownerDoc.importNode) != "undefined")  {
        for(var i=0;i < nodes.length;i++) {
            nodeTo.appendChild(ownerDoc.importNode(nodes[i], true));
        };
    } else {
        for(var i=0;i < nodes.length;i++) {
            nodeTo.appendChild(nodes[i].cloneNode(true));
        };
    };
};

/**
 * <p> Moves the childNodes of nodeFrom to nodeTo</p>
 * <p> <b>Note:</b> The second object's original content is deleted before 
 * the move operation, unless you supply a true third parameter</p>
 * @argument nodeFrom the Node to copy the childNodes from
 * @argument nodeTo the Node to copy the childNodes to
 * @argument bPreserveExisting whether to preserve the original content of nodeTo, default is
 */ 
Sarissa.moveChildNodes = function(nodeFrom, nodeTo, bPreserveExisting) {
    if((!nodeFrom) || (!nodeTo)){
        throw "Both source and destination nodes must be provided";
    };
    if(!bPreserveExisting){
        Sarissa.clearChildNodes(nodeTo);
    };
    var nodes = nodeFrom.childNodes;
    // if within the same doc, just move, else copy and delete
    if(nodeFrom.ownerDocument == nodeTo.ownerDocument){
        while(nodeFrom.firstChild){
            nodeTo.appendChild(nodeFrom.firstChild);
        };
    } else {
        var ownerDoc = nodeTo.nodeType == Node.DOCUMENT_NODE ? nodeTo : nodeTo.ownerDocument;
        if(typeof(ownerDoc.importNode) != "undefined") {
           for(var i=0;i < nodes.length;i++) {
               nodeTo.appendChild(ownerDoc.importNode(nodes[i], true));
           };
        }else{
           for(var i=0;i < nodes.length;i++) {
               nodeTo.appendChild(nodes[i].cloneNode(true));
           };
        };
        Sarissa.clearChildNodes(nodeFrom);
    };
};

/** 
 * <p>Serialize any object to an XML string. All properties are serialized using the property name
 * as the XML element name. Array elements are rendered as <code>array-item</code> elements, 
 * using their index/key as the value of the <code>key</code> attribute.</p>
 * @argument anyObject the object to serialize
 * @argument objectName a name for that object
 * @return the XML serializationj of the given object as a string
 */
Sarissa.xmlize = function(anyObject, objectName, indentSpace){
    indentSpace = indentSpace?indentSpace:'';
    var s = indentSpace  + '<' + objectName + '>';
    var isLeaf = false;
    if(!(anyObject instanceof Object) || anyObject instanceof Number || anyObject instanceof String 
        || anyObject instanceof Boolean || anyObject instanceof Date){
        s += Sarissa.escape(""+anyObject);
        isLeaf = true;
    }else{
        s += "\n";
        var itemKey = '';
        var isArrayItem = anyObject instanceof Array;
        for(var name in anyObject){
            s += Sarissa.xmlize(anyObject[name], (isArrayItem?"array-item key=\""+name+"\"":name), indentSpace + "   ");
        };
        s += indentSpace;
    };
    return s += (objectName.indexOf(' ')!=-1?"</array-item>\n":"</" + objectName + ">\n");
};

/** 
 * Escape the given string chacters that correspond to the five predefined XML entities
 * @param sXml the string to escape
 */
Sarissa.escape = function(sXml){
    return sXml.replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&apos;");
};

/** 
 * Unescape the given string. This turns the occurences of the predefined XML 
 * entities to become the characters they represent correspond to the five predefined XML entities
 * @param sXml the string to unescape
 */
Sarissa.unescape = function(sXml){
    return sXml.replace(/&apos;/g,"'")
        .replace(/&quot;/g,"\"")
        .replace(/&gt;/g,">")
        .replace(/&lt;/g,"<")
        .replace(/&amp;/g,"&");
};
//   EOF


/* document.getElementsBySelector(selector)
   - returns an array of element objects from the current document
     matching the CSS selector. Selectors can contain element names, 
     class names and ids and can be nested. For example:
     
       elements = document.getElementsBySelect('div#main p a.external')
     
     Will return an array of all 'a' elements with 'external' in their 
     class attribute that are contained inside 'p' elements that are 
     contained inside the 'div' element which has id="main"

   New in version 0.4: Support for CSS2 and CSS3 attribute selectors:
   See http://www.w3.org/TR/css3-selectors/#attribute-selectors

   Version 0.4 - Simon Willison, March 25th 2003
   -- Works in Phoenix 0.5, Mozilla 1.3, Opera 7, Internet Explorer 6, Internet Explorer 5 on Windows
   -- Opera 7 fails 
*/

function getAllChildren(e) {
  // Returns all children of element. Workaround required for IE5/Windows. Ugh.
  return e.all ? e.all : e.getElementsByTagName('*');
}

document.getElementsBySelector = function(selector) {
  // Attempt to fail gracefully in lesser browsers
  if (!document.getElementsByTagName) {
    return new Array();
  }
  // Split selector in to tokens
  var tokens = selector.split(' ');
  var currentContext = new Array(document);
  for (var i = 0; i < tokens.length; i++) {
    token = tokens[i].replace(/^\s+/,'').replace(/\s+$/,'');;
    if (token.indexOf('#') > -1) {
      // Token is an ID selector
      var bits = token.split('#');
      var tagName = bits[0];
      var id = bits[1];
      var element = document.getElementById(id);
      if (tagName && element.nodeName.toLowerCase() != tagName) {
        // tag with that ID not found, return false
        return new Array();
      }
      // Set currentContext to contain just this element
      currentContext = new Array(element);
      continue; // Skip to next token
    }
    if (token.indexOf('.') > -1) {
      // Token contains a class selector
      var bits = token.split('.');
      var tagName = bits[0];
      var className = bits[1];
      if (!tagName) {
        tagName = '*';
      }
      // Get elements matching tag, filter them for class selector
      var found = new Array;
      var foundCount = 0;
      for (var h = 0; h < currentContext.length; h++) {
        var elements;
        if (tagName == '*') {
            elements = getAllChildren(currentContext[h]);
        } else {
            elements = currentContext[h].getElementsByTagName(tagName);
        }
        for (var j = 0; j < elements.length; j++) {
          found[foundCount++] = elements[j];
        }
      }
      currentContext = new Array;
      var currentContextIndex = 0;
      for (var k = 0; k < found.length; k++) {
        if (found[k].className && found[k].className.match(new RegExp('\\b'+className+'\\b'))) {
          currentContext[currentContextIndex++] = found[k];
        }
      }
      continue; // Skip to next token
    }
    // Code to deal with attribute selectors
    if (token.match(/^(\w*)\[(\w+)([=~\|\^\$\*]?)=?"?([^\]"]*)"?\]$/)) {
      var tagName = RegExp.$1;
      var attrName = RegExp.$2;
      var attrOperator = RegExp.$3;
      var attrValue = RegExp.$4;
      if (!tagName) {
        tagName = '*';
      }
      // Grab all of the tagName elements within current context
      var found = new Array;
      var foundCount = 0;
      for (var h = 0; h < currentContext.length; h++) {
        var elements;
        if (tagName == '*') {
            elements = getAllChildren(currentContext[h]);
        } else {
            elements = currentContext[h].getElementsByTagName(tagName);
        }
        for (var j = 0; j < elements.length; j++) {
          found[foundCount++] = elements[j];
        }
      }
      currentContext = new Array;
      var currentContextIndex = 0;
      var checkFunction; // This function will be used to filter the elements
      switch (attrOperator) {
        case '=': // Equality
          checkFunction = function(e) { return (e.getAttribute(attrName) == attrValue); };
          break;
        case '~': // Match one of space seperated words 
          checkFunction = function(e) { return (e.getAttribute(attrName).match(new RegExp('\\b'+attrValue+'\\b'))); };
          break;
        case '|': // Match start with value followed by optional hyphen
          checkFunction = function(e) { return (e.getAttribute(attrName).match(new RegExp('^'+attrValue+'-?'))); };
          break;
        case '^': // Match starts with value
          checkFunction = function(e) { return (e.getAttribute(attrName).indexOf(attrValue) == 0); };
          break;
        case '$': // Match ends with value - fails with "Warning" in Opera 7
          checkFunction = function(e) { return (e.getAttribute(attrName).lastIndexOf(attrValue) == e.getAttribute(attrName).length - attrValue.length); };
          break;
        case '*': // Match ends with value
          checkFunction = function(e) { return (e.getAttribute(attrName).indexOf(attrValue) > -1); };
          break;
        default :
          // Just test for existence of attribute
          checkFunction = function(e) { return e.getAttribute(attrName); };
      }
      currentContext = new Array;
      var currentContextIndex = 0;
      for (var k = 0; k < found.length; k++) {
        if (checkFunction(found[k])) {
          currentContext[currentContextIndex++] = found[k];
        }
      }
      // alert('Attribute Selector: '+tagName+' '+attrName+' '+attrOperator+' '+attrValue);
      continue; // Skip to next token
    }
    // If we get here, token is JUST an element (not a class or ID selector)
    tagName = token;
    var found = new Array;
    var foundCount = 0;
    for (var h = 0; h < currentContext.length; h++) {
      var elements = currentContext[h].getElementsByTagName(tagName);
      for (var j = 0; j < elements.length; j++) {
        found[foundCount++] = elements[j];
      }
    }
    currentContext = found;
  }
  return currentContext;
}

/* That revolting regular expression explained 
/^(\w+)\[(\w+)([=~\|\^\$\*]?)=?"?([^\]"]*)"?\]$/
  \---/  \---/\-------------/    \-------/
    |      |         |               |
    |      |         |           The value
    |      |    ~,|,^,$,* or =
    |   Attribute 
   Tag
*/








if(!XMPP) {
    var XMPP = {};
}

/**
* Creates a new instance of BOSH. BOSH, or Bidirectional-streams Over Synchronous HTTP. Provides
* request and response over HTTP and is intended for use in the broswer.
*/
XMPP.BOSH = function(binding, configuration) {
    if (binding.charAt(0) == '/') {
        this.binding = binding;
    }
    else {
        this.binding = new Poly9.URLParser(binding);
    }
    this.requestCount = 0;
    this.failedRequests = new Array();
    this.lastPollTime = 0;
    this._configure($H(configuration).merge(this.defaultConfiguration));
    this.listeners = {
        "success": [],
        "failure": [],
        "exception": []
    }
    this._requestQueue = new Array();
}

XMPP.BOSH.prototype = {
    defaultConfiguration: {
        maxConcurrentRequests: 2
    },
    _boshVersion: 1.6,
    _headers: {
        'post' : [],
        'get': ['Cache-Control', 'no-store', 'Cache-Control', 'no-cache', 'Pragma', 'no-cache']
    },
    _configure: function(configuration) {
        for(var i in configuration) {
            if (!(typeof configuration[i] == "Function")) {
                this[i] = configuration[i];
            }
        }
    },
    connect: function(successfulConnectionCallback, unsuccessfulConnectionCallback) {
        // create our connection listeners
        var success = this._processConnectionResponse.bind(this, successfulConnectionCallback);
        var failure = this._processConnectionFailure.bind(this, unsuccessfulConnectionCallback);

        // add our connection listeners
        this.listeners["success"].push(success);
        this.listeners["failure"].push(failure)
        this.listeners["exception"].push(failure);

        var bodyContent = this._buildSessionRequest(this["maxConcurrentRequests"] - 1,
            this._getNextRID(), true, 10, this._boshVersion);
        //console.debug("Initial request: " +  bodyContent);
        this.doRequest(bodyContent, true);
    },
    isConnected: function() {
        return this._isConnected && !this._isDisconnecting;
    },
    disconnect: function(xml, successfulDisconnectionCallback, unsuccessfulDisconnectionCallback) {
        this._cancelPoll();
        this._clearListeners();
        if(!this.isConnected()) {
            return;
        }

        // create our disconnection listeners
        var success = this._processDisconnectionResponse.bind(this,
                successfulDisconnectionCallback);
        var failure = this._processDisconnectionFailure.bind(this,
                unsuccessfulDisconnectionCallback);

        // add our connection listeners
        this.listeners["success"].push(success);
        this.listeners["failure"].push(failure)
        this.listeners["exception"].push(failure);

        this._isDisconnecting = true;

        var attrs = {
            xmlns: "http://jabber.org/protocol/httpbind",
            rid: this._getNextRID(),
            sid: this._sid,
            type: "terminate"
        }
        this.doRequest(org.jive.util.XML.element("body", (xml ? xml : ""), attrs));
    },
    _processConnectionResponse: function(callback, responseBody) {
        //console.debug("Intial response: " +  responseBody);

        this._sid = responseBody.getAttribute("sid");
        this._pollingInterval = responseBody.getAttribute("polling") * 1000;
        // if we have a polling interval of 1, we want to add an extra second for a little bit of
        // padding.
        //if(this._pollingInterval <= 1000 && this._pollingInterval > 0) {
        //    this._pollingInterval += 1000;
        //}
        
        this._wait = responseBody.getAttribute("wait");
        this._inactivity = responseBody.getAttribute("inactivity");
        var serverRequests = responseBody.getAttribute("requests");
        if (!serverRequests) {
            this._maxConcurrentRequests = this["maxConcurrentRequests"];
        }
        else {
            this._maxConcurrentRequests = Math.min(serverRequests, this["maxConcurrentRequests"]);
        }
        this._isConnected = true;
        this._clearListeners();
        if(callback) {
            var arg;
            if(responseBody.firstChild) {
                arg = responseBody.firstChild;
            }
            callback(arg);
        }
    },
    _processConnectionFailure: function(callback, request, header) {
        alert("Connection to the server failed: " + header);
        this._clearListeners();
        if(callback) {
            callback();
        }
    },
    _processDisconnectionResponse: function(callback, responseBody) {
        this._isDisconnecting = false;
        this._isConnected = false;
        this._clearListeners();
        if (callback) {
            callback();
        }
    },
    _processDisconnectionFailure: function(callback, responseBody) {
        this._isDisconnecting = false;
        this._isConnected = false;
        this._clearListeners();
        if (callback) {
            callback();
        }
    },
    _buildSessionRequest: function(holdValue, ridValue, secureValue, waitValue, version) {
        var attrs = {
            xmlns: "http://jabber.org/protocol/httpbind",
            hold: holdValue,
            rid: ridValue,
            secure: secureValue,
            wait: waitValue,
            ver: version
        }

        return org.jive.util.XML.element("body", "", attrs);
    },
    send: function(request) {
        if(!this.isConnected()) {
            throw Error("Not connected, cannot send packets.");
        }

        if (this._shouldQueueRequest()) {
            this._queueRequest(request);
        }
        else {
            this._sendQueuedRequests(request);
        }
    },
    _send: function(requests) {
        if (!this.isConnected()) {
            throw Error("Not connected, cannot send packets.");
        }
        this._cancelPoll();
        this.doRequest(this._createRequest(requests.join("")), requests.length <= 0);
    },
    _shouldQueueRequest: function() {
        return this.requestCount >= this._maxConcurrentRequests;
    },
    _queueRequest: function(request) {
        this._requestQueue.push(request);
    },
    destroy: function() {
        this._clearListeners();
        this._cancelPoll();
    },
    _handlePoll: function(pollTime) {
        this._cancelPoll();
        if(!pollTime) {
            pollTime = this._pollingInterval;
        }
        this._timer = new TimeoutExecutor(this._pollServer.bind(this), pollTime);
    },
    _pollServer: function(responseHandler) {
        if(!this._isConnected) {
            return;
        }
        if(this._areRequestsQueued()) {
            this._sendQueuedRequests();
        }
        if (this.isCurrentlyPolling) {
            return;
        }
        var currentTime = new Date().valueOf();
        var currentInterval = currentTime - this.lastPollTime;
        if(currentInterval < this._pollingInterval) {
            var delayTime = (this._pollingInterval - currentInterval);

            this._handlePoll(delayTime);
            return;
        }
        this.doRequest(this._createRequest(), true);
    },
    _cancelPoll: function() {
        if (this._timer) {
            this._timer.cancel();
            delete this._timer;
        }
    },
    _getNextRID: function() {
        if (!this._rid) {
            this._rid = Math.floor(Math.random() * 1000000);
        }
        return ++this._rid;
    },
    _createRequest: function(bodyContent) {
        var attrs = {
            xmlns: "http://jabber.org/protocol/httpbind",
            rid: this._getNextRID(),
            sid: this._sid
        }

        return org.jive.util.XML.element("body", bodyContent, attrs);
    },
    addListener: function(event, eventHandler) {
        if(!this._isConnected) {
            throw Error("Must be connected to add a connection listener.");
        }
        this.listeners[event].push(eventHandler);
    },
    _clearListeners: function() {
        for(var event in this.listeners) {
            this.listeners[event] = [];
        }
    },
    _fireEvent: function(event) {
        if(!this.listeners[event]) {
            return;
        }
        var args = $A(arguments);
        args.shift();
        var listenerCount = this.listeners[event].length;
        for (var i = 0; i < listenerCount; i++) {
            this.listeners[event][i].apply(null, args);
        }
    },
    doRequest: function(bodyContent, isRequestPoll) {
        if (this.failureState) {
            throw Error("HTTP connection in failure state and must be reset.");
        }
        var successCallback = this._successCallback.bind(this, isRequestPoll);
        var failureCallback = this._failureCallback.bind(this, bodyContent);
        var exceptionCallback = this._exceptionCallback.bind(this, bodyContent);

        if(isRequestPoll) {
            this.isCurrentlyPolling = true;
        }

        this.requestCount++;
        var requestUrl;
        var method;
        if (typeof this.binding == "string") {
            requestUrl = this.binding;
            method = "post";
        }
        return new Ajax.Request(requestUrl, {
            method: method,
            postBody: bodyContent,
            requestHeaders: this._headers[method],
            onSuccess: successCallback,
            onException: exceptionCallback,
            onFailure: failureCallback
        });
    },
    _successCallback: function(isRequestPoll, originalRequest) {
        this.requestCount--;
        if (originalRequest.responseXML.firstChild) {
            var body = originalRequest.responseXML.firstChild;
            // Special case for initial connection.
            if(this._sid) {
                body = $A(body.childNodes);
            }
            this._fireEvent("success", body);
        }

        if(isRequestPoll) {
            this.lastPollTime = new Date().valueOf();
        }

        if(this._isConnected) {
            this._checkQueueAndPoll(isRequestPoll)
        }
    },
    _checkQueueAndPoll: function(isLastRequestPoll) {
        if (this._areRequestsQueued()) {
            this._sendQueuedRequests();
        }
        else if(!isLastRequestPoll){
            if (!this._timer) {
                this._pollServer();
            }
        }
        // Is our last request a poll?
        else {
            this.isCurrentlyPolling = false;
            this._handlePoll();
        }
    },
    _areRequestsQueued: function() {
        return this._requestQueue.length > 0;
    },
    _sendQueuedRequests: function(xml) {
        if(xml) {
            this._requestQueue.push(xml);
        }
        this._send(this._requestQueue.compact());
        this._requestQueue.clear();
    },
    _failureCallback: function(bodyContent, originalRequest, header) {
        this.requestCount--;
        // Turning on a failure state will cause an subsequent requests to fail.
        this._isConnected = false;
        this.failureState = true;
        this.failedRequests.splice(0, 0, bodyContent);
        this._fireEvent("failure", bodyContent, header);
    },
    _exceptionCallback: function(bodyContent, originalRequest, error) {
        this.requestCount--;
        // Turning on a failure state will cause an subsequent requests to fail.
        this._isConnected = false;
        this.failureState = true;
        alert("Request exception! " + error);
        this.failedRequests.splice(0, 0, bodyContent);
        this._fireEvent("exception", originalRequest, error);
    }
}

var TimeoutExecutor = Class.create();
TimeoutExecutor.prototype = {
    initialize: function(callback, timeout) {
        this.callback = callback;
        this.timeout = timeout;
        this.currentlyExecuting = false;

        this.registerCallback();
    },
    registerCallback: function() {
        this.timeoutID = setTimeout(this.onTimerEvent.bind(this), this.timeout);
    },
    onTimerEvent: function() {
        try {
            this.currentlyExecuting = true;
            if (this.callback && this.callback instanceof Function) {
                this.callback();
            }
        }
        finally {
            this.currentlyExecuting = false;
            delete this.timeoutID;
        }
    },
    cancel: function() {
        if (!this.currentlyExecuting && this.timeoutID) {
            clearTimeout(this.timeoutID);
            delete this.timeoutID;
        }
    },
    reset: function() {
        if (!this.currentlyExecuting && this.timeoutID) {
            clearTimeout(this.timeoutID);
            delete this.timeoutID;
            this.registerCallback();
        }
    }
}

if(!org) {
    var org = {};
}
if(!org.jive) {
    org.jive = {};
}
if(!org.jive.util) {
    org.jive.util = {}
}

org.jive.util.XML = {
    element: function(name, content, attributes) {
        var att_str = '';
        if (attributes) {
            att_str = this.formatAttributes(attributes);
        }
        var xml;
        if (!content) {
            xml = '<' + name + att_str + '/>';
        }
        else {
            xml = '<' + name + att_str + '>' + content + '</' + name + '>';
        }
        return xml;
    },
    formatAttributes: function(attrs) {
        var attr_string = "";

        for (var attr in attrs) {
            var attr_value = attrs[attr];
            attr_string += ' ' + attr + '="' + attr_value + '"';
        }

        return attr_string;
    }
}


/**
 * @projectDescription 	Poly9's polyvalent URLParser class
 *
 * @author	Denis Laprise - denis@poly9.com - http://poly9.com
 * @version	0.1
 * @namespace	Poly9
 *
 * Usage: var p = new Poly9.URLParser('http://user:password@poly9.com/pathname?arguments=1#fragment');
 * p.getHost() == 'poly9.com';
 * p.getProtocol() == 'http';
 * p.getPathname() == '/pathname';
 * p.getQuerystring() == 'arguments=1';
 * p.getFragment() == 'fragment';
 * p.getUsername() == 'user';
 * p.getPassword() == 'password';
 *
 * See the unit test file for more examples.
 * URLParser is freely distributable under the terms of an MIT-style license.
 */

if (typeof Poly9 == 'undefined')
    var Poly9 = {};

/**
 * Creates an URLParser instance
 *
 * @classDescription	Creates an URLParser instance
 * @return {Object}	return an URLParser object
 * @param {String} url	The url to parse
 * @constructor
 * @exception {String}  Throws an exception if the specified url is invalid
 */
Poly9.URLParser = function(url) {
    this._fields = {'Username' : 4, 'Password' : 5, 'Port' : 7, 'Protocol' : 2, 'Host' : 6, 'Pathname' : 8, 'URL' : 0, 'Querystring' : 9, 'Fragment' : 10};
    this._values = {};
    this._regex = null;
    this.version = 0.1;
    this._regex = /^((\w+):\/\/)?((\w+):?(\w+)?@)?([^\/\?:]+):?(\d+)?(\/?[^\?#]+)?\??([^#]+)?#?(\w*)/;
    for (var f in this._fields)
        this['get' + f] = this._makeGetter(f);
    if (typeof url != 'undefined')
        this._parse(url);
}

/**
 * @method
 * @param {String} url	The url to parse
 * @exception {String} 	Throws an exception if the specified url is invalid
 */
Poly9.URLParser.prototype.setURL = function(url) {
    this._parse(url);
}

Poly9.URLParser.prototype._initValues = function() {
    for (var f in this._fields)
        this._values[f] = '';
}

Poly9.URLParser.prototype._parse = function(url) {
    this._initValues();
    var r = this._regex.exec(url);
    if (!r) throw "DPURLParser::_parse -> Invalid URL"
    for (var f in this._fields) if (typeof r[this._fields[f]] != 'undefined')
        this._values[f] = r[this._fields[f]];
}

Poly9.URLParser.prototype._makeGetter = function(field) {
    return function() {
        return this._values[field];
    }
}

/**
 * Creates a new XMPP connection which passes XMPP packets to the server and processes XMPP packets from
 * the server.
 *
 * @param {String} url the url used for communication.
 * @param {String} domain the host being connected to.
 * @param {Object} connectionListener a connection listener which will be notified when there is a connection
 * error, when the connection is established, and when authentication is successful.
 */
function XMPPConnection(url, domain, connectionListener) {
    this.connection = new XMPP.BOSH(url);
    this.domain = domain;
    this.isConnected = false;
    this.isAuthenticated = false;

    this._packetFilters = new Array();
    this._outgoingPacketFilters = new Array();
    this._connectionListeners = new Array();
    this.addConnectionListener(connectionListener);
}

XMPPConnection.prototype = {
/**
 * Adds a connection listener which will be notified when there is a connection
 * error, when the connection is established, and when authentication is successful.
 * {
 * 		connectionSuccessful: function(connection)
 * 		connectionFailed: function(connection, error)
 * 		authenticationSuccessful: function(connection)
 * 		authenticationFailed: function(connection, error)
 * 		connectionClosed: function(connection, error)
 * 		packetsReceived: function(requestID, packetCount)
 * 		packetsProcessed: function(requestID)
 * }
 *
 * @param {Object} connectionListener the connection listener which is being added.
 */
    addConnectionListener: function(connectionListener) {
        if (!connectionListener) {
            return;
        }
        this._connectionListeners.push(connectionListener);
    },
/**
 * Removes a connection listener.
 *
 * @param {Object} connectionListener the listener which is being removed.
 */
    removeConnectionListener: function(connectionListener) {
        if (!connectionListener) {
            return;
        }
        var index = this._connectionListeners.indexOf(connectionListener);
        if (index >= 0) {
            this._connectionListeners.splice(index, 1);
        }
    },
    _fireEvent: function(event, error) {
        var self = this;
        this._connectionListeners.each(function(listener) {
            if (listener[event]) {
                try {
                    listener[event](self, error);
                }
                catch (error) {
                    alert("Error processing listener: " +  event);
                }
            }
        });
    },
/**
 * Connects the the XMPP server provided in the XMPPConnection constructor using HTTP binding.
 * After a successful connection the connectionSuccessful event will be fired to any connection
 * listeners. If the connection is not successful the connectionFailed event will be fired.
 */
    connect: function() {
        this.connection.connect(this._configureConnection.bind(this));
    },
    logout: function(packet) {
        if (this.loggedOut) {
            return;
        }
        this.connection.disconnect((packet ? packet.toXML() : ""), this.destroy.bind(this),
                this.destroy.bind(this));
    },
/**
 * Closes the connection to the server. If an error is passed in it will be passed
 * along to the conenction listeners.
 *
 * @param {Error} error an error if it occured to close the connection.
 */
    destroy: function(error) {
        if (!this.isConnected) {
            return;
        }
        this.isConnected = false;
        this.isAuthenticated = false;

        delete this.authentication;
        delete this.username;
        delete this.password;
        this._packetHandler = Prototype.emptyFunction;

        this._packetFilters.clear();
        this._outgoingPacketFilters.clear();

        if (!this.loggedOut && !error) {
            error = new Error("connection lost");
        }

        this._fireEvent("connectionClosed", (!this.loggedOut ? error : null));
        this._connectionListeners.clear();
    },
/**
 * Authenticates with the server using the provided username and password. The SASL methods currently supported
 * are plain and anonymous. If no username is provided an anonymous session is created with the conencted server.
 * After succesful authentication, the authenticationSuccessful event is fired to all connection listeners and, if
 * authentication fails, the authenticationFailed event is fired.
 *
 * @param {String} username the username which will be used to authenticate with the server.
 * @param {String} password the password to use to authenticate with the server.
 * @param {String} resource the resource, which will uniquely identify this session from any others currently
 * using the same username on the server.
 */
    login: function(username, password, resource) {
        if (!this.authentication.auth["anonymous"] && (!username || username == "")) {
            throw new Error("Username must be provided to login.");
        }
        if (!this.authentication.auth["anonymous"] && (!password || password == "")) {
            throw new Error("Password must be provided to login.");
        }
        // don't save password for security purposes.
        this.username = username;
        this.resource = (!resource ? "office" : resource);

        var auth;
        if (!username) {
            auth = new XMPP.SASLAuth.Anonymous();
        }
        else {
            auth = new XMPP.SASLAuth.Plain(username, password, this.domain);
        }

        var authHandler = this._handleAuthentication.bind(this, auth);
        this.connection.addListener("success", authHandler);
        this.connection.send(auth.request);
    },
    _handleAuthentication: function(auth, response) {
        if(!response || response.length == 0) {
            return;
        }
        var status = auth.handleResponse(0, response[0]);
        if (status.authComplete) {
            if (status.authSuccess) {
                this.connection._clearListeners();
                this._addListeners();
                this._bindConnection();
            }
            else {
                this._fireEvent("authenticationFailed");
                this.connection.disconnect();
            }
        }
    },
    _configureConnection: function(features) {
        this.isConnected = true;

        var authentication = {};
        for (var i = 0; i < features.childNodes.length; i++) {
            var feature = features.childNodes[i];
            if (feature.tagName == "mechanisms") {
                authentication.auth = this._configureAuthMechanisms(feature);
            }
            else if (feature.tagName == "bind") {
                authentication.bind = true;
            }
            else if (feature.tagName == "session") {
                authentication.session = true;
            }
        }
        this.authentication = authentication;
        this._fireEvent("connectionSuccessful");
    },
    _configureAuthMechanisms: function(mechanisms) {
        var authMechanism = {};
        for (var i = 0; i < mechanisms.childNodes.length; i++) {
            var mechanism = mechanisms.childNodes[i];
            if (mechanism.firstChild.nodeValue == "PLAIN") {
                authMechanism["plain"] = true;
            }
            else if (mechanism.firstChild.nodeValue == "ANONYMOUS") {
                authMechanism["anonymous"] = true;
            }
        }

        if (!authMechanism) {
            authMechanism = function() {
                // here we will throw an error
                return false;
            }
        }

        return authMechanism;
    },
    _addListeners: function() {
        this.connection.addListener("success", this._handlePackets.bind(this));
        this.connection.addListener("failure", this._handleFailure.bind(this));
        this.connection.addListener("exception", this._handleException.bind(this));
    },
    _bindConnection: function() {
        var bindIQ = new XMPP.IQ("set");
        bindIQ.setXMLNS("jabber:client");

        var bind = bindIQ.addExtension("bind", "urn:ietf:params:xml:ns:xmpp-bind");

        bind.appendChild(bindIQ.doc.createElement("resource"))
                .appendChild(bindIQ.doc.createTextNode(this.resource));

        //console.debug("Bind the connection! " +  bindIQ.doc.documentElement);

        var callback = function(packet) {
            var bind = packet.getExtension("bind");

            //console.debug("Bind Response: " +  bind);

            var jid = bind.firstChild;
            this._jid = jid.firstChild.nodeValue;
            this._establishSession();
        }.bind(this);
        var id = bindIQ.getID();
        var packetFilter = new org.jive.spank.PacketFilter(callback, function(packet) {
            return packet.getID() == id;
        });

        this.sendPacket(bindIQ, packetFilter);
    },
    _establishSession: function() {
        var sessionIQ = new XMPP.IQ("set");
        sessionIQ.setXMLNS("jabber:client");

        sessionIQ.addExtension("session", "urn:ietf:params:xml:ns:xmpp-session");
        //console.debug("Establishing session: " +  sessionIQ.doc.documentElement);

        var connection = this;
        var callback = function(originalRequest) {
            connection.isAuthenticated = true;
            connection._fireEvent("authenticationSuccessful");
        }
        var id = sessionIQ.getID();
        var packetFilter = new org.jive.spank.PacketFilter(callback, function(packet) {
            return packet.getID() == id;
        });

        this.sendPacket(sessionIQ, packetFilter);
    },
/**
 * Sends a packet to the connected XMPP server. The packet, or a group of packets,
 * is wrapped in a body tag and sent to the server using HTTPBinding.
 *
 * @param {XMPP.Packet} packet a valid xmpp packet.
 * @param {org.jive.spank.PacketFilter} convinence for adding a PacketFilter,
 * the filter will be removed upon its execution.
 */
    sendPacket: function(packet, packetFilter) {
        if (!packet || !(typeof packet == "object" && packet instanceof XMPP.Packet)) {
            return;
        }

        if (packetFilter) {
            this.addPacketFilter(packetFilter, true);
        }

        this._handlePacket(this._outgoingPacketFilters.slice(), packet);
        this.connection.send(packet.toXML());
    },
    
    sendXML: function(packetXML) {
        this.connection.send(packetXML);    
    },
    
    _handleFailure: function(request, header) {
        alert("Request failure: " +  header);
        this.destroy(header);
    },
    _handleException: function(request, error) {
        alert("Request exception: " +  error);
        this.destroy(error);
    },
    _handlePackets: function(packets) {
        var len = packets.length;
        this._fireEvent("packetsReceived");
        for (var i = 0; i < len; i++) {
            var packetElement = packets[i].cloneNode(true);
            var packetType = packetElement.tagName;
            var packet;

            if (packetType == "iq") {
                packet = new XMPP.IQ(null, null, null, packetElement);
            }
            else if (packetType == "presence") {
                packet = new XMPP.Presence(null, null, packetElement);
            }
            else if (packetType == "message") {
                packet = new XMPP.Message(null, null, null, packetElement);
                //alert(packet.toXML())
            }
            else {
                alert("Server returned unknown packet, tossing: " +  packetElement);
                continue;
            }
            // Slice the array so we have a copy of it. This keeps us safe in case a listener
            // is removed while processing packets.
            this._handlePacket(connection._packetFilters.slice(), packet);

        }
        this._fireEvent("packetsProcessed");
    },
/**
 * Adds a PacketFilter to the connection. An optional parameter of removeOnExecution when true will cause the
 * PacketFilter to be removed from the connection upon its execution. The PacketFilter is checked if it should
 * be executed for each packet that is recieved on the connection.
 *
 * @param {org.jive.spank.PacketFilter} packetFilter the filter being added to the connection.
 * @param {boolean} removeOnExecution (optional) true if the filter should be removed after it has been
 * exectuted for the first time.
 */
    addPacketFilter: function(packetFilter, removeOnExecution) {
        if (!packetFilter || !(packetFilter instanceof org.jive.spank.PacketFilter)) {
            throw Error("PacketFilter must be an instance of PacketFilter");
        }
        packetFilter.removeOnExecution = removeOnExecution;
        this._packetFilters.push(packetFilter);
    },
/**
 * Removes a PacketFilter from the connection.
 *
 * @param {org.jive.spank.PacketFilter} packetFilter the packet filter which is being removed.
 */
    removePacketFilter: function(packetFilter) {
        if (!packetFilter) {
            return;
        }

        var index = this._packetFilters.indexOf(packetFilter);
        if (index >= 0) {
            this._packetFilters.splice(index, 1);
        }
    },
/**
 * Adds an outgoing PacketFilter to the connection. An outgoing PacketFilter is executed on every packet being
 * sent from this connection.
 *
 * @param {org.jive.spank.PacketFilter} packetFilter the PacketFilter which will be executed on each packet being
 * sent to the server.
 */
    addOutgoingPacketFilter: function(packetFilter) {
    
        if (!packetFilter || !(packetFilter instanceof org.jive.spank.PacketFilter)) {
            throw Error("PacketFilter must be an instance of PacketFilter");
        }
        this._outgoingPacketFilters.push(packetFilter);
    },
    
    _handlePacket: function(packetFilters, packet) {
        for (var i = packetFilters.length - 1; i >= 0; i--) {
            try {
                if (packetFilters[i].accept(packet) && packetFilters[i].removeOnExecution) {
                    this.removePacketFilter(packetFilters[i]);
                }
            }
            catch(e) {
                //alert("Error processing packet: " +  e.message);
                
                if (packetFilters[i].removeOnExecution) {
                    this.removePacketFilter(packetFilters[i]);
                }
            }
        }

    }
}

XMPP.SASLAuth = {};
XMPP.SASLAuth.Plain = function(username, password, domain) {
    var authContent = username + "@" + domain;
    authContent += '\u0000';
    authContent += username;
    authContent += '\u0000';
    authContent += password;

    authContent = util.base64.encode(authContent);

    var attrs = {
        mechanism: "PLAIN",
        xmlns: "urn:ietf:params:xml:ns:xmpp-sasl"
    }

    // TODO would like to remove this dependency and create an auth packet
    this.request = org.jive.util.XML.element("auth", authContent, attrs);

    //console.debug("Plain auth request: " +  this.request);
    this.stage = 0;
}

XMPP.SASLAuth.Plain.prototype = {
    handleResponse: function(stage, response) {
        var success = response.tagName == "success";
        return {
            authComplete: true,
            authSuccess: success,
            authStage: stage++
        };
    }
}

XMPP.SASLAuth.Anonymous = function() {
    var attrs = {
        mechanism: "ANONYMOUS",
        xmlns: "urn:ietf:params:xml:ns:xmpp-sasl"
    }

    this.request = org.jive.util.XML.element("auth", null, attrs);
    //console.debug("Plain auth request: " +  this.request);
}

XMPP.SASLAuth.Anonymous.prototype = {
    handleResponse: function(stage, responseBody) {
        var success = responseBody.tagName == "success";
        return {
            authComplete: true,
            authSuccess: success,
            authStage: stage++
        };
    }
}

if(!org) {
    var org = {};
}
if(!org.jive) {
    org.jive = {};
}
if (!org.jive.spank) {
    org.jive.spank = {};
}
org.jive.spank.chat = {};
/**
 * Creates a ChatManager object. The ChatManager object will hook up the listeners on the
 * connection object to deal with incoming and outgoing chats.
 *
 * @param {XMPPConnection} connection the connection object which this ChatManager is being
 * initialized for.
 * @param {String} server the server for which this ChatManager is handling chats.
 * @param {boolean} shouldUseThreads boolean indicating whether threads should be used to uniquely identify
 * conversations between two entities.
 */
org.jive.spank.chat.Manager = function(connection, server, shouldUseThreads) {
    if (!connection || !(connection instanceof XMPPConnection)) {
        throw Error("connection required for ChatManager.");
    }

    this.getConnection = function () {
        return connection;
    }

    this.servers = {};
    if (server) {
        this.servers[server] = false;
    }

    var self = this;
    connection.addConnectionListener({
        connectionClosed: function() {
            self.destroy();
        }
    });

    this.packetFilter = new org.jive.spank.PacketFilter(this._createMessageHandler(),
            this._createMessageFilter());
    connection.addPacketFilter(this.packetFilter);

    this._chatSessions = new Array();
    this._chatSessionListeners = new Array();
    this._baseID = util.StringUtil.randomString(5);
    this._threadID = 1;
    this.shouldUseThreads = shouldUseThreads;

    this.presenceFilter = new org.jive.spank.PacketFilter(this._presenceHandler.bind(this), function(packet) {
        return packet.getPacketType() == "presence" && packet.getType() == "unavailable";
    });
    connection.addPacketFilter(this.presenceFilter);
}

org.jive.spank.chat.Manager.prototype = {
    _createMessageHandler: function() {
        var manager = this;
        return function(message) {
            manager._handleMessage(message);
        }
    },
    _createMessageFilter: function() {
        return function(packet) {
            return packet.getPacketType() == "message" && packet.getType() == "chat" && packet.getBody();
        }
    },
    _presenceHandler: function(packet) {
        // If the user sends an unavailable from the resource we are chatting with we want to revert
        // to undefined for the resource
        var chatSession = this._chatSessions.find(function(session) {
            return session.sessionMatches(packet.getFrom(), null, true);
        });
        if (!chatSession || this.servers[packet.getFrom().getDomain()]) {
            return;
        }
        var bareJID = chatSession.getJID().getBareJID();
        chatSession.getJID = function() {
            return bareJID;
        };
    },
    _handleMessage: function(message) {
        //console.debug("Handling message: " +  message.getID());

        var chatSession = this._chatSessions.find(function(session) {
            return session.sessionMatches(message.getFrom(), message.getThread());
        }); 

        if (!chatSession) {
            chatSession = this.createSession(message.getFrom(), (this.shouldUseThreads ?
                                                                 message.getThread() : null));
        }
        chatSession._handleMessage(message);
                
    },
/**
 * A chat session listener listens for new chat sessions to be created on the connection.
 *
 * @param {Function} listener called when a new session is created with the manager and session as
 * parameters.
 */
    addChatSessionListener: function(listener) {
        this._chatSessionListeners.push(listener);
    },
/**
 * Removes a chat session listener from the manager.
 *
 * @param {Function} listener the listener being removed.
 */
    removeChatSessionListener: function(listener) {
        if (!listener) {
            return;
        }
        var index = this._chatSessionListeners.indexOf(listener);
        if (index >= 0) {
            this._chatSessionListeners.splice(index, 1);
        }
    },
/**
 * Closes a chat session.
 *
 * @param {org.jive.spank.chat.Session} session the session being closed.
 */
    closeChatSession: function(session) {
        if (!session) {
            return;
        }

        var index = this._chatSessions.indexOf(session);
        if (index < 0) {
            return;
        }

        this._chatSessions.splice(index, 1);
        delete session._messageListeners;
        this._fireChatSessionClosed(session);
    },
    _fireNewChatSessionCreated: function(session) {
        var manager = this;
        this._chatSessionListeners.each(function(listener) {
            if (listener.created) {
                listener.created(manager, session);
            }
        });
    },
    _fireChatSessionClosed: function(session) {
        var manager = this;
        this._chatSessionListeners.each(function(listener) {
            if (listener.closed) {
                listener.closed(manager, session);
            }
        });
    },
/**
 * Returns a chat session given a jid and a thread that uniquely identify a session. The thread parameter is
 * optional and only utilized if threads are enabled.
 *
 * @param {XMPP.JID} jid the jid for which to find the releated chat session.
 * @param {String} thread (optional) the thread for which to find the related chat session.
 */
    getSession: function(jid, thread) {
        return this._chatSessions.find(function(session) {
            return session.sessionMatches(jid, thread);
        });
    },
    createSession: function(jid, thread) {
        if (!jid) {
            throw new Error("JID must be specified.");
        }
        if (!thread && this.shouldUseThreads) {
            thread = this._createThreadID();
        }

        var session = new org.jive.spank.chat.Session(this, jid, thread);
        this._chatSessions.push(session);
        this._fireNewChatSessionCreated(session);
        return session;
    },
    registerDomain: function(domain, shouldMatchFullJID) {
        this.servers[domain] = shouldMatchFullJID;
    },
    _createThreadID: function() {
        return this._baseID + this._threadID++;
    },
    destroy: function() {
        for (var i = 0; i < this._chatSessions.length; i++) {
            this.closeChatSession(this._chatSessions[i]);
        }
        this._chatSessions.clear();

        this._chatSessionListeners.clear();
        delete this._chatSessionListeners;
        this.getConnection = Prototype.emptyFunction;
    }
}

org.jive.spank.chat.Session = function(manager, jid, thread) {
    this.getJID = function() {
        return jid;
    };
    this.getThread = function() {
        return thread;
    };
    this.getManager = function() {
        return manager;
    };

    this._messageListeners = new Array();
}

org.jive.spank.chat.Session.prototype = {
    getJIDString: function() {
        if (this.getManager().servers[this.getJID().getDomain()]) {
            return this.getJID().toString();
        }
        else {
            return this.getJID().toBareJID();
        }
    },
    sessionMatches: function(jid, thread, matchFullJID) {
        var jidMatches;
        if (this.getManager().servers[jid.getDomain()] || matchFullJID) {
            jidMatches = jid.toString() == this.getJID().toString();
        }
        else {
            jidMatches = jid.toBareJID() == this.getJID().toBareJID();
        }

        if (this.getManager().shouldUseThreads && thread) {
            return jidMatches && this.getThread() == thread;
        }
        else {
            return jidMatches;
        }
    },
    addListener: function(listener) {
        if (!listener) {
            return;
        }
        this._messageListeners.push(listener);
    },
    _handleMessage: function(message) {
        var session = this;
        var jid = message.getFrom();
        
        this.getJID = function() {
            return jid;
        }
        
        this._messageListeners.each(function(listener) {      
            if (listener.messageRecieved) {
                listener.messageRecieved(session, message);
            }
        });
    },
    sendMessage: function(messageBody, message) {
        if (!message) {
            message = new XMPP.Message("chat", this.getManager().getConnection()._jid,
                    this.getJID());
        }
        else {
            message.setTo(this.getJID());
            message.setType("chat");
            message.setBody(messageBody);
        }
        message.setBody(messageBody);
        message.setThread(this.getThread());

        this.getManager().getConnection().sendPacket(message);
    }
}

/**
 * A listener utilized by the ChatManager to notify interested parties when a new ChatSession is
 * created or destroyed.
 *
 * @param {Function} sessionCreated called when a new chat session is created.
 * @param {Function} sessionClosed called when a caht session is closed.
 */
org.jive.spank.chat.ChatSessionListener = function(sessionCreated, sessionClosed) {
    this.created = sessionCreated;
    this.closed = sessionClosed;
}

org.jive.spank.presence = {};

/**
 * Creates a presence manager. The presence manager class handles the user's presence, and
 * also keeps track of all presences recieved from remote users. Presence interceptors can
 * be added in order to add extensions to sent presence packets.
 *
 * @param {XMPPConnection} connection the connection on which this presence manager will use.
 * @param {XMPP.JID} all packets originating from this manager will be sent to this JID if they
 * do not already have a to set on them.
 * @param {String} the mode to process subscriptions, accept, reject, or manual.
 */
org.jive.spank.presence.Manager = function(connection, jid, subscriptionMode) {
    if (!connection || !(connection instanceof XMPPConnection)) {
        throw Error("Connection required for the presence manager.");
    }

    this.getConnection = function() {
        return connection;
    }

    var self = this;
    connection.addConnectionListener({
        connectionClosed: function() {
            self.destroy();
        }
    });

    if (!jid) {
        this._presencePacketFilter = new org.jive.spank.PacketFilter(this._createPresencePacketHandler(),
                function(packet) {
                    return packet.getPacketType() == "presence";
                });
    }
    else {
        this._presencePacketFilter = new org.jive.spank.PacketFilter(this._createPresencePacketHandler(),
                function(packet) {
                    return packet.getPacketType() == "presence" && packet.getFrom().toBareJID() == jid.toBareJID();
                });
    }

    connection.addPacketFilter(this._presencePacketFilter);
    this._presenceListeners = new Array();
    this._presence = {};
    this._jid = jid;
    this.mode = subscriptionMode;
}

org.jive.spank.presence.Manager.prototype = {
/**
 * Sends a presence packet to the server
 *
 * @param {XMPP.Presence} presence
 */
    sendPresence: function(presence) {
        if (!presence) {
            presence = new XMPP.Presence();
        }
        if (!presence.getTo() && this._jid) {
            presence.setTo(this._jid.toString());
        }
        this.getCurrentPresence = function() {
            return presence;
        };
        this.getConnection().sendPacket(presence);
    },
/**
 * The subscription mode will allow for the default handling of subscription packets, either
 * accepting all, rejecting all, or manual. The default mode is manual.
 * @param {String} mode can be either accept, reject, or manual.
 */
    setSubscriptionMode: function(mode) {
        this.mode = mode;
    },
    addPresenceListener: function(presenceListener) {
        if (!presenceListener || !(presenceListener instanceof Function)) {
            throw Error("Presence listener must be function");
        }
        this._presenceListeners.push(presenceListener);
    },
    getHighestResource: function(jid) {
        var bareJID = jid.toBareJID();
        if (!this._presence[bareJID]) {
            return null;
        }

        var highest;
        for (var resource in this._presence[bareJID].resources) {
            var presence = this._presence[bareJID].resources[resource];
            if (!highest || presence.getPriority() >= highest.getPriority) {
                highest = presence;
            }
        }
        return highest;
    },
    getPresence: function(jid) {
        if (!jid.getResource()) {
            return this.getHighestResource(jid);
        }
        var bareJID = jid.toBareJID();
        if (!this._presence[bareJID]) {
            return null;
        }
        else {
            return this._presence[bareJID].resources[jid.getResource()];
        }
    },
    _createPresencePacketHandler: function() {
        var manager = this;
        return function(presencePacket) {
            manager._handlePresencePacket(presencePacket);
        }
    },
    _handlePresencePacket: function(presencePacket) {
        var type = presencePacket.getType();
        if (type == "available" || type == "unavailable") {
            var jid = presencePacket.getFrom();
            var bareJID = jid.toBareJID();
            if (!this._presence[bareJID] && type == "available") {
                this._presence[bareJID] = {};
                this._presence[bareJID].resources = {};
            }
            else if (!this._presence[bareJID]) {
                return;
            }
            var resource = jid.getResource();
            if (type == "available") {
                this._presence[jid.toBareJID()].resources[resource] = presencePacket;
            }
            else {
                delete this._presence[jid.toBareJID()].resources[resource];
            }
        }
        else if ((presencePacket.getType() == "subscribe"
                || presencePacket.getType() == "unsubscribe")
                && (this.mode == "accept" || this.mode == "reject")) {
            var response = new XMPP.Presence(presencePacket.getFrom());
            response.setType((this.mode == "accept" && presencePacket.getType() != "unsubscribe"
                    ? "subscribed" : "unsubscribed"));
            this.getConnection().sendPacket(response);
        }
        if (!this._presenceListeners) {
            return;
        }
        this._presenceListeners.each(function(presenceListener) {
            presenceListener(presencePacket);
        });
    },
    setJID: function(jid) {
        this._jid = jid;
    },
    destroy: function() {
        delete this._presence;
        if (this.getConnection()) {
            this.getConnection().removePacketFilter(this._presencePacketFilter);
        }
        this.getConnection = Prototype.emptyFunction;
        delete this._presenceListeners;
    }
}

org.jive.spank.roster = {};

/**
 * Creates a roster, the appropriate listeners will then be registered with the XMPP Connection.
 * After the listeners are established, the user roster is requested and the users intial presence
 * is sent.
 *
 * @param {XMPPConnection} connection the XMPP connection which this roster will use.
 * @param {Function} onLoadCallback an optional callback which will be called when the roster is
 * loaded.
 * @param {org.jive.spank.presence.Manager} Specify a custom presence manager for the roster, if one
 * is not provided it will be created.
 */
org.jive.spank.roster.Manager = function(connection, onLoadCallback, presenceManager) {
    if (!connection || !(connection instanceof XMPPConnection)) {
        throw Error("Connection required for the roster manager.");
    }

    var self = this;
    connection.addConnectionListener({
        connectionClosed: function() {
            self.destroy();
        }
    });
    this.getConnection = function() {
        return connection;
    }

    this.rosterPacketFilter = new org.jive.spank.PacketFilter(this._rosterPacketHandler(),
            this._createRosterPacketFilter);

    connection.addPacketFilter(this.rosterPacketFilter);

    if (!presenceManager) {
        presenceManager = new org.jive.spank.presence.Manager(connection);
    }

    this.onLoadCallback = onLoadCallback;
    var rosterPacket = new org.jive.spank.roster.Packet();
    this._initialRequestID = rosterPacket.getID();
    connection.sendPacket(rosterPacket);

    this.rosterListeners = new Array();
}

org.jive.spank.roster.Manager.prototype = {
    getRoster: function() {
        return this._roster;
    },
    _rosterPacketHandler: function() {
        var manager = this;
        return function(rosterPacket) {
            manager._handleRosterPacket(
                    new org.jive.spank.roster.Packet(null, null, null, rosterPacket.rootNode.cloneNode(true)));
        }
    },
    _createRosterPacketFilter: function(packet) {
        var query = packet.getExtension("query");
        return query != null && query.namespaceURI == "jabber:iq:roster";
    },
    _handleRosterPacket: function(rosterPacket) {
        //console.debug("Roster packet recieved " +  rosterPacket.getID());

        if (rosterPacket.getID() == this._initialRequestID) {
            this._handleInitialResponse(rosterPacket);
        }
        else if (rosterPacket.getType() == "set") {
            this._handleRosterAdd(rosterPacket, true);
        }
    },
    _handleInitialResponse: function(rosterPacket) {
        this._roster = {};
        this._users = {};
        this._handleRosterAdd(rosterPacket, false);
        if (this.onLoadCallback && this.onLoadCallback instanceof Function) {
            this.onLoadCallback(this);
            this.onLoadCallback = Prototype.emptyFunction;
        }

        presenceManager.sendPresence();
    },
    _handleRosterAdd: function(rosterPacket, shouldFireListeners) {
        var items = rosterPacket.getItems();
        var roster = this._roster;
        var users = this._users;
        var added = new Array();
        var removed = new Array();
        var updated = new Array();
        // TODO refactor this to make it a bit nicer
        items.each(function(item) {
            var jid = item.getJID().toBareJID();
            
            if (item.getSubscription() == "remove") {
                item = users[jid];
                if (!item) {
                    return;
                }
                delete users[jid];
                if (roster["Unfiled"] && roster["Unfiled"][item.getName()]) {
                    delete roster["Unfiled"][item.getJID().toString()];
                }
                var groups = item.getGroups();
                for (var i = 0; i < groups.length; i++) {
                    var group = groups[i];
                    if (!roster[group]) {
                        continue;
                    }
                    delete roster[group][item.getJID().toString()];
                }
                removed.push(item);
                return;
            }
            var isUpdated = false;
            var isAdded = false;
            // Delete any of the users old groups...
            var oldItem;
            if (users[jid]) {
                oldItem = users[jid];
                var oldGroups = oldItem.getGroups();
                var groups = item.getGroups();
                for (var i = 0; i < oldGroups.length; i++) {
                    var group = groups[i];
                    if (groups.indexOf(oldGroups[i]) < 0 && roster[group]) {
                        if (!isUpdated) {
                            isUpdated = true;
                            updated.push(item);
                        }
                        delete roster[group][oldItem.getJID().toString()];
                    }
                }
            }
            else {
                isAdded = true;
                added.push(item);
            }

            if (!isUpdated && !isAdded
                    && (oldItem.getName() != item.getName()
                    || oldItem.getSubscription() != item.getSubscription())) {
                isUpdated = true;
                updated.push(item);
            }
            users[jid] = item;

	    if (jid.indexOf("@") > -1) {		// ignore transports & gateways
		    var groups = item.getGroups();
		    for (var i = 0; i < groups.length; i++) {
			var group = groups[i];
			if (!roster[group]) {
			    roster[group] = {};
			}
			if (!roster[group][item.getJID().toString()] && !isUpdated && !isAdded) {
			    isUpdated = true;
			    updated.push(item);
			}
			roster[group][item.getJID().toString()] = item;
		    }

		    // No groups, add to unfiled.
		    if (groups.length == 0) {
			if (!roster["Unfiled"]) {
			    roster["Unfiled"] = {};
			}
			if (!roster["Unfiled"][item.getJID().toString()] && !isUpdated && !isAdded) {
			    isUpdated = true;
			    updated.push(item);
			}
			roster["Unfiled"][item.getJID().toString()] = item;
		    }
	    }            
        });
        if (shouldFireListeners) {
            this._fireRosterUpdates(added, updated, removed);
        }
    },
    _fireRosterUpdates: function(added, updated, removed) {
        this.rosterListeners.each(function(listener) {
        
alert(added.length    + " " + updated.length);

            if (added.length > 0 && listener.onAdded) {
                listener.onAdded(added);
            }
            if (updated.length > 0 && listener.onUpdated) {
                listener.onUpdated(updated);
            }
            if (removed.length > 0 && listener.onRemoved) {
                listener.onRemoved(removed);
            }
        });
    },
    addEntry: function(jid, name, groups) {
        var packet = new org.jive.spank.roster.Packet("set");
        var item = packet.addItem(jid, name);
        if (groups) {
            item.addGroups(groups);
        }

        //console.debug("adding contact: " +  packet.doc.documentElement);
        this.getConnection().sendPacket(packet);

        var presence = new XMPP.Presence(jid);
        presence.setType("subscribe");
        this.getConnection().sendPacket(presence);
    },
    removeEntry: function(jid) {
        var packet = new org.jive.spank.roster.Packet("set");
        var item = packet.addItem(jid);
        item.setSubscription("remove");

        //console.debug("removing roster entry: " +  packet.doc.documentElement);
        this.getConnection().sendPacket(packet);
    },
/**
 * Adds a roster listener.
 *
 * @param {Object} rosterListener contains onAdded, onUpdated, and onRemoved
 */
    addRosterListener: function(rosterListener) {
        this.rosterListeners.push(rosterListener);
    },
/**
 * Removes a roster listener.
 *
 * @param {Object} rosterListener the listener to remove.
 */
    removeRosterListener: function(rosterListener) {
        if (!rosterListener) {
            return;
        }

        var index = this.rosterListeners.indexOf(rosterListener);
        if (index >= 0) {
            this.rosterListeners.splice(index, 1);
        }
    },
    destroy: function() {
        this.rosterListeners.clear();
        this.getConnection = Prototype.emptyFunction;
        delete this._roster;
        delete this._users;
        delete this._handleRosterAdd;
        this.onLoadCallback = Prototype.emptyFunction;
        delete this._initialRequestID;
    }
}


org.jive.spank.disco = {
    _connections: [],
/**
 * Retrieves a singleton for the connection which is the service discovery manager.
 *
 * @param {XMPP.Connection} connection the connection to retrieve the singleton for.
 */
    getManager: function(connection) {
        var discoManager = org.jive.spank.disco._connections.detect(
                function(connection, discoManager) {
            return discoManager._connection == connection;
        }.bind(null, connection));

        if (discoManager == null) {
            discoManager
                    = new org.jive.spank.disco.Manager(connection);
            org.jive.spank.disco._connections.push(discoManager);
            connection.addConnectionListener({
                connectionClosed: function() {
                    var index = org.jive.spank.disco._connections.indexOf(this);

                    if (index >= 0) {
                        org.jive.spank.disco._connections.splice(index, 1);
                    }
                }.bind(discoManager)
            });
        }
        return discoManager;
    }
}

/**
 * The disco manager manages service discovery on an XMPP Connection.
 *
 * @param {XMPP.Connection} connection the connection which this service discovery manager will
 * handle disco for.
 */
org.jive.spank.disco.Manager = function(connection) {
    this._connection = connection;
    var self = this;
    this.features = new Array();
    this.infoCache = {};
    var discoFilter = new org.jive.spank.PacketFilter(function(request) {
        self._handleDiscoResquest(request);
    }, function(packet) {
        return packet.getPacketType() == "iq" && packet.getType() == "get" && packet.getQuery()
                && packet.getQuery().getAttribute("xmlns") ==
                   "http://jabber.org/protocol/disco#info";
    });
    connection.addPacketFilter(discoFilter);
    connection.addConnectionListener({
        connectionClosed: function() {
            self.destroy();
        }
    });
}

org.jive.spank.disco.Manager.prototype = {
    getCategory: function(jid, shouldClearCache) {
        var discoverPacket = this.infoCache[jid.toString()];
        if (!discoverPacket || shouldClearCache) {
            this.discoverInfo(null, jid);
            return null;
        }

        if (discoverPacket.getType() != "result") {
            return null;
        }

        var query = discoverPacket.getExtension("query");
        var info = query.childNodes;
        for (var i = 0; i < info.length; i++) {
            if (info[i].tagName == "identity") {
                return info[i].getAttribute("category");
            }
        }
        return null;
    },
    hasFeature: function(jid, feature, callback, shouldClearCache) {
        var discoverPacket = this.infoCache[jid.toString()];
        if (!discoverPacket || shouldClearCache) {
            var infoCallback = callback;
            if(callback) {
                infoCallback = function(jid, feature, callback, discoverPacket) {
                    callback(this._hasFeature(discoverPacket, feature), jid, feature);
                }.bind(this, jid, feature, callback);
            }
            this.discoverInfo(infoCallback, jid);
            return false;
        }

        if (discoverPacket.getType() != "result") {
            return false;
        }

        var hasFeature = this._hasFeature(discoverPacket, feature);
        if(callback) {
            callback(hasFeature, jid, feature)
        }

        return hasFeature
    },
    _hasFeature: function(discoverPacket, feature) {
        var query = discoverPacket.getExtension("query");
        var info = query.childNodes;
        for (var i = 0; i < info.length; i++) {
            if (info[i].tagName == "feature") {
                if (info[i].getAttribute("var") == feature) {
                    return true;
                }
            }
        }
        return false;
    },
    discoverInfo: function(infoCallback, jid, node) {
        var getInfo = new XMPP.IQ("get", this._connection._jid, jid.toString());
        var id = getInfo.getID();

        var query = getInfo.setQuery("http://jabber.org/protocol/disco#info");
        if (node) {
            query.setAttribute("node", node);
        }

        this._connection.sendPacket(getInfo, new org.jive.spank.PacketFilter(
                function(packet) {
                    this.infoCache[jid.toString()] = packet;
                    if (infoCallback) {
                        infoCallback(packet);
                    }
                }.bind(this), function(packet) {
            return packet.getID() == id;
        }));
    },
    
    discoverItems: function(itemsCallback, jid, node) {
        if (!itemsCallback) {
            return;
        }
        var getInfo = new XMPP.IQ("get", this._connection._jid, jid.toString());
        var id = getInfo.getID();

        var query = getInfo.setQuery("http://jabber.org/protocol/disco#items");
        if (node) {
            query.setAttribute("node", node);
        }

        this._connection.sendPacket(getInfo, new org.jive.spank.PacketFilter(
                function(packet) {

                    if (itemsCallback) {
                        itemsCallback(packet);
                    }
                }.bind(this), function(packet) {
            return packet.getID() == id;
        }));
    },
    _processItemsResponse: function(itemsResponse) {
        var query = itemsResponse.getExtension("query");
        var items = query.childNodes;
        var itemList = [];
        for (var i = 0; i < items.length; i++) {
            var jid = items[i].getAttribute("jid");
            var name = items[i].getAttribute("name");
            if (!jid || !name) {
                continue;
            }

            itemList.push({jid: new XMPP.JID(jid), name: name});
        }
        return itemList;
    },
    addFeature: function(feature) {
        this.features.push(feature);
    },
    removeFeature: function(feature) {
        var index = this.features.indexOf(feature);
        if (index >= 0) {
            this.features.splice(index, 1);
        }
    },
    _handleDiscoResquest: function(get) {
        var result = new XMPP.IQ("result", this._connection._jid, get.getFrom());
        result.setID(get.getID());
        var query = result.setQuery("http://jabber.org/protocol/disco#info");
        var identity = query.appendChild(result.doc.createElement("identity"));
        identity.setAttribute("category", "client");
        identity.setAttribute("name", "spank");
        identity.setAttribute("type", "web");

        for (var i = 0; i < this.features.length; i++) {
            var feature = this.features[i];
            var featureNode = query.appendChild(result.doc.createElement("feature"));
            featureNode.setAttribute("var", feature);
        }
        this._connection.sendPacket(result);
    },
    destroy: function() {
        this.infoCache = {};
        var index = org.jive.spank.disco._connections.indexOf(this);
        if (index >= 0) {
            org.jive.spank.disco._connections.splice(index, 1);
        }
    }
}

org.jive.spank.muc = {};

/**
 * The multi-user chat manager has functions for room creation, adding and removing invitation
 * listeners, and retrieving multi-user chat conference servers and rooms from the XMPP server.
 *
 * @param {XMPPConnection} connection the XMPPConnection which this manager utilizes for its
 * communications.
 * @param {org.jive.spank.chat.Manager} chatManager the chat manager is used for private chats
 * originating inside of MUC rooms.
 */
org.jive.spank.muc.Manager = function(connection, chatManager) {
    this._connection = connection;
    var self = this;
    connection.addConnectionListener({
        connectionClosed: function() {
            self.destroy();
        }
    });
    this.invitationListeners = new Array();
    this.rooms = new Array();
    this.chatManager = chatManager;
    org.jive.spank.disco.getManager(connection).addFeature("http://jabber.org/protocol/muc");
}

org.jive.spank.muc.Manager.prototype = {
/**
 * Returns a list of conference servers operating on the server. If the server argument is
 * not specifed the currently connected server is used.
 *
 * @param {Function} serversCallback the function that is called with the server list when
 * the response is recieved.
 * @param {XMPP.JID} server (optional) the server to retrieve the list of conference servers
 * from.
 */
    getConferenceServers: function(serversCallback, server) {
        if (!server) {
            server = new XMPP.JID(this._connection.domain);
        }

        var infoCallback = function(infoResponse) {
            var query = infoResponse.getExtension("query");
            var infoNodes = query.childNodes;
            for (var i = 0; i < infoNodes.length; i++) {
                var info = infoNodes[i];
                if (info.tagName == "feature") {
                    if (info.getAttribute("var") == "http://jabber.org/protocol/muc") {
                        serversCallback(infoResponse.getFrom());
                        return;
                    }
                }
            }
        };

        var discoManager = org.jive.spank.disco.getManager(this._connection);

        var callback = function(infoCallback, discoManager, items) {
            var itemJids = items.pluck("jid");
            var discoverInfo = discoManager.discoverInfo.bind(discoManager, infoCallback);
            itemJids.each(discoverInfo)
        }.bind(this, infoCallback, discoManager);

        discoManager.discoverItems(callback, server);
    },
/**
 * Retrieves a list of rooms from a MUC service. To receive the info on the rooms pass
 * the returned structure to #retrieveRoomsInfo.
 *
 * @param {XMPP.JID} serviceJID the jid of the service for which to retrieve rooms.
 * @param {Function} roomsCallback the callback called with the rooms as its argument when the
 * server returns the rooms response.
 */
    retrieveRooms: function(serviceJID, roomsCallback) {
        if (!serviceJID || !roomsCallback) {
            return;
        }

        var callback = function(items) {
            var itemList = {};
            for (var i = 0; i < items.length; i++) {
                var jid = items[i].jid.toString();
                var name = items[i].name;

                itemList[jid] = {
                    name: name
                };
            }
            roomsCallback(itemList);
        }

        org.jive.spank.disco.getManager(this._connection).discoverItems(callback, serviceJID);
    },
    retrieveRoomsInfo: function(rooms, roomsCallback) {
        if (!rooms || !roomsCallback) {
            return;
        }

        var count = 0;

        var callback = function(callback, infoResponse) {
            var jid = infoResponse.getFrom().toString();
            var query = infoResponse.getExtension("query");
            var info = query.childNodes;
            var room = rooms[jid];
            if (room) {
                for (var i = 0; i < info.length; i++) {
                    if (info[i].tagName == "feature") {

                    }
                    else if (info[i].tagName == "x") {
                        var xdata = new XMPP.XData(null, info[i]);
                        var fields = xdata.getFields();
                        for (var j = 0; j < fields.length; j++) {
                            var field = fields[j].variable;
                            rooms[jid][field] = {};
                            for (var value in fields[j]) {
                                rooms[jid][field][value] = fields[j][value];
                            }
                        }
                    }
                }
            }
            if (--count <= 0) {
                callback(rooms);
            }
        }.bind(null, roomsCallback);

        var hasRooms = false;
        for (var room in rooms) {
            count++;
            hasRooms = true;
            org.jive.spank.disco.getManager(this._connection).discoverInfo(callback, room);
        }

        if(!hasRooms) {
            roomsCallback({});
        }
    },
/**
 * Retrieves a conference room to be joined.
 *
 * @param {XMPP.JID} roomJID the jid of the room to be created.
 */
    createRoom: function(roomJID) {
        this.chatManager.registerDomain(roomJID.getDomain(), true);
        return new org.jive.spank.muc.Room(this, roomJID);
    },
    getRoom: function(roomJID) {
        return this.rooms.detect(function(value, index) {
            return value.jid.toString() == roomJID.toString();
        });
    },
    _addRoom: function(room) {
        this.rooms.push(room);
    },
    _removeRoom: function(room) {
        if (!room) {
            return;
        }
        var index = this.rooms.indexOf(room);
        if (index >= 0) {
            this.rooms.splice(index, 1);
        }
    },
    addInvitationsListener: function(invitationListener) {
        if (!(invitationListener instanceof Function)) {
            throw Error("invitation listener must be a function.");
        }
        var invitationListeners = this.invitationListeners;
        if (this.invitationListeners.length <= 0) {
            this.invitationFilter = new org.jive.spank.PacketFilter(function(packet) {
                var userPacket = new org.jive.spank.muc.User(null, null, packet.rootNode.cloneNode(true));
                var invitation = userPacket.getInvite();

                if (invitation) {
                    invitationListeners.each(function(listener) {
                        listener(invitation);
                    });
                }
            },
                    function(packet) {
                        if (packet.getPacketType() != "message") {
                            return false;
                        }
                        var ex = packet.getExtension("x");
                        if (!ex) {
                            return false;
                        }

                        return ex.getAttribute("xmlns") == "http://jabber.org/protocol/muc#user";
                    });
            this._connection.addPacketFilter(this.invitationFilter);
        }
        this.invitationListeners.push(invitationListener);
    },
    removeInvitationsListener: function(invitationListener) {
        if (!invitationListener || !(invitationListener instanceof Function)) {
            throw Error("listeners must be a function");
        }

        var index = this.invitationListeners.indexOf(invitationListener);
        if (index >= 0) {
            this.invitationListeners.splice(index, 1);
        }
        if (this.invitationListeners.size() <= 0 && this.invitationFilter) {
            this._connection.removePacketFilter(this.invitationFilter);
            delete this.invitationFilter;
        }
    },
    declineInvitation: function(from, room, reason) {
        if (!room || !from) {
            throw Error("Cannot decline invitation, invitiation missing information");
        }
        var packet = new org.jive.spank.muc.User(room);
        packet.setDecline(from, reason);
        this._connection.sendPacket(packet);
    },
    destroy: function() {
        for (var i = 0; i < this.rooms.length; i++) {
            this.rooms[i].leave(true);
        }
        this.rooms.clear();
        this.invitationListeners.clear();
    },
    
    sendRoomPresence: function(presence) {
    
        for (var i = 0; i < this.rooms.length; i++) {
            this.rooms[i].sendRoomPresence(presence);
            log("Sending room presence to " + this.rooms[i].jid);
        }

    }    
}

org.jive.spank.muc.Room = function(manager, roomJID) {
    this.manager = manager;
    this.connection = manager._connection;
    this.jid = roomJID;
    this.listeners = {};
    this.presenceListeners = new Array();
    this.messageListeners = new Array();
    this.isJoined = false;
}

org.jive.spank.muc.Room.prototype = {
/**
 * Joins a MultiUserChat room by sending an available presence.
 *
 * @param {String} nickname
 * @param {String} password
 * @param {Function} joinCallback onSuccess is called if the room is joined successfully
 * and onError is called if it is not.
 * @param {Function} occupantListener
 */
    join: function(nickname, password, joinCallback, messageListener, occupantListener) {
        var roomJID = this.jid;
        var presence = new XMPP.Presence(roomJID.toString() + "/" + nickname);
        var mucExtension = presence.addExtension("x", "http://jabber.org/protocol/muc");
        if (password) {
            var passwordElement = mucExtension.appendChild(
                    presence.doc.createElement("password"));
            passwordElement.appendChild(presence.doc.createTextNode(password));
        }
        this._initPresenceManager(nickname, occupantListener);
        if (messageListener) {
            this._initMessageListener(messageListener);
        }
        var packetFilter;
        if (joinCallback && (joinCallback.onSuccess || joinCallback.onError)) {
            var room = this;
            packetFilter = new org.jive.spank.PacketFilter(function(packet) {
                if (packet.getError() && joinCallback.onError) {
                    this.presenceManager.destroy();
                    this.presenceManager = undefined;
                    this.connection.removePacketFilter(this.messageFilter);
                    joinCallback.onError(packet);
                }
                else if (!packet.getError() && joinCallback.onSuccess) {
                    this.manager._addRoom(this);
                    this.nickname = nickname;
                    room.occupantJid = new XMPP.JID(roomJID.toString() + "/" + nickname);
                    room.isJoined = true;
                    this.presenceManager._handlePresencePacket(packet);
                    joinCallback.onSuccess(new org.jive.spank.muc.Occupant(packet));
                }
                joinCallback = Prototype.emptyFunction;
            }.bind(this),
                    function(packet) {
                        return packet.getFrom().toString() == presence.getTo().toString();
                    });
        }
        this.connection.sendPacket(presence, packetFilter);
    },
    create: function(nickname, configuration, createCallback, messageListener, occupantListener) {
        var callback = {};
        if (createCallback.onSuccess) {
            callback.onSuccess = this._createSuccess.bind(this, createCallback.onSuccess,
                    configuration, messageListener, occupantListener);
        }
        this.join(nickname, null, callback);
    },
    _createSuccess: function(callback, configuration, messageListener, occupantListener, occupant) {
        var _handleConfigurationForm = function(occupant, configuration, callback, messageListener,
                                                occupantListener, room, configurationForm) {
            var answerForm = configurationForm.getAnswerForm();
            for (var answer in configuration) {
                answerForm.setAnswer(answer, [configuration[answer]]);
            }
            this.sendConfigurationForm(answerForm);
            this.addOccupantListener(occupantListener);
            this._initMessageListener(messageListener);
            callback(occupant);
            callback = Prototype.emptyFunction;
        };
        this.getConfigurationForm(_handleConfigurationForm.bind(this, occupant, configuration,
                callback, messageListener, occupantListener));
    },
    
    leave: function(shouldNotRemove) {
        this.isJoined = false;

        this.connection.removePacketFilter(this.messageFilter);
        delete this.connection;

        try {
            var presence = new XMPP.Presence();
            presence.setType("unavailable");
            this.presenceManager.sendPresence(presence);
        }
        catch(error) {
            // ohh well
        }

        this.presenceManager.destroy();
        this.presenceManager = undefined;
        if (!shouldNotRemove) {
            this.manager._removeRoom(this);
        }
    },
    
    sendRoomPresence: function(presence) {
        try {
             this.connection.sendPacket(presence);
        }
        catch(error) {
            // ohh well
        }
    },
    _initPresenceManager: function(nickname, occupantListener) {
        this.presenceManager = new org.jive.spank.presence.Manager(this.connection,
                new XMPP.JID(this.jid.toString() + "/" + nickname));
        if (occupantListener) {
            this.addOccupantListener(occupantListener);
        }
    },
    addOccupantListener: function(occupantListener) {
        var presenceListener = this._handlePresence.bind(this, occupantListener);
        this.presenceManager.addPresenceListener(presenceListener);
    },
    _handlePresence: function(occupantListener, presence) {
        if (presence.getError()) {
            return;
        }

        var user = new org.jive.spank.muc.User(null, null, presence.rootNode.cloneNode(true));
        var isLocalUser = user.getStatusCodes().indexOf('110');
        var isNickChange = user.getStatusCodes().indexOf('303');
        
//        if(isLocalUser && isNickChange) {
//            this.presenceManager.setJID()
//        }
        occupantListener(new org.jive.spank.muc.Occupant(presence));
    },
/**
 * Returns an array of all current occupants of the room.
 */
    getOccupants: function() {
        var occupants = new Array();
        var roomPresences = this.presenceManager._presence[this.jid];
        if (!roomPresences) {
            return occupants;
        }
        var resources = roomPresences.resources;
        for (var resource in resources) {
            var presence = resources[resource];
            if (presence.getType != "unavailable") {
                occupants.push(new org.jive.spank.muc.Occupant(presence));
            }
        }
        return occupants;
    },
    getOccupant: function(nick) {
        var userJID = this.jid + "/" + nick;

        var presence = this.presenceManager._presence[this.jid].resources[nick];
        if (presence == null || presence.getType() == "unavailable") {
            return null;
        }
        else {
            return new org.jive.spank.muc.Occupant(presence);
        }
    },
    _initMessageListener: function(messageListener) {
        var room = this;
        this.messageFilter = new org.jive.spank.PacketFilter(function(message) {
            room.messageListeners.each(function(listener) {
                var handled = false;
                if (message.getSubject() && listener.subjectUpdated) {
                    listener.subjectUpdated(room, message.getFrom(), message.getSubject());
                    handled = true;
                }
                var ex = message.getExtension("x");
                if (ex) {
                    var isUser = ex.getAttribute("xmlns") == "http://jabber.org/protocol/muc#user";
                    if (isUser && listener.invitationDeclined) {
                        var user = new org.jive.spank.muc.User(null, null, message.rootNode);
                        var decline = user.getDecline();
                        if (decline) {
                            listener.invitationDeclined(decline);
                            handled = true;
                        }
                    }
                }
                if (listener.messageReceived && !handled) {
                    listener.messageReceived(message);
                }
            });
        }, function(packet) {
            return packet.getFrom().toBareJID() == room.jid.toBareJID()
                    && packet.getPacketType() == "message" && (packet.getType()
                    == "groupchat" || packet.getType() == "normal");
        });

        this.connection.addPacketFilter(this.messageFilter);
        if (messageListener) {
            this.messageListeners.push(messageListener);
        }
    },
    sendMessage: function(messageBody, message) {
        if (!message) {
            message = new XMPP.Message("groupchat", this.connection, this.jid);
        }
        else {
            message.setType("groupchat");
            message.setFrom(this.jid);
        }
        message.setBody(messageBody);

        this.connection.sendPacket(message);
    },
    addMessageListener: function(messageListener) {
        if (!messageListener) {
            throw Error("listeners cannot be null");
        }
        this.messageListeners.push(messageListener);
    },
    removeMessageListener: function(messageListener) {
        if (!messageListener) {
            throw Error("listeners must be a function");
        }

        var index = this.messageListeners.indexOf(messageListener);
        if (index >= 0) {
            this.messageListeners.splice(index, 1);
        }
    },
    setSubject: function(subject) {
        var message = new XMPP.Message(this.jid);
        var ex = message.addExtension("subject");
        ex.appendChild(message.doc.createTextNode(subject));

        this.connection.sendPacket(message);
    },
    invite: function(jid, reason) {
        var invite = new org.jive.spank.muc.User(this.jid);
        invite.setInvite(jid, reason);
        this.connection.sendPacket(invite);
    },
    changeNickname: function(nickname) {
        if (!this.isJoined) {
            throw Error("Cannot change nickname if not in room.");
        }

        var presence = new XMPP.Presence();
        presence.setTo(new XMPP.JID(this.jid.toBareJID() + "/" + nickname));
        this.connection.sendPacket(presence);
        this.nickname = nickname;
    },
    getConfigurationForm: function(callback) {
        var iq = new XMPP.IQ("get", this.jid, this.jid.getBareJID());
        iq.setQuery("http://jabber.org/protocol/muc#owner");
        var packetFilter = org.jive.spank.PacketFilter.filter.IDFilter(function(response) {
            if (response.getExtension("x", "jabber:x:data")) {
                callback(this.jid.getBareJID(), new XMPP.XData(null,
                        response.getExtension("x", "jabber:x:data")));
            }
        }.bind(this), iq);
        this.connection.sendPacket(iq, packetFilter);
    },
    sendConfigurationForm: function(form, callback) {
        var iq = new XMPP.IQ("set", this.jid, this.jid.getBareJID());
        var query = iq.setQuery("http://jabber.org/protocol/muc#owner");

        var formNode = form.rootNode.cloneNode(true);
        if (iq.doc.importNode) {
            iq.doc.importNode(formNode, true);
        }
        query.appendChild(formNode);

        // trim the form
        var nodes = formNode.childNodes;
        for (var i = nodes.length - 1; i >= 0; i--) {
            if (!nodes[i].hasChildNodes()) {
                formNode.removeChild(nodes[i]);
            }
        }

        var packetFilter;
        if (callback) {
            packetFilter = org.jive.spank.PacketFilter.filter.IDFilter(function(response, jid) {
                callback(jid.getBareJID());
            }.bind(this), iq);
        }
        this.connection.sendPacket(iq, packetFilter);
    }
}

org.jive.spank.muc.Occupant = function(presence) {
    this.presence = presence;
}

org.jive.spank.muc.Occupant.prototype = {
    getAffiliation: function() {
        var user = this.presence.getExtension("x");
        if (user == null) {
            return null;
        }
        return user.firstChild.getAttribute("affiliation");
    },
    getRole: function() {
        var user = this.presence.getExtension("x");
        if (user == null) {
            return null;
        }
        return user.firstChild.getAttribute("role");
    },
    getNick: function() {
        return this.presence.getFrom().getResource();
    },
    getRoom: function() {
        return this.presence.getFrom().toBareJID();
    }
}

/**
 * When packets are recieved by spank they go through a packet filter in order for interested parties
 * to be able to recieve them.
 *
 * @param {Function} callback the callback to execute after the filter test is passed.
 * @param {Function} filterTest the test to see if the callback should be executed, if this parameter
 * is undefined then all packets will be accepted.
 */
org.jive.spank.PacketFilter = function(callback, filterTest) {
    if (!callback) {
        throw Error("Callback must be specified");
    }

    this.getFilterTest = function() {
        return filterTest
    };
    this.getCallback = function() {
        return callback
    };
}

org.jive.spank.PacketFilter.prototype = {
/**
 * Tests the packet using the filter test and passes it to the callback if it passes the
 * test.
 *
 * @param {Object} packet the packet to test and pass to the callback if the test passes
 * @return {Boolean} true if the callback was executed and false if it was not.
 */
    accept: function(packet) {
        if (!packet || !(packet instanceof XMPP.Packet)) {
            return;
        }
        var filterTest = this.getFilterTest();
        var executeCallback = true;
        if (filterTest) {
            executeCallback = filterTest(packet);
        }
        if (executeCallback) {
            var callback = this.getCallback();
            callback(packet);
        }
        return executeCallback;
    }
}

org.jive.spank.PacketFilter.filter = {
    IDFilter: function(callback, packet) {
        return new org.jive.spank.PacketFilter(callback, function(packet, testPacket) {
            return testPacket.getID() == packet.getID();
        }.bind(null, packet));
    }
}

org.jive.spank.x = {}

org.jive.spank.x.chatstate = {
    _connections: [],
/**
 * Retrieves a singleton for the connection which is the chat state manager.
 *
 * @param {org.jive.spank.chat.Manager} manager the chat manager to retrieve the singleton for.
 */
    getManager: function(manager) {
        var connection = manager.getConnection();
        var chatStateManager = org.jive.spank.x.chatstate._connections.detect(
                function(connection, chatStateManager) {
            return chatStateManager._connection == connection;
        }.bind(null, connection));

        if (chatStateManager == null) {
            chatStateManager
                    = new org.jive.spank.x.chatstate.Manager(manager);
            org.jive.spank.x.chatstate._connections.push(chatStateManager);
            connection.addConnectionListener({
                connectionClosed: function() {
                    var index = org.jive.spank.x.chatstate._connections.indexOf(this);

                    if (index >= 0) {
                        org.jive.spank.x.chatstate._connections.splice(index, 1);
                    }
                }.bind(chatStateManager)
            });
        }
        return chatStateManager;
    }
}

org.jive.spank.x.chatstate.Manager = function(manager) {
    this._manager = manager;
    this._connection = manager.getConnection();
    this._lastState = {};
    this._lastStateSent = {};
    this._stateListeners = new Array();
    var self = this;
    this._connection.addPacketFilter(new org.jive.spank.PacketFilter(this._handleIncomingState.bind(this),
            function(packet) {
                return packet.getPacketType() == "message"
                        && (packet.getType() == "chat" || packet.getType() == "groupchat")
                        && packet.getExtension(null, "http://jabber.org/protocol/chatstates")
                        && !packet.getExtension("x", "jabber:x:delay");
            }));
    manager.getConnection().addConnectionListener({
        connectionClosed: function() {
            self.destroy();
        }
    });
    org.jive.spank.disco.getManager(manager.getConnection()).addFeature("http://jabber.org/protocol/chatstates");
}

org.jive.spank.x.chatstate.Manager.prototype = {
    setCurrentState: function(chatState, jid, message, isMultiUserChat) {
        var created = false;
        if (!message) {
            message = new XMPP.Message((isMultiUserChat ? "groupchat" : "chat"), null, jid);
            created = true;
        }
        if (!isMultiUserChat && !this.shouldSendState(jid)) {
            if (created) {
                return null;
            }
            else {
                return message;
            }
        }
        chatState = (chatState ? chatState : "active");
        message.addExtension(chatState, "http://jabber.org/protocol/chatstates");
        this._lastStateSent[jid.toString()] = chatState;
        return message;
    },
    shouldSendState: function(jid) {
        if (this._lastState[jid.toString()]) {
            return this._lastState[jid.toString()];
        }
        // if there is no resource attached we cannot send an iq request so we have to operate
        // purely on whether or not we have already sent them a state at this point, if we have
        // then don't send them anything.
        if (!jid.getResource()) {
            return !this._lastStateSent[jid.toString()];
        }
        var disco = org.jive.spank.disco.getManager(this._connection);
        var category = disco.getCategory(jid);
        if (!category) {
            return false;
        }
        // This is a MUC we can send state
        else if (category == "conference") {
            return true;
        }

        return disco.hasFeature(jid, "http://jabber.org/protocol/chatstates");
    },
    addStateListener: function(stateListener) {
        if (stateListener && stateListener instanceof Function) {
            this._stateListeners.push(stateListener);
        }
    },
    removeStateListener: function(stateListener) {
        if (stateListener) {
            var i = this._stateListeners.indexOf(stateListener);
            if (i >= 0) {
                this._stateListeners.splice(i, 1);
            }
        }
    },
    _handleIncomingState: function(message) {
        var from = message.getFrom().toString();
        var extension = message.getExtension(null, "http://jabber.org/protocol/chatstates");
        this._lastState[from] = extension.tagName;
        for (var i = 0; i < this._stateListeners.length; i++) {
            this._stateListeners[i](message.getFrom(), extension.tagName, message.getType() == "groupchat");
        }
    },
    destroy: function() {

    }
}

var util = {
    Integer: {
        randomInt: function(intFrom, intTo, intSeed) {
            // Make sure that we have integers.
            intFrom = Math.floor(intFrom);
            intTo = Math.floor(intTo);

            // Return the random number.
            return(
                    Math.floor(
                            intFrom +
                            (
                                    (intTo - intFrom + 1) *

                                    // Seed the random number if a value was passed.
                                    Math.random(
                                            (intSeed != null) ? intSeed : 0
                                            )
                                    ))
                    );
        }
    },
    StringUtil: {
        randomString: function(len) {
            // Define local variables.
            var strLargeText = "";
            var intValue = 0;
            var arrCharacters = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ" ;

            // Loop over number of characters in string.
            for (var intI = 0; intI < len; intI++) {

                // Get a random value between 0 and the length of the
                // character list.
                intValue = util.Integer.randomInt(0, (arrCharacters.length - 1), intI);

                // Append a character that is randomly chosen
                strLargeText += arrCharacters.charAt(intValue);

            }
            return strLargeText;
        }
    }
};

XMPP.Packet = function() {
};

XMPP.Packet.packetID = 1;

XMPP.Packet.prototype = {
    idBase: util.StringUtil.randomString(5),
    _init: function(packetType, from, to, element) {
        this.doc = Sarissa.getDomDocument();
        var created = !element;
        if (!element) {
            element = this.doc.createElement(packetType);
        }
        // Fix for safari, IE6 doesn't support importNode but works
        // fine with just appendChild
        else if (!_SARISSA_IS_IE) {
            element = this.doc.importNode(element, true);
        }
        this.doc.appendChild(element);

        this.rootNode = this.doc.firstChild;
        if (created) {
            this.addAttributes({id: this._nextPacketID()});
            this.setFrom(from);
            this.setTo(to);
        }
    },
    getPacketType: function() {
        return this.rootNode.tagName;
    },
    getID: function() {
        return this.rootNode.getAttribute("id");
    },
    setID: function(id) {
        this.rootNode.setAttribute("id", id);
    },
    _nextPacketID: function() {
        return this.idBase + "-" + XMPP.Packet.packetID++;
    },
    removeAttributes: function(attributes) {
        for (var i = 0; i < attributes.length; i++) {
            this.rootNode.removeAttribute(attributes[i]);
        }
    },
    addAttributes: function(attributes) {
        for (var attr in attributes) {
            this.rootNode.setAttribute(attr, attributes[attr]);
        }
    },
    setFrom: function(fromValue) {
        if (!fromValue || fromValue == "") {
            this.removeAttributes($A("from"));
        }
        else {
            this.addAttributes({ from: fromValue });
        }
    },
    getFrom: function() {
        if (this.from) {
            return this.from;
        }
        var from = this.rootNode.getAttribute("from");
        if (!from) {
            this.from = null;
        }
        else {
            this.from = new XMPP.JID(from);
        }
        return this.from;
    },
    setTo: function(toValue) {
        this.to = null;
        if (!toValue || toValue == "") {
            this.removeAttributes($A("to"));
        }
        else {
            this.addAttributes({ to: toValue });
        }
    },
/**
 * Returns the JID of the user to whom this packet is being directed.
 *
 * @return {XMPP.JID} the JID of the user to whom this packet is being directed.
 */
    getTo: function() {
        if (this.to) {
            return this.to;
        }
        var to = this.rootNode.getAttribute("to");
        if (!to) {
            this.to = null;
        }
        else {
            this.to = new XMPP.JID(to);
        }
        return this.to;
    },
/**
 * Sets the namespace of the packet.
 * NOTE: Opera requires that the namespace of an element be set when it is created
 * so this method will not work in opera.
 *
 * @param {String} xmlnsValue the namespace to be set on the packet.
 */
    setXMLNS: function(xmlnsValue) {
        if (!xmlnsValue || xmlnsValue == "") {
            this.removeAttributes($A("xmlns"));
        }
        else {
            this.addAttributes({ xmlns: xmlnsValue });
        }
    },
/**
 * Serializes the packet to a string.
 */
    toXML: function() {
        var xml = this.doc.xml ? this.doc.xml
                : (new XMLSerializer()).serializeToString(this.doc);
        if (xml.indexOf('<?xml version="1.0"?>') >= 0) {
            // 'fix' for opera so that it doesn't pass this along.
            xml = xml.substr('<?xml version="1.0"?>'.length);
        }
        return xml;
    },
   
    getDoc: function() {

        return this.doc;
    },

    
/**
 * Creates and adds an extension to the packet, returning the created extension.
 *
 * @param {String} extensionName the name of the extension that is being created.
 * @param {String} extensionNamespace (optional) the namespace of the extension that is
 * being created
 */
    addExtension: function(extensionName, extensionNamespace) {
        if (extensionNamespace && this.doc.createElementNS) {
            this.extension = this.rootNode.appendChild(
                    this.doc.createElementNS(extensionNamespace,
                            extensionName));
        }
        else {
            this.extension = this.rootNode.appendChild(
                    this.doc.createElement(extensionName));
        }
        if (extensionNamespace) {
            this.extension.setAttribute("xmlns", extensionNamespace);
        }

        return this.extension;
    },
    addTextExtension: function(textNodeName, textNodeContent) {
        var textNode = this.addExtension(textNodeName);
        textNode.appendChild(this.doc.createTextNode(textNodeContent));
    },
/**
 * Returns the first packet extension that matches the given arguments. Note that this method returns the
 * actual element inside of the document and not a clone. Both arguments are
 * optional, and, if none match null is returned.
 *
 * @param {String} extensionName the name of the extension that is to be returned.
 * @param {String} namespace the namespace of extension that is to be returned.
 */
    getExtension: function(extensionName, namespace) {
        var nodes = this.getExtensions(extensionName, namespace);
        if (!nodes || nodes.length <= 0) {
            return null;
        }
        else {
            return nodes[0];
        }
    },
    getExtensions: function(extensionName, namespace) {
        if (!extensionName) {
            extensionName = "*";
        }
        var nodes = this.rootNode.getElementsByTagName(extensionName);
        if (nodes.length <= 0) {
            return new Array();
        }

        var collector = function(node) {
            if (!namespace || node.getAttribute("xmlns") == namespace) {
                return node;
            }
            else {
                return null;
            }
        }

        return $A(nodes).collect(collector).toArray().compact();
    },
/**
 * Removes and returns the first extension in the list of extensions with the given
 * name.
 *
 * @param {String} extensionName the name of the extension to be removed from the extensions.
 */
    removeExtension: function(extensionName) {
        var extensions = this.rootNode.childNodes;
        for (var i = 0; i < extensions.length; i++) {
            if (extensions[i].tagName == extensionName) {
                return this.rootNode.removeChild(extensions[i]);
            }
        }
    },
/**
 * If the packet contains an error returns the error code, or null if there is no error.
 */
    getError: function() {
        var error = this.getExtension("error");
        if (error == null) {
            return null;
        }
        else {
            return error.getAttribute("code");
        }
    }
};

XMPP.IQ = function(packetType, from, to, element, init) {
    if (init) {
        return;
    }
    this._init("iq", from, to, element);

    if (!element) {
        this.setType(packetType);
    }
};

XMPP.IQ.prototype = Object.extend(new XMPP.Packet(), {
    setType: function(packetType) {
        if (!packetType || packetType == "") {
            packetType = "get";
        }
        this.addAttributes({ type: packetType });
    },
    getType: function() {
        return this.rootNode.getAttribute("type");
    },
    setQuery: function(xmlns) {
        return this.addExtension("query", xmlns);
    },
    getQuery: function() {
        return this.getExtension("query");
    }
});

XMPP.Registration = function(packetType, to, element) {
    this._init("iq", null, to, element);

    if (!element) {
        this.setType(packetType);
        this.setQuery("jabber:iq:register");
    }
}

XMPP.Registration.prototype = Object.extend(new XMPP.IQ(null, null, null, null, true), {
    getInstructions: function() {
        var instructions = this.getExtension("instructions");
        if (!instructions) {
            return null;
        }
        else if (!instructions.firstChild) {
            return "";
        }
        return  instructions.firstChild.nodeValue;
    },
    setAttributes: function(map) {
        for (var attr in map) {
            this.addTextExtension(attr, map[attr]);
        }
    }
});

XMPP.Presence = function(to, from, element) {
    this._init("presence", from, to, element);
}

XMPP.Presence.prototype = Object.extend(new XMPP.Packet(), {
    setType: function(presenceType) {
        if (!presenceType || presenceType == "" || presenceType == "available") {
            this.removeAttributes($A("type"));
        }
        else {
            this.addAttributes({ type : presenceType});
        }
    },
    getType: function() {
        var type = this.rootNode.getAttribute("type")
        return (type ? type : "available");
    },
    setPriority: function(priority) {
        if (!priority || !(priority instanceof Number)) {
            this.removeExtension("priority");
        }
        else {
            this.addTextExtension("priority", priority);
        }
    },
    getPriority: function() {
        var priority = this.getExtension("priority");
        if (priority) {
            return priority.firstChild.nodeValue;
        }
        else {
            return 0;
        }
    },
    setMode: function(mode) {
        if (!mode || mode == "" || mode == "available") {
            this.removeExtension("show");
        }
        else {
            this.addTextExtension("show", mode);
        }
    },
    getMode: function() {
        var show = this.getExtension("show");
        if (show) {
            return show.firstChild.nodeValue;
        }
        else {
            return null;
        }
    },
    setStatus: function(status) {
        if (!status || status == "") {
            this.removeExtension("status");
        }
        else {
            this.addTextExtension("status", status);
        }
    },
    getStatus: function() {
        var status = this.getExtension("status");
        if (status && status.firstChild) {
            return status.firstChild.nodeValue.escapeHTML();
        }
        else {
            return null;
        }
    }
});

XMPP.Message = function(packetType, from, to, element) {
    if (!packetType && !from && !to && !element) {
        return;
    }
    this._init("message", from, to, element);

    if (!element) {
        this.setType(packetType);
    }
}

XMPP.Message.prototype = Object.extend(new XMPP.Packet(), {
    setType: function(messageType) {
        if (!messageType || messageType == "" || messageType == "normal") {
            this.removeAttributes($A("type"));
        }
        else {
            this.addAttributes({ type : messageType });
        }
    },
    getType: function() {
        var type = this.rootNode.getAttribute("type");
        if (!type) {
            type = "normal";
        }
        return type;
    },
    setSubject: function(subject) {
        if (!subject || subject == "") {
            this.removeExtension("subject");
        }
        else {
            this.addTextExtension("subject", subject);
        }
    },
    getSubject: function(dontEscapeSubject) {
        var subject = this.getExtension("subject");
        if (!subject) {
            return null;
        }
        else if (!subject.firstChild) {
            return "";
        }
        return  (dontEscapeSubject) ? subject.firstChild.nodeValue
                : subject.firstChild.nodeValue.escapeHTML();
    },
    setBody: function(body) {
        if (!body || body == "") {
            this.removeExtension("body");
        }
        else {
            this.addTextExtension("body", body);
        }
    },
    getBody: function() {
        var body = this.getExtension("body");
        if (!body) {
            return null;
        }
        return {
            body: body.firstChild.nodeValue.escapeHTML(),
            lang: body.getAttribute("lang")
        }
    },
    getBodies: function() {
        var bodies = this.getExtensions("body");
        if (!bodies) {
            return null;
        }
        return bodies.collect(function(body) {
            return {
                body: body.firstChild.nodeValue.escapeHTML(),
                lang: body.getAttribute("lang")
            }
        });
    },
    setThread: function(thread) {
        if (!thread || thread == "") {
            this.removeExtension("thread");
        }
        else {
            this.addTextExtension("thread", thread);
        }
    },
    getThread: function() {
        var threadExtension = this.getExtension("thread");
        if (!threadExtension) {
            return null;
        }

        return threadExtension.firstChild.nodeValue;
    }
});

org.jive.spank.roster.Packet = function(packetType, from, to, element) {
    this._init("iq", from, to, element);

    if (!element) {
        this.setType(packetType);
        this.setQuery("jabber:iq:roster");
    }
}

org.jive.spank.roster.Packet.prototype = Object.extend(new XMPP.IQ(null, null, null, null, true), {
    getItems: function() {
        var items = new Array();
        var nodes = this.getExtension().childNodes;
        for (var i = 0; i < nodes.length; i++) {
            if (nodes[i].tagName != "item") {
                continue;
            }

            var item = new org.jive.spank.roster.Item(nodes[i].cloneNode(true));
            items.push(item);
        }
        return items;
    },
    addItem: function(jid, name) {
        var item = this.doc.createElement("item");
        this.getExtension().appendChild(item);

        item.setAttribute("jid", jid.toBareJID());
        if (name) {
            item.setAttribute("name", name);
        }

        return new org.jive.spank.roster.Item(item);
    }
});

org.jive.spank.roster.Item = function(element) {
    this._element = element;
}

org.jive.spank.roster.Item.prototype = {
    getJID: function() {
        var attr = this._element.getAttribute("jid");
        if (!attr) {
            return null;
        }
        else {
            return new XMPP.JID(attr);
        }
    },
    getName: function() {
        return this._element.getAttribute("name");
    },
    isSubscriptionPending: function() {
        return this._element.getAttribute("ask");
    },
    getSubscription: function() {
        return this._element.getAttribute("subscription");
    },
    setSubscription: function(subscription) {
        this._element.setAttribute("subscription", subscription);
    },
    getGroups: function() {
        var nodes = this._element.childNodes;
        var groups = new Array();
        for (var i = 0; i < nodes.length; i++) {
            if (nodes[i].tagName == "group" && nodes[i].firstChild) {
                groups.push(nodes[i].firstChild.nodeValue);
            }
        }
        return groups;
    },
    addGroups: function(groups) {
        for (var i = 0; i < groups.length; i++) {
            var groupNode = this._element.appendChild(this._element.ownerDocument
                    .createElement("group"));
            groupNode.appendChild(this._element.ownerDocument.createTextNode(groups[i]));
        }
    }
}

org.jive.spank.muc.User = function(to, from, element) {
    this._init("message", from, to, element);

    if (!element) {
        this.addExtension("x", "http://jabber.org/protocol/muc#user");
    }
}

org.jive.spank.muc.User.prototype = Object.extend(new XMPP.Message(), {
    setInvite: function(jid, reason) {
        if (!jid || !(jid instanceof XMPP.JID)) {
            throw Error("Inivte must contain invitee, provide a JID");
        }

        var invite = this.doc.createElement("invite");
        this.getExtension().appendChild(invite);

        invite.setAttribute("to", jid.toString());

        if (reason) {
            var reasonNode = this.doc.createElement("reason");
            reasonNode.appendChild(this.doc.createTextNode(reason));
            invite.appendChild(reasonNode);
        }
    },
    getInvite: function() {
        var ex = this._getEx();
        var childNodes = ex.childNodes;
        var invite;
        for (var i = 0; i < childNodes.length; i++) {
            var node = childNodes[i];
            if (node.tagName == "invite") {
                invite = node;
                break;
            }
        }
        if (!invite) {
            return null;
        }

        var reason = invite.firstChild;
        if (reason) {
            reason = reason.firstChild.nodeValue;
        }
        else {
            reason = null;
        }

        var invitation = {};
        invitation["room"] = this.getFrom();
        invitation["from"] = invite.getAttribute("from");
        if (reason) {
            invitation["reason"] = reason;
        }
        return invitation;
    },
    setDecline: function(jid, reason) {
        if (!jid || !(jid instanceof XMPP.JID)) {
            throw Error("Invite must contain invitee, provide a JID");
        }

        var decline = this.doc.createElement("decline");
        this.getExtension().appendChild(decline);

        decline.setAttribute("to", jid.toString());

        if (reason) {
            var reasonNode = this.doc.createElement("reason");
            reasonNode.appendChild(this.doc.createTextNode(reason));
            decline.appendChild(reasonNode);
        }
    },
    getDecline: function() {
        var ex = this._getEx();
        var childNodes = ex.childNodes;
        var declineNode;
        for (var i = 0; i < childNodes.length; i++) {
            var node = childNodes[i];
            if (node.tagName == "decline") {
                declineNode = node;
                break;
            }
        }
        if (!declineNode) {
            return null;
        }

        var reason = declineNode.firstChild;
        if (reason) {
            reason = reason.firstChild.nodeValue;
        }
        else {
            reason = null;
        }

        var decline = {};
        decline.room = this.getFrom();
        decline.from = declineNode.getAttribute("from");
        if (reason) {
            decline.reason = reason;
        }
        return decline;
    },
/**
 * Returns any status codes that are attached to the MUC#User element.
 */
    getStatusCodes: function() {
        if(this._statusCodes) {
            return this._statusCodes;
        }
        var ex = this._getEx();

        var statusCodes = new Array();
        var childNodes = ex.childNodes;
        for (var i = 0; i < childNodes.length; i++) {
            var node = childNodes[i];
            if (node.tagName == "status") {
                statusCodes.push(node.getAttribute("code"));
            }
        }

        this._statusCodes = statusCodes;
        return this._statusCodes;
    },
    getItem: function() {
        if(this._item) {
            return this._item;
        }
        var ex = this._getEx();
        var itemNode = ex.getElementsByTagName("item");
        if(!itemNode) {
            throw Error("Item could not be loaded from packet.");
        }
        var item = {};
        for(var attrName in itemNode.attributes) {
            var attr = itemNode.attributes[attrName];

            item[attr.name] = attr.value;
        }

        this._item = item;
        return item;
    },
    _getEx: function() {
        var ex = this._extension;
        if (!ex) {
            ex = this.getExtension("x");
            if (!ex) {
                return null;
            }
            this._extension = ex;
        }
        return ex;
    }
});

/**
 * A message with the muc owner extension attached to it. The owner extension handles
 * things like destroying a room and changing user roles.
 *
 * @param {JID} to the room handling the owner packet.
 * @param {JID} from the owner sending the request.
 * @param {Element} element the base element used to create this packet.
 */
org.jive.spank.muc.Owner = function(packetType, to, from, element) {
    this._init("iq", from, to, element);

    if (!element) {
        this.setQuery("http://jabber.org/protocol/muc#owner");
    }
}

org.jive.spank.muc.Owner.prototype = Object.extend(new XMPP.IQ(null, null, null, null, true), {
    setDestroy: function(reason, jid) {
        var destroy = this.doc.createElement("destroy");
        this.getExtension().appendChild(destroy);

        if (jid) {
            destroy.setAttribute("jid", jid.toString());
        }
        if (reason) {
            var reasonNode = this.doc.createElement("reason");
            reasonNode.appendChild(this.doc.createTextNode(reason));
            destroy.appendChild(reasonNode);
        }
    },
    getDestroy: function() {
        var childNodes = this.getExtension().childNodes;
        var destroyNode;

        for (var i = 0; i < childNodes.length; i++) {
            if (childNodes[i].tagName == "destroy") {
                destroyNode = childNodes[i];
                break;
            }
        }

        if (destroyNode) {
            var jid = destroyNode.getAttribute("jid");
            var reason;
            if (destroyNode.firstChild) {
                reason = destroyNode.firstChild.firstChild.nodeValue;
            }
            return {
                jid: jid,
                reason: reason
            }
        }
        else {
            return null;
        }
    },
    addItem: function(affiliation, jid, nick, role, reason, actor) {
        var item = this.doc.createElement("item");
        this.getExtension().appendChild(item);

        if (affiliation) {
            item.setAttribute("affiliation", affiliation);
        }
        if (jid && jid instanceof XMPP.JID) {
            item.setAttribute("jid", jid.toString());
        }
        if (nick) {
            item.setAttribute("nick", nick);
        }
        if (role) {
            item.setAttribute("role", role);
        }
        if (reason) {
            var reasonNode = this.doc.createElement("reason");
            reasonNode.appendChild(this.doc.createTextNode(reason));
            destroy.appendChild(reasonNode);
        }
        if (actor) {
            var actorNode = this.doc.createElement("actor");
            actorNode.setAttribute("jid", actor);
            destroy.appendChild(actorNode);
        }
    }
});

org.jive.spank.search = {
    _managers: [],
/**
 * Retrieves a singleton for the connection which is the chat state manager.
 *
 * @param {org.jive.spank.chat.Manager} manager the chat manager to retrieve the singleton for.
 */
    getManager: function(connection) {
        var searchManager = org.jive.spank.search._managers.detect(
                function(connection, searchManager) {
            return searchManager._connection == connection;
        }.bind(null, connection));

        if (searchManager == null) {
            searchManager
                    = new org.jive.spank.search.Manager(connection);
            org.jive.spank.search._managers.push(searchManager);
            connection.addConnectionListener({
                connectionClosed: function() {
                    var index = org.jive.spank.search._managers.indexOf(this);

                    if (index >= 0) {
                        org.jive.spank.search._managers.splice(index, 1);
                    }
                }.bind(searchManager)
            });
        }
        return searchManager;
    }

};

/**
 * Creates a search manager.
 * @param connection the connection that this search manager will utilize.
 */
org.jive.spank.search.Manager = function(connection) {
    this._connection = connection;
}

org.jive.spank.search.Manager.prototype = {
    getSearchServices: function(serviceCallback, server) {
        if(!serviceCallback) {
            throw Error("Callback must be specified to forward returned services to");
        }
        if (!server) {
            server = new XMPP.JID(this._connection.domain);
        }

        var infoCallback = function(serviceCallback, hasFeature, jid, feature) {
            if(hasFeature) {
                serviceCallback(jid);
            }
        }.bind(this, serviceCallback);

        var discoManager = org.jive.spank.disco.getManager(this._connection);
        var callback = function(infoCallback, discoManager, items) {
            items.pluck("jid").each(function(jid) {
                discoManager.hasFeature(jid, "jabber:iq:search", infoCallback)
            });
        }.bind(this, infoCallback, discoManager);

        discoManager.discoverItems(callback, server);
    },
/**
 * Retrieves a search form from a service
 */
    getSearchForm: function(serviceJID, callback) {
        var iq = this._createSearchPacket("get", serviceJID);
        var packetFilter = org.jive.spank.PacketFilter.filter.IDFilter(
                this._processSearchForm.bind(this, callback), iq);
        this._connection.sendPacket(iq, packetFilter)
    },
    _processSearchForm: function(callback, searchFormPacket) {
        var service = searchFormPacket.getFrom();
        var searchForm = new XMPP.XData(null, searchFormPacket.getExtension("x", "jabber:x:data"));
        callback(service, searchForm);
    },
    submitSearch: function(serviceJID, answerForm, searchResultsCallback) {
        if(!serviceJID) {
            throw Error("Service JID must be specified.");
        }
        if(!answerForm) {
            throw Error("Answer form must be specified.");
        }
        if(!searchResultsCallback) {
            throw Error("Search results callback must be specified.");
        }
        var iq = this._createSearchPacket("set", serviceJID);
        answerForm.addToExtension(iq.getExtension("query", "jabber:iq:search"));

        this._connection.sendPacket(iq, org.jive.spank.PacketFilter.filter.IDFilter(
                this._processSearchResults.bind(this, searchResultsCallback), iq));
    },
    _createSearchPacket: function(iqType, serviceJID) {
        var iq = new XMPP.IQ(iqType, null, serviceJID);
        iq.addExtension("query", "jabber:iq:search");
        return iq;
    },
    _processSearchResults: function(searchResultsCallback, searchResultPacket) {
        var searchForm = new XMPP.XData(null,
                searchResultPacket.getExtension("x", "jabber:x:data"));
        searchResultsCallback(searchForm.getReportedValues());
    }
}

XMPP.JID = function(jid) {
    this.jid = jid.toLowerCase();
}

XMPP.JID.prototype = {
    toString: function() {
        return this.jid;
    },
    toBareJID: function() {
        if (!this.bareJID) {
            var i = this.jid.indexOf("/");
            if (i < 0) {
                this.bareJID = this.jid;
            }
            else {
                this.bareJID = this.jid.slice(0, i);
            }
        }
        return this.bareJID;
    },
    getBareJID: function() {
        return new XMPP.JID(this.toBareJID());
    },
    getResource: function() {
        var i = this.jid.indexOf("/");
        if (i < 0) {
            return null;
        }
        else {
            return this.jid.slice(i + 1);
        }
    },
    getNode: function() {
        var i = this.jid.indexOf("@");
        if (i < 0) {
            return null;
        }
        else {
            return this.jid.slice(0, i);
        }
    },
    getDomain: function() {
        var i = this.jid.indexOf("@");
        var j = this.jid.indexOf("/");
        
        if (i < 0) {
        
            if (j < 0) {
                return this.jid;
            }
            else return this.jid.slice(0, j);
        }
        else {
            if (j < 0) {
                return this.jid.slice(i + 1);
            }
            else {
                return this.jid.slice(i + 1, j);
            }
        }
    },
    equals: function(jid, shouldTestBareJID) {
        if(shouldTestBareJID) {
            return jid.toBareJID() == this.toBareJID();
        }
        else {
            return jid.jid == this.jid;
        }
    }
}

XMPP.PacketExtension = function() {
};

XMPP.PacketExtension.prototype = {
    _init: function(fieldName, namespace, element) {
        this.doc = Sarissa.getDomDocument();
        var created = !element;
        if (!element) {
            if (namespace && this.doc.createElementNS) {
                element = this.doc.createElementNS(namespace,
                        fieldName);
            }
            else {
                element = this.doc.createElement(fieldName);
            }
            if (namespace) {
                element.setAttribute("xmlns", namespace);
            }
        }
        // Fix for safari, IE6 doesn't support importNode but works
        // fine with just appendChild
        else if (!_SARISSA_IS_IE) {
            element = this.doc.importNode(element, true);
        }
        this.doc.appendChild(element);

        this.rootNode = this.doc.firstChild;
    }
}

/**
 * Create a new XData form. This class contains convience methods for parsing, and manipulating
 * DataForms. It is a Packet Extension, so it is contained inside of either and IQ or Message
 * packet, adding it to a Presence, generally doesn't have a use.
 *
 * @class
 * @xep http://www.xmpp.org/extensions/xep-0004.html
 */
XMPP.XData = function(type, element) {
    this._init("x", "jabber:x:data", element);

    if (type) {
        this.setType(type);
    }
}

/**
 * Enumeration of the possible field types present in an XData form.
 */
XMPP.XData.FieldType = {
    /**
     *
     */
    hidden: "hidden",
    /**
     *
     */
    bool: "boolean",
    /**
     *
     */
    fixed: "fixed",
    /**
     *
     */
    jidMulti: "jid-multi",
    /**
     *
     */
    listMulti: "list-multi",
    /**
     *
     */
    jidSingle: "jid-single",
    /**
     *
     */
    listSingle: "list-single",
    /**
     *
     */
    textMulti: "text-multi",
    /**
     *
     */
    textSingle: "text-single"
}

XMPP.XData.prototype = Object.extend(new XMPP.PacketExtension(), {
    getType: function() {
        var type = this.rootNode.getAttribute("type");
        if(!type) {
            return "form";
        }
        else {
            return type;
        }
    },
/**
 * Valid form types are as follows:
 * form  	The form-processing entity is asking the form-submitting entity to complete a form.
 * submit 	The form-submitting entity is submitting data to the form-processing entity.
 * cancel 	The form-submitting entity has cancelled submission of data to the form-processing entity.
 * result 	The form-processing entity is returning data (e.g., search results) to the
 * form-submitting entity, or the data is a generic data set
 *
 * @param {String} type the type of the form.
 */
    setType: function(type) {
        if (!type) {
            type = "form";
        }

        this.rootNode.setAttribute("type", type);
    },
    /**
     * Returns all of the fields in a form in an Array. The Array structure is as follows:
     * [{
     *      // A unique variable name for the field
     *      variable: 'state',
     *      // A user friendly label to be displayed to the end user.
     *      fieldLabel: 'State of Residence',
     *      // The type that the field is, see XMPP.XData.FieldType, for an enumeration of possible
     *      // types.
     *      type: 'list-single',
     *      // An array of either possible values to be selected by the user or, the value which
     *      // has been entered by the user.
     *      values: ['PA', 'OR'],
     *      // A true or false of whether or not this field is required for proper submission of the
     *      // form.
     *      isRequired: true
     * }]
     */
    getFields: function(onlyReturnRequired, dontEscapeFieldValues) {
        var fields = this.rootNode.childNodes;

        var toReturn = new Array();
        for (var i = 0; i < fields.length; i++) {
            var field = fields[i];
            if (field.tagName != "field" || field.getAttribute("type")
                    == XMPP.XData.FieldType.hidden) {
                continue;
            }

            var fieldJson = this._parseFieldJson(field, dontEscapeFieldValues);
            if (!onlyReturnRequired || (onlyReturnRequired && fieldJson.required)) {
                toReturn.push(fieldJson);
            }
        }
        return toReturn;
    },
    _parseFieldJson: function(field, dontEscapeFieldValues) {
        var variable = field.getAttribute("var");
        var fieldLabel = field.getAttribute("label");
        var type = field.getAttribute("type");
        var values = new Array();
        var fieldValues = field.childNodes;
        var isRequired = false;
        for (var j = 0; j < fieldValues.length; j++) {
            if (fieldValues[j].tagName != "value" || !fieldValues[j].firstChild) {
                isRequired = fieldValues[j].tagName == "required";
                continue;
            }
            var value;
            if (dontEscapeFieldValues) {
                value = fieldValues[j].firstChild.nodeValue;
            }
            else {
                value = fieldValues[j].firstChild.nodeValue.escapeHTML();
            }
            values.push(value);
        }
        return {
            variable: variable,
            fieldLabel: fieldLabel,
            values: values,
            type: type,
            required: isRequired
        }
    },
    getAnswerForm: function() {
        var answerForm = new XMPP.XData("submit");

        var fields = $A(this.rootNode.childNodes);
        fields.each(function(field) {
            if (field.tagName != "field" || !field.getAttribute("var")) {
                return;
            }

            var shouldCloneChildren = field.getAttribute("type") == XMPP.XData.FieldType.hidden
                    || field.getAttribute("type") == XMPP.XData.FieldType.bool;
            var clone = field.cloneNode(shouldCloneChildren);
            var node;
            if (answerForm.doc.importNode) {
                node = answerForm.doc.importNode(clone, true);
            }
            else {
                node = clone;
            }
            answerForm.rootNode.appendChild(node);
        });
        return answerForm;
    },
    setAnswer: function(variable, answers) {
        if(this.getType() != "submit") {
            throw Error("Answers can only be set on data forms of type 'submit'.");
        }
        var field = this._getField(variable);
        if (!field) {
            throw Error("Form does not contain field for " + variable);
        }

        // Purge any default values.
        $A(field.childNodes).each(function(field, childNode) {
            field.removeChild(childNode);
        }.bind(null, field))

        answers.each(function(answer) {
            var textNode = field.appendChild(this.doc.createElement("value"));
            textNode.appendChild(this.doc.createTextNode(answer));
        }.bind(this));
    },
    /**
    * Returns an array of arrays comprising the reported values, the first row will list the
    * reported fields and subsequent rows will list the results.
    */
    getReportedValues: function() {
        var reportedNode = $A(this.rootNode.childNodes).detect(function(childNode) {
            return childNode.tagName == "reported";
        });

        var reportedFields = $A(reportedNode.childNodes).collect(function(childNode) {
            return {
                label: childNode.getAttribute("label"),
                variable: childNode.getAttribute("var")
            }
        });

        var reportedValues = new Array();
        reportedValues.push(reportedFields);
        $A(this.rootNode.childNodes).each(function(values, childNode) {
            if (childNode.tagName != "item") {
                return;
            }

            values.push($A(childNode.childNodes).collect(function(itemNode) {
                if(!itemNode.firstChild || !itemNode.firstChild.firstChild) { return null; }
                return {
                    variable: itemNode.getAttribute("var"),
                    value: itemNode.firstChild.firstChild.nodeValue
                }
            }).compact());
        }.bind(null, reportedValues));

        return reportedValues;
    },
    getField: function(variable) {
        var field = this._getField(variable);
        if(!field) {
            return null;
        }

        return this._parseFieldJson(field);
    },
    _getField: function(variable) {
        return $A(this.rootNode.childNodes).detect(function(field) {
            return field.getAttribute("var") == variable;
        });
    },
    getInstructions: function() {
        var instructionNode = $A(this.rootNode.childNodes).detect(function(childNode) {
            return childNode.nodeName == "instructions";
        });
        if (instructionNode && instructionNode.firstChild) {
            return instructionNode.firstChild.nodeValue;
        }
        else {
            return null;
        }
    },
    /**
     * Adds the answer form to the given packet extension.
     */
    addToExtension: function(extension) {
        if (!_SARISSA_IS_IE) {
            this.rootNode = extension.ownerDocument.importNode(this.rootNode, true);
        }
        extension.appendChild(this.rootNode);
    }
});

XMPP.DelayInformation = function(element) {
    this.element = element;
}

XMPP.DelayInformation.prototype = {
    getDate: function() {
        var stamp = this.element.getAttribute("stamp");

        var date = new Date();
        var datetime = stamp.split("T");
        date.setUTCFullYear(datetime[0].substr(0, 4), datetime[0].substr(4, 2) - 1,
                datetime[0].substr(6, 2));
        var time = datetime[1].split(":");
        date.setUTCHours(time[0], time[1], time[2]);
        return date;
    }
}

/**
 *
 *  Base64 encode / decode
 *  http://www.webtoolkit.info/
 *
 **/

util.base64 = {

// private property
    _keyStr : "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=",

// public method for encoding
    encode : function (input) {
        var output = "";
        var chr1, chr2, chr3, enc1, enc2, enc3, enc4;
        var i = 0;

        input = util.base64._utf8_encode(input);

        while (i < input.length) {

            chr1 = input.charCodeAt(i++);
            chr2 = input.charCodeAt(i++);
            chr3 = input.charCodeAt(i++);

            enc1 = chr1 >> 2;
            enc2 = ((chr1 & 3) << 4) | (chr2 >> 4);
            enc3 = ((chr2 & 15) << 2) | (chr3 >> 6);
            enc4 = chr3 & 63;

            if (isNaN(chr2)) {
                enc3 = enc4 = 64;
            }
            else if (isNaN(chr3)) {
                enc4 = 64;
            }

            output = output +
                     this._keyStr.charAt(enc1) + this._keyStr.charAt(enc2) +
                     this._keyStr.charAt(enc3) + this._keyStr.charAt(enc4);

        }

        return output;
    },

// public method for decoding
    decode : function (input) {
        var output = "";
        var chr1, chr2, chr3;
        var enc1, enc2, enc3, enc4;
        var i = 0;

        input = input.replace(/[^A-Za-z0-9\+\/\=]/g, "");

        while (i < input.length) {

            enc1 = this._keyStr.indexOf(input.charAt(i++));
            enc2 = this._keyStr.indexOf(input.charAt(i++));
            enc3 = this._keyStr.indexOf(input.charAt(i++));
            enc4 = this._keyStr.indexOf(input.charAt(i++));

            chr1 = (enc1 << 2) | (enc2 >> 4);
            chr2 = ((enc2 & 15) << 4) | (enc3 >> 2);
            chr3 = ((enc3 & 3) << 6) | enc4;

            output = output + String.fromCharCode(chr1);

            if (enc3 != 64) {
                output = output + String.fromCharCode(chr2);
            }
            if (enc4 != 64) {
                output = output + String.fromCharCode(chr3);
            }

        }

        output = util.base64._utf8_decode(output);

        return output;

    },

// private method for UTF-8 encoding
    _utf8_encode : function (string) {
        string = string.replace(/\r\n/g, "\n");
        var utftext = "";

        for (var n = 0; n < string.length; n++) {

            var c = string.charCodeAt(n);

            if (c < 128) {
                utftext += String.fromCharCode(c);
            }
            else if ((c > 127) && (c < 2048)) {
                utftext += String.fromCharCode((c >> 6) | 192);
                utftext += String.fromCharCode((c & 63) | 128);
            }
            else {
                utftext += String.fromCharCode((c >> 12) | 224);
                utftext += String.fromCharCode(((c >> 6) & 63) | 128);
                utftext += String.fromCharCode((c & 63) | 128);
            }

        }

        return utftext;
    },

// private method for UTF-8 decoding
    _utf8_decode : function (utftext) {
        var string = "";
        var i = 0;
        var c, c1, c2 = 0;

        while (i < utftext.length) {

            c = utftext.charCodeAt(i);

            if (c < 128) {
                string += String.fromCharCode(c);
                i++;
            }
            else if ((c > 191) && (c < 224)) {
                c2 = utftext.charCodeAt(i + 1);
                string += String.fromCharCode(((c & 31) << 6) | (c2 & 63));
                i += 2;
            }
            else {
                c2 = utftext.charCodeAt(i + 1);
                var c3 = utftext.charCodeAt(i + 2);
                string += String.fromCharCode(((c & 15) << 12) | ((c2 & 63) << 6) | (c3 & 63));
                i += 3;
            }

        }

        return string;
    }
}
