# Validateur, a Clojure Validation Library

Validateur is a [Clojure validation library](http://clojurevalidations.info) inspired by Ruby's ActiveModel. Validateur is functional: validators are
functions, validation sets are higher-order functions, validation results are returned as values.


## Supported Clojure versions

Validateur is built from the ground up for Clojure 1.3 and up.



## Maven Artifacts

### The Latest Release

With Leiningen:

    [com.novemberain/validateur "1.4.0"]

With Maven:

    <dependency>
      <groupId>com.novemberain</groupId>
      <artifactId>validateur</artifactId>
      <version>1.4.0</version>
    </dependency>



## Documentation & Examples

Please refer to the [documentation guides](http://clojurevalidations.info) for Validateur.

Our test suite has usage examples for each validator, built-in validation functions have docstrings.


## Continuous Integration

[![Continuous Integration status](https://secure.travis-ci.org/michaelklishin/validateur.png)](http://travis-ci.org/michaelklishin/validateur)


## Development

Validateur uses [Leiningen 2](https://github.com/technomancy/leiningen/blob/master/doc/TUTORIAL.md). Make
sure you have it installed and then run tests against all supported Clojure versions using

    lein2 all test

Then create a branch and make your changes on it. Once you are done with your changes and all
tests pass, submit a pull request on Github.


## License

Copyright (C) 2011-2013 Michael S. Klishin

Distributed under the Eclipse Public License, the same as Clojure.
