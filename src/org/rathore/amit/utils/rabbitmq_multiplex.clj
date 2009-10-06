(ns org.rathore.amit.utils.rabbitmq-multiplex
  (:require clojure.contrib.accumulators))

(import '(com.rabbitmq.client.impl ChannelManager))
(use 'org.rathore.amit.utils.rabbitmq)
(use 'org.rathore.amit.utils.logger)

(def CONNECTION (ref nil))

(defn connection-closed? []
  (if-not @CONNECTION
    true
    (not (.isOpen @CONNECTION))))

(defn reset-connection []
  (dosync
   (ref-set CONNECTION nil)))

(defn init-connection [q-host q-username q-password]
  (dosync
   (if (or (nil? @CONNECTION) (connection-closed?))
     (do
       (ref-set CONNECTION (new-connection-for q-host q-username q-password))
       (log-message "RIBBIT!!")))))

(defn new-channel [q-host q-username q-password]
  (init-connection q-host q-username q-password)
  (log-message "Using connection " (.hashCode @CONNECTION))
  (let [channel (.createChannel @CONNECTION)]
   (log-message "CHAN:" channel)
    (if channel
      channel
      (do
	(log-message "RESET!!")
	(reset-connection)
	(new-channel q-host q-username q-password)))))

(defn- dispatch-message [payload q-host q-username q-password]
  (log-message "Trying to send message:" payload)
  (let [channel (new-channel q-host q-username q-password)]
    (drop-on-channel channel (payload :q-name) (payload :q-message-string)))
  (log-message "Sent message:" payload))

(defn send-on-q [q-host q-username q-password q-name q-message-string]
  (let [payload {:q-name q-name :q-message-string q-message-string}]
    (dispatch-message payload q-host q-username q-password)))


	
  