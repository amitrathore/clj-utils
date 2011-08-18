(ns org.rathore.amit.utils.rabbitmq
  (:import (com.rabbitmq.client  ConnectionFactory MessageProperties QueueingConsumer))
  (:use org.rathore.amit.utils.rabbit-pool
        org.rathore.amit.utils.clojure
        org.rathore.amit.utils.logger)
  (:use clojure.stacktrace)
  (:use clojure.contrib.except
        clojure.contrib.str-utils))

(def DEFAULT-EXCHANGE-NAME "default-exchange")
(def DEFAULT-EXCHANGE-TYPE "direct")
(def FANOUT-EXCHANGE-TYPE "fanout")

(def *PREFETCH-COUNT* 1)

(defn init-rabbitmq-connection [q-host q-username q-password]
  (init-pool q-host q-username q-password))

(defn- wait-for-seconds [n]
  (log-message "message-seq: waiting" n "seconds to reconnect to RabbitMQ...")
  (Thread/sleep (* 1000 n)))

(defn create-channel-guaranteed []
  ;;(log-message "[Rabbit: active idle max]" (pool-status))
  (let [c (get-connection-from-pool)] ;;is outside try, so rabbit-down-exception bubbles up
    (try 
     (let [ch (.createChannel c)]
       (return-connection-to-pool c)
       (.basicQos ch *PREFETCH-COUNT*)
       ch)
     (catch Exception e
       (log-message "create-channel, error:" (class e) " creating channel, retrying...")
       (invalidate-connection c)
       (wait-for-seconds (rand-int 2))
       #(create-channel-guaranteed)))))

(defn create-channel []
  (trampoline create-channel-guaranteed))

(defn delete-queue [q-name]
  (with-open [chan (create-channel)]
    (.queueDelete chan q-name)))

(defn utf-payload [message-object]
  (.getBytes (str message-object) "UTF-8"))

(defn send-message
  ([routing-key message-object]
     (send-message DEFAULT-EXCHANGE-NAME DEFAULT-EXCHANGE-TYPE routing-key message-object))
  ([exchange-name exchange-type routing-key message-object]
     (with-open [channel (create-channel)]
       (.exchangeDeclare channel exchange-name exchange-type)
       (.queueDeclare channel routing-key false false false nil)
       (.basicPublish channel exchange-name routing-key nil (utf-payload message-object)))))

(defn send-message-if-queue
  ([routing-key message-object]
     (send-message-if-queue DEFAULT-EXCHANGE-NAME DEFAULT-EXCHANGE-TYPE routing-key message-object))
  ([exchange-name exchange-type routing-key message-object]
     (with-open [channel (create-channel)]
       (.basicPublish channel exchange-name routing-key nil (utf-payload message-object)))))

(defn delivery-from [channel consumer]
  (let [delivery (.nextDelivery consumer)]    
    [(String. (.getBody delivery) "UTF-8") #(.basicAck channel (.. delivery getEnvelope getDeliveryTag) false)]))

(defn consumer-for [channel exchange-name exchange-type queue-name routing-key]
  (let [consumer (QueueingConsumer. channel)]
    (.exchangeDeclare channel exchange-name exchange-type)
    (.queueDeclare channel queue-name false false false nil)
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
     (with-open [channel (create-channel)]
       (let [consumer (consumer-for channel exchange-name exchange-type queue-name routing-key)]
         (delivery-from channel consumer)))))

(declare guaranteed-delivery-from)

(defn recover-from-delivery [exchange-name exchange-type queue-name routing-key channel-atom consumer-atom]
  (try 
   (wait-for-seconds (rand-int 7))
   (let [new-channel (create-channel)
         new-consumer (consumer-for new-channel exchange-name exchange-type queue-name routing-key)]
     (reset! channel-atom new-channel)
     (reset! consumer-atom new-consumer)
     #(guaranteed-delivery-from exchange-name exchange-type queue-name routing-key channel-atom consumer-atom))
   (catch Exception e
     (log-message "recover-from-delivery: got error" (class e) "Retrying...")
     #(recover-from-delivery exchange-name exchange-type queue-name routing-key channel-atom consumer-atom))))

(defn guaranteed-delivery-from [exchange-name exchange-type queue-name routing-key channel-atom consumer-atom]
  (try
   (delivery-from @channel-atom @consumer-atom)
   (catch Exception e
     (log-message "guaranteed-delivery-from: got-error" (class e) "Recovering...")
     #(recover-from-delivery exchange-name exchange-type queue-name routing-key channel-atom consumer-atom))))

(defn- lazy-message-seq [exchange-name exchange-type queue-name routing-key channel-atom consumer-atom]
  (lazy-seq
    (let [message (trampoline guaranteed-delivery-from exchange-name exchange-type queue-name routing-key channel-atom consumer-atom)]
      (cons message (lazy-message-seq exchange-name exchange-type queue-name routing-key channel-atom consumer-atom)))))

(defn message-seq 
  ([channel queue-name]
     (message-seq DEFAULT-EXCHANGE-NAME DEFAULT-EXCHANGE-TYPE channel queue-name queue-name))
  ([exchange-name exchange-type channel routing-key]
     (message-seq exchange-name exchange-type channel (random-queue) routing-key))
  ([exchange-name exchange-type channel queue-name routing-key]
     (let [channel-atom (atom channel) 
           consumer-atom (atom (consumer-for channel exchange-name exchange-type queue-name routing-key))]
       (lazy-message-seq exchange-name exchange-type queue-name routing-key channel-atom consumer-atom))))

(defn start-queue-message-handler 
  ([routing-key handler-fn]
     (start-queue-message-handler DEFAULT-EXCHANGE-NAME DEFAULT-EXCHANGE-TYPE routing-key handler-fn))
  ([queue-name routing-key handler-fn]
     (with-open [channel (create-channel)]
       (doseq [[m ack-fn] (message-seq DEFAULT-EXCHANGE-NAME DEFAULT-EXCHANGE-TYPE channel queue-name routing-key)]
         (handler-fn m ack-fn))))
  ([exchange-name exchange-type routing-key handler-fn]
     (with-open [channel (create-channel)]
       (doseq [[m ack-fn] (message-seq exchange-name exchange-type channel routing-key)]
         (handler-fn m ack-fn))))
  ([exchange-name exchange-type queue-name routing-key handler-fn]
     (with-open [channel (create-channel)]
       (doseq [[m ack-fn] (message-seq exchange-name exchange-type channel queue-name routing-key)]
         (handler-fn m ack-fn)))))
