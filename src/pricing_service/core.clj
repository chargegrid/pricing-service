(ns pricing-service.core
  (:require [pricing-service.routes :refer [app]]
            [pricing-service.queue :as queue]
            [org.httpkit.server :refer [run-server]]
            [clojure.tools.logging :as log]
            [pricing-service.persistence.db :as db]
            [pricing-service.settings :refer [config]]
            [perseverance.core :as p])
  (:gen-class))

(defn -main [& args]
  (p/retry {} (db/attempt-migrate))
  (queue/setup)
  (let [port (:port config)]
    (log/info "Server started at port " port)
    (run-server app {:port port})))
