(ns ^{:doc "Validateur is a validation library inspired by Ruby's ActiveModel.
Validateur is functional: validators are functions, validation sets are higher-order
functions, validation results are returned as values."}
  validateur.validation
  (:require clojure.string)
  (:use [clojure.set :as cs]
        [clojurewerkz.support.core :only [assoc-with]]))


;;
;; Implementation
;;

(defn as-vec
  [arg]
  (if (sequential? arg)
    (vec arg)
    (vec [arg])))

(defn- member?
  [coll x]
  (some #(= x %) coll))


(defn- not-allowed-to-be-blank?
  [v ^Boolean allow-nil ^Boolean allow-blank]
  (or (and (nil? v)                  (not allow-nil))
      (and (clojure.string/blank? v) (not allow-blank))))

(defn- allowed-to-be-blank?
  [v ^Boolean allow-nil ^Boolean allow-blank]
  (or (and (nil? v)                  allow-nil)
      (and (clojure.string/blank? v) allow-blank)))


(defn- equal-length-of
  [attribute actual expected-length allow-nil allow-blank]
  (if (or (= expected-length (count actual))
          (allowed-to-be-blank? actual allow-nil allow-blank))
    [true {}]
    [false {attribute #{(str "must be " expected-length " characters long")}}]))

(defn- range-length-of
  [attribute actual xs allow-nil allow-blank]
  (if (or (member? xs (count actual))
          (allowed-to-be-blank? actual allow-nil allow-blank))
    [true {}]
    [false {attribute #{(str "must be from " (first xs) " to " (last xs) " characters long")}}]))



;;
;; API
;;

(defn presence-of
  "Returns a function that, when given a map, will validate presence of the attribute in that map.

   Used in conjunction with validation-set:

   (use 'validateur.validation)

   (validation-set
     (presence-of :name)
     (presence-of :age))"
  [attribute]
  (let [f (if (vector? attribute) get-in get)]
    (fn [m]
      (let [v      (f m attribute)
            errors (if v {} {attribute #{"can't be blank"}})]
        [(empty? errors) errors]))))

(def ^{:private true}
  assoc-with-union (partial assoc-with cs/union))

(defn numericality-of
  "Returns a function that, when given a map, will validate that the value of the attribute in that map is numerical.

   Accepted options:

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

   (use 'validateur.validation)

   (validation-set
     (presence-of :name)
     (presence-of :age)
     (numericality-of :age :only-integer true :gte 18))"
  [attribute & {:keys [allow-nil only-integer gt gte lt lte equal-to odd even] :or {allow-nil false
                                                                                     only-integer false
                                                                                     odd false
                                                                                     even false}}]
  (let [f (if (vector? attribute) get-in get)]
    (fn [m]
      (let [v      (f m attribute)
            errors (atom {})]
        ;; this code below is old, stupid and disgusting. It will be rewritten soon, please DO NOT use it as
        ;; example of how Clojure atoms should be used. MK.
        (if (and (nil? v) (not allow-nil))
          (swap! errors assoc attribute #{"can't be blank"}))
        (when (and v (not (number? v)))
          (swap! errors assoc-with-union attribute #{"should be a number"}))
        (when (and v only-integer (not (integer? v)))
          (swap! errors assoc-with-union attribute #{"should be an integer"}))
        (when (and v (number? v) odd (not (odd? v)))
          (swap! errors assoc-with-union attribute #{"should be odd"}))
        (when (and v (number? v) even (not (even? v)))
          (swap! errors assoc-with-union attribute #{"should be even"}))
        (when (and v (number? v) equal-to (not (= equal-to v)))
          (swap! errors assoc-with-union attribute #{(str "should be equal to " equal-to)}))
        (when (and v (number? v) gt (not (> v gt)))
          (swap! errors assoc-with-union attribute #{(str "should be greater than " gt)}))
        (when (and v (number? v) gte (not (>= v gte)))
          (swap! errors assoc-with-union attribute #{(str "should be greater than or equal to " gte)}))
        (when (and v (number? v) lt (not (< v lt)))
          (swap! errors assoc-with-union attribute #{(str "should be less than " lt)}))
        (when (and v (number? v) lte (not (<= v lte)))
          (swap! errors assoc-with-union attribute #{(str "should be less than or equal to " lte)}))
        [(empty? @errors) @errors]))))


(defn acceptance-of
  "Returns a function that, when given a map, will validate that the value of the attribute in that map is accepted.
   By default, values that are considered accepted: true, \"true\", \"1\". Primarily used for validation of data that comes from
   Web forms.

   Accepted options:

   :allow-nil (default: false): should nil values be allowed?
   :accept (default: #{true, \"true\", \"1\"}): pass to use a custom list of values that will be considered accepted

   Used in conjunction with validation-set:

   (use 'validateur.validation)

   (validation-set
     (presence-of :name)
     (presence-of :age)
     (acceptance-of :terms))"
  [attribute & {:keys [allow-nil accept] :or {allow-nil false accept #{true "true", "1"}}}]
  (let [f (if (vector? attribute) get-in get)]
    (fn [m]
      (let [v (f m attribute)]
        (if (and (nil? v) (not allow-nil))
          [false {attribute #{"can't be blank"}}]
          (if (accept v)
            [true {}]
            [false {attribute #{"must be accepted"}}]))))))



(defn inclusion-of
  "Returns a function that, when given a map, will validate that the value of the attribute in that map is one of the given.

   Accepted options:

   :allow-nil (default: false): should nil values be allowed?
   :in (default: nil): a collection of valid values for the attribute

   Used in conjunction with validation-set:

   (use 'validateur.validation)

   (validation-set
     (presence-of :name)
     (presence-of :age)
     (inclusion-of :team :in #{\"red\" \"blue\"}))"
  [attribute & {:keys [allow-nil in] :or {allow-nil false}}]
  (let [f (if (vector? attribute) get-in get)]
    (fn [m]
      (let [v (f m attribute)]
        (if (and (nil? v) (not allow-nil))
          [false {attribute #{"can't be blank"}}]
          (if (in v)
            [true {}]
            [false {attribute #{(str "must be one of: " (clojure.string/join ", " in))}}]))))))



(defn exclusion-of
  "Returns a function that, when given a map, will validate that the value of the attribute in that map is not one of the given.

   Accepted options:

   :allow-nil (default: false): should nil values be allowed?
   :in (default: nil): a collection of invalid values for the attribute

   Used in conjunction with validation-set:

   (use 'validateur.validation)

   (validation-set
     (presence-of :name)
     (presence-of :age)
     (exclusion-of :status :in #{\"banned\" \"non-activated\"}))"
  [attribute & {:keys [allow-nil in] :or {allow-nil false}}]
  (let [f (if (vector? attribute) get-in get)]
    (fn [m]
      (let [v (f m attribute)]
        (if (and (nil? v) (not allow-nil))
          [false {attribute #{"can't be blank"}}]
          (if-not (in v)
            [true {}]
            [false {attribute #{(str "must not be one of: " (clojure.string/join ", " in))}}]))))))



(defn format-of
  "Returns a function that, when given a map, will validate that the value of the attribute in that map is in the given format.

   Accepted options:

   :allow-nil (default: false): should nil values be allowed?
   :allow-blank (default: false): should blank string values be allowed?
   :format (default: nil): a regular expression of the format
   :message (default: \"has incorrect format\"): an error message for invalid values

   Used in conjunction with validation-set:

   (use 'validateur.validation)

   (validation-set
     (presence-of :username)
     (presence-of :age)
     (format-of :username :format #\"[a-zA-Z0-9_]\")"
  [attribute & {:keys [allow-nil allow-blank format message]
                :or {allow-nil false allow-blank false message "has incorrect format"}}]
  (let [f (if (vector? attribute) get-in get)]
    (fn [m]
      (let [v (f m attribute)]
        (if (not-allowed-to-be-blank? v allow-nil allow-blank)
          [false {attribute #{"can't be blank"}}]
          (if (or (allowed-to-be-blank? v allow-nil allow-blank)
                  (re-find format v))
            [true {}]
            [false {attribute #{message}}]))))))



(defn length-of
  "Returns a function that, when given a map, will validate that the value of the attribute in that map is of the given length.

   Accepted options:

   :allow-nil (default: false): should nil values be allowed?
   :allow-blank (default: false): should blank string values be allowed?
   :is (default: nil): an exact length, as long
   :within (default: nil): a range of lengths

   Used in conjunction with validation-set:

   (use 'validateur.validation)

   (validation-set
     (presence-of :name)
     (presence-of :age)
     (length-of :password :within (range 6 100))

   (validation-set
     (presence-of :name)
     (presence-of :age)
     (length-of :zip :is 5)"
  [attribute & {:keys [allow-nil is within allow-blank] :or {allow-nil false allow-blank false}}]
  (let [f (if (vector? attribute) get-in get)]
    (fn [m]
      (let [v (f m attribute)]
        (if (not-allowed-to-be-blank? v allow-nil allow-blank)
          [false {attribute #{"can't be blank"}}]
          (if within
            (range-length-of attribute v within allow-nil allow-blank)
            (equal-length-of attribute v is     allow-nil allow-blank)))))))




(defn validation-set
  "Takes a collection of validators and returns a function that, when given a map, will run all
   the validators against that map and collect all the error messages that are then returned
   as a set.

   Example:

   (use 'validateur.validation)

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

(defn valid?
  "Takes a validation set and a map.

   Returns true if validation returned no errors, false otherwise"
  [vs m]
  (empty? (vs m)))

(def invalid? (complement valid?))
