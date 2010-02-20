(ns org.rathore.amit.utils.rabbitmq
  (:import (com.rabbitmq.client ConnectionParameters ConnectionFactory MessageProperties QueueingConsumer))
  (:import (com.runa StableChannels))
  (:use org.rathore.amit.utils.clojure org.rathore.amit.utils.logger)
  (:use clojure.stacktrace))

(defn new-multiplexer [q-host q-username q-password]
  (let [stable-channels (StableChannels. q-host q-username q-password)]
    (fn [accessor]
      (cond 
	(= accessor :new-channel) (.createChannel stable-channels)))))

(def *rabbitmq-multiplexer*)

(defn delivery-seq [ch q]
  (lazy-seq
    (let [d (.nextDelivery q)
          m (String. (.getBody d))]
      (.basicAck ch (.. d getEnvelope getDeliveryTag) false)
      (cons m (delivery-seq ch q)))))

(defn queue-seq 
  ([conn queue-name]
     (queue-seq conn nil nil queue-name))
  ([conn exchange-name exchange-type queue-name]
     (let [ch (.createChannel conn)]
       (.queueDeclare ch queue-name)
       (if (and exchange-name exchange-type)
         (do
           (.exchangeDeclare ch exchange-name exchange-type)
           (.queueBind ch queue-name exchange-name "")))
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

(defn do-with-connection [conn new-connection-thunk the-function])

;(defmacro with-connection [connection q-host q-username q-password & exprs]
;  `(with-open [~connection (new-connection-for ~q-host ~q-username ~q-password)]
;     (do ~@exprs)))

(defmacro with-connection [connection q-host q-username q-password & exprs]
  `(let [xx# (fn zz# [~connection]
               (with-open [~connection ~connection]
                 (try
                  (do ~@exprs)
                  (catch Exception e#
                    (log-message "Catching...")
                    (log-exception e#)
                    (Thread/sleep 10000)
                    (log-message "and recovering...")
                    (zz# (new-connection-for ~q-host ~q-username ~q-password))))))]
     (xx# (new-connection-for ~q-host ~q-username ~q-password))))

(defn drop-on-channel 
  ([chan q-name q-message-string]
     (drop-on-channel chan "" q-name q-message-string))
  ([chan exchange-name q-name q-message-string]
     (with-open [channel chan]
       ;q-declare args: queue-name, passive, durable, exclusive, autoDelete other-args-map                                  
       (.queueDeclare channel q-name) ; true false false auto-delete-queue (new java.util.HashMap))
       (.basicPublish channel exchange-name q-name false true nil (.getBytes q-message-string)))))

(defn start-queue-message-handler-for-function-amqp 
  ([q-host q-username q-password q-name the-function]
     (start-queue-message-handler-for-function-amqp q-host q-username q-password nil nil q-name the-function))
  ([q-host q-username q-password exchange-name exchange-type q-name the-function]
     (with-connection connection q-host q-username q-password
       (doseq [message (queue-seq connection exchange-name exchange-type q-name)]
         (the-function message)))))

(defn send-on-q 
  ([q-name q-message-string]
     (let [channel (*rabbitmq-multiplexer* :new-channel)]
       (drop-on-channel channel q-name q-message-string)))
  ([exchange-name q-name q-message-string]
     (let [channel (*rabbitmq-multiplexer* :new-channel)]
       (drop-on-channel channel exchange-name q-name q-message-string))))  