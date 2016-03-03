(ns validateur.test.validation-test
  (:require [validateur.validation :as vr]
            #?(:clj  [clojure.test :refer :all]
               :cljs [cljs.test :refer-macros [is are deftest testing]])))

#?(:clj (println (str "Using Clojure version " *clojure-version*)))

(defn test-message-fn [type map attr & args]
  [type map attr args])
;;
;; validation-set
;;

(deftest validate-some-test
  "Tests for the validate-some helper."
  (let [v (vr/validate-some
           (vr/presence-of :cake-count :message "missing_cake")
           (vr/validate-by :cake-count odd? :message "even_cake"))]
    (is (= [true #{}] (v {:cake-count 1}))
        "Odd cake counts are valid.")

    (is (= [false {:cake-count #{"even_cake"}}] (v {:cake-count 2}))
        "Even cake counts only throw the second error, since the first
      validation passed.")

    (is (= [false {:cake-count #{"missing_cake"}}] (v {}))
        "The second validation never gets called and never throws a
        NPE, as it would if we just composed them up.")))

(deftest presence-validation-using-set
  (let [v (vr/validation-set
           (vr/presence-of :name) (vr/presence-of :age))]
    (is (vr/valid? v {:name "Joe" :age 28}))
    (is (vr/valid? (v {:name "Joe" :age 28})))
    (is (not (vr/invalid? v {:name "Joe" :age 28})))
    (is (not (vr/valid? v {:name "Joe"})))
    (is (not (vr/valid? (v {:name "Joe"}))))
    (is (vr/invalid? v {:name "Joe" :age nil}))
    (is (vr/invalid? v {:name "Joe" :age ""}))
    (is (vr/invalid? v {:name "Joe" :age "   "}))
    (is (not (vr/invalid? v {:name "Joe" :age " a "})))
    (is (vr/invalid? v {:name "Joe"}))
    (is (not (vr/valid? v {:age 30})))
    (is (vr/invalid? v {:age 30}))
    (is (= {:age #{"can't be blank"}} (v {:name "Joe"})))
    (is (= {} (v {:name "Joe" :age 28})))))

(deftest presence-validation-with-nested-attributes-using-set
  (let [v (vr/validation-set
           (vr/presence-of [:personal :name])
           (vr/presence-of [:personal :age]))]
    (is (vr/valid? v {:personal {:name "Joe" :age 28}}))
    (is (vr/valid? (v {:personal {:name "Joe" :age 28}})))
    (is (not (vr/invalid? v {:personal {:name "Joe" :age 28}})))
    (is (not (vr/valid? v {:personal {:name "Joe"}})))
    (is (not (vr/valid? (v {:person {:name "Joe"}}))))
    (is (vr/invalid? v {:personal {:name "Joe" :age nil}}))
    (is (vr/invalid? v {:personal {:name "Joe" :age ""}}))
    (is (vr/invalid? v {:personal {:name "Joe" :age "   "}}))
    (is (not (vr/invalid? v {:personal {:name "Joe" :age " a "}})))
    (is (vr/invalid? v {:personal {:name "Joe"}}))
    (is (not (vr/valid? v {:personal {:age 30}})))
    (is (vr/invalid? v {:personal {:age 30}}))
    (is (= {[:personal :age] #{"can't be blank"}} (v {:personal {:name "Joe"}})))
    (is (= {} (v {:personal {:name "Joe" :age 28}})))))

;;
;; compose-set
;;


(deftest presence-compose-validation-set
  (let [vn (vr/validation-set
             (vr/presence-of :name))
        va (vr/validation-set
             (vr/presence-of :age))
        v  (vr/compose-sets va vn)]
    (is (vr/valid? v {:name "Joe" :age 28}))
    (is (not (vr/invalid? v {:name "Joe" :age 28})))
    (is (not (vr/valid? v {:name "Joe"})))
    (is (vr/invalid? v {:name "Joe" :age nil}))
    (is (vr/invalid? v {:name "Joe" :age ""}))
    (is (vr/invalid? v {:name "Joe" :age "   "}))
    (is (not (vr/invalid? v {:name "Joe" :age " a "})))
    (is (vr/invalid? v {:name "Joe"}))
    (is (not (vr/valid? v {:age 30})))
    (is (vr/invalid? v {:age 30}))
    (is (= {:age #{"can't be blank"}} (v {:name "Joe"})))
    (is (= {} (v {:name "Joe" :age 28})))))



;;
;; presence-of
;;

(deftest test-presence-validator-with-one-attribute
  (let [v (vr/presence-of :name)]
    (is (fn? v))
    (is (= [true {}]                             (v {:name "Michael"})))
    (is (= [false {:name #{"can't be blank"}}] (v {:age 28})))))

(deftest test-presence-validator-with-one-nested-attribute
  (let [v (vr/presence-of [:address :street])]
    (is (fn? v))
    (is (= [true {}]                                          (v {:address {:street "Old Rd"}})))
    (is (= [false {[:address :street] #{"can't be blank"}}] (v {:address {}})))))

(deftest test-presence-validator-with-many-attributes-requiring-all
  (let [v (vr/presence-of #{:name :msg})]
    (is (= [true {}]                                                    (v {:name "Radek" :msg "Hello, World!"})))
    (is (= [false {:name #{"can't be blank"}}]                          (v {:msg "Hello, World!"})))
    (is (= [false {:msg #{"can't be blank"}}]                           (v {:name "Radek"})))
    (is (= [false {:name #{"can't be blank"} :msg #{"can't be blank"}}] (v {})))))

(deftest test-presence-validator-with-many-attributes-requiring-any
  (let [v (vr/presence-of #{:name :msg} :any true)]
    (is (= [true {}]                                                    (v {:name "Radek" :msg "Hello, World!"})))
    (is (= [true {}]                                                    (v {:msg "Hello, World!"})))
    (is (= [true {}]                                                    (v {:name "Radek"})))
    (is (= [false {:name #{"can't be blank"} :msg #{"can't be blank"}}] (v {})))))

(deftest test-presence-of-validator-with-custom-message
  (let [v (vr/presence-of :name :message "Низя, должно быть заполнено!")]
    (is (= [false {:name #{"Низя, должно быть заполнено!"}}] (v {:age 28})))))

(deftest test-presence-of-validator-with-optional-message-fn
  (let [v (vr/presence-of :name :message-fn test-message-fn)]
    (is (= [false {:name #{[:blank {:age 28} :name nil]}}] (v {:age 28})))))

;;
;; numericality-of
;;


(deftest test-numerical-integer-only-validator-with-one-attribute
  (let [v (vr/numericality-of :age :only-integer true)]
    (is (fn? v))
    (is (= [true {}]                                                       (v {:age 26})))
    (is (= [false {:age #{"should be a number" "should be an integer"}}] (v {:age "Twenty six"})))
    (is (= [false {:age #{"should be an integer"}}]                      (v {:age 26.6})))))

(deftest ^:focus test-numerical-integer-only-validator-with-one-nested-attribute
         (let [v (vr/numericality-of [:profile :age] :only-integer true)]
           (is (fn? v))
           (is (= [true {}]                                                                  (v {:profile {:age 26}})))
           (is (= [false {[:profile :age] #{"should be a number" "should be an integer"}}] (v {:profile {:age "Twenty six"}})))
           (is (= [false {[:profile :age] #{"should be an integer"}}]                      (v {:profile {:age 26.6}})))))


(deftest test-numerical-validator-with-one-attribute-that-is-gt-than-a-value
  (let [v (vr/numericality-of :age :gt 40)]
    (is (fn? v))
    (is (= [true {}] (v {:age 46})))
    (is (= [false {:age #{"can't be blank"}}]            (v {:age nil})))
    (is (= [false {:age #{"should be a number"}}]        (v {:age "Twenty six"})))
    (is (= [false {:age #{"should be greater than 40"}}] (v {:age 26.6})))))


(deftest test-numerical-validator-with-one-attribute-that-is-lt-than-a-value
  (let [v (vr/numericality-of :age :lt 40)]
    (is (fn? v))
    (is (= [true {}] (v {:age 36})))
    (is (= [false {:age #{"can't be blank"}}]         (v {:age nil})))
    (is (= [false {:age #{"should be a number"}}]     (v {:age "Twenty six"})))
    (is (= [false {:age #{"should be less than 40"}}] (v {:age 46.6})))))


(deftest test-numerical-validator-with-one-attribute-that-is-gte-than-a-value
  (let [v (vr/numericality-of :age :gte 40)]
    (is (fn? v))
    (is (= [true {}] (v {:age 46})))
    (is (= [true {}] (v {:age 40})))
    (is (= [false {:age #{"can't be blank"}}]                        (v {:age nil})))
    (is (= [false {:age #{"should be a number"}}]                    (v {:age "Twenty six"})))
    (is (= [false {:age #{"should be greater than or equal to 40"}}] (v {:age 26.6})))))


(deftest test-numerical-validator-with-one-attribute-that-is-lte-than-a-value
  (let [v (vr/numericality-of :age :lte 40)]
    (is (fn? v))
    (is (= [true {}] (v {:age 36})))
    (is (= [true {}] (v {:age 40})))
    (is (= [false {:age #{"can't be blank"}}]                     (v {:age nil})))
    (is (= [false {:age #{"should be a number"}}]                 (v {:age "Twenty six"})))
    (is (= [false {:age #{"should be less than or equal to 40"}}] (v {:age 46.6})))))



(deftest test-numerical-validator-with-one-attribute-that-is-equal-to-a-value
  (let [v (vr/numericality-of :age :equal-to 40)]
    (is (fn? v))
    (is (= [true {}]                                   (v {:age 40})))
    (is (= [false {:age #{"can't be blank"}}]        (v {:age nil})))
    (is (= [false {:age #{"should be a number"}}]    (v {:age "Twenty six"})))
    (is (= [false {:age #{"should be equal to 40"}}] (v {:age 46.6})))
    (is (= [false {:age #{"should be equal to 40"}}] (v {:age 100})))))



(deftest test-numerical-validator-with-one-attribute-that-is-odd
  (let [v (vr/numericality-of :age :odd true)]
    (is (fn? v))
    (is (= [true {}]                                (v {:age 41})))
    (is (= [false {:age #{"can't be blank"}}]     (v {:age nil})))
    (is (= [false {:age #{"should be a number"}}] (v {:age "Twenty six"})))
    (is (= [false {:age #{"should be odd"}}]      (v {:age 20})))))


(deftest test-numerical-validator-with-one-attribute-that-is-even
  (let [v (vr/numericality-of :age :even true)]
    (is (fn? v))
    (is (= [true {}]                                (v {:age 40})))
    (is (= [false {:age #{"can't be blank"}}]     (v {:age nil})))
    (is (= [false {:age #{"should be a number"}}] (v {:age "Twenty six"})))
    (is (= [false {:age #{"should be even"}}]     (v {:age 21})))))


(deftest test-numerical-validator-with-optional-messages
  (let [msgs {:number "number" :blank "blank"
              :only-integer "integer" :equal-to "equal to "}
        v #(vr/numericality-of :age %1 %2 :messages msgs)]
    (testing "only integers"
      (is (= [false {:age #{"number" "integer"}}]
             ((v :only-integer true) {:age "Twenty six"})))
      (is (= [false {:age #{"integer"}}]
             ((v :only-integer true) {:age 26.6}))))
    (testing "equal to"
      (is (= [false {:age #{"blank"}}]
             ((v :equal-to 1) {:age nil})))
      (is (= [false {:age #{"equal to 1"}}]
             ((v :equal-to 1) {:age 26.6}))))
    (testing "optional messages are merged with default ones"
      (is (= [false {:age #{"should be greater than 40"}}]
             ((v :gt 40) {:age 20})))
      (is (= [false {:age #{"should be odd"}}]
             ((v :odd true) {:age 20}))))))

(deftest test-numerical-validator-with-optional-message-fn
  (let [v #(vr/numericality-of :age %1 %2 :message-fn test-message-fn)]
    (is (= [false {:age #{[:number {:age "26"} :age nil]
                          [:only-integer {:age "26"} :age nil]}}]
           ((v :only-integer true) {:age "26"})))
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
    (is (= [true {}]                                               (v {:terms-and-conditions true})))
    (is (= [false {:terms-and-conditions #{"must be accepted"}}] (v {:terms-and-conditions "I do not approve it"})))))

(deftest test-acceptance-validator-with-custom-message
  (let [v (vr/acceptance-of :terms-and-conditions :message "ZOMG")]
    (is (fn? v))
    (is (= [true {}]                                               (v {:terms-and-conditions true})))
    (is (= [false {:terms-and-conditions #{"ZOMG"}}] (v {:terms-and-conditions "I do not approve it"})))))


(deftest test-acceptance-validator-with-one-attribute-and-custom-accepted-values-list
  (let [v (vr/acceptance-of :terms-and-conditions :accept #{"yes", "hell yes"})]
    (is (fn? v))
    (is (= [true {}]                                               (v {:terms-and-conditions "yes"})))
    (is (= [true {}]                                               (v {:terms-and-conditions "hell yes"})))
    (is (= [false {:terms-and-conditions #{"must be accepted"}}] (v {:terms-and-conditions true})))
    (is (= [false {:terms-and-conditions #{"must be accepted"}}] (v {:terms-and-conditions "I do not approve it"})))
    (is (= [false {:terms-and-conditions #{"must be accepted"}}] (v {:terms-and-conditions "1"})))
    (is (= [false {:terms-and-conditions #{"must be accepted"}}] (v {:terms-and-conditions "jeez no"})))))

(deftest test-acceptance-validator-with-custom-blank-message
  (let [v (vr/acceptance-of :terms-and-conditions :blank-message "ZOMG")]
    (is (fn? v))
    (is (= [true {}]
           (v {:terms-and-conditions true})))
    (is (= [false {:terms-and-conditions #{"ZOMG"}}]
           (v {:terms-and-conditions nil})))))

(deftest test-acceptance-validator-with-custom-message-fn
  (let [v (vr/acceptance-of :terms-and-conditions :message-fn test-message-fn)
        valid {:terms-and-conditions true}
        invalid {:terms-and-conditions "I do not approve it"}
        invalid-blank {:terms-and-conditions nil}
        default-acceptance-values #{"true" true "1"}]
    (is (fn? v))
    (is (= [true {}] (v valid)))
    (is (= [false {:terms-and-conditions
                   #{[:acceptance invalid :terms-and-conditions
                      [default-acceptance-values]]}}]
           (v invalid )))
    (is (= [false {:terms-and-conditions
                   #{[:blank invalid-blank :terms-and-conditions nil]}}]
           (v invalid-blank )))))

(deftest test-acceptance-validator-with-custom-accepted-values-and-message-fn
  (let [acceptance-values #{"yes" "hell yes"}
        v (vr/acceptance-of :terms-and-conditions :message-fn test-message-fn
                         :accept acceptance-values)
        valid {:terms-and-conditions "hell yes"}
        invalid {:terms-and-conditions "I do not approve it"}]
    (is (fn? v))
    (is (= [true {}] (v valid)))
    (is (= [false {:terms-and-conditions
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
    (is (= [true {}] (v {:turing 1 "von neumann" 2 1954 4591})))
    (is (= [false {:babbage #{"unknown key"}}] (v {:babbage "lovelace"})))
    (is (= [false {4591 #{"unknown key"}}] (v {4591 6.28})))
    (is (= [false {"church" #{"unknown key"}}] (v {"church" "none"})))))

;;
;; inclusion-of
;;

(deftest test-inclusion-validator
  (let [v (vr/inclusion-of :genre :in #{"trance", "dnb"})]
    (is (fn? v))
    (is (= [false {:genre #{"can't be blank"}}]              (v {:genre nil})))
    (is (= [true {}]                                           (v {:genre "trance"})))
    (is (= [true {}]                                           (v {:genre "dnb"})))
    (is (= [false {:genre #{"must be one of: dnb, trance"}}] (v {:genre true})))
    (is (= [false {:genre #{"must be one of: dnb, trance"}}] (v {:genre "I do not approve it"})))
    (is (= [false {:genre #{"must be one of: dnb, trance"}}] (v {:genre "1"})))))

(deftest test-inclusion-validator-with-booleans
  (let [v (vr/inclusion-of :truth :in #{false true})]
    (is (fn? v))
    (is (= [false {:truth #{"can't be blank"}}]              (v {:truth nil})))
    (is (= [true {}]                                           (v {:truth true})))
    (is (= [true {}]                                           (v {:truth false})))
    (is (= [false {:truth #{"must be one of: false, true"}}] (v {:truth "foo"})))
    (is (= [false {:truth #{"must be one of: false, true"}}] (v {:truth "I do not approve it"})))
    (is (= [false {:truth #{"must be one of: false, true"}}] (v {:truth "1"})))))

(deftest test-inclusion-validator-with-nested-attributes
  (let [v (vr/inclusion-of [:track :genre] :in #{"trance", "dnb"})]
    (is (fn? v))
    (is (= [false {[:track :genre] #{"can't be blank"}}]     (v {[:track :genre] nil})))
    (is (= [true {}]                                           (v {:track {:genre "trance"}})))
    (is (= [true {}]                                           (v {:track {:genre "dnb"}})))
    (is (= [false {[:track :genre] #{"must be one of: dnb, trance"}}] (v {:track {:genre true}})))
    (is (= [false {[:track :genre] #{"must be one of: dnb, trance"}}] (v {:track {:genre "I do not approve it"}})))
    (is (= [false {[:track :genre] #{"must be one of: dnb, trance"}}] (v {:track {:genre "1"}})))))

(deftest test-inclusion-validator-with-custom-message
  (let [v (vr/inclusion-of :genre :in #{"trance", "dnb"}
                        :message "one of: ")]
    (is (fn? v))
    (is (= [false {:genre #{"one of: dnb, trance"}}]
           (v {:genre "dub step"})))
    (is (= [true {}]
           (v {:genre "trance"})))))

(deftest test-inclusion-validator-with-custom-blank-message
  (let [v (vr/inclusion-of :genre :in #{"trance", "dnb"}
                        :blank-message "test")]
    (is (fn? v))
    (is (= [false {:genre #{"test"}}]
           (v {:genre nil})))
    (is (= [true {}]
           (v {:genre "trance"})))))

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
           (v {:genre "1"})))))

(deftest test-inclusion-validation-with-allow-nil-true
  (let [v (vr/inclusion-of :genre :in #{"trance" "dnb"} :allow-nil true)]
    (is (fn? v))
    (is (= [true {}] (v {:genre "trance"})))
    (is (= [true {}] (v {:genre "dnb"})))
    (is (= [false {:genre #{"must be one of: dnb, trance"}}] (v {:genre "hiphop"})))
    (is (= [true {}] (v {:category "trance"})))
    (is (= [true {}] (v {:genre nil})))))

;;
;; exclusion-of
;;

(deftest test-exclusion-validator
  (let [v (vr/exclusion-of :genre :in #{"trance", "dnb"})]
    (is (fn? v))
    (is (= [false {:genre #{"can't be blank"}}] (v {:genre nil})))
    (is (= [true {}]                              (v {:genre "rock"})))
    (is (= [true {}]                              (v {:genre "power metal"})))
    (let [[result _] (v {:genre "trance"})]
      (is (not result)))
    (let [[result _] (v {:genre "dnb"})]
      (is (not result)))))

(deftest test-exclusion-validator-with-booleans
  (let [v (vr/exclusion-of :truth :in #{true false})]
    (is (fn? v))
    (is (= [false {:truth #{"can't be blank"}}] (v {:truth nil})))
    (is (= [true {}]                              (v {:truth "rock"})))
    (is (= [true {}]                              (v {:truth "power metal"})))
    (let [[result _] (v {:truth true})]
      (is (not result)))
    (let [[result _] (v {:truth false})]
      (is (not result)))))

(deftest test-exclusion-validator-with-custom-message
  (let [v (vr/exclusion-of :genre :in #{"trance", "dnb"}
                        :message "not one of: ")]
    (is (fn? v))
    (let [[result _] (v {:genre "trance"})]
      (is (not result)))
    (is (= [true {}]
           (v {:genre "swing"})))))

(deftest test-exclusion-validator-with-custom-blank-message
  (let [v (vr/exclusion-of :genre :in #{"trance", "dnb"}
                        :blank-message "test")]
    (is (fn? v))
    (is (= [false {:genre #{"test"}}]
           (v {:genre nil})))
    (is (= [true {}]
           (v {:genre "swing"})))))

(deftest test-exclusion-validator-with-optional-message-fn
  (let [v (vr/exclusion-of :genre :in #{"trance", "dnb"}
                        :message-fn test-message-fn)]
    (is (fn? v))
    (is (= [false {:genre #{[:blank {:genre nil} :genre nil]}}]
           (v {:genre nil})))
    (is (= [true {}]
           (v {:genre "rock"})))
    (is (= [true {}]
           (v {:genre "power metal"})))
    (is (= [false {:genre #{[:exclusion {:genre "trance"} :genre
                             [#{"trance", "dnb"}]]}}]
           (v {:genre "trance"})))
    (is (= [false {:genre #{[:exclusion {:genre "dnb"} :genre
                             [#{"trance", "dnb"}]]}}]
           (v {:genre "dnb"})))))

;;
;; length-of
;;

(deftest test-length-validator-with-fixed-length
  (let [v (vr/length-of :title :is 11)]
    (is (fn? v))
    (is (= [false {:title #{"can't be blank"}}]             (v {:title nil})))
    (is (= [true {}]                                          (v {:title "power metal"})))
    (is (= [false {:title #{"must be 11 characters long"}}] (v {:title "trance"})))
    (is (= [false {:title #{"must be 11 characters long"}}] (v {:title "dnb"})))
    (is (= [false {:title #{"must be 11 characters long"}}] (v {:title "melodic power metal"})))))

(deftest test-length-validator-with-fixed-length-that-allows-blanks
  (let [v (vr/length-of :title :is 11 :allow-blank true)]
    (is (fn? v))
    (is (= [false {:title #{"can't be blank"}}]             (v {:title nil})))
    (is (= [true {}]                                          (v {:title ""})))
    (is (= [true {}]                                          (v {:title "power metal"})))
    (is (= [false {:title #{"must be 11 characters long"}}] (v {:title "trance"})))
    (is (= [false {:title #{"must be 11 characters long"}}] (v {:title "dnb"})))
    (is (= [false {:title #{"must be 11 characters long"}}] (v {:title "melodic power metal"})))))

(deftest test-length-validator-with-fixed-length-that-allows-nil
  (let [v (vr/length-of :title :is 11 :allow-nil true)]
    (is (fn? v))
    (is (= [true {}]                                          (v {:title nil})))
    (is (= [false {:title #{"can't be blank"}}]             (v {:title ""})))
    (is (= [true {}]                                          (v {:title "power metal"})))
    (is (= [false {:title #{"must be 11 characters long"}}] (v {:title "trance"})))
    (is (= [false {:title #{"must be 11 characters long"}}] (v {:title "dnb"})))
    (is (= [false {:title #{"must be 11 characters long"}}] (v {:title "melodic power metal"})))))

(deftest test-length-validator-with-fixed-length-that-allows-blanks-and-nil
  (let [v (vr/length-of :title :is 11 :allow-blank true :allow-nil true)]
    (is (fn? v))
    (is (= [true {}]                                          (v {:title nil})))
    (is (= [true {}]                                          (v {:title ""})))
    (is (= [true {}]                                          (v {:title "power metal"})))
    (is (= [false {:title #{"must be 11 characters long"}}] (v {:title "trance"})))
    (is (= [false {:title #{"must be 11 characters long"}}] (v {:title "dnb"})))
    (is (= [false {:title #{"must be 11 characters long"}}] (v {:title "melodic power metal"})))))

(deftest test-length-validator-with-fixed-length-with-optional-blank-message
  (let [v (vr/length-of :title :is 11 :blank-message "test blank")]
    (is (fn? v))
    (is (= [false {:title #{"test blank"}}]
           (v {:title nil})))
    (is (= [true {}]
           (v {:title "power metal"})))))

(deftest test-length-validator-with-fixed-length-with-optional-message-fn
  (let [v (vr/length-of :title :is 11 :message-fn test-message-fn)]
    (is (fn? v))
    (is (= [false {:title #{[:blank {:title nil} :title nil]}}]
           (v {:title nil})))
    (is (= [true {}]
           (v {:title "power metal"})))
    (is (= [false {:title #{[:length:is {:title "trance"} :title [11]]}}]
           (v {:title "trance"})))))


(deftest test-length-validator-with-range-length
  (let [v (vr/length-of :title :within (range 9 13))]
    (is (fn? v))
    (is (= [false {:title #{"can't be blank"}}]             (v {:title nil})))
    (is (= [true {}]                                          (v {:title "power metal"})))
    (is (= [false {:title #{"must be from 9 to 12 characters long"}}] (v {:title "trance"})))
    (is (= [false {:title #{"must be from 9 to 12 characters long"}}] (v {:title "dnb"})))
    (is (= [false {:title #{"must be from 9 to 12 characters long"}}] (v {:title "melodic power metal"})))))

(deftest test-length-validator-with-range-length-with-optional-blank-message
  (let [v (vr/length-of :title :within (range 9 13) :blank-message "test blank")]
    (is (fn? v))
    (is (= [false {:title #{"test blank"}}]
           (v {:title nil})))
    (is (= [true {}]
           (v {:title "power metal"})))))

(deftest test-length-validator-with-range-length-that-allows-blanks
  (let [v (vr/length-of :title :within (range 9 13) :allow-blank true)]
    (is (fn? v))
    (is (= [false {:title #{"can't be blank"}}]                       (v {:title nil})))
    (is (= [true {}]                                                    (v {:title ""})))
    (is (= [true {}]                                                    (v {:title "power metal"})))
    (is (= [false {:title #{"must be from 9 to 12 characters long"}}] (v {:title "trance"})))
    (is (= [false {:title #{"must be from 9 to 12 characters long"}}] (v {:title "dnb"})))
    (is (= [false {:title #{"must be from 9 to 12 characters long"}}] (v {:title "melodic power metal"})))))

(deftest test-length-validator-with-range-length-that-allows-nil
  (let [v (vr/length-of :title :within (range 9 13) :allow-nil true)]
    (is (fn? v))
    (is (= [true {}]                                                    (v {:title nil})))
    (is (= [false {:title #{"can't be blank"}}]                       (v {:title ""})))
    (is (= [true {}]                                                    (v {:title "power metal"})))
    (is (= [false {:title #{"must be from 9 to 12 characters long"}}] (v {:title "trance"})))
    (is (= [false {:title #{"must be from 9 to 12 characters long"}}] (v {:title "dnb"})))
    (is (= [false {:title #{"must be from 9 to 12 characters long"}}] (v {:title "melodic power metal"})))))

(deftest test-length-validator-with-range-length-that-allows-blanks-and-nil
  (let [v (vr/length-of :title :within (range 9 13) :allow-blank true :allow-nil true)]
    (is (fn? v))
    (is (= [true {}]                                                    (v {:title nil})))
    (is (= [true {}]                                                    (v {:title ""})))
    (is (= [true {}]                                                    (v {:title "power metal"})))
    (is (= [false {:title #{"must be from 9 to 12 characters long"}}] (v {:title "trance"})))
    (is (= [false {:title #{"must be from 9 to 12 characters long"}}] (v {:title "dnb"})))
    (is (= [false {:title #{"must be from 9 to 12 characters long"}}] (v {:title "melodic power metal"})))))

(deftest test-length-validator-with-range-length-with-optional-message-fn
  (let [v (vr/length-of :title :within (range 9 13) :message-fn test-message-fn)]
    (is (fn? v))
    (is (= [false {:title #{[:blank {:title nil} :title nil]}}]
           (v {:title nil})))
    (is (= [true {}]
           (v {:title "power metal"})))
    (is (= [false {:title #{[:length:within {:title "trance"} :title [(range 9 13)]]}}]
           (v {:title "trance"})))))

(deftest test-length-validator-with-fixed-length-and-non-string-input
  (let [v (vr/length-of :items :is 3)]
    (is (fn? v))
    (is (= [true {}]                                  (v {:items [1 2 3]})))
    (is (= [true {}]                                  (v {:items #{1 2 3}})))
    (is (= [false {:items #{"must be 3 items long"}}] (v {:items [1 2]})))
    (is (= [false {:items #{"must be 3 items long"}}] (v {:items #{1 2}})))))

(deftest test-length-validator-with-range-length-and-non-string-input
  (let [v (vr/length-of :items :within (range 3 7))]
    (is (fn? v))
    (is (= [true {}]                                            (v {:items [1 2 3]})))
    (is (= [true {}]                                            (v {:items [1 2 3 4 5 6]})))
    (is (= [true {}]                                            (v {:items #{1 2 3}})))
    (is (= [true {}]                                            (v {:items #{1 2 3 4 5 6}})))
    (is (= [false {:items #{"must be from 3 to 6 items long"}}] (v {:items [1 2]})))
    (is (= [false {:items #{"must be from 3 to 6 items long"}}] (v {:items [1 2 3 4 5 6 7]})))
    (is (= [false {:items #{"must be from 3 to 6 items long"}}] (v {:items #{1 2}})))
    (is (= [false {:items #{"must be from 3 to 6 items long"}}] (v {:items #{1 2 3 4 5 6 7}})))))

;;
;; format-of
;;

(deftest test-format-of-validator
  (let [v (vr/format-of :id :format #"abc-\d\d\d")]
    (is (fn? v))
    (is (= [false {:id #{"can't be blank"}}]       (v {:id nil})))
    (is (= [true {}]                                 (v {:id "abc-123"})))
    (is (= [false {:id #{"has incorrect format"}}] (v {:id "123-abc"})))))


(deftest test-format-of-validator-that-allows-blanks
  (let [v (vr/format-of :id :format #"abc-\d\d\d" :allow-blank true)]
    (is (= [false {:id #{"can't be blank"}}]       (v {:id nil})))
    (is (= [true {}]                                 (v {:id ""})))
    (is (= [true {}]                                 (v {:id "abc-123"})))
    (is (= [false {:id #{"has incorrect format"}}] (v {:id "123-abc"})))))

(deftest test-format-of-validator-that-allows-nil
  (let [v (vr/format-of :id :format #"abc-\d\d\d" :allow-nil true)]
    (is (= [true {}]                                 (v {:id nil})))
    (is (= [false {:id #{"can't be blank"}}]       (v {:id ""})))
    (is (= [true {}]                                 (v {:id "abc-123"})))
    (is (= [false {:id #{"has incorrect format"}}] (v {:id "123-abc"})))))

(deftest test-format-of-validator-that-allows-blanks-and-nil
  (let [v (vr/format-of :id :format #"abc-\d\d\d" :allow-blank true :allow-nil true)]
    (is (= [true {}]                                 (v {:id nil})))
    (is (= [true {}]                                 (v {:id ""})))
    (is (= [true {}]                                 (v {:id "abc-123"})))
    (is (= [false {:id #{"has incorrect format"}}] (v {:id "123-abc"})))))

(deftest test-format-of-validator-with-optional-blank-message
  (let [v (vr/format-of :id :format #"abc-\d\d\d" :blank-message "test blank")]
    (is (fn? v))
    (is (= [false {:id #{"test blank"}}]           (v {:id nil})))
    (is (= [true {}]                                 (v {:id "abc-123"})))))

(deftest test-format-of-validator-with-custom-message
  (let [v (vr/format-of :id :format #"abc-\d\d\d" :message "is improperly formatted")]
    (is (= [false {:id #{"is improperly formatted"}}] (v {:id "123-abc"})))))

(deftest test-format-of-validator-with-optional-message-fn
  (let [test-message-equals-regexp (fn [t m attr & args] [t m attr (map str args)])
        v (vr/format-of :id :format #"abc-\d\d\d" :message-fn test-message-equals-regexp)]
    (is (fn? v))
    (is (= [false {:id #{[:blank {:id nil} :id []]}}]
           (v {:id nil})))
    (is (= [true {}]
           (v {:id "abc-123"})))
    (is (= [false {:id #{[:format {:id "123-abc"} :id [(str #"abc-\d\d\d")]]}}]
           (v {:id "123-abc"})))))

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

;; validate-by

(deftest test-nested-validate-by
  (let [m "Field can't be empty."
        nested (vr/validate-by [:user :name] not-empty :message m)]
    (is (fn? nested))
    (is (= [false {[:user :name] #{m}}]
           (nested {:user {:name ""}})))))

(deftest test-nest-unnest
  (is (empty? (vr/unnest :a {:a "bee"}))
      "Unnesting a bare, non-vector key will filter it.")

  (is (empty? (vr/unnest :a {[:a] "bee"}))
      "Even if the inner key is a vector, it'll still get
      filtered (otherwise we'd have empty attributes)")

  (is (= {[:b] "see"}
         (vr/unnest :a {[:a :b] "see"
                        [:a] "bee"
                        [:d] "ee!"}))
      "unnest filters out elements that don't match the prefix.")
  (are [attr input result]
    (and (= result (vr/nest attr input))
         (= input (vr/unnest attr (vr/nest attr input))))
    :a {[:b :c] "dee!" [:e] "eff!"} {[:a :b :c] "dee!"
                                     [:a :e] "eff!"}
    [:a] {[:b :c] "dee!" [:e] "eff!"} {[:a :b :c] "dee!"
                                       [:a :e] "eff!"}))

(deftest test-single-validate-by
  (let [m "Field can't be empty."
        v (vr/validate-by :x even?)]
    (is (fn? v))
    (is (= [false {:x #{"Failed predicate validation."}}]
           (v {:x 123})))
    (is (= [true {}]
           (v {:x 12})))))

;; nested

(deftest test-nested
  (let [v (vr/nested :user (vr/validation-set
                            (vr/presence-of :name)
                            (vr/presence-of :age)))
        extra-nested (vr/nested [:user :profile]
                                (vr/validation-set
                                 (vr/presence-of :age)
                                 (vr/presence-of [:birthday :year])))]
    (is (fn? v))
    (is (= {[:user :age] #{"can't be blank"}
            [:user :name] #{"can't be blank"}}
           (v {})))
    (is (= {[:user :age] #{"can't be blank"}}
           (v {:user {:name "name"}})))
    (is (= {} (extra-nested {:user {:profile {:age 10
                                              :birthday {:year 2004}}}})))
    (is (= {[:user :profile :birthday :year] #{"can't be blank"}}
           (extra-nested {:user {:profile {:age 10}}})))))

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

(deftest test-validate-with-predicate-predicate-returns-false-with-custom-message-fn
  (let [v (vr/validate-with-predicate :id (constantly false)
                                      :message-fn (fn [m] (str "test" (count m))))]
    (is (= [false {:id #{"test0"}}]
           (v {})))))

;;
;; validity-of
;;

(deftest test-validity-of-validation-fails
  (let [v (vr/validation-set
            (vr/presence-of :person)
            (vr/validity-of :person
              (vr/format-of :name :format #"[A-Za-zł]+")
              (vr/inclusion-of :status :in #{:active :inactive})))]
    (is (= {:person #{"can't be blank"}
            [:person :name] #{"can't be blank"}
            [:person :status] #{"can't be blank"}}
           (v {})))
    (is (= {[:person :name] #{"can't be blank"}
            [:person :status] #{"can't be blank"}}
           (v {:person {}})))
    (is (= {[:person :name] #{"can't be blank"}
            [:person :status] #{"can't be blank"}}
           (v {:person {:name nil :status nil}})))
    (is (= {[:person :name] #{"has incorrect format"}
            [:person :status] #{"must be one of: :active, :inactive"}}
           (v {:person {:name "!!!" :status :foo}})))))

(deftest test-validity-of-validation-succeeds
  (let [v (vr/validation-set
            (vr/presence-of :person)
            (vr/validity-of :person
              (vr/format-of :name :format #"[A-Za-zł]+")
              (vr/inclusion-of :status :in #{:active :inactive})))]
    (is (= {} (v {:person {:name "Michał" :status :active}})))))

;;
;; validate-nested
;;

(deftest test-validate-nested-validation-fails
  (let [person-v (vr/validation-set
                   (vr/presence-of :name)
                   (vr/format-of :name :format #"[A-Za-z]+"))
        v (vr/validate-nested :person person-v)]
    (is (= [false {[:person :name] #{"can't be blank"}}]
           (v {})))
    (is (= [false {[:person :name] #{"can't be blank"}}]
           (v {:person {}})))
    (is (= [false {[:person :name] #{"can't be blank"}}]
           (v {:person {:name nil}})))
    (is (= [false {[:person :name] #{"can't be blank"}}]
           (v {:person {:name ""}})))
    (is (= [false {[:person :name] #{"has incorrect format"}}]
           (v {:person {:name "123"}})))))

(deftest test-validate-nested-validation-succeeds
  (let [person-v (vr/validation-set
                   (vr/presence-of :name)
                   (vr/format-of :name :format #"[A-Za-z]+"))
        v (vr/validate-nested :person person-v)]
    (is (= [true {}]
           (v {:person {:name "Michał"}})))))

(deftest test-validate-nested-validation-fails-with-custom-message
  (let [person-v (vr/validation-set
                   (vr/presence-of :name)
                   (vr/format-of :name :format #"[A-Za-z]+"))
        v (vr/validate-nested :person person-v :message "test")]
    (is (= [false {[:person :name] #{"test"}}]
           (v {})))
    (is (= [false {[:person :name] #{"test"}}]
           (v {:person {}})))
    (is (= [false {[:person :name] #{"test"}}]
           (v {:person {:name nil}})))
    (is (= [false {[:person :name] #{"test"}}]
           (v {:person {:name ""}})))
    (is (= [false {[:person :name] #{"test"}}]
           (v {:person {:name "123"}})))))

(deftest test-validate-nested-validation-fails-with-custom-message-fn
  (let [person-v (vr/validation-set
                   (vr/presence-of :name)
                   (vr/format-of :name :format #"[A-Za-z]+"))
        v (vr/validate-nested :person person-v
            :message-fn (fn [m]
                          (str "test" (+ (count m) (count (:person m))))))]
    (is (= [false {[:person :name] #{"test0"}}]
           (v {})))
    (is (= [false {[:person :name] #{"test0"}}]
           (v {:person {}})))
    (is (= [false {[:person :name] #{"test1"}}]
           (v {:person {:name nil}})))
    (is (= [false {[:person :name] #{"test1"}}]
           (v {:person {:name ""}})))
    (is (= [false {[:person :name] #{"test1"}}]
           (v {:person {:name "123"}})))))

;;
;; Error Reporting
;;

(deftest errors-test
  (let [v (vr/validation-set
           (vr/presence-of :cake-count))
        compound-v (vr/validation-set
                    (vr/presence-of [:a :b])
                    (vr/presence-of :c))]

    (is (vr/errors? :cake-count (v {}))
        "Missing key triggers an error.")

    (is (= (vr/errors [:cake-count] (v {}))
           (vr/errors :cake-count (v {}))
           #{"can't be blank"})
        "errors returns the actual error.")

    (is (vr/errors? :cake-count {[:cake-count] #{"something"}})
        "It works if the error map has a nested, single keyword (as
        happens when we use unnest)")

    (is (not (vr/errors? :cake-count (v {:cake-count "hi!"})))
        "No errors, since cake-count is present.")

    (testing "errors? Works for nested keywords too"
      (is (vr/errors? [:a :b] (compound-v {})))
      (is (not (vr/errors? [:a :b] (compound-v {:a {:b "something"}})))))))

;;
;; Implementation functions
;;


(deftest test-as-vec
  (is (= [1 2 3] (vr/as-vec [1 2 3])))
  (is (= [1 2 3] (vr/as-vec '(1 2 3))))
  (is (= [10] (vr/as-vec 10)))
  (is (= [{:a 1 :b 2}] (vr/as-vec {:a 1 :b 2}))))
