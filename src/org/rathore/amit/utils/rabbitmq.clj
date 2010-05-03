(ns org.rathore.amit.utils.rabbitmq
  (:import (com.rabbitmq.client ConnectionParameters ConnectionFactory MessageProperties QueueingConsumer))
  (:use org.rathore.amit.utils.clojure org.rathore.amit.utils.logger)
  (:use clojure.stacktrace)
  (:use clojure.contrib.except))

(def DEFAULT-EXCHANGE-NAME "")
(def DEFAULT-EXCHANGE-TYPE "direct")
(def FANOUT-EXCHANGE-TYPE "fanout")
(def RABBITMQ-CONNECTION (atom nil))

(declare new-connection)

(def init-rabbitmq-connection 
  (create-runonce
   (fn [q-host q-username q-password]
     (reset! RABBITMQ-CONNECTION (new-connection q-host q-username q-password)))))

(defn new-connection [q-host q-username q-password]
  (let [params (doto (ConnectionParameters.)
		 (.setVirtualHost "/")
		 (.setUsername q-username)
                 (.setPassword q-password))]
    (.newConnection (ConnectionFactory. params) q-host)))

(defn new-channel []
  (if (nil? @RABBITMQ-CONNECTION)
    (throwf "RABBITMQ-CONNECTION is not initialized!"))
  (doto (.createChannel @RABBITMQ-CONNECTION)
    (.basicQos 1)))

(defn send-message
  ([routing-key message-object]
     (send-message DEFAULT-EXCHANGE-NAME DEFAULT-EXCHANGE-TYPE routing-key message-object))
  ([exchange-name exchange-type routing-key message-object]
     (with-open [channel (new-channel)]
       (.exchangeDeclare channel exchange-name exchange-type)
       (.queueDeclare channel routing-key)
       (.basicPublish channel exchange-name routing-key nil (.getBytes (str message-object))))))

(defn delivery-from [channel consumer]
  (let [delivery (.nextDelivery consumer)]
    (.basicAck channel (.. delivery getEnvelope getDeliveryTag) false)
    (String. (.getBody delivery))))

(defn consumer-for [channel exchange-name exchange-type queue-name routing-key]
  (let [consumer (QueueingConsumer. channel)]
    (.exchangeDeclare channel exchange-name exchange-type)
    (.queueDeclare channel queue-name)
    (.queueBind channel queue-name exchange-name routing-key)
    (.basicConsume channel queue-name consumer)
    consumer))

(defn random-queue []
  (str (java.util.UUID/randomUUID)))

(defn next-message-from
  ([queue-name]
     (next-message-from DEFAULT-EXCHANGE-NAME DEFAULT-EXCHANGE-TYPE queue-name queue-name))
  ([exchange-name exchange-type routing-key]
     (next-message-from exchange-name exchange-type (random-queue) routing-key))
  ([exchange-name exchange-type queue-name routing-key]
     (with-open [channel (new-channel)]
       (let [consumer (consumer-for channel exchange-name exchange-type queue-name routing-key)]
         (delivery-from channel consumer)))))

(defn- lazy-message-seq [channel consumer]
  (lazy-seq
   (let [message (delivery-from channel consumer)]
     (cons message (lazy-message-seq channel consumer)))))

(defn message-seq 
  ([channel queue-name]
     (message-seq DEFAULT-EXCHANGE-NAME DEFAULT-EXCHANGE-TYPE channel queue-name queue-name))
  ([exchange-name exchange-type channel routing-key]
     (message-seq exchange-name exchange-type channel (random-queue) routing-key))
  ([exchange-name exchange-type channel queue-name routing-key]
     (let [consumer (consumer-for channel exchange-name exchange-type queue-name routing-key)]
       (lazy-message-seq channel consumer))))

(defn start-queue-message-handler 
  ([routing-key handler-fn]
     (start-queue-message-handler DEFAULT-EXCHANGE-NAME DEFAULT-EXCHANGE-TYPE routing-key handler-fn))
  ([queue-name routing-key handler-fn]
     (with-open [channel (new-channel)]
       (doseq [m (message-seq DEFAULT-EXCHANGE-NAME DEFAULT-EXCHANGE-TYPE
                              channel queue-name routing-key)]
         (handler-fn m))))
  ([exchange-name exchange-type routing-key handler-fn]
     (with-open [channel (new-channel)]
       (doseq [m (message-seq exchange-name exchange-type channel routing-key)]
         (handler-fn m)))))
