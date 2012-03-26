(ns validateur.validation
  (:use [clojure.set :as set]
        [clojurewerkz.support.core :only (assoc-with)])
  (:require [clojure.string]))


;;
;; Implementation
;;

(defn as-vec
  [arg]
  (if (sequential? arg)
    (vec arg)
    (vec [arg])))

(defn- concat-with-separator
  [v s]
  (apply str (interpose s v)))

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
    [false { attribute #{(str "must be " expected-length " characters long")} }]))

(defn- range-length-of
  [attribute actual xs allow-nil allow-blank]
  (if (or (member? xs (count actual))
          (allowed-to-be-blank? actual allow-nil allow-blank))
    [true {}]
    [false { attribute #{(str "must be from " (first xs) " to " (last xs) " characters long")} }]))



;;
;; API
;;

(defn presence-of
  [attribute]
  (fn [m]
    (let [v      (if (vector? attribute)
                   (get-in m attribute)
                   (attribute m))
          errors (if v {} { attribute #{"can't be blank"} })]
      [(empty? errors) errors])))


(defn numericality-of
  [attribute & { :keys [allow-nil only-integer gt gte lt lte equal-to odd even] :or { allow-nil false
                                                                                     only-integer false
                                                                                     odd false
                                                                                     event false }}]
  (fn [m]
    (let [v       (if (vector? attribute)
                    (get-in m attribute)
                    (attribute m))
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
  [attribute & { :keys [allow-nil accept] :or { allow-nil false accept #{true "true", "1"} }}]
  (fn [m]
    (let [v (if (vector? attribute)
              (get-in m attribute)
              (get m attribute))]
      (if (and (nil? v) (not allow-nil))
        [false { attribute #{"can't be blank"} }]
        (if (accept v)
          [true {}]
          [false { attribute #{"must be accepted"} }])))))



(defn inclusion-of
  [attribute & { :keys [allow-nil in] :or { allow-nil false }}]
  (fn [m]
    (let [v (if (vector? attribute)
              (get-in m attribute)
              (get m attribute))]
      (if (and (nil? v) (not allow-nil))
        [false { attribute #{"can't be blank"} }]
        (if (in v)
          [true {}]
          [false { attribute #{(str "must be one of: " (concat-with-separator in ", "))} }])))))



(defn exclusion-of
  [attribute & { :keys [allow-nil in] :or { allow-nil false }}]
  (fn [m]
    (let [v (if (vector? attribute)
              (get-in m attribute)
              (get m attribute))]
      (if (and (nil? v) (not allow-nil))
        [false { attribute #{"can't be blank"} }]
        (if-not (in v)
          [true {}]
          [false { attribute #{(str "must not be one of: " (concat-with-separator in ", "))} }])))))



(defn format-of
  [attribute & { :keys [allow-nil allow-blank format] :or { allow-nil false allow-blank false }}]
  (fn [m]
    (let [v (if (vector? attribute)
              (get-in m attribute)
              (get m attribute))]
      (if (not-allowed-to-be-blank? v allow-nil allow-blank)
        [false { attribute #{"can't be blank"} }]
        (if (or (allowed-to-be-blank? v allow-nil allow-blank)
                (re-find format v))
          [true {}]
          [false { attribute #{"has incorrect format"} }])))))



(defn length-of
  [attribute & { :keys [allow-nil is within allow-blank] :or { allow-nil false allow-blank false }}]
  (fn [m]
    (let [v (if (vector? attribute)
              (get-in m attribute)
              (get m attribute))]
      (if (not-allowed-to-be-blank? v allow-nil allow-blank)
        [false { attribute #{"can't be blank"} }]
        (if within
          (range-length-of attribute v within allow-nil allow-blank)
          (equal-length-of attribute v is     allow-nil allow-blank))))))




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
