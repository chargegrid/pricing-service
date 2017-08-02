(ns pricing-service.routes
  (:require [pricing-service.calculate :as calc]
            [pricing-service.persistence.store :as store]
            [pricing-service.schema :as schema]
            [schema.core :as s]
            [compojure.api.sweet :refer :all]
            [ring.util.http-response :refer [ok created not-found]]
            [ring.middleware.logger :refer [wrap-with-logger]]
            [ring.middleware.reload :refer [wrap-reload]]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [camel-snake-kebab.core :refer [->kebab-case ->snake_case_string]]
            [cheshire.generate :refer [add-encoder encode-str]]
            [clj-time.core :as t]
            [onelog.core :as log])
  (:import (org.joda.time LocalTime)
           (com.fasterxml.jackson.core JsonGenerator)))

(defn ?ok [result]
  (if-not (nil? result) (ok result)))

(defn encode-local-time
  "Encode a joda LocalTime to the json generator."
  [^LocalTime tm ^JsonGenerator jg]
  (let [pad (fn [n] (format "%02d" n))]
    (.writeString jg (str (pad (t/hour tm)) ":" (pad (t/minute tm))))))

(add-encoder LocalTime encode-local-time)

(defn calculate-session [tenant-id policy-id session]
  (if-not (empty? (store/get-policy tenant-id policy-id))
    (ok
      {:price (calc/calculate (transform-keys ->kebab-case session)
                              (store/get-policy-rules tenant-id policy-id))})
    (not-found {:error "The specified pricing policy does not exist"})))

(defn list-rules-in-policy [tenant-id policy-id]
  (if-not (empty? (store/get-policy tenant-id policy-id))
    (ok (store/get-policy-rules tenant-id policy-id))
    (not-found {:error "The specified pricing policy does not exist"})))

(defn add-policy [tenant-id {:keys [name]}]
  (created (store/add-policy! tenant-id name)))

(defn update-policy [tenant-id policy-id {:keys [name]}]
  (store/update-policy! tenant-id policy-id name)
  (ok))

(defn remove-policy-rule [tenant-id policy-id rule-id]
  (store/remove-policy-rule! tenant-id policy-id rule-id)
  (ok))


(defn add-policy-rule [tenant-id policy-id rule]
  (if-not (empty? (store/get-policy tenant-id policy-id))
    (created (store/add-policy-rule! policy-id
                                      (transform-keys ->kebab-case rule)))
    (not-found {:error "The specified pricing policy does not exist"})))

(defn list-policies-for-evse [tenant-id evse-id]
  (let [results (store/get-policy-rules-by-evse tenant-id evse-id)
        grouped (group-by #(select-keys % [:policy-id :policy-name]) results)]
    (mapv (fn [r]
            (let [policy (first r)
                  rules (second r)]
              {:id    (:policy-id policy)
               :name  (:policy-name policy)
               :rules rules})) grouped)))

(def options {:format
              {:formats [:json]
               :response-opts
               {:json
                {:key-fn ->snake_case_string}}}})

(def api-routes
  (api options
       (context "" []
                :header-params [x-tenant-id :- s/Uuid]
                (GET "/pricing-policies" []
                     (?ok (store/get-all-policies x-tenant-id)))
                (GET "/pricing-policies/:id" [id]
                     :path-params [id :- s/Uuid]
                     (?ok (store/get-policy x-tenant-id id)))
                (POST "/pricing-policies/:id/attach-to-evses" [id]
                      :path-params [id :- s/Uuid]
                      :body [evses schema/AttachEvsesToPolicy]
                      (ok (store/attach-policy-to-evses x-tenant-id id (:evse_ids evses))))
                (POST "/pricing-policies" []
                      :body [policy schema/Policy]
                      (add-policy x-tenant-id policy))
                (PUT "/pricing-policies/:id" [id]
                      :body [policy schema/Policy]
                      :path-params [id :- s/Uuid]
                      (update-policy x-tenant-id id policy))
                (GET "/pricing-policies/:id/rules" [id]
                     :path-params [id :- s/Uuid]
                     (list-rules-in-policy x-tenant-id id))
                (POST "/pricing-policies/:id/rules" [id]
                      :path-params [id :- s/Uuid]
                      :body [rule schema/PolicyRule]
                      (add-policy-rule x-tenant-id id rule))
                (DELETE "/pricing-policies/:id/rules/:rule-id" [id rule-id]
                      :path-params [id :- s/Uuid
                                    rule-id :- s/Uuid]
                      (remove-policy-rule x-tenant-id id rule-id))
                (POST "/pricing-policies/:id/calculate" [id]
                      :path-params [id :- s/Uuid]
                      :body [session schema/Session]
                      (calculate-session x-tenant-id id session))
                (GET "/evses/:evse-id/pricing-policies" [evse-id]
                  :path-params [evse-id :- schema/EvseId]
                  (?ok (list-policies-for-evse x-tenant-id evse-id))))))

(def app (-> #'api-routes
             wrap-with-logger
             wrap-reload))
