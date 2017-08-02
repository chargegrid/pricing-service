(defproject pricing-service "0.1.0-SNAPSHOT"
  :description "Gets sessions from queue, adds price, puts them on other queue as transaction"

  :dependencies [[org.clojure/clojure "1.8.0"]
                 [clj-time "0.11.0"]
                 [metosin/compojure-api "1.0.1"]
                 [com.novemberain/langohr "3.5.0"]
                 [org.clojure/data.json "0.2.6"]
                 [ring/ring-defaults "0.1.5"]
                 [ring/ring-json "0.4.0"]
                 [ring/ring-devel "1.4.0"]
                 [ring-cors "0.1.7"]
                 [cprop "0.1.6"]
                 [org.clojure/algo.generic "0.1.2"]
                 [http-kit "2.1.18"]
                 [clojurewerkz/support "1.1.0"]
                 [camel-snake-kebab "0.3.2"]
                 ;; database
                 [korma "0.4.2"]
                 [org.postgresql/postgresql "9.4.1207.jre7"]
                 [ragtime "0.5.2"]
                 ;; logging
                 [org.clojure/tools.logging "0.3.1"]
                 [ch.qos.logback/logback-classic "1.1.6"]
                 [ring.middleware.logger "0.5.0" :exclusions [org.slf4j/slf4j-log4j12]]
                 [org.slf4j/log4j-over-slf4j "1.7.18"]
                 [com.grammarly/perseverance "0.1.2"]]

  :profiles {:uberjar {:aot :all}}

  :target-path "target/%s/"

  :main pricing-service.core
  :uberjar-name "pricing-service.jar")
