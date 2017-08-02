(ns pricing-service.persistence.conversions
  (:require [korma.core :refer [raw]]
            [clojure.string :as s]
            [clj-time.core :as t])
  (:import (java.sql Time)
           (org.postgresql.util PGobject)
           (org.joda.time LocalTime)))

(defn pgarray->keywords [arr]
  (mapv keyword (.getArray arr)))

;; PG enums are PGObjects
(defn kw->pgobject
  [kw type]
  (doto (PGobject.)
    (.setType type)
    (.setValue (name kw))))

;; JDBC expects java.sql.Time for TIME fields, but we want to pass around Joda DateTimes
;; in our app (or in this case LocalTime) and clj-time doesn't provide the coercion we need
;; This looks hackish, but is the only solution I found that works without getting screwed
;; by local TZ settings.
;; See: http://stackoverflow.com/questions/9422753/converting-joda-localtime-to-sql-time/9422817#9422817
(defn local-time->sql-time [time]
  (-> time
      .toDateTimeToday
      .getMillis
      Time.))

(defn sql-time->local-time [time]
  (LocalTime/fromDateFields time))

(defn json-time->local-time [time-str]
  (t/local-time (read-string (subs time-str 0 2)) (read-string (subs time-str 3 5))))

;; Yes this is ugly, but inserting arrays of enum in PG can't be done in another way...
(defn keywords->pgarray-of-enum [coll type]
  (raw (str "'{" (s/join "," (mapv name coll)) "}'::" type "[]")))

(defn modify
  "Return record with field modified by fn if field exists, otherwise return record"
  [record field fn]
  (if (field record)
    (assoc record field (fn (field record)))
    record))