(ns pricing-service.queue
  (:require [langohr.core :as rmq]
            [langohr.channel :as lch]
            [langohr.basic :as lb]
            [langohr.consumers :as lc]
            [pricing-service.settings :refer [config]]
            [clojure.data.json :as json]
            [pricing-service.calculate :as calc]
            [pricing-service.schema :as schema]
            [clojurewerkz.support.json]
            [clojure.tools.logging :as log]
            [camel-snake-kebab.extras :refer [transform-keys]]
            [camel-snake-kebab.core :refer [->kebab-case ->snake_case_string]]))

;; Send and receive messages

(defn publish-message [channel msg]
  (let [conf (:amqp config)
        exchange (:sessions-exchange conf)
        routing-key (:transactions-routing-key conf)]
    (lb/publish channel exchange routing-key
                (json/write-str msg :key-fn ->snake_case_string)
                {:content-type "application/json" :persistent true})))

(defn handle-message [channel meta ^bytes payload]
  (let [data (-> (String. payload "UTF-8")
                 (json/read-str :key-fn keyword)
                 schema/coerce-session)
        session (transform-keys ->kebab-case data)]
    (if-let [error (:error data)]
      (log/error "Cannot parse session " (pr-str error))
      (let [transaction (calc/handle-session session)]
        (log/info "Calculated price for RabbitMQ session: " (pr-str transaction))
        (publish-message channel transaction)))))

;; Basic RabbitMQ setup/subscribe/connect
(defn subscribe-declare-queue [ch handler]
  (let [conf (:amqp config)
        queue (:sessions-evse-queue-name conf)]
    (lc/subscribe ch queue handler {:auto-ack true})))

(defn shutdown [ch conn]
  (rmq/close ch)
  (rmq/close conn))

(defn setup []
  (let [connection (rmq/connect {:uri (-> config :amqp :url)})
        channel (lch/open connection)]
    (subscribe-declare-queue channel handle-message)
    (.addShutdownHook (Runtime/getRuntime)
                      (Thread. #(shutdown channel connection)))))
