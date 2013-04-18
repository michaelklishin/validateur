(ns validateur.test
  (:use clojure.test validateur.validation))

(defn invalid [key msg] [false {key #{msg}}])
(def valid [true {}])

(defn message-fn
  ([type attr val] [type attr val])
  ([type attr val & args] [type attr val args]))

(deftest test-presence-of
  (let [invalid (partial invalid :a)]

    (testing "valid values"
      (are [x] (= valid ((presence-of :a) {:a x}))
           [] "a" 1 [1]))

    (testing "invalid values"
      
      (testing "with default options"
        (let [invalid (invalid "can't be blank")]
          (are [x] (= invalid ((presence-of :a) x))
               nil {} {:b "b"} {:a nil} {:a ""})))
      
      (testing "with optional message"
        (is (= ((presence-of :a :message "test") {})
               [false {:a #{"test"}}])))
      
      (testing "with optional message-fn"
        (is (= ((presence-of :a :message-fn message-fn) {:a ""})
               [false {:a #{[:blank :a ""]}}]))))))
