---
title: "Getting Started with Validateur, a Clojure validations library"
layout: article
---

## About this guide

This guide combines an overview of Validateur with a quick tutorial that helps you to get started with it.
It should take just a few minutes to read and study the provided code examples. This guide covers:

 * Features of Validateur
 * Clojure version requirement
 * How to add Validateur dependency to your project
 * A brief introduction to Validateur capabilities
 * An overview of built-in validators

This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by/3.0/">Creative Commons Attribution 3.0 Unported License</a> (including images & stylesheets). The source is available [on Github](https://github.com/clojurewerkz/validateur.docs).


## What version of Validateur does this guide cover?

This guide covers Validateur 2.5.x, including pre-release versions.


## Validateur Overview

Validateur is a validation library inspired by Ruby's ActiveModel. Validateur is functional: validators are
functions, validation sets are higher-order functions, validation results are returned as values.

Around this small core, Validateur can be extended with any custom validator you may need: it is as easy as
defining a Clojure function that conforms to a straightforward contract.


## Supported Clojure versions

Validateur requires Clojure 1.6+ or ClojureScript 0.0-2138+.


## Adding Validateur Dependency To Your Project

Validateur artifacts are [released to Clojars](https://clojars.org/com.novemberain/validateur).

### With Leiningen

    [com.novemberain/validateur "2.5.0"]

### With Maven

Add Clojars repository definition to your `pom.xml`:

``` xml
<repository>
  <id>clojars.org</id>
  <url>http://clojars.org/repo</url>
</repository>
```

And then the dependency:

``` xml
<dependency>
  <groupId>com.novemberain</groupId>
  <artifactId>validateur</artifactId>
  <version>2.5.0</version>
</dependency>
```

It is recommended to stay up-to-date with new versions. New releases and important changes are announced [@ClojureWerkz](http://twitter.com/ClojureWerkz).

## Usage

With Validateur you define validation sets that compose one or more validators:

``` clojure
(ns my.app
  (:require [validateur.validation :refer :all]))

(validation-set
  (presence-of :email)
  (presence-of [:address :street])
  (presence-of [:card :cvc]))
```

Any function that returns either a pair of `[true {}]` to indicate successful validation or `[false map-of-keys-to-sets-of-errors]` to indicate validation failure and return error messages can be used as a validator. Validation sets are then passed to `validateur.validation/valid?` together with a map to validate:

``` clojure
(ns my.app
  (:require [validateur.validation :refer :all]))

(let [v (validation-set
         (presence-of :name)
         (presence-of :age))]
  (println (valid? v {:name "Joe" :age 28}))
  (println (invalid? v {:name "Joe" :age 28}))
  (println (valid? v {:name "Joe"})))
```

`validateur.validation/invalid?` is a complement to `validateur.validation/valid?`.

To retrive a map of keys to error messages simply call the validator with a map:

``` clojure
(ns my.app
  (:require [validateur.validation :refer [validation-set presence-of format-of]]))

(let [v (validation-set
         (presence-of :user-name)
         (format-of :user-name
                    :format #"^[^\s]*$"
                    :message "may not contain whitespace"))]
  (v {:user-name "99 bananas"}))
;= {:user-name #{"may not contain whitespace"}}
```

### Validating Nested Attributes

`nested` is a validator runner for nested attributes:

``` clojure
(require '[validateur.validation :refer :all])

(let [v (vr/nested :user (vr/validation-set
                            (vr/presence-of :name)
                            (vr/presence-of :age)))
        extra-nested (vr/nested [:user :profile]
                                (vr/validation-set
                                 (vr/presence-of :age)
                                 (vr/presence-of [:birthday :year])))]
  (v {})
  ;= {[:user :age] #{"can't be blank"}
      [:user :name] #{"can't be blank"}}
  (v {:user {:name "name"}})
  ;= {[:user :age] #{"can't be blank"}}
  (extra-nested {:user {:profile {:age 10
                                  :birthday {:year 2004}}}})
  ;= {}
  (extra-nested {:user {:profile {:age 10}}})
  ;= {[:user :profile :birthday :year] #{"can't be blank"}}
```

### validate-by

`validate-by` is a validator function that returns a function that,
when given a map, will validate that the + value of the attribute in
that map is one of the given:

``` clojure
(require '[validateur.validation :refer :all])

(validation-set
   (presence-of :name)
   (presence-of :age)
   (validate-by [:user :name] not-empty :message \"Username can't be empty!\"))
```


### unnest

`unnest` is a helper function useful for building UIs that validate on
the fly. Here's a basic example. Let's write some code to render a UI
off of a nested map and build up live validation for that map off of
component validators. Here are the components:

```clojure
(def profile-validator
  (vr/validation-set
   (vr/presence-of #{:first-name :last-name})))

(def secret-validator
  (vr/validation-set
   (vr/length-of :password :within (range 5 15))
   (vr/length-of :phone :is 10)))
   ```

And then the composed, user account validator:

```clojure
(def account-validator
  (vr/compose-sets
   (vr/nested :secrets secret-validator)
   (vr/nested :profile profile-validator)))
```

Next are the "rendering" functions. Imagine that these are input components responsible for validating their input and displaying errors when present. Our "render" phase will just print.

```clojure
(defn render-profile [profile errors]
  (prn "Profile: " profile)
  (prn "Profile Errors: " errors))

(defn render-secrets [secrets errors]
  (prn "Secrets: " secrets)
  (prn "Secret Errors: " errors))

(defn submit-button
  "Renders a submit button that can only submit when no errors are
  present."
  [errors]
  (prn "All Errors: " errors))
```

The `render-account` function renders all subcomponents, performs global validation and routes the errors and data where each needs to go:

```clojure
(defn render-account
  "This function accepts an account object, validates the entire thing
  using the subvalidators defined above, then uses `unnested` to pull
  out specific errors for each component.

  The entire validation error map is passed into `submit-button`,
  which might only allow a server POST on click of the full error map
  is empty."
  [{:keys [secrets profile] :as account}]
  (let [errors (account-validator account)]
    (render-profile profile (vr/unnest :profile errors))
    (render-secrets secrets (vr/unnest :secrets errors))
    (submit-button errors)))
```

Let's see this function in action. Calling `render-account` with an invalid map triggers a render that shows off a bunch of errors:

```clojure
(render-account
   {:secrets {:password "face"
              :phone "703555555512323"}
    :profile {:first-name "Queequeg"}})


"Profile: " {:first-name "Queequeg"}
"Errors: " {[:last-name] #{"can't be blank"}}
"Secrets: " {:password "face", :phone "703555555512323"}
"Errors: " {[:phone] #{"must be 10 characters long"}, [:password] #{"must be from 5 to 14 characters long"}}
"All Errors: " {[:profile :last-name] #{"can't be blank"}, [:secrets :phone] #{"must be 10 characters long"}, [:secrets :password] #{"must be from 5 to 14 characters long"}}
```

Calling `render-account` with a valid map prints only the data:

```clojure
(render-account
 {:secrets {:password "faceknuckle"
            :phone "7035555555"}
  :profile {:first-name "Queequeg"
            :last-name "Kokovoko"}})

"Profile: " {:last-name "Kokovoko", :first-name "Queequeg"}
"Errors: " {}
"Secrets: " {:password "faceknuckle", :phone "7035555555"}
"Errors: " {}
"All Errors: " {}
```

### nest

`nest` is a helper function that makes it easy to validate dynamic data that's not part of the actual map you pass into the validator. For example, say you wanted to validate all user accounts, then build up a map of userid -> validation errors:

```clojure
(for [account (get-all-accounts)]
  (vr/nest (:id account)
           (account-validator account)))

{[100 :profile :first-name] "can't be blank"
 [200 :profile :last-name] "can't be blank"
 ;; etc
 }
```


## Tell Us What You Think!

Please take a moment to tell us what you think about this guide [on Twitter](http://twitter.com/clojurewerkz).

Let us know what was unclear or what has not been covered. Maybe you do not like the guide style or grammar or discover spelling mistakes. Reader feedback is key to making the
documentation better.
