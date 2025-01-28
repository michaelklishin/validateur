;; Copyright (c) 2011-2025 Michael S. Klishin, Alex Petrov, and the ClojureWerkz team
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file epl-v10.html at the root of this distribution.
;; By using this software in any fashion, you are agreeing to be bound by
;; the terms of this license.
;; You must not remove this notice, or any other, from this software.

(ns validateur.validation
  "Validateur is a validation library inspired by Ruby's ActiveModel.
   Validateur is functional: validators are functions, validation sets are higher-order
   functions, validation results are returned as values."
  (:require clojure.string
            [clojure.set :as cs]))


;;
;; Implementation
;;

(defn as-vec
  [arg]
  (if (sequential? arg)
    (vec arg)
    (vec [arg])))

(defn ^{:private true} prefix?
  "Returns true if the left vector is a strict prefix of the right
  vector, false otherwise."
  [l r]
  (let [lcount (count l)
        rcount (count r)]
    (and (< lcount rcount)
         (= l (subvec r 0 lcount)))))

(defn ^{:private true} member?
  [coll x]
  (some #(= x %) coll))


(defn ^{:private true} not-allowed-to-be-blank?
  [v ^Boolean allow-nil ^Boolean allow-blank]
  (or (and (nil? v)                  (not allow-nil))
      (and (not (nil? v)) (clojure.string/blank? v) (not allow-blank))))

(defn ^{:private true} allowed-to-be-blank?
  [v ^Boolean allow-nil ^Boolean allow-blank]
  (or (and (nil? v)                  allow-nil)
      (and (not (nil? v)) (clojure.string/blank? v) allow-blank)))


(defn ^{:private true} equal-length-of
  [m attribute actual expected-length allow-nil allow-blank message-fn]
  (if (or (= expected-length (count actual))
          (allowed-to-be-blank? actual allow-nil allow-blank))
    [true {}]
    [false {attribute #{(message-fn :length:is m attribute expected-length)}}]))

(defn ^{:private true} range-length-of
  [m attribute actual xs allow-nil allow-blank message-fn]
  (if (or (member? xs (count actual))
          (allowed-to-be-blank? actual allow-nil allow-blank))
    [true {}]
    [false {attribute #{(message-fn :length:within m attribute xs)}}]))


(defn ^{:private true} attribute-presence
  [attribute msg-fn]
  (let [f (if (vector? attribute) get-in get)]
    (fn [m]
      (let [value   (f m attribute)
            invalid (or (nil? value)
                        (and (string? value) (clojure.string/blank? value)))]
        (if invalid
          {attribute #{(msg-fn :blank m attribute)}}
          nil)))))


;;
;; API
;;

(defn presence-of
  "Returns a function that, when given a map, will validate presence of the attribute(s) in that map.

   Attributes can be given as a set or as a single attribute.
   Individual attributes can be vectors and they are treated as arguments to get-in (nested attributes).

   Accepted options:
   :message (default:\"can't be blank\"): returned error message
   :message-fn (default:nil): function to retrieve message with signature (fn [type map attribute & args])
                              type will be :blank, args will be nil
   :any (default:nil): if true, validation succeeds when any attribute from the set is present
                       the default is to require all attributes from the set to be present

   Used in conjunction with validation-set:

   (require '[validateur.validation :refer :all])

   (validation-set
     (presence-of :name)
     (presence-of :age))

   Or on its own:

   (presence-of #{:name :age})"
  [attributes & {:keys [message message-fn any] :or {message "can't be blank"}}]
  (let [attrs      (if (set? attributes) attributes #{attributes})
        msg-fn     (or message-fn (constantly message))
        validators (map #(attribute-presence % msg-fn) attrs)]
    (if any
      (fn [m]
        (loop [validators-left validators
               errors          {}]
          (if (empty? validators-left)
            [(empty? errors) errors]
            (let [errors-new ((first validators-left) m)]
              (if (empty? errors-new)
                [true {}]
                (recur (rest validators-left) (conj errors errors-new)))))))
      (fn [m]
        (let [errors (reduce #(conj %1 (%2 m)) {} validators)]
          [(empty? errors) errors])))))

(defn ^{:private true} assoc-with-union
  [m k v]
  (assoc m k (apply cs/union [(get m k) v])))

(defn numericality-of
  "Returns a function that, when given a map, will validate that the value of the attribute in that map is numerical.

   Accepted options:

   :messages : a map of type->message to be merge with defaults
   :message-fn (default:nil):
               function to retrieve message with signature (fn [type map attribute & args])
               type will be one of [:blank :number :integer  :odd  :even
                                    :equal-to :gt  :gte :lt :lte]
               args will be the numeric constraint if any

   :allow-nil (default: false): should nil values be allowed?
   :only-integer (default: false): should only integer values be allowed?
   :even (default: false): should even values be allowed?
   :odd (default: false): should odd values be allowed?
   :equal-to: accept only values equal to the given
   :gt: accept only values greater than the given
   :gte: accept only values greater than or equal to the given
   :lt: accept only values less than the given
   :lte: accept only values less than or equal to the given

   Used in conjunction with validation-set:

   (require '[validateur.validation :refer :all])

   (validation-set
     (presence-of :name)
     (presence-of :age)
     (numericality-of :age :only-integer true :gte 18))"
  [attribute & {:keys [allow-nil only-integer gt gte lt lte equal-to odd even messages message-fn]
                :or {allow-nil false, only-integer false, odd false, even false}}]
  (let [f (if (vector? attribute) get-in get)
        msgs (merge {:blank "can't be blank"
                     :number "should be a number" :only-integer "should be an integer"
                     :odd "should be odd" :even "should be even"
                     :equal-to "should be equal to " :gt "should be greater than "
                     :gte  "should be greater than or equal to " :lt "should be less than "
                     :lte "should be less than or equal to "}
                    messages)]
    (fn [m]
      (let [v (f m attribute)
            msg-fn (or message-fn (fn [type _ _ & args] (apply str (msgs type) args)))
            e (reduce
               (fn [errors [type [validation args]]]
                 (if (validation)
                   (assoc-with-union errors attribute #{(apply msg-fn type m attribute args)})
                   errors))
               {}
               {:blank        [#(and (nil? v) (not allow-nil))                    []]
                :number       [#(and v (not (number? v)))                         []]
                :only-integer [#(and v only-integer (not (integer? v)))           []]
                :odd          [#(and v (number? v) odd (not (odd? v)))            []]
                :even         [#(and v (number? v) even (not (even? v)))          []]
                :equal-to     [#(and v (number? v) equal-to (not (= equal-to v))) [equal-to]]
                :gt           [#(and v (number? v) gt (not (> v gt)))             [gt]]
                :gte          [#(and v (number? v) gte (not (>= v gte)))          [gte]]
                :lt           [#(and v (number? v) lt (not (< v lt)))             [lt]]
                :lte          [#(and v (number? v) lte (not (<= v lte)))          [lte]]})]
        [(empty? e) e]))))


(defn acceptance-of
  "Returns a function that, when given a map, will validate that the value of the attribute in that map is accepted.
   By default, values that are considered accepted: true, \"true\", \"1\". Primarily used for validation of data that comes from
   Web forms.

   Accepted options:

   :message (default:\"must be accepted\"): returned error message
   :blank-message (default:\"can't be blank\"): returned error message if value is not present
   :message-fn function to retrieve message with signature (fn [type map attribute & args]).
               type will be :blank or :acceptance, args will be the set of accepted values

   :allow-nil (default: false): should nil values be allowed?
   :accept (default: #{true, \"true\", \"1\"}): pass to use a custom list of values that will be considered accepted

   Used in conjunction with validation-set:

   (require '[validateur.validation :refer :all])

   (validation-set
     (presence-of :name)
     (presence-of :age)
     (acceptance-of :terms))"
  [attribute & {:keys [allow-nil accept message blank-message message-fn]
                :or {allow-nil false, accept #{true "true" "1"},
                     message "must be accepted", blank-message "can't be blank"}}]
  (let [f (if (vector? attribute) get-in get)
        msg-fn (fn [t m msg & args]
                 (if message-fn (apply message-fn t m attribute args) msg))]
    (fn [m]
      (let [v (f m attribute)]
        (if (and (nil? v) (not allow-nil))
          [false {attribute #{(msg-fn :blank m blank-message)}}]
          (if (contains? accept v)
            [true {}]
            [false {attribute #{(msg-fn :acceptance m message accept)}}]))))))



(defn all-keys-in
  "Returns a function that, when given a map, will validate that all keys in the map are drawn from a set of allowed keys.

   Accepted options:

   :unknown-message (default:\"unknown key\"): returned error message if key is not in allowed set

   Used in conjunction with validation-set:

   (validation-set
     (all-keys-in #{:church :turing :gödel}))"
  [allowed-keys & {:keys [unknown-message]
                   :or {unknown-message "unknown key"}}]
  {:pre [(set? allowed-keys)]}
  (fn [m]
    (let [map-keys (set (keys m))
          ;; Remove all allowed keys from map keys and if there are
          ;; any left over then that's a problem.
          invalid-keys (cs/difference map-keys allowed-keys)]
      (if (empty? invalid-keys)
        [true {}]
        [false (reduce (fn [m key] (assoc m key #{unknown-message}))
                       {}
                       invalid-keys)]))))



(defn inclusion-of
  "Returns a function that, when given a map, will validate that the value of the attribute in that map is one of the given.

   Accepted options:

   :blank-message (default:\"can't be blank\"): returned error message if value is not present
   :message (default: \"must be one of: \"): returned error message
                                             (with comma-separated valid values appended)
   :message-fn (default:nil): function to retrieve message with signature (fn [type map attribute & args]).
                              type will be :blank or :inclusion, args will be the set of valid values

   :allow-nil (default: false): should nil values be allowed?
   :in (default: nil): a collection of valid values for the attribute

   Used in conjunction with validation-set:

   (require '[validateur.validation :refer :all])

   (validation-set
     (presence-of :name)
     (presence-of :age)
     (inclusion-of :team :in #{\"red\" \"blue\"}))"
  [attribute & {:keys [allow-nil in message blank-message message-fn]
                :or {allow-nil false, message "must be one of: ",
                     blank-message "can't be blank"}}]
  (let [f (if (vector? attribute) get-in get)
        blank-msg-fn (fn [m] (if message-fn (message-fn :blank m attribute)
                                blank-message))
        msg-fn (fn [m] (if message-fn (message-fn :inclusion m attribute in)
                           (str message (clojure.string/join ", " (sort in)))))]
    (fn [m]
      (let [v (f m attribute)]
        (if (nil? v)
          (if allow-nil
            [true {}]
            [false {attribute #{(blank-msg-fn m)}}])
          (if (contains? in v)
            [true {}]
            [false {attribute #{(msg-fn m)}}]))))))



(defn exclusion-of
  "Returns a function that, when given a map, will validate that the value of the attribute in that map is not one of the given.

   Accepted options:

   :blank-message (default:\"can't be blank\"): returned error message if value is not present
   :message-fn (default nil): function to retrieve message with signature (fn [type map attribute & args]).
                              type will be :blank or :exclusion, args will be the set of invalid values
   :message (default: \"must not be one of: \"): returned error message
                                                 (with comma separated list of invalid values appended)
   :allow-nil (default: false): should nil values be allowed?
   :in (default: nil): a collection of invalid values for the attribute

   Used in conjunction with validation-set:

   (require '[validateur.validation :refer :all])

   (validation-set
     (presence-of :name)
     (presence-of :age)
     (exclusion-of :status :in #{\"banned\" \"non-activated\"}))"
  [attribute & {:keys [allow-nil in message blank-message message-fn]
                :or {allow-nil false, message "must not be one of: ",
                     blank-message "can't be blank"}}]
  (let [f (if (vector? attribute) get-in get)
        blank-msg-fn (fn [m] (if message-fn (message-fn :blank m attribute)
                                blank-message))
        msg-fn (fn [m] (if message-fn (message-fn :exclusion m attribute in)
                          (str message (clojure.string/join ", " in))))]
    (fn [m]
      (let [v (f m attribute)]
        (if (and (nil? v) (not allow-nil))
          [false {attribute #{(blank-msg-fn m)}}]
          (if-not (contains? in v)
            [true {}]
            [false {attribute #{(msg-fn m)}}]))))))



(defn format-of
  "Returns a function that, when given a map, will validate that the value of the attribute in that map is in the given format.

   Accepted options:

   :allow-nil (default: false): should nil values be allowed?
   :allow-blank (default: false): should blank string values be allowed?
   :format (default: nil): a regular expression of the format
   :message (default: \"has incorrect format\"): an error message for invalid values
   :blank-message (default:\"can't be blank\"): returned error message if value is not present
   :message-fn (default nil): function to retrieve message with signature (fn [type map attribute & args]).
                              type will be :format or :blank, args will be the applied format

   Used in conjunction with validation-set:

   (require '[validateur.validation :refer :all])

   (validation-set
     (presence-of :username)
     (presence-of :age)
     (format-of :username :format #\"[a-zA-Z0-9_]\")"
  [attribute & {:keys [allow-nil allow-blank format message blank-message message-fn]
                :or {allow-nil false, allow-blank false, message "has incorrect format",
                     blank-message "can't be blank"}}]
  (let [f (if (vector? attribute) get-in get)
        msg-fn (fn [t m] (if message-fn (apply message-fn t m attribute
                                              (when (= t :format) [format]))
                            (if (= t :blank) blank-message message)))]
    (fn [m]
      (let [v (f m attribute)]
        (if (not-allowed-to-be-blank? v allow-nil allow-blank)
          [false {attribute #{(msg-fn :blank m)}}]
          (if (or (allowed-to-be-blank? v allow-nil allow-blank)
                  (re-matches format v))
            [true {}]
            [false {attribute #{(msg-fn :format m)}}]))))))



(defn length-of
  "Returns a function that, when given a map, will validate that the value of the attribute in that map is of the given length.

   Accepted options:

   :allow-nil (default: false): should nil values be allowed?
   :allow-blank (default: false): should blank string values be allowed?
   :is (default: nil): an exact length, as long
   :within (default: nil): a range of lengths
   :blank-message (default:\"can't be blank\"): returned error message if value is not present
   :message-fn (default nil): function to retrieve message with signature (fn [type m attribute & args]).
                              type will be :length:is or :length:within, args will be the applied number or range

   Used in conjunction with validation-set:

   (require '[validateur.validation :refer :all])

   (validation-set
     (presence-of :name)
     (presence-of :age)
     (length-of :password :within (range 6 100))

   (validation-set
     (presence-of :name)
     (presence-of :age)
     (length-of :zip :is 5)"
  [attribute & {:keys [allow-nil is within allow-blank blank-message message-fn]
                :or {allow-nil false, allow-blank false,
                     blank-message "can't be blank"}}]
  (let [f (if (vector? attribute) get-in get)
        msg-fn-blank #(if message-fn (message-fn :blank % attribute) blank-message)
        msg-fn-is (or message-fn #(str "must be " %4 " characters long"))
        msg-fn-within (or message-fn #(str "must be from " (first %4) " to "
                                           (last %4) " characters long"))]
    (fn [m]
      (let [v (f m attribute)]
        (if (not-allowed-to-be-blank? v allow-nil allow-blank)
          [false {attribute #{(msg-fn-blank m)}}]
          (if within
            (range-length-of m attribute v within allow-nil allow-blank msg-fn-within)
            (equal-length-of m attribute v is     allow-nil allow-blank msg-fn-is)))))))



(defn validate-when
  "Returns a function that, when given a map, will run the validator against that map if and
  only if the predicate function returns true.  The predicate function will be given the same
  map on which the validator may run.

  Example:

  (require '[validateur.validation :refer :all])

  (validate-when #(contains? % :name) (presence-of :name))"
  [predicate validator]
  (fn [m]
    (if (predicate m)
      (validator m)
      [true {}])))

(defn validate-by
  "Returns a function that, when given a map, will validate that the
  value of the attribute in that map is one of the given.


   Accepted options:

   :message (default: \"Failed predicate validation.\")

   Used in conjunction with validation-set:

   (require '[validateur.validation :refer :all])

   (validation-set
     (presence-of :name)
     (presence-of :age)
     (validate-by [:user :name] not-empty :message \"Username can't be empty!\"))"
  [attr pred & {:keys [message]
                :or {message "Failed predicate validation."}}]
  (let [f (if (vector? attr) get-in get)]
    (fn [m]
      (let [v (f m attr)]
        (if (pred v)
          [true {}]
          [false {attr #{message}}])))))

(defn nest
  "Takes an attribute (either a single key or a vector of keys) and a
     validation set and prefixes the keys in the validation set's
     error map with that attribute."
  [attr m]
  (let [attr (as-vec attr)]
    (->> (for [[k messages] m
               :let [k (into attr (as-vec k))]]
           [k messages])
         (into {}))))

(defn nested
  "Takes an attribute (either a single key or a vector of keys) and a
  validation set, and returns a function that will apply the supplied
  validation set to the inner value located at `attr`."
  [attr vset]
  (let [f (if (vector? attr) get-in get)]
    (fn [m] (nest attr (vset (f m attr))))))

(defn unnest
  "Takes an attribute (either a single key or a vector of keys) and a
     validation set and returns a new validation set with that
     attribute removed from the beginning of any matching internal
     attributes. Entries in the validation set that aren't prefixed
     with the supplied attribute will be filtered."
  [attr m]
  (let [attr (as-vec attr)
        attrcount (count attr)]
    (->> (for [[k messages] m
               :when (and (sequential? k)
                          (> (count k) 1)
                          (prefix? attr k))]
           [(subvec k attrcount (count k)) messages])
         (into {}))))

(defn validate-nested
  "Returns a function that, when given a map, will validate that the
  value of at key attr in that map passes validation using the given
  validator (a function as returned by validation-set).

  Accepted options:

  :message (default: \"is invalid\"): an error message for invalid values
  :message-fn (default:nil): function to retrieve message with signature (fn [map])

  Example:

  (require '[validateur.validation :refer :all])

  (def foo-validator (validation-set (presence-of :foo)))

  (validation-set (validate-nested :bar foo-validator))"
  [attr validator & {:keys [message message-fn]}]
  (let [get-fn (if (vector? attr) get-in get)
        validate-fn (if (or message message-fn)
                      (let [msg-fn (or message-fn (constantly message))]
                        (fn [m]
                          (reduce-kv (fn [out k v]
                                       (assoc out k #{(msg-fn m)}))
                            {}
                            (validator m))))
                      validator)]
    (fn [m]
      (let [value (get-fn m attr)
            result (validate-fn value)]
        (if (seq result)
          [false (nest attr result)]
          [true {}])))))

(defn validate-with-predicate
  "Returns a function that, when given a map, will validate that the predicate returns
  true when given the map.

  Accepted options:

  :message (default: \"is invalid\"): an error message for invalid values
  :message-fn (default:nil): function to retrieve message with signature (fn [map])

  Example:

  (require '[validateur.validation :refer :all])

  (validate-with-predicate :name #(contains? % :name))"
  [attribute predicate & {:keys [message message-fn] :or {message "is invalid"}}]
  (let [message-fn (or message-fn (constantly message))]
    (fn [m]
      (if (predicate m)
        [true {}]
        [false {attribute #{(message-fn m)}}]))))

(defn validity-of
  "Takes an attribute (either a single key or a vector of keys) and an
  arbitrary number of validators and returns a function that, when
  given a map, will validate the map using all the supplied
  validators, accumulating any errors into a single errors map.

  Example:

  (require '[validateur.validation :refer :all])

  (vr/validity-of :person
    (vr/presence-of :name)
    (vr/format-of :name :format #\"\\p{Alpha}+\")
    (vr/inclusion-of :status :in #{:active :inactive}))"
  [attr & validators]
  (let [get-fn (if (vector? attr) get-in get)]
    (fn [m]
      (let [nested (get-fn m attr)]
        (reduce (fn [[accu-ok accu-errors] f]
                  (let [[ok errors] (f nested)]
                    [(and accu-ok ok)
                     (merge-with cs/union accu-errors (nest attr errors))]))
          [true {}]
          validators)))))



(defn validation-set
  "Takes a collection of validators and returns a function that, when given a map, will run all
   the validators against that map and collect all the error messages that are then returned
   as a set.

   Example:

   (require '[validateur.validation :refer :all])

   (validation-set
     (presence-of :name)
     (presence-of :age)
     (length-of :password :within (range 6 100))"
  [& validators]
  (fn [m]
    (reduce (fn [accu f]
              (let [[ok errors] (f m)]
                (merge-with cs/union accu errors)))
            {}
            validators)))

(defn validate-some
  "Takes a sequence of validators and returns a validator that returns
  the result of the FIRST failing validator, short-circuiting
  after. This is helpful when downstream validations call functions
  that can't have nil inputs, for example.

  Example:

  (require '[validateur.validation :refer :all])

  (validate-some
    (presence-of :name)
    (validate-by :name not-empty))"
  [& validators]
  (fn [m]
    (loop [[v & rest] validators]
      (if-not v
        [true #{}]
        (let [[passed? :as result] (v m)]
          (if passed?
            (recur rest)
            result))))))

(defn compose-sets
  "Takes a collection of validation-sets and returns a validaton-set function which applies
   all given validation-set and merges the results.

   Example:

   (let [user (validation-set (presence-of :user))
         pass (validation-set (presence-of :pass))
         signup-form (compose-sets user pass)]
     (valid? signup-form {:user \"rich\" :pass \"secret\"}))"
  [& fns]
  (fn [data]
    (apply merge-with cs/union ((apply juxt fns) data))))

(defn valid?
  "Takes a validation set and a map.

   Returns true if validation returned no errors, false otherwise"
  ([vsm]
    (empty? vsm))
  ([vs m]
    (empty? (vs m))))

(def invalid? (complement valid?))

(defn errors
  "Takes in a key (either a single keyword or a nested key) and
  returns any errors present in the validation error map for that key,
  or nil if none are present."
  [k m]
  (let [[k & rest :as ks] (if (sequential? k) k [k])]
    (not-empty
     (if (nil? rest)
       (or (m k) (m ks))
       (m ks)))))

(def errors? (comp boolean errors))
