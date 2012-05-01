# Validateur

[![Continuous Integration status](https://secure.travis-ci.org/michaelklishin/validateur.png)](http://travis-ci.org/michaelklishin/validateur)

Validateur is a validation library inspired by Ruby's ActiveModel. Validateur is functional: validators are
functions, validation sets are higher-order functions, validation results are returned as values.


## Supported Clojure versions

Validateur is built from the ground up for Clojure 1.3 and up.



## Maven Artifacts

### The Latest Release

With Leiningen:

    [com.novemberain/validateur "1.1.0"]

With Maven:

    <dependency>
      <groupId>com.novemberain</groupId>
      <artifactId>validateur</artifactId>
      <version>1.1.0</version>
    </dependency>



## Documentation & Examples

### Basic Example

With Validateur you define validation sets that compose one or more validators:

``` clojure
(ns my.app
  (:use validateur.validation))

(validation-set
  (presence-of :email)
  (presence-of [:address :street])
  (presence-of [:card :cvc]))
```

Any function that returns either a pair of

``` clojure
[true #{}]
```

to indicate successful validation or

``` clojure
[false set-of-validation-error-messages]
```

to indicate validation failure and return error messages can be used as a validator. Validation sets are then passed to
`validateur.core/valid?` together with a map to validate:

``` clojure
(let [v (validation-set
           (presence-of :name)
           (presence-of :age))]
    (is (valid? v {:name "Joe" :age 28}))
    (is (not (invalid? v {:name "Joe" :age 28})))
    (is (not (valid? v {:name "Joe"}))))
```

`validateur.core/invalid?` is a complement to `validateur.core/valid?`.


We are working on documentation guides for Validateur as well as other [ClojureWerkz projects](http://clojurewerkz.org).
Our test suite has usage examples for each validator.



## Development

Validateur uses [Leiningen 2](https://github.com/technomancy/leiningen/blob/master/doc/TUTORIAL.md). Make
sure you have it installed and then run tests against all supported Clojure versions using

    lein2 all test

Then create a branch and make your changes on it. Once you are done with your changes and all
tests pass, submit a pull request on Github.


## License

Copyright (C) 2011-2012 Michael S. Klishin

Distributed under the Eclipse Public License, the same as Clojure.
