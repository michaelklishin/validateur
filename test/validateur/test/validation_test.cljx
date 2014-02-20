(ns validateur.test.validation-test
  (:require [validateur.validation :as vr]
            #+clj [clojure.test :refer :all]
            #+cljs [cemerick.cljs.test :as t])
  #+cljs (:require-macros [cemerick.cljs.test :refer (is deftest testing)]))

#+clj (println (str "Using Clojure version " *clojure-version*))

(defn test-message-fn [type map attr & args]
  [type map attr args])
;;
;; validation-set
;;


(deftest presence-validation-using-set
  (let [v (vr/validation-set
           (vr/presence-of :name) (vr/presence-of :age))]
    (is (vr/valid? v { :name "Joe", :age 28 }))
    (is (vr/valid? (v { :name "Joe", :age 28 })))
    (is (not (vr/invalid? v { :name "Joe", :age 28 })))
    (is (not (vr/valid? v { :name "Joe" })))
    (is (not (vr/valid? (v { :name "Joe" }))))
    (is (vr/invalid? v { :name "Joe" :age nil }))
    (is (vr/invalid? v { :name "Joe" :age "" }))
    (is (vr/invalid? v { :name "Joe" :age "   " }))
    (is (not (vr/invalid? v { :name "Joe" :age " a " })))
    (is (vr/invalid? v { :name "Joe" }))
    (is (not (vr/valid? v { :age 30 })))
    (is (vr/invalid? v { :age 30 }))
    (is (= {:age #{ "can't be blank" }} (v { :name "Joe" })))
    (is (= {} (v { :name "Joe", :age 28 })))))

;;
;; compose-set
;;


(deftest presence-compose-validation-set
  (let [vn (vr/validation-set
             (vr/presence-of :name))
        va (vr/validation-set
             (vr/presence-of :age))
        v  (vr/compose-sets va vn)]
    (is (vr/valid? v { :name "Joe", :age 28 }))
    (is (not (vr/invalid? v { :name "Joe", :age 28 })))
    (is (not (vr/valid? v { :name "Joe" })))
    (is (vr/invalid? v { :name "Joe" :age nil }))
    (is (vr/invalid? v { :name "Joe" :age "" }))
    (is (vr/invalid? v { :name "Joe" :age "   " }))
    (is (not (vr/invalid? v { :name "Joe" :age " a " })))
    (is (vr/invalid? v { :name "Joe" }))
    (is (not (vr/valid? v { :age 30 })))
    (is (vr/invalid? v { :age 30 }))
    (is (= {:age #{ "can't be blank" }} (v { :name "Joe" })))
    (is (= {} (v { :name "Joe", :age 28 })))))



;;
;; presence-of
;;

(deftest test-presence-validator-with-one-attribute
  (let [v (vr/presence-of :name)]
    (is (fn? v))
    (is (= [true {}]                             (v { :name "Michael" })))
    (is (= [false { :name #{"can't be blank"} }] (v { :age 28 })))))

(deftest test-presence-validator-with-one-nested-attribute
  (let [v (vr/presence-of [:address :street])]
    (is (fn? v))
    (is (= [true {}]                                          (v { :address { :street "Old Rd" } })))
    (is (= [false { [:address :street] #{"can't be blank"} }] (v { :address {} })))))

(deftest test-presence-of-validator-with-custom-message
  (let [v (vr/presence-of :name :message "Низя, должно быть заполнено!")]
    (is (= [false { :name #{"Низя, должно быть заполнено!"} }] (v { :age 28 })))))

(deftest test-presence-of-validator-with-optional-message-fn
  (let [v (vr/presence-of :name :message-fn test-message-fn)]
    (is (= [false { :name #{[:blank {:age 28} :name nil]} }] (v { :age 28 })))))

;;
;; numericality-of
;;


(deftest test-numerical-integer-only-validator-with-one-attribute
  (let [v (vr/numericality-of :age :only-integer true)]
    (is (fn? v))
    (is (= [true {}]                                                       (v { :age 26 })))
    (is (= [false { :age #{"should be a number" "should be an integer"} }] (v { :age "Twenty six" })))
    (is (= [false { :age #{"should be an integer"} }]                      (v { :age 26.6 })))))

(deftest ^:focus test-numerical-integer-only-validator-with-one-nested-attribute
         (let [v (vr/numericality-of [:profile :age] :only-integer true)]
           (is (fn? v))
           (is (= [true {}]                                                                  (v { :profile { :age 26 }})))
           (is (= [false { [:profile :age] #{"should be a number" "should be an integer"} }] (v { :profile { :age "Twenty six" }})))
           (is (= [false { [:profile :age] #{"should be an integer"} }]                      (v { :profile { :age 26.6 }})))))


(deftest test-numerical-validator-with-one-attribute-that-is-gt-than-a-value
  (let [v (vr/numericality-of :age :gt 40)]
    (is (fn? v))
    (is (= [true {}] (v { :age 46 })))
    (is (= [false { :age #{"can't be blank"} }]            (v { :age nil })))
    (is (= [false { :age #{"should be a number"} }]        (v { :age "Twenty six" })))
    (is (= [false { :age #{"should be greater than 40"} }] (v { :age 26.6 })))))


(deftest test-numerical-validator-with-one-attribute-that-is-lt-than-a-value
  (let [v (vr/numericality-of :age :lt 40)]
    (is (fn? v))
    (is (= [true {}] (v { :age 36 })))
    (is (= [false { :age #{"can't be blank"} }]         (v { :age nil })))
    (is (= [false { :age #{"should be a number"} }]     (v { :age "Twenty six" })))
    (is (= [false { :age #{"should be less than 40"} }] (v { :age 46.6 })))))


(deftest test-numerical-validator-with-one-attribute-that-is-gte-than-a-value
  (let [v (vr/numericality-of :age :gte 40)]
    (is (fn? v))
    (is (= [true {}] (v { :age 46 })))
    (is (= [true {}] (v { :age 40 })))
    (is (= [false { :age #{"can't be blank"} }]                        (v { :age nil })))
    (is (= [false { :age #{"should be a number"} }]                    (v { :age "Twenty six" })))
    (is (= [false { :age #{"should be greater than or equal to 40"} }] (v { :age 26.6 })))))


(deftest test-numerical-validator-with-one-attribute-that-is-lte-than-a-value
  (let [v (vr/numericality-of :age :lte 40)]
    (is (fn? v))
    (is (= [true {}] (v { :age 36 })))
    (is (= [true {}] (v { :age 40 })))
    (is (= [false { :age #{"can't be blank"} }]                     (v { :age nil })))
    (is (= [false { :age #{"should be a number"} }]                 (v { :age "Twenty six" })))
    (is (= [false { :age #{"should be less than or equal to 40"} }] (v { :age 46.6 })))))



(deftest test-numerical-validator-with-one-attribute-that-is-equal-to-a-value
  (let [v (vr/numericality-of :age :equal-to 40)]
    (is (fn? v))
    (is (= [true {}]                                   (v { :age 40 })))
    (is (= [false { :age #{"can't be blank"} }]        (v { :age nil })))
    (is (= [false { :age #{"should be a number"} }]    (v { :age "Twenty six" })))
    (is (= [false { :age #{"should be equal to 40"} }] (v { :age 46.6 })))
    (is (= [false { :age #{"should be equal to 40"} }] (v { :age 100 })))))



(deftest test-numerical-validator-with-one-attribute-that-is-odd
  (let [v (vr/numericality-of :age :odd true)]
    (is (fn? v))
    (is (= [true {}]                                (v { :age 41 })))
    (is (= [false { :age #{"can't be blank"} }]     (v { :age nil })))
    (is (= [false { :age #{"should be a number"} }] (v { :age "Twenty six" })))
    (is (= [false { :age #{"should be odd"} }]      (v { :age 20 })))))


(deftest test-numerical-validator-with-one-attribute-that-is-even
  (let [v (vr/numericality-of :age :even true)]
    (is (fn? v))
    (is (= [true {}]                                (v { :age 40 })))
    (is (= [false { :age #{"can't be blank"} }]     (v { :age nil })))
    (is (= [false { :age #{"should be a number"} }] (v { :age "Twenty six" })))
    (is (= [false { :age #{"should be even"} }]     (v { :age 21 })))))


(deftest test-numerical-validator-with-optional-messages
  (let [msgs {:number "number" :blank "blank"
              :only-integer "integer" :equal-to "equal to "}
        v #(vr/numericality-of :age %1 %2 :messages msgs)]
    (testing "only integers"
      (is (= [false { :age #{"number" "integer"} }]
             ((v :only-integer true) { :age "Twenty six" })))
      (is (= [false { :age #{"integer"} }]
             ((v :only-integer true) { :age 26.6 }))))
    (testing "equal to"
      (is (= [false { :age #{"blank"} }]
             ((v :equal-to 1) { :age nil })))
      (is (= [false { :age #{"equal to 1"} }]
             ((v :equal-to 1) { :age 26.6 }))))
    (testing "optional messages are merged with default ones"
      (is (= [false { :age #{"should be greater than 40"} }]
             ((v :gt 40) { :age 20 })))
      (is (= [false { :age #{"should be odd"} }]
             ((v :odd true) { :age 20 }))))))

(deftest test-numerical-validator-with-optional-message-fn
  (let [v #(vr/numericality-of :age %1 %2 :message-fn test-message-fn)]
    (is (= [false {:age #{[:number {:age "26"} :age nil]
                          [:only-integer {:age "26"} :age nil]} }]
           ((v :only-integer true) {:age "26" })))
    (is (= [false {:age #{[:only-integer
                           {:age 26.6 :other-key 1} :age nil]}}]
           ((v :only-integer true) {:age 26.6 :other-key 1})))
    (is (= [false {:age #{[:equal-to {:age 26.6} :age [666]]}}]
           ((v :equal-to 666) {:age 26.6})))))
;;
;; acceptance-of
;;

(deftest test-acceptance-validator-with-one-attribute
  (let [v (vr/acceptance-of :terms-and-conditions)]
    (is (fn? v))
    (is (= [true {}]                                               (v { :terms-and-conditions true })))
    (is (= [false { :terms-and-conditions #{"must be accepted"} }] (v { :terms-and-conditions "I do not approve it" })))))

(deftest test-acceptance-validator-with-custom-message
  (let [v (vr/acceptance-of :terms-and-conditions :message "ZOMG")]
    (is (fn? v))
    (is (= [true {}]                                               (v { :terms-and-conditions true })))
    (is (= [false { :terms-and-conditions #{"ZOMG"} }] (v { :terms-and-conditions "I do not approve it" })))))


(deftest test-acceptance-validator-with-one-attribute-and-custom-accepted-values-list
  (let [v (vr/acceptance-of :terms-and-conditions :accept #{"yes", "hell yes"})]
    (is (fn? v))
    (is (= [true {}]                                               (v { :terms-and-conditions "yes" })))
    (is (= [true {}]                                               (v { :terms-and-conditions "hell yes" })))
    (is (= [false { :terms-and-conditions #{"must be accepted"} }] (v { :terms-and-conditions true })))
    (is (= [false { :terms-and-conditions #{"must be accepted"} }] (v { :terms-and-conditions "I do not approve it" })))
    (is (= [false { :terms-and-conditions #{"must be accepted"} }] (v { :terms-and-conditions "1" })))
    (is (= [false { :terms-and-conditions #{"must be accepted"} }] (v { :terms-and-conditions "jeez no" })))))

(deftest test-acceptance-validator-with-custom-blank-message
  (let [v (vr/acceptance-of :terms-and-conditions :blank-message "ZOMG")]
    (is (fn? v))
    (is (= [true {}]
           (v { :terms-and-conditions true })))
    (is (= [false { :terms-and-conditions #{"ZOMG"} }]
           (v { :terms-and-conditions nil })))))

(deftest test-acceptance-validator-with-custom-message-fn
  (let [v (vr/acceptance-of :terms-and-conditions :message-fn test-message-fn)
        valid { :terms-and-conditions true}
        invalid { :terms-and-conditions "I do not approve it"}
        invalid-blank { :terms-and-conditions nil}
        default-acceptance-values #{"true" true "1"}]
    (is (fn? v))
    (is (= [true {}] (v valid)))
    (is (= [false { :terms-and-conditions
                   #{[:acceptance invalid :terms-and-conditions
                      [default-acceptance-values]]}}]
           (v invalid )))
    (is (= [false { :terms-and-conditions
                   #{[:blank invalid-blank :terms-and-conditions nil]}}]
           (v invalid-blank )))))

(deftest test-acceptance-validator-with-custom-accepted-values-and-message-fn
  (let [acceptance-values #{"yes" "hell yes"}
        v (vr/acceptance-of :terms-and-conditions :message-fn test-message-fn
                         :accept acceptance-values)
        valid { :terms-and-conditions "hell yes"}
        invalid { :terms-and-conditions "I do not approve it"}]
    (is (fn? v))
    (is (= [true {}] (v valid)))
    (is (= [false { :terms-and-conditions
                   #{[:acceptance invalid :terms-and-conditions
                      [acceptance-values]]}}]
           (v invalid )))))

;;
;; all-keys-in
;;

(deftest test-allowed-keys-validator
  (let [allowed-keys #{:turing "von neumann" 1954}
        v (vr/all-keys-in allowed-keys)]
    (is (fn? v))
    (is (= [true {}] (v {:turing "top"})))
    (is (= [true {}] (v {"von neumann" :von-neumann})))
    (is (= [true {}] (v {1954 1954})))
    (is (= [true {}] (v {:turing 1, "von neumann" 2, 1954 4591})))
    (is (= [false {:babbage #{"unknown key"}}] (v {:babbage "lovelace"})))
    (is (= [false {4591 #{"unknown key"}}] (v {4591 6.28})))
    (is (= [false {"church" #{"unknown key"}}] (v {"church" "none"})))))

;;
;; inclusion-of
;;

(deftest test-inclusion-validator
  (let [v (vr/inclusion-of :genre :in #{"trance", "dnb"})]
    (is (fn? v))
    (is (= [false { :genre #{"can't be blank"} }]              (v { :genre nil })))
    (is (= [true {}]                                           (v { :genre "trance" })))
    (is (= [true {}]                                           (v { :genre "dnb" })))
    (is (= [false { :genre #{"must be one of: trance, dnb"} }] (v { :genre true })))
    (is (= [false { :genre #{"must be one of: trance, dnb"} }] (v { :genre "I do not approve it" })))
    (is (= [false { :genre #{"must be one of: trance, dnb"} }] (v { :genre "1" })))))

(deftest test-inclusion-validator-with-booleans
  (let [v (vr/inclusion-of :truth :in #{true, false})]
    (is (fn? v))
    (is (= [false { :truth #{"can't be blank"} }]              (v { :truth nil })))
    (is (= [true {}]                                           (v { :truth true })))
    (is (= [true {}]                                           (v { :truth false })))
    (is (= [false { :truth #{"must be one of: true, false"} }] (v { :truth "foo" })))
    (is (= [false { :truth #{"must be one of: true, false"} }] (v { :truth "I do not approve it" })))
    (is (= [false { :truth #{"must be one of: true, false"} }] (v { :truth "1" })))))

(deftest test-inclusion-validator-with-nested-attributes
  (let [v (vr/inclusion-of [:track :genre] :in #{"trance", "dnb"})]
    (is (fn? v))
    (is (= [false { [:track :genre] #{"can't be blank"} }]     (v { [:track :genre] nil })))
    (is (= [true {}]                                           (v { :track {:genre "trance" }})))
    (is (= [true {}]                                           (v { :track {:genre "dnb" }})))
    (is (= [false { [:track :genre] #{"must be one of: trance, dnb"} }] (v {:track {:genre true }})))
    (is (= [false { [:track :genre] #{"must be one of: trance, dnb"} }] (v {:track {:genre "I do not approve it" }})))
    (is (= [false { [:track :genre] #{"must be one of: trance, dnb"} }] (v {:track {:genre "1" }})))))

(deftest test-inclusion-validator-with-custom-message
  (let [v (vr/inclusion-of :genre :in #{"trance", "dnb"}
                        :message "one of: ")]
    (is (fn? v))
    (is (= [false { :genre #{"one of: trance, dnb"} }]
           (v { :genre "dub step" })))
    (is (= [true {}]
           (v { :genre "trance" })))))

(deftest test-inclusion-validator-with-custom-blank-message
  (let [v (vr/inclusion-of :genre :in #{"trance", "dnb"}
                        :blank-message "test")]
    (is (fn? v))
    (is (= [false { :genre #{"test"} }]
           (v { :genre nil })))
    (is (= [true {}]
           (v { :genre "trance" })))))

(deftest test-inclusion-validator-with-optional-message-fn
  (let [v (vr/inclusion-of :genre :in #{"trance", "dnb"}
                        :message-fn test-message-fn)]
    (is (fn? v))
    (is (= [false {:genre #{[:blank {:genre nil} :genre nil]}}]
           (v {:genre nil})))
    (is (= [true {}]
           (v {:genre "trance"})))
    (is (= [true {}]
           (v {:genre "dnb"})))
    (is (= [false {:genre #{[:inclusion {:genre true} :genre
                             [#{"trance", "dnb"}]]}}]
           (v {:genre true})))
    (is (= [false {:genre #{[:inclusion {:genre "dub step"} :genre
                             [#{"trance", "dnb"}]]}}]
           (v {:genre "dub step"})))
    (is (= [false {:genre #{[:inclusion {:genre "1"} :genre
                             [#{"trance", "dnb"}]]}}]
           (v { :genre "1" })))))

(deftest test-inclusion-validation-with-allow-nil-true
  (let [v (vr/inclusion-of :genre :in #{"trance" "dnb"} :allow-nil true)]
    (is (fn? v))
    (is (= [true {}] (v {:genre "trance"})))
    (is (= [true {}] (v {:genre "dnb"})))
    (is (= [false {:genre #{"must be one of: trance, dnb"}}] (v {:genre "hiphop"})))
    (is (= [true {}] (v {:category "trance"})))
    (is (= [true {}] (v {:genre nil})))))

;;
;; exclusion-of
;;

(deftest test-exclusion-validator
  (let [v (vr/exclusion-of :genre :in #{"trance", "dnb"})]
    (is (fn? v))
    (is (= [false { :genre #{"can't be blank"} }]                  (v { :genre nil })))
    (is (= [true {}]                                               (v { :genre "rock" })))
    (is (= [true {}]                                               (v { :genre "power metal" })))
    (is (= [false { :genre #{"must not be one of: trance, dnb"} }] (v { :genre "trance" })))
    (is (= [false { :genre #{"must not be one of: trance, dnb"} }] (v { :genre "dnb" })))))

(deftest test-exclusion-validator-with-booleans
  (let [v (vr/exclusion-of :truth :in #{true false})]
    (is (fn? v))
    (is (= [false { :truth #{"can't be blank"} }]                  (v { :truth nil })))
    (is (= [true {}]                                               (v { :truth "rock" })))
    (is (= [true {}]                                               (v { :truth "power metal" })))
    (is (= [false { :truth #{"must not be one of: true, false"} }] (v { :truth true })))
    (is (= [false { :truth #{"must not be one of: true, false"} }] (v { :truth false })))))

(deftest test-exclusion-validator-with-custom-message
  (let [v (vr/exclusion-of :genre :in #{"trance", "dnb"}
                        :message "not one of: ")]
    (is (fn? v))
    (is (= [false { :genre #{"not one of: trance, dnb"} }]
           (v { :genre "trance" })))
    (is (= [true {}]
           (v { :genre "swing" })))))

(deftest test-exclusion-validator-with-custom-blank-message
  (let [v (vr/exclusion-of :genre :in #{"trance", "dnb"}
                        :blank-message "test")]
    (is (fn? v))
    (is (= [false { :genre #{"test"} }]
           (v { :genre nil })))
    (is (= [true {}]
           (v { :genre "swing" })))))

(deftest test-exclusion-validator-with-optional-message-fn
  (let [v (vr/exclusion-of :genre :in #{"trance", "dnb"}
                        :message-fn test-message-fn)]
    (is (fn? v))
    (is (= [false { :genre #{[:blank {:genre nil} :genre nil]} }]
           (v { :genre nil })))
    (is (= [true {}]
           (v { :genre "rock" })))
    (is (= [true {}]
           (v { :genre "power metal" })))
    (is (= [false { :genre #{[:exclusion {:genre "trance"} :genre
                             [#{"trance", "dnb"}]]} }]
           (v { :genre "trance" })))
    (is (= [false { :genre #{[:exclusion {:genre "dnb"} :genre
                             [#{"trance", "dnb"}]]} }]
           (v { :genre "dnb" })))))

;;
;; length-of
;;

(deftest test-length-validator-with-fixed-length
  (let [v (vr/length-of :title :is 11)]
    (is (fn? v))
    (is (= [false { :title #{"can't be blank"} }]             (v { :title nil })))
    (is (= [true {}]                                          (v { :title "power metal" })))
    (is (= [false { :title #{"must be 11 characters long"} }] (v { :title "trance" })))
    (is (= [false { :title #{"must be 11 characters long"} }] (v { :title "dnb" })))
    (is (= [false { :title #{"must be 11 characters long"} }] (v { :title "melodic power metal" })))))

(deftest test-length-validator-with-fixed-length-that-allows-blanks
  (let [v (vr/length-of :title :is 11 :allow-blank true)]
    (is (fn? v))
    (is (= [false { :title #{"can't be blank"} }]             (v { :title nil })))
    (is (= [true {}]                                          (v { :title "" })))
    (is (= [true {}]                                          (v { :title "power metal" })))
    (is (= [false { :title #{"must be 11 characters long"} }] (v { :title "trance" })))
    (is (= [false { :title #{"must be 11 characters long"} }] (v { :title "dnb" })))
    (is (= [false { :title #{"must be 11 characters long"} }] (v { :title "melodic power metal" })))))

(deftest test-length-validator-with-fixed-length-that-allows-nil
  (let [v (vr/length-of :title :is 11 :allow-nil true)]
    (is (fn? v))
    (is (= [true {}]                                          (v { :title nil })))
    (is (= [false { :title #{"can't be blank"} }]             (v { :title "" })))
    (is (= [true {}]                                          (v { :title "power metal" })))
    (is (= [false { :title #{"must be 11 characters long"} }] (v { :title "trance" })))
    (is (= [false { :title #{"must be 11 characters long"} }] (v { :title "dnb" })))
    (is (= [false { :title #{"must be 11 characters long"} }] (v { :title "melodic power metal" })))))

(deftest test-length-validator-with-fixed-length-that-allows-blanks-and-nil
  (let [v (vr/length-of :title :is 11 :allow-blank true :allow-nil true)]
    (is (fn? v))
    (is (= [true {}]                                          (v { :title nil })))
    (is (= [true {}]                                          (v { :title "" })))
    (is (= [true {}]                                          (v { :title "power metal" })))
    (is (= [false { :title #{"must be 11 characters long"} }] (v { :title "trance" })))
    (is (= [false { :title #{"must be 11 characters long"} }] (v { :title "dnb" })))
    (is (= [false { :title #{"must be 11 characters long"} }] (v { :title "melodic power metal" })))))

(deftest test-length-validator-with-fixed-length-with-optional-blank-message
  (let [v (vr/length-of :title :is 11 :blank-message "test blank")]
    (is (fn? v))
    (is (= [false { :title #{"test blank"} }]
           (v { :title nil })))
    (is (= [true {}]
           (v { :title "power metal" })))))

(deftest test-length-validator-with-fixed-length-with-optional-message-fn
  (let [v (vr/length-of :title :is 11 :message-fn test-message-fn)]
    (is (fn? v))
    (is (= [false {:title #{[:blank {:title nil} :title nil]}}]
           (v { :title nil })))
    (is (= [true {}]
           (v { :title "power metal" })))
    (is (= [false { :title #{[:length:is {:title "trance"} :title [11]]} }]
           (v { :title "trance" })))))


(deftest test-length-validator-with-range-length
  (let [v (vr/length-of :title :within (range 9 13))]
    (is (fn? v))
    (is (= [false { :title #{"can't be blank"} }]             (v { :title nil })))
    (is (= [true {}]                                          (v { :title "power metal" })))
    (is (= [false { :title #{"must be from 9 to 12 characters long"} }] (v { :title "trance" })))
    (is (= [false { :title #{"must be from 9 to 12 characters long"} }] (v { :title "dnb" })))
    (is (= [false { :title #{"must be from 9 to 12 characters long"} }] (v { :title "melodic power metal" })))))

(deftest test-length-validator-with-range-length-with-optional-blank-message
  (let [v (vr/length-of :title :within (range 9 13) :blank-message "test blank")]
    (is (fn? v))
    (is (= [false { :title #{"test blank"} }]
           (v { :title nil })))
    (is (= [true {}]
           (v { :title "power metal" })))))

(deftest test-length-validator-with-range-length-that-allows-blanks
  (let [v (vr/length-of :title :within (range 9 13) :allow-blank true)]
    (is (fn? v))
    (is (= [false { :title #{"can't be blank"} }]                       (v { :title nil })))
    (is (= [true {}]                                                    (v { :title "" })))
    (is (= [true {}]                                                    (v { :title "power metal" })))
    (is (= [false { :title #{"must be from 9 to 12 characters long"} }] (v { :title "trance" })))
    (is (= [false { :title #{"must be from 9 to 12 characters long"} }] (v { :title "dnb" })))
    (is (= [false { :title #{"must be from 9 to 12 characters long"} }] (v { :title "melodic power metal" })))))

(deftest test-length-validator-with-range-length-that-allows-nil
  (let [v (vr/length-of :title :within (range 9 13) :allow-nil true)]
    (is (fn? v))
    (is (= [true {}]                                                    (v { :title nil })))
    (is (= [false { :title #{"can't be blank"} }]                       (v { :title "" })))
    (is (= [true {}]                                                    (v { :title "power metal" })))
    (is (= [false { :title #{"must be from 9 to 12 characters long"} }] (v { :title "trance" })))
    (is (= [false { :title #{"must be from 9 to 12 characters long"} }] (v { :title "dnb" })))
    (is (= [false { :title #{"must be from 9 to 12 characters long"} }] (v { :title "melodic power metal" })))))

(deftest test-length-validator-with-range-length-that-allows-blanks-and-nil
  (let [v (vr/length-of :title :within (range 9 13) :allow-blank true :allow-nil true)]
    (is (fn? v))
    (is (= [true {}]                                                    (v { :title nil })))
    (is (= [true {}]                                                    (v { :title "" })))
    (is (= [true {}]                                                    (v { :title "power metal" })))
    (is (= [false { :title #{"must be from 9 to 12 characters long"} }] (v { :title "trance" })))
    (is (= [false { :title #{"must be from 9 to 12 characters long"} }] (v { :title "dnb" })))
    (is (= [false { :title #{"must be from 9 to 12 characters long"} }] (v { :title "melodic power metal" })))))

(deftest test-length-validator-with-range-length-with-optional-message-fn
  (let [v (vr/length-of :title :within (range 9 13) :message-fn test-message-fn)]
    (is (fn? v))
    (is (= [false { :title #{[:blank {:title nil} :title nil]} }]
           (v { :title nil })))
    (is (= [true {}]
           (v { :title "power metal" })))
    (is (= [false { :title #{[:length:within {:title "trance"} :title [(range 9 13)]]} }]
           (v { :title "trance" })))))

;;
;; format-of
;;

(deftest test-format-of-validator
  (let [v (vr/format-of :id :format #"abc-\d\d\d")]
    (is (fn? v))
    (is (= [false { :id #{"can't be blank"} }]       (v { :id nil })))
    (is (= [true {}]                                 (v { :id "abc-123" })))
    (is (= [false { :id #{"has incorrect format"} }] (v { :id "123-abc" })))))


(deftest test-format-of-validator-that-allows-blanks
  (let [v (vr/format-of :id :format #"abc-\d\d\d" :allow-blank true)]
    (is (= [false { :id #{"can't be blank"} }]       (v { :id nil })))
    (is (= [true {}]                                 (v { :id "" })))
    (is (= [true {}]                                 (v { :id "abc-123" })))
    (is (= [false { :id #{"has incorrect format"} }] (v { :id "123-abc" })))))

(deftest test-format-of-validator-that-allows-nil
  (let [v (vr/format-of :id :format #"abc-\d\d\d" :allow-nil true)]
    (is (= [true {}]                                 (v { :id nil })))
    (is (= [false { :id #{"can't be blank"} }]       (v { :id "" })))
    (is (= [true {}]                                 (v { :id "abc-123" })))
    (is (= [false { :id #{"has incorrect format"} }] (v { :id "123-abc" })))))

(deftest test-format-of-validator-that-allows-blanks-and-nil
  (let [v (vr/format-of :id :format #"abc-\d\d\d" :allow-blank true :allow-nil true)]
    (is (= [true {}]                                 (v { :id nil })))
    (is (= [true {}]                                 (v { :id "" })))
    (is (= [true {}]                                 (v { :id "abc-123" })))
    (is (= [false { :id #{"has incorrect format"} }] (v { :id "123-abc" })))))

(deftest test-format-of-validator-with-optional-blank-message
  (let [v (vr/format-of :id :format #"abc-\d\d\d" :blank-message "test blank")]
    (is (fn? v))
    (is (= [false { :id #{"test blank"} }]           (v { :id nil })))
    (is (= [true {}]                                 (v { :id "abc-123" })))))

(deftest test-format-of-validator-with-custom-message
  (let [v (vr/format-of :id :format #"abc-\d\d\d" :message "is improperly formatted")]
    (is (= [false { :id #{"is improperly formatted"} }] (v { :id "123-abc" })))))

(deftest test-format-of-validator-with-optional-message-fn
  (let [test-message-equals-regexp (fn [t m attr & args] [t m attr (map str args)])
        v (vr/format-of :id :format #"abc-\d\d\d" :message-fn test-message-equals-regexp)]
    (is (fn? v))
    (is (= [false { :id #{[:blank {:id nil} :id []]} }]
           (v { :id nil })))
    (is (= [true {}]
           (v { :id "abc-123" })))
    (is (= [false { :id #{[:format {:id "123-abc"} :id ["abc-\\d\\d\\d"]]} }]
           (v { :id "123-abc" })))))

;;
;; validate-when
;;

(deftest test-validate-when-predicate-returns-false
  (let [v (vr/validate-when (constantly false) (vr/presence-of :id))]
    (is (fn? v))
    (is (= [true {}]
           (v {})))))

(deftest test-validate-when-predicate-returns-true
  (let [v (vr/validate-when (constantly true) (vr/presence-of :id))]
    (is (fn? v))
    (is (= [false {:id #{"can't be blank"}}]
           (v {})))))

(deftest test-validate-when-is-given-map
  (let [predicate (fn [m] (= (:id m) "abc-123"))
        v (vr/validate-when predicate (vr/presence-of :nonexistent))]
    (is (= [false {:nonexistent #{"can't be blank"}}]
           (v {:id "abc-123"})))
    (is (= [true {}]
           (v {:id "123-abc"})))))

;;
;; validate-with-predicate
;;

(deftest test-validate-with-predicate-predicate-returns-false
  (let [v (vr/validate-with-predicate :id (constantly false))]
    (is (fn? v))
    (is (= [false {:id #{"is invalid"}}]
           (v {})))))

(deftest test-validate-with-predicate-predicate-returns-true
  (let [v (vr/validate-with-predicate :id (constantly true))]
    (is (fn? v))
    (is (= [true {}]
           (v {})))))

(deftest test-validate-with-predicate-predicate-returns-false-with-custom-message
  (let [v (vr/validate-with-predicate :id (constantly false) :message "test")]
    (is (= [false {:id #{"test"}}]
           (v {})))))


;;
;; Implementation functions
;;


(deftest test-as-vec
  (is (= [1 2 3] (vr/as-vec [1 2 3])))
  (is (= [1 2 3] (vr/as-vec '(1 2 3))))
  (is (= [10] (vr/as-vec 10)))
  (is (= [{ :a 1 :b 2 }] (vr/as-vec { :a 1 :b 2 }))))
