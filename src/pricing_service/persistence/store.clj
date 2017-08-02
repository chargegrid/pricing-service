(ns pricing-service.persistence.store
  (:require [pricing-service.persistence.db]
            [korma.core :refer :all :as k]
            [pricing-service.persistence.conversions :refer :all]
            [clj-time.jdbc]
            [onelog.core :as log])
  (:import (java.util UUID)))

(defn uuid [] (UUID/randomUUID))

;; entities
(declare policies)
(declare policy-rules)
(declare policies-evses)

(defentity policies
           (has-many policy-rules {:fk :policy_id})
           (has-many policies-evses {:fk :policy_id}))

(defentity policy-rules
           (table :policy_rules)
           (belongs-to policies {:fk :policy_id})
           (entity-fields :*)
           (transform (fn [rule]
                        (-> rule
                            (modify :days pgarray->keywords)
                            (modify :unit keyword)
                            (modify :time-from sql-time->local-time)
                            (modify :time-to sql-time->local-time))))
           (prepare (fn [rule]
                      (-> rule
                          (modify :days #(keywords->pgarray-of-enum % "weekday"))
                          (modify :unit #(kw->pgobject % "unit"))
                          (modify :time-from local-time->sql-time)
                          (modify :time-to local-time->sql-time)))))

(defentity policies-evses
           (table :policies_evses)
           (belongs-to policies {:fk :policy_id}))

;; Queries

(defn get-all-policies [tenant-id]
  (select policies
          (where {:tenant-id tenant-id})
          (with policies-evses) ; TODO: Optimize once we have customers
          (with policy-rules))) ; TODO: Optimize once we have customers


(defn get-policy [tenant-id id]
  (first (select policies
                 (where {:id id :tenant-id tenant-id})
                 (with policies-evses) ; TODO: Optimize once we have customers
                 (with policy-rules)))) ; TODO: Optimize once we have customers

; TODO: make tenant- & history-aware, preferably perform in one SQL query
(defn attach-policy-to-evses [tenant-id policy-id evses]
  (doseq [evse-id evses]
    (do
      (log/info "Deleting policy for " evses)
      (delete policies-evses
              (where {:evse_id evse-id}))
      (insert policies-evses
              (values {:policy_id policy-id
                       :evse_id evse-id}))))
  nil)



(defn get-policy-rules [tenant-id policy-id]
  (select policy-rules
          (join :right policies (= :policy-rules.policy-id :policies.id))
          (where {:policies.tenant-id tenant-id :policy-id policy-id})))


(defn get-policy-rules-by-evse [tenant-id evse-id]
  (select policy-rules
          (fields [:policies.name :policy-name])
          (join :right policies-evses (= :policies_evses.policy_id :policy_rules.policy_id))
          (join :right policies (= :policies.id :policy_rules.policy_id))
          (where {:policies.tenant-id tenant-id})
          (where {:policies-evses.evse-id evse-id})))

(defn update-vals [map vals f]
  (reduce #(update-in % [%2] f) map vals))

(defn add-policy-rule! [policy-id rule]
  (let [id (uuid)
        rule-with-times (update-vals rule [:time-from :time-to] json-time->local-time)
        full-rule (assoc rule-with-times :id id :policy-id policy-id)]
    (insert policy-rules (values full-rule))))


; TODO: Validate tenant-id, policy-id and it is in future
(defn remove-policy-rule! [tenant-id policy-id rule-id]
  (delete policy-rules
    (where {:id rule-id})))



(defn add-policy! [tenant-id name]
  (let [id (uuid)]
    (insert policies (values {:id id :tenant-id tenant-id :name name}))))

(defn update-policy! [tenant-id policy-id name]
  (update policies
    (set-fields {:name name})
    (where {:tenant-id tenant-id
            :id policy-id})))


