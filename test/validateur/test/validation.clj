(ns validateur.test.validation
  (:use [clojure.test]
        [validateur.validation]))

(println (str "Using Clojure version " *clojure-version*))

;;
;; validation-set
;;


(deftest presence-validation-using-set
  (let [v (validation-set
           (presence-of :name) (presence-of :age))]
    (is (valid? v { :name "Joe", :age 28 }))
    (is (not (invalid? v { :name "Joe", :age 28 })))
    (is (not (valid? v { :name "Joe" })))
    (is (invalid? v { :name "Joe" }))
    (is (not (valid? v { :age 30 })))
    (is (invalid? v { :age 30 }))
    (is (= {:age #{ "can't be blank" }} (v { :name "Joe" })))
    (is (= {} (v { :name "Joe", :age 28 })))))



;;
;; presence-of
;;

(deftest test-presence-validator-with-one-attribute
  (let [v (presence-of :name)]
    (is (fn? v))
    (is (= [true {}]                             (v { :name "Michael" })))
    (is (= [false { :name #{"can't be blank"} }] (v { :age 28 })))))

(deftest test-presence-validator-with-one-nested-attribute
  (let [v (presence-of [:address :street])]
    (is (fn? v))
    (is (= [true {}]                                          (v { :address { :street "Old Rd" } })))
    (is (= [false { [:address :street] #{"can't be blank"} }] (v { :address {} })))))



;;
;; numericality-of
;;


(deftest test-numerical-integer-only-validator-with-one-attribute
  (let [v (numericality-of :age :only-integer true)]
    (is (fn? v))
    (is (= [true {}]                                                       (v { :age 26 })))
    (is (= [false { :age #{"should be a number" "should be an integer"} }] (v { :age "Twenty six" })))
    (is (= [false { :age #{"should be an integer"} }]                      (v { :age 26.6 })))))

(deftest ^:focus test-numerical-integer-only-validator-with-one-nested-attribute
         (let [v (numericality-of [:profile :age] :only-integer true)]
           (is (fn? v))
           (is (= [true {}]                                                                  (v { :profile { :age 26 }})))
           (is (= [false { [:profile :age] #{"should be a number" "should be an integer"} }] (v { :profile { :age "Twenty six" }})))
           (is (= [false { [:profile :age] #{"should be an integer"} }]                      (v { :profile { :age 26.6 }})))))


(deftest test-numerical-validator-with-one-attribute-that-is-gt-than-a-value
  (let [v (numericality-of :age :gt 40)]
    (is (fn? v))
    (is (= [true {}] (v { :age 46 })))
    (is (= [false { :age #{"can't be blank"} }]            (v { :age nil })))
    (is (= [false { :age #{"should be a number"} }]        (v { :age "Twenty six" })))
    (is (= [false { :age #{"should be greater than 40"} }] (v { :age 26.6 })))))


(deftest test-numerical-validator-with-one-attribute-that-is-lt-than-a-value
  (let [v (numericality-of :age :lt 40)]
    (is (fn? v))
    (is (= [true {}] (v { :age 36 })))
    (is (= [false { :age #{"can't be blank"} }]         (v { :age nil })))
    (is (= [false { :age #{"should be a number"} }]     (v { :age "Twenty six" })))
    (is (= [false { :age #{"should be less than 40"} }] (v { :age 46.6 })))))


(deftest test-numerical-validator-with-one-attribute-that-is-gte-than-a-value
  (let [v (numericality-of :age :gte 40)]
    (is (fn? v))
    (is (= [true {}] (v { :age 46 })))
    (is (= [true {}] (v { :age 40 })))
    (is (= [false { :age #{"can't be blank"} }]                        (v { :age nil })))
    (is (= [false { :age #{"should be a number"} }]                    (v { :age "Twenty six" })))
    (is (= [false { :age #{"should be greater than or equal to 40"} }] (v { :age 26.6 })))))


(deftest test-numerical-validator-with-one-attribute-that-is-lte-than-a-value
  (let [v (numericality-of :age :lte 40)]
    (is (fn? v))
    (is (= [true {}] (v { :age 36 })))
    (is (= [true {}] (v { :age 40 })))
    (is (= [false { :age #{"can't be blank"} }]                     (v { :age nil })))
    (is (= [false { :age #{"should be a number"} }]                 (v { :age "Twenty six" })))
    (is (= [false { :age #{"should be less than or equal to 40"} }] (v { :age 46.6 })))))



(deftest test-numerical-validator-with-one-attribute-that-is-equal-to-a-value
  (let [v (numericality-of :age :equal-to 40)]
    (is (fn? v))
    (is (= [true {}]                                   (v { :age 40 })))
    (is (= [false { :age #{"can't be blank"} }]        (v { :age nil })))
    (is (= [false { :age #{"should be a number"} }]    (v { :age "Twenty six" })))
    (is (= [false { :age #{"should be equal to 40"} }] (v { :age 46.6 })))
    (is (= [false { :age #{"should be equal to 40"} }] (v { :age 100 })))))



(deftest test-numerical-validator-with-one-attribute-that-is-odd
  (let [v (numericality-of :age :odd true)]
    (is (fn? v))
    (is (= [true {}]                                (v { :age 41 })))
    (is (= [false { :age #{"can't be blank"} }]     (v { :age nil })))
    (is (= [false { :age #{"should be a number"} }] (v { :age "Twenty six" })))
    (is (= [false { :age #{"should be odd"} }]      (v { :age 20 })))))


(deftest test-numerical-validator-with-one-attribute-that-is-even
  (let [v (numericality-of :age :even true)]
    (is (fn? v))
    (is (= [true {}]                                (v { :age 40 })))
    (is (= [false { :age #{"can't be blank"} }]     (v { :age nil })))
    (is (= [false { :age #{"should be a number"} }] (v { :age "Twenty six" })))
    (is (= [false { :age #{"should be even"} }]     (v { :age 21 })))))



;;
;; acceptance-of
;;

(deftest test-acceptance-validator-with-one-attribute
  (let [v (acceptance-of :terms-and-conditions)]
    (is (fn? v))
    (is (= [true {}]                                               (v { :terms-and-conditions true })))
    (is (= [false { :terms-and-conditions #{"must be accepted"} }] (v { :terms-and-conditions "I do not approve it" })))))


(deftest test-acceptance-validator-with-one-attribute-and-custom-accepted-values-list
  (let [v (acceptance-of :terms-and-conditions :accept #{"yes", "hell yes"})]
    (is (fn? v))
    (is (= [true {}]                                               (v { :terms-and-conditions "yes" })))
    (is (= [true {}]                                               (v { :terms-and-conditions "hell yes" })))
    (is (= [false { :terms-and-conditions #{"must be accepted"} }] (v { :terms-and-conditions true })))
    (is (= [false { :terms-and-conditions #{"must be accepted"} }] (v { :terms-and-conditions "I do not approve it" })))
    (is (= [false { :terms-and-conditions #{"must be accepted"} }] (v { :terms-and-conditions "1" })))
    (is (= [false { :terms-and-conditions #{"must be accepted"} }] (v { :terms-and-conditions "jeez no" })))))


;;
;; inclusion-of
;;

(deftest test-inclusion-validator
  (let [v (inclusion-of :genre :in #{"trance", "dnb"})]
    (is (fn? v))
    (is (= [false { :genre #{"can't be blank"} }]              (v { :genre nil })))
    (is (= [true {}]                                           (v { :genre "trance" })))
    (is (= [true {}]                                           (v { :genre "dnb" })))
    (is (= [false { :genre #{"must be one of: trance, dnb"} }] (v { :genre true })))
    (is (= [false { :genre #{"must be one of: trance, dnb"} }] (v { :genre "I do not approve it" })))
    (is (= [false { :genre #{"must be one of: trance, dnb"} }] (v { :genre "1" })))))

(deftest test-inclusion-validator-with-nested-attributes
  (let [v (inclusion-of [:track :genre] :in #{"trance", "dnb"})]
    (is (fn? v))
    (is (= [false { [:track :genre] #{"can't be blank"} }]     (v { [:track :genre] nil })))
    (is (= [true {}]                                           (v { :track {:genre "trance" }})))
    (is (= [true {}]                                           (v { :track {:genre "dnb" }})))
    (is (= [false { [:track :genre] #{"must be one of: trance, dnb"} }] (v {:track {:genre true }})))
    (is (= [false { [:track :genre] #{"must be one of: trance, dnb"} }] (v {:track {:genre "I do not approve it" }})))
    (is (= [false { [:track :genre] #{"must be one of: trance, dnb"} }] (v {:track {:genre "1" }})))))



;;
;; exclusion-of
;;

(deftest test-exclusion-validator
  (let [v (exclusion-of :genre :in #{"trance", "dnb"})]
    (is (fn? v))
    (is (= [false { :genre #{"can't be blank"} }]                  (v { :genre nil })))
    (is (= [true {}]                                               (v { :genre "rock" })))
    (is (= [true {}]                                               (v { :genre "power metal" })))
    (is (= [false { :genre #{"must not be one of: trance, dnb"} }] (v { :genre "trance" })))
    (is (= [false { :genre #{"must not be one of: trance, dnb"} }] (v { :genre "dnb" })))))



;;
;; length-of
;;

(deftest test-length-validator-with-fixed-length
  (let [v (length-of :title :is 11)]
    (is (fn? v))
    (is (= [false { :title #{"can't be blank"} }]             (v { :title nil })))
    (is (= [true {}]                                          (v { :title "power metal" })))
    (is (= [false { :title #{"must be 11 characters long"} }] (v { :title "trance" })))
    (is (= [false { :title #{"must be 11 characters long"} }] (v { :title "dnb" })))
    (is (= [false { :title #{"must be 11 characters long"} }] (v { :title "melodic power metal" })))))

(deftest test-length-validator-with-fixed-length-that-allows-blanks
  (let [v (length-of :title :is 11 :allow-blank true :allow-nil true)]
    (is (fn? v))
    (is (= [true {}] (v { :title nil })))
    (is (= [true {}] (v { :title "" })))
    (is (= [true {}]                                          (v { :title "power metal" })))
    (is (= [false { :title #{"must be 11 characters long"} }] (v { :title "trance" })))
    (is (= [false { :title #{"must be 11 characters long"} }] (v { :title "dnb" })))
    (is (= [false { :title #{"must be 11 characters long"} }] (v { :title "melodic power metal" })))))

(deftest test-length-validator-with-range-length
  (let [v (length-of :title :within (range 9 13))]
    (is (fn? v))
    (is (= [false { :title #{"can't be blank"} }]             (v { :title nil })))
    (is (= [true {}]                                          (v { :title "power metal" })))
    (is (= [false { :title #{"must be from 9 to 12 characters long"} }] (v { :title "trance" })))
    (is (= [false { :title #{"must be from 9 to 12 characters long"} }] (v { :title "dnb" })))
    (is (= [false { :title #{"must be from 9 to 12 characters long"} }] (v { :title "melodic power metal" })))))

(deftest test-length-validator-with-range-length-that-allows-blanks
  (let [v (length-of :title :within (range 9 13) :allow-nil true :allow-blank true)]
    (is (fn? v))
    (is (= [true {}] (v { :title nil })))
    (is (= [true {}] (v { :title "" })))
    (is (= [true {}]                                          (v { :title "power metal" })))
    (is (= [false { :title #{"must be from 9 to 12 characters long"} }] (v { :title "trance" })))
    (is (= [false { :title #{"must be from 9 to 12 characters long"} }] (v { :title "dnb" })))
    (is (= [false { :title #{"must be from 9 to 12 characters long"} }] (v { :title "melodic power metal" })))))


;;
;; format-of
;;

(deftest test-format-of-validator
  (let [v (format-of :id :format #"abc-\d\d\d")]
    (is (fn? v))
    (is (= [false { :id #{"can't be blank"} }]       (v { :id nil })))
    (is (= [true {}]                                 (v { :id "abc-123" })))
    (is (= [false { :id #{"has incorrect format"} }] (v { :id "123-abc" })))))


(deftest test-format-of-validator-that-allows-blanks
  (let [v (format-of :id :format #"abc-\d\d\d" :allow-blank true)]
    (is (= [false { :id #{"can't be blank"} }]       (v { :id nil })))
    (is (= [true {}]                                 (v { :id "" })))
    (is (= [true {}]                                 (v { :id "abc-123" })))
    (is (= [false { :id #{"has incorrect format"} }] (v { :id "123-abc" })))))

(deftest test-format-of-validator-with-custom-message
  (let [v (format-of :id :format #"abc-\d\d\d" :message "is improperly formatted")]
    (is (= [false { :id #{"is improperly formatted"} }] (v { :id "123-abc" })))))



;;
;; Implementation functions
;;


(deftest test-as-vec
  (is (= [1 2 3] (as-vec [1 2 3])))
  (is (= [1 2 3] (as-vec '(1 2 3))))
  (is (= [10] (as-vec 10)))
  (is (= [{ :a 1 :b 2 }] (as-vec { :a 1 :b 2 }))))
