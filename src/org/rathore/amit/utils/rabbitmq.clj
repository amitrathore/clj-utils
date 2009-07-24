(ns org.rathore.amit.utils.rabbitmq
  (:import (com.rabbitmq.client ConnectionParameters ConnectionFactory MessageProperties QueueingConsumer)))
(require '(org.danlarkin [json :as json]))

(defn delivery-seq [ch q]
  (lazy-seq
    (let [d (.nextDelivery q)
          m (String. (.getBody d))]
      (.basicAck ch (.. d getEnvelope getDeliveryTag) false)
      (cons m (delivery-seq ch q)))))

(defn queue-seq [conn queue-name]
  (let [ch (.createChannel conn)]
    (.queueDeclare ch queue-name)
    (let [consumer (QueueingConsumer. ch)]
      (.basicConsume ch queue-name consumer)
      (delivery-seq ch consumer))))

(defmacro with-connection [connection q-host q-username q-password & exprs]
  `(with-open [~connection (let [params# (doto (ConnectionParameters.)
					 (.setVirtualHost "/")
					 (.setUsername ~q-username)
					 (.setPassword ~q-password))
				factory# (ConnectionFactory. params#)]
			    (.newConnection factory# ~q-host))]
     (do ~@exprs)))

(defn send-on-transport-amqp [q-host q-username q-password q-name q-message-object]
  (with-connection connection q-host q-username q-password
    (with-open [channel (.createChannel connection)]
      (doto channel
	;q-declare args: queue-name, passive, durable, exclusive, autoDelete other-args-map
	(.queueDeclare q-name); true false false auto-delete-queue (new java.util.HashMap))
	(.basicPublish "" q-name false true nil (.getBytes (json/encode-to-str q-message-object)))))))

(defn start-queue-message-handler-for-function-amqp [q-host q-username q-password q-name the-function]
  (with-connection connection q-host q-username q-password
    (with-open [messages (queue-seq connection q-name)]
      (doseq [message messages]
	(do
	  (println "message:" message)
	  (the-function (json/decode-from-str message)))))))