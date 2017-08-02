(ns pricing-service.persistence.db
  (:require [korma.db :refer [defdb postgres]]
            [ragtime.jdbc :as jdbc]
            [ragtime.core :refer [migrate-all into-index]]
            [clojure.tools.logging :as log]
            [pricing-service.settings :refer [config]]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [camel-snake-kebab.core :refer [->kebab-case ->snake_case]]
            [perseverance.core :as p])
  (:import (org.postgresql.util PSQLException)))

(def db-spec-string
  (let [{:keys [user password host db port]} (:database config)]
    (str "jdbc:postgresql://" user ":" password "@" host ":" port "/" db)))

(def db-opts (merge (:database config)
                    {:naming {:keys ->kebab-case
                              :fields ->snake_case}}))

(defn migrate []
  (let [store (jdbc/sql-database db-spec-string)
        migrations (jdbc/load-resources "migrations")
        index (into-index migrations)]
    (log/info "Running migrations if need be...")
    (migrate-all store index migrations)))

(defn attempt-migrate []
  (p/retriable {:catch [PSQLException]} (migrate)))

; Setup korma
(defdb db (postgres db-opts))
