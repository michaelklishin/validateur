(ns validateur.validation
  "Validateur is a validation library inspired by Ruby's ActiveModel.
   Validateur is functional: validators are functions, validation sets are higher-order
   functions, validation results are returned as values."
  (:require clojure.string
            [clojure.set :as cs]
            [clojurewerkz.support.core :as sp]))


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
      (and (not (nil? v)) (clojure.string/blank? v) (not allow-blank))))

(defn- allowed-to-be-blank?
  [v ^Boolean allow-nil ^Boolean allow-blank]
  (or (and (nil? v)                  allow-nil)
      (and (not (nil? v)) (clojure.string/blank? v) allow-blank)))


(defn- equal-length-of
  [m attribute actual expected-length allow-nil allow-blank message-fn]
  (if (or (= expected-length (count actual))
          (allowed-to-be-blank? actual allow-nil allow-blank))
    [true {}]
    [false {attribute #{(message-fn :length:is m attribute expected-length)}}]))

(defn- range-length-of
  [m attribute actual xs allow-nil allow-blank message-fn]
  (if (or (member? xs (count actual))
          (allowed-to-be-blank? actual allow-nil allow-blank))
    [true {}]
    [false {attribute #{(message-fn :length:within m attribute xs)}}]))



;;
;; API
;;

(defn presence-of
  "Returns a function that, when given a map, will validate presence of the attribute in that map.

   Accepted options:
   :message (default:\"can't be blank\"): returned error message
   :message-fn (default:nil): function to retrieve message with signature (fn [type map attribute & args])
                              type will be :blank, args will be nil

   Used in conjunction with validation-set:

   (use 'validateur.validation)

   (validation-set
     (presence-of :name)
     (presence-of :age))"
  [attribute & {:keys [message message-fn] :or {message "can't be blank"}}]
  (let [f (if (vector? attribute) get-in get)
        msg-fn (or message-fn (constantly message))]
    (fn [m]
      (let [value  (f m attribute)
            res    (and (not (nil? value))
                        (if (string? value)
                          (not (empty? (clojure.string/trim value))) true))
            msg (msg-fn :blank m attribute)
            errors (if res {} {attribute #{msg}})]
        [(empty? errors) errors]))))

(def ^{:private true}
  assoc-with-union (partial sp/assoc-with cs/union))

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

   (use 'validateur.validation)

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

   (use 'validateur.validation)

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
          (if (accept v)
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

   (use 'validateur.validation)

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
                          (str message (clojure.string/join ", " in))))]
    (fn [m]
      (let [v (f m attribute)]
        (if (nil? v)
          (if allow-nil
            [true {}]
            [false {attribute #{(blank-msg-fn m)}}])
          (if (in v)
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

   (use 'validateur.validation)

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
          (if-not (in v)
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

   (use 'validateur.validation)

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
                  (re-find format v))
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

   (use 'validateur.validation)

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

(defn compose-sets
  [& fns]
  "Takes a collection of validation-sets and returns a validaton-set function which applies
   all given validation-set and merges the results.

   Example:

   (let [user (validation-set (presence-of :user))
         pass (validation-set (presence-of :pass))
         signup-form (validation-comp user pass)]
     (valid? signup-form {:user \"rich\" :pass \"secret\"}))"
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
