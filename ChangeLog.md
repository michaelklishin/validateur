## Changes Between 1.7.0 and 1.8.0

## Validation Set Composition

Validateur now supports composition of validation sets. To
compose several sets, use `validateur.validation/compose-sets`:

``` clojure
(let [vn (vr/validation-set
           (vr/presence-of :name))
      va (vr/validation-set
           (vr/presence-of :age))
      v  (vr/compose-sets va vn)]
  ;= true
  (vr/valid? v { :name "Joe" :age 28 }))
```

Contributed by hura.


## Changes between Validateur 1.6.0 and 1.7.0

### ClojureScript Support

Validateur now supports ClojureScript.

Contributed by Konstantin Shabanov.


## Changes between Validateur 1.5.0 and 1.6.0

### Corrected logic in blank/nil validations

Corrected the logic in the allowed-to-be-blank functions to properly allow nil values
when allow-nil is true, but allow-blank is false. Previously, both allow-blank and allow-nil
had to be set to true to allow nil values due to clojure's blank? function returning true for nil.

Contributed by Wes Johnson.

### Clojure 1.3 No Longer Supported

Clojure 1.3 is no longer supported by Validateur.



## Changes between Validateur 1.4.0 and 1.5.0

### Optional messages in built-in validators

All built-in validators but length-of accept optional messages for all
cases. Their formatting is fixed and based in default ones.
For example:

``` clojure
((inclusion-of :genre :in #{"trance", "dnb"} :message "debe pertenecer a:")
 {:genre "pasodoble"})
;; [false {:genre #{"debe pertenecer a: trance, dnb"}}]
```

### Optional function callback to parametrize the construction of messages

All built-in validators accept an optional function callback which
will be called by the validator to build the returned error message.
The main goal is to facilitate the inclusion of i18n in messages (like
previous one but in a more flexible way).
For example:

``` clojure
((inclusion-of :genre :in #{"trance", "dnb"}
               :message-fn (fn [validator map prop & args]
                              [validator map prop args]))
 {:genre "pasodoble"})
;; [false {:genre #{[:inclusion {:genre "pasodoble"} :genre (#{"trance" "dnb"})]}}]
```


## Changes between Validateur 1.3.0 and 1.4.0

### Clojure 1.5.1 By Default

Validateur now depends on `org.clojure/clojure` version `1.5.1` which
includes an important bug fix.

## Changes between Validateur 1.2.0 and 1.3.0

### clojurewerkz.support 0.14.0

[ClojureWerkz Support](https://github.com/clojurewerkz/support) upgraded to `v0.14.0`.

### Clojure 1.5

Validateur now depends on `org.clojure/clojure` version `1.5.0`. It is
still compatible with Clojure 1.3 and if your `project.clj` depends on
a different version, it will be used, but 1.5 is the default now.

We encourage all users to upgrade to 1.5, it is a drop-in replacement
for the majority of projects out there.


## Changes between Validateur 1.1.0 and 1.2.0

### Doc strings

Built-in validation functions now have doc strings.

### clojurewerkz.support 0.6.0

[ClojureWerkz Support](https://github.com/clojurewerkz/support) upgraded to `v0.6.0`.


## Changes between Validateur 1.0.0 and 1.1.0

### Nested attributes support

`presence-of` and other DSL functions now support nested attributes (think `clojure.core/get-in`),
for example:

``` clojure
(validation-set
  (presence-of :email)
  (presence-of [:address :street])
  (presence-of [:card :cvc]))
```


### clojurewerkz.support dependency

Validateur now depends on [ClojureWerkz Support](https://github.com/clojurewerkz/support).


### Leiningen 2

Validateur now uses [Leiningen 2](https://github.com/technomancy/leiningen/wiki/Upgrading).
