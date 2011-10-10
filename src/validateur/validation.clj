(ns validateur.validation
  (:use [clojure.set :as set]))

;;
;; API
;;

(declare as-vec)
(declare assoc-with)
(declare concat-with-separator)

(defn presence-of
  [attribute]
  (fn [m]
    (let [errors (if (attribute m) {} { attribute #{"can't be blank"} })]
      [(empty? errors) errors])))


(defn numericality-of
  [attribute & { :keys [allow-nil only-integer gt gte lt lte equal-to odd even] :or { allow-nil false, only-integer false, odd false, event false }}]
  (fn [m]
    (let [v       (attribute m)
          errors  (atom {})]
      (if (and (nil? v) (not allow-nil))
        (reset! errors (assoc @errors attribute #{"can't be blank"})))
      (when (and v (not (number? v)))
        (reset! errors (assoc-with set/union @errors attribute #{"should be a number"})))
      (when (and v only-integer (not (integer? v)))
        (reset! errors (assoc-with set/union @errors attribute #{"should be an integer"})))
      (when (and v (number? v) odd (not (odd? v)))
        (reset! errors (assoc-with set/union @errors attribute #{"should be odd"})))
      (when (and v (number? v) even (not (even? v)))
        (reset! errors (assoc-with set/union @errors attribute #{"should be even"})))
      (when (and v (number? v) equal-to (not (= equal-to v)))
        (reset! errors (assoc-with set/union @errors attribute #{(str "should be equal to " equal-to)})))
      (when (and v (number? v) gt (not (> v gt)))
        (reset! errors (assoc-with set/union @errors attribute #{(str "should be greater than " gt)})))
      (when (and v (number? v) gte (not (>= v gte)))
        (reset! errors (assoc-with set/union @errors attribute #{(str "should be greater than or equal to " gte)})))
      (when (and v (number? v) lt (not (< v lt)))
        (reset! errors (assoc-with set/union @errors attribute #{(str "should be less than " lt)})))
      (when (and v (number? v) lte (not (<= v lte)))
        (reset! errors (assoc-with set/union @errors attribute #{(str "should be less than or equal to " lte)})))
      [(empty? @errors) @errors])))


(defn acceptance-of
  [attribute & { :keys [allow-nil accept] :or { allow-nil false, accept #{true, "true", "1"} }}]
  (fn [m]
    (let [v (attribute m)]
      (if (and (nil? v) (not allow-nil))
        [false, { attribute #{"can't be blank"} }]
        (if (accept v)
          [true, {}]
          [false, { attribute #{"must be accepted"} }])))))


(defn inclusion-of
  [attribute & { :keys [allow-nil in] :or { allow-nil false }}]
  (fn [m]
    (let [v (attribute m)]
      (if (and (nil? v) (not allow-nil))
        [false, { attribute #{"can't be blank"} }]
        (if (in v)
          [true, {}]
          [false, { attribute #{(str "must be one of: " (concat-with-separator in ", "))} }])))))


(defn exclusion-of
  [attribute & { :keys [allow-nil in] :or { allow-nil false }}]
  (fn [m]
    (let [v (attribute m)]
      (if (and (nil? v) (not allow-nil))
        [false, { attribute #{"can't be blank"} }]
        (if-not (in v)
          [true, {}]
          [false, { attribute #{(str "must not be one of: " (concat-with-separator in ", "))} }])))))



(defn format-of
  [attribute & { :keys [allow-nil format] :or { allow-nil false }}]
  (fn [m]
    (let [v (attribute m)]
      (if (and (nil? v) (not allow-nil))
        [false, { attribute #{"can't be blank"} }]
        (if (re-find format v)
          [true, {}]
          [false, { attribute #{"has incorrect format"} }])))))


(defn- equal-length-of
  [attribute actual expected-length]
  (if (= expected-length (count actual))
    [true, {}]
    [false, { attribute #{(str "must be " expected-length " characters long")} }]))

(defn length-of
  [attribute & { :keys [allow-nil is maximum minimum within] :or { allow-nil false }}]
  (fn [m]
    (let [v (attribute m)]
      (if (and (nil? v) (not allow-nil))
        [false, { attribute #{"can't be blank"} }]
        (case [(nil? is) (nil? maximum) (nil? minimum) (nil? within)]
          [false true true true] (equal-length-of attribute v is))))))



(defn validation-set
  [& validators]
  (fn [m]
    (reduce (fn [accu f]
              (let [[ok errors] (f m)]
                (merge-with set/union accu errors)))
            {}
            validators)))

(defn valid?
  [vs m]
  (empty? (vs m)))

(def invalid? (complement valid?))



;;
;; Implementation
;;

(defn assoc-with
  ([f m k v]
     (let [ov (k m)
           nv (apply f [ov v])]
       (assoc m k nv)))
  ([f m k v & kvs]
     (let [ret (assoc-with f m k v)]
       (if kvs
         (recur f ret (first kvs) (second kvs) (nnext kvs))
         ret))))

(defn as-vec
  [arg]
  (if (sequential? arg)
    (vec arg)
    (vec [arg])))

(defn- concat-with-separator
  [v s]
  (apply str (interpose s v)))