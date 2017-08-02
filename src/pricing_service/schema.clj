(ns pricing-service.schema
  (:require [schema.core :as s]
            [schema.coerce :as coerce]
            [ring.swagger.coerce :refer [coercer]])
  (:import (org.joda.time DateTime)))

;; These schema's are enforced for incoming API or RabbitMQ JSON.
;; Internally kebab-casing is used.

(s/defschema Policy
             {:name s/Str})

(s/defschema PolicyRule
             {:start_at  DateTime
              :end_at    DateTime
              :time_from String
              :time_to   String
              :days      [(s/enum :mon :tue :wed :thu :fri :sat :sun)]
              :unit      (s/enum :kwh :min :transaction)
              :step_size s/Int
              :price     s/Num})

(s/defschema Session
             {:started_at DateTime
              :ended_at   DateTime
              :volume     s/Num
              :user_id    String
              :evse_id    String
              :tenant_id  s/Uuid})

(s/defschema AttachEvsesToPolicy
            {:evse_ids [String]})

(def EvseIdRegEx
  #"[A-Z]{2}\*?[A-Z0-9]{3}\*?[E][A-Z0-9][A-Z0-9*]{0,30}")

(def EvseId (s/pred #(re-matches EvseIdRegEx %)))

(def coerce-session
  (coerce/coercer Session (coercer :json)))
