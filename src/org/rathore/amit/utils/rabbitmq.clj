(ns org.rathore.amit.utils.rabbitmq
  (:import (com.rabbitmq.client ConnectionParameters ConnectionFactory MessageProperties QueueingConsumer)))
(use 'org.rathore.amit.utils.clojure)
(use 'org.rathore.amit.utils.logger)

(defn delivery-seq [ch q]
  (lazy-seq
    (let [d (.nextDelivery q)
          m (String. (.getBody d))]
      (.basicAck ch (.. d getEnvelope getDeliveryTag) false)
      (cons m (delivery-seq ch q)))))

;(defn queue-seq [conn queue-name]
;  (let [ch (.createChannel conn)]
;    (.queueDeclare ch queue-name)
;    (let [consumer (QueueingConsumer. ch)]
;      (.basicConsume ch queue-name consumer)
;      (delivery-seq ch consumer))))

(defn queue-seq 
  ([conn queue-name]
     (queue-seq conn nil nil queue-name))
  ([conn exchange-name exchange-type queue-name]
     (let [ch (.createChannel conn)]
       (.queueDeclare ch queue-name)
       (if (and exchange-name exchange-type)
         (do
           (log-message "exchange declared with" exchange-name exchange-type)
           (.exchangeDeclare ch exchange-name exchange-type)
           (.queueBind ch queue-name exchange-name ""))
         (log-message "skipping exchanges!"))
       (let [consumer (QueueingConsumer. ch)]
         (.basicConsume ch queue-name consumer)
         (delivery-seq ch consumer)))))

(defn new-connection-for [q-host q-username q-password]
  (let [params (doto (ConnectionParameters.)
		 (.setVirtualHost "/")
		 (.setUsername q-username)
		  (.setPassword q-password))
	factory (ConnectionFactory. params)]
    (.newConnection factory q-host)))

(defmacro with-connection [connection q-host q-username q-password & exprs]
  `(with-open [~connection (new-connection-for ~q-host ~q-username ~q-password)]
     (do ~@exprs)))

(defn drop-on-channel 
  ([chan q-name q-message-string]
     (drop-on-channel chan"" q-name q-message-string))
  ([chan exchange-name q-name q-message-string]
     (with-open [channel chan]
       (log-message "drop-on-channel:" channel, "exchange-name:" exchange-name)
       ;q-declare args: queue-name, passive, durable, exclusive, autoDelete other-args-map                                  
       (.queueDeclare channel q-name) ; true false false auto-delete-queue (new java.util.HashMap))
       (.basicPublish channel exchange-name q-name false true nil (.getBytes q-message-string))
       (log-message "dropped-on-channel:" channel))))

;(defn drop-on-new-channel [connection q-name q-message-string]
;  (with-open [channel (.createChannel connection)]
;    (drop-on-channel channel q-name q-message-string)))

;(defn send-on-transport-amqp [q-host q-username q-password q-name q-message-string]
;  (with-connection connection q-host q-username q-password
;    (drop-on-new-channel connection q-name q-message-string)))

(defn start-queue-message-handler-for-function-amqp 
  ([q-host q-username q-password q-name the-function]
     (start-queue-message-handler-for-function-amqp q-host q-username q-password nil nil q-name the-function))
  ([q-host q-username q-password exchange-name exchange-type q-name the-function]
     (with-connection connection q-host q-username q-password
       (doseq [message (queue-seq connection exchange-name exchange-type q-name)]
         (the-function message)))))
