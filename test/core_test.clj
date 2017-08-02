(ns core-test
  (:require [clojure.test :refer :all]
            [pricing-service.calculate :refer [calculate]]
            [clj-time.core :as t]))

(deftest my-first-test
  (testing "Basic policy"
    (is (= (calculate {:volume 5 :started-at (t/date-time 1986 10 14) :ended-at (t/date-time 1986 10 15)}
                      {:elements [{:start-at  (t/date-time 1986 10 14)
                                   :end-at    (t/date-time 1986 10 15)
                                   :time-from 300
                                   :time-to   400
                                   :days      [:mon :tue :wed :thu :fri :sat :sun]
                                   :unit      :kwh
                                   :step-size 3
                                   :price     2.6}]})
           5.2M)))
  (testing "Basic policy which does not apply"
    (is (= (calculate {:volume 5 :started-at (t/date-time 1986 10 14) :ended-at (t/date-time 1986 10 15)}
                      {:elements [{:start-at  (t/date-time 1986 10 14)
                                   :end-at    (t/date-time 1986 10 15)
                                   :time-from 300
                                   :time-to   400
                                   :days      [:mon :wed :thu :fri :sat :sun]
                                   :unit      :kwh
                                   :step-size 3
                                   :price     2.6}]})
           0M))))
