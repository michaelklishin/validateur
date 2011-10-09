(ns validateur.test.validation
  (:use [clojure.test]
        [validateur.validation]))

;;
;; validation-set
;;


;; (deftest presence-validation
;;   (def v (validation-set
;;           (presence-of :name)
;;           (presence-of :age)))
;;   (is (valid? v { :name "Joe", :age 28 }))
;;   (is-not (valid? v { :name "Joe" }))
;;   (is-not (valid? v { :age 30 })))



;;
;; presence-of
;;

(deftest test-presence-validator-with-one-attribute
  (def v (presence-of :name))
  (is (fn? v))
  (is (= [true, {}] (apply v [{ :name "Michael" }])))
  (is (= [false, { :name #{"can't be blank"} }] (apply v [{ :age 28 }]))))



;;
;; numericality-of
;;


(deftest test-numerical-integer-only-validator-with-one-attribute
  (def v (numericality-of :age :only-integer true))
  (is (fn? v))
  (is (= [true, {}] (apply v [{ :age 26 }])))
  (is (= [false, { :age #{"should be a number" "should be an integer"} }] (apply v [{ :age "Twenty six" }])))
  (is (= [false, { :age #{"should be an integer"} }] (apply v [{ :age 26.6 }]))))


(deftest test-numerical-validator-with-one-attribute-that-is-gt-than-a-value
  (def v (numericality-of :age :gt 40))
  (is (fn? v))
  (is (= [true, {}] (apply v [{ :age 46 }])))
  (is (= [false, { :age #{"can't be blank"} }] (apply v [{ :age nil }])))
  (is (= [false, { :age #{"should be a number"} }] (apply v [{ :age "Twenty six" }])))
  (is (= [false, { :age #{"should be greater than 40"} }] (apply v [{ :age 26.6 }]))))


(deftest test-numerical-validator-with-one-attribute-that-is-lt-than-a-value
  (def v (numericality-of :age :lt 40))
  (is (fn? v))
  (is (= [true, {}] (apply v [{ :age 36 }])))
  (is (= [false, { :age #{"can't be blank"} }] (apply v [{ :age nil }])))
  (is (= [false, { :age #{"should be a number"} }] (apply v [{ :age "Twenty six" }])))
  (is (= [false, { :age #{"should be less than 40"} }] (apply v [{ :age 46.6 }]))))


(deftest test-numerical-validator-with-one-attribute-that-is-gte-than-a-value
  (def v (numericality-of :age :gte 40))
  (is (fn? v))
  (is (= [true, {}] (apply v [{ :age 46 }])))
  (is (= [true, {}] (apply v [{ :age 40 }])))
  (is (= [false, { :age #{"can't be blank"} }] (apply v [{ :age nil }])))
  (is (= [false, { :age #{"should be a number"} }] (apply v [{ :age "Twenty six" }])))
  (is (= [false, { :age #{"should be greater than or equal to 40"} }] (apply v [{ :age 26.6 }]))))


(deftest test-numerical-validator-with-one-attribute-that-is-lte-than-a-value
  (def v (numericality-of :age :lte 40))
  (is (fn? v))
  (is (= [true, {}] (apply v [{ :age 36 }])))
  (is (= [true, {}] (apply v [{ :age 40 }])))
  (is (= [false, { :age #{"can't be blank"} }] (apply v [{ :age nil }])))
  (is (= [false, { :age #{"should be a number"} }] (apply v [{ :age "Twenty six" }])))
  (is (= [false, { :age #{"should be less than or equal to 40"} }] (apply v [{ :age 46.6 }]))))



(deftest test-numerical-validator-with-one-attribute-that-is-equal-to-a-value
  (def v (numericality-of :age :equal-to 40))
  (is (fn? v))
  (is (= [true, {}] (apply v [{ :age 40 }])))
  (is (= [false, { :age #{"can't be blank"} }] (apply v [{ :age nil }])))
  (is (= [false, { :age #{"should be a number"} }] (apply v [{ :age "Twenty six" }])))
  (is (= [false, { :age #{"should be equal to 40"} }] (apply v [{ :age 46.6 }])))
  (is (= [false, { :age #{"should be equal to 40"} }] (apply v [{ :age 100 }]))))



(deftest test-numerical-validator-with-one-attribute-that-is-odd
  (def v (numericality-of :age :odd true))
  (is (fn? v))
  (is (= [true, {}] (apply v [{ :age 41 }])))
  (is (= [false, { :age #{"can't be blank"} }] (apply v [{ :age nil }])))
  (is (= [false, { :age #{"should be a number"} }] (apply v [{ :age "Twenty six" }])))
  (is (= [false, { :age #{"should be odd"} }] (apply v [{ :age 20 }]))))


(deftest test-numerical-validator-with-one-attribute-that-is-even
  (def v (numericality-of :age :even true))
  (is (fn? v))
  (is (= [true, {}] (apply v [{ :age 40 }])))
  (is (= [false, { :age #{"can't be blank"} }] (apply v [{ :age nil }])))
  (is (= [false, { :age #{"should be a number"} }] (apply v [{ :age "Twenty six" }])))
  (is (= [false, { :age #{"should be even"} }] (apply v [{ :age 21 }]))))



;;
;; acceptance-of
;;

(deftest test-acceptance-validator-with-one-attribute
  (def v (acceptance-of :terms-and-conditions))
  (is (fn? v))
  (is (= [true, {}] (apply v [{ :terms-and-conditions true }])))
  (is (= [false, { :terms-and-conditions #{"must be accepted"} }] (apply v [{ :terms-and-conditions "I do not approve it" }]))))


(deftest test-acceptance-validator-with-one-attribute-and-custom-accepted-values-list
  (def v (acceptance-of :terms-and-conditions :accept #{"yes", "hell yes"}))
  (is (fn? v))
  (is (= [true, {}] (apply v [{ :terms-and-conditions "yes" }])))
  (is (= [true, {}] (apply v [{ :terms-and-conditions "hell yes" }])))
  (is (= [false, { :terms-and-conditions #{"must be accepted"} }] (apply v [{ :terms-and-conditions true }])))
  (is (= [false, { :terms-and-conditions #{"must be accepted"} }] (apply v [{ :terms-and-conditions "I do not approve it" }])))
  (is (= [false, { :terms-and-conditions #{"must be accepted"} }] (apply v [{ :terms-and-conditions "1" }])))
  (is (= [false, { :terms-and-conditions #{"must be accepted"} }] (apply v [{ :terms-and-conditions "jeez no" }]))))


;;
;; inclusion-of
;;

(deftest test-inclusion-validator
  (def v (inclusion-of :genre :in #{"trance", "dnb"}))
  (is (fn? v))
  (is (= [false, { :genre #{"can't be blank"} }] (apply v [{ :genre nil }])))
  (is (= [true, {}] (apply v [{ :genre "trance" }])))
  (is (= [true, {}] (apply v [{ :genre "dnb" }])))
  (is (= [false, { :genre #{"must be one of: trance, dnb"} }] (apply v [{ :genre true }])))
  (is (= [false, { :genre #{"must be one of: trance, dnb"} }] (apply v [{ :genre "I do not approve it" }])))
  (is (= [false, { :genre #{"must be one of: trance, dnb"} }] (apply v [{ :genre "1" }]))))



;;
;; exclusion-of
;;

(deftest test-exclusion-validator
  (def v (exclusion-of :genre :in #{"trance", "dnb"}))
  (is (fn? v))
  (is (= [false, { :genre #{"can't be blank"} }] (apply v [{ :genre nil }])))
  (is (= [true, {}] (apply v [{ :genre "rock" }])))
  (is (= [true, {}] (apply v [{ :genre "power metal" }])))
  (is (= [false, { :genre #{"must not be one of: trance, dnb"} }] (apply v [{ :genre "trance" }])))
  (is (= [false, { :genre #{"must not be one of: trance, dnb"} }] (apply v [{ :genre "dnb" }]))))


;;
;; format-of
;;

(deftest test-format-of-validator
  (def v (format-of :id :format #"abc-\d\d\d"))
  (is (fn? v))
  (is (= [false, { :id #{"can't be blank"} }] (apply v [{ :id nil }])))
  (is (= [true, {}] (apply v [{ :id "abc-123" }])))
  (is (= [false, { :id #{"has incorrect format"} }] (apply v [{ :id "123-abc" }]))))



;;
;; Implementation functions
;;


(deftest test-as-vec
  (is (= [1 2 3] (as-vec [1 2 3])))
  (is (= [1 2 3] (as-vec '(1 2 3))))
  (is (= [10] (as-vec 10)))
  (is (= [{ :a 1, :b 2 }] (as-vec { :a 1, :b 2 }))))


(deftest test-assoc-with
  (is (= (assoc-with clojure.set/union {} :a #{"should not be nil"}) { :a #{"should not be nil"} }))
  (is (= (assoc-with clojure.set/union { :a #{1} } :a #{2}) { :a #{1 2} }))
  (is (= (assoc-with clojure.set/union { :a #{1} } :b #{2}) { :a #{1}, :b #{2} }))
  (is (= (assoc-with clojure.set/union { :a #{1} } :a #{2}, :b #{3}) { :a #{1 2}, :b #{3} })))
