# Validateur

Validateur is a validation library inspired by Ruby's ActiveModel. Validateur is functional: validators are
functions, validation sets are higher-order functions, validation results are returned as values.


## Supported Clojure versions

Validateur is built from the ground up for Clojure 1.3 and up.


## Usage

We are working on documentation guides for the 1.1.0 release. Our test suite has usage examples for each
validator.


## Continuous Integration

[![Continuous Integration status](https://secure.travis-ci.org/michaelklishin/validateur.png)](http://travis-ci.org/michaelklishin/validateur)

CI is hosted by [travis-ci.org](http://travis-ci.org)


## Development

Validateur uses [Leiningen 2](https://github.com/technomancy/leiningen/blob/master/doc/TUTORIAL.md). Make
sure you have it installed and then run tests against Clojure 1.3.0 and 1.4.0[-beta4] using

    lein2 with-profile dev:1.4 test

Then create a branch and make your changes on it. Once you are done with your changes and all
tests pass, submit a pull request on Github.


## License

Copyright (C) 2011-2012 Michael S. Klishin

Distributed under the Eclipse Public License, the same as Clojure.
