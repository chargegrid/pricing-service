(ns pricing-service.calculate
  (:require [clj-time.core :as t]
            [pricing-service.persistence.store :as store]
            [clojure.tools.logging :as log]))

(defn ceil [n] (int (Math/ceil n)))

(defn duration [from to]
  "Returns duration between from and to in minutes"
  (t/in-minutes (t/interval from to)))

(defn weekday [date]
  "Returns day of the week from a joda DateTime"
  (let [weekdays {1 :mon 2 :tue 3 :wed 4 :thu 5 :fri 6 :sat 7 :sun}]
    (get weekdays (t/day-of-week date))))

; TODO work with specific dates as well
(defn filter-elements [elements {:keys [started-at ended-at volume]}]
  "Filter a coll of pricing elements for a given transaction based on the weekday"
  (filter (fn [{days :days}]
            (some #(= % (weekday started-at)) days))
          elements))

(defn per-element [{:keys [unit step-size price]} {:keys [started-at ended-at volume]}]
  "Calculate the price for a given transaction and a given pricing element"
  (let [unit-amount (case unit
                      :kwh volume
                      :min (duration started-at ended-at)
                      :transaction 1)]
    (* (ceil (/ unit-amount step-size)) (bigdec price))))

(defn calculate [transaction rules]
  "Calculate the amount for a session, given a pricing policy"
  (if (empty? rules)
    (do
      (log/warnf "No pricing policy and/or policy rules found for tenant %s and evse %s" (:tenant-id transaction) (:evse-id transaction))
      0M)
    (let [applicable (filter-elements rules transaction)]
      (if (empty? applicable)
        0M
        (reduce + (for [elem applicable]
                    (per-element elem transaction)))))))

(defn handle-session [session]
  (let [rules (store/get-policy-rules-by-evse (:tenant-id session) (:evse-id session))
        price (calculate session rules)]
    (assoc session :price price)))