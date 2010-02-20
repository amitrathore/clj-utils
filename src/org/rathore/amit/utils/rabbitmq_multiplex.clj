(ns org.rathore.amit.utils.rabbitmq-multiplex
  (:require clojure.contrib.accumulators))

(import '(com.rabbitmq.client.impl ChannelManager))
(import '(com.runa StableChannels))
(use 'org.rathore.amit.utils.rabbitmq)
(use 'org.rathore.amit.utils.logger)

(defn new-multiplexer [q-host q-username q-password]
  (let [stable-channels (StableChannels. q-host q-username q-password)]
    (fn [accessor]
      (cond 
	(= accessor :new-channel) (.createChannel stable-channels)))))

(def *rabbitmq-multiplexer*)

(defn send-on-q 
  ([q-name q-message-string]
     (let [channel (*rabbitmq-multiplexer* :new-channel)]
       (drop-on-channel channel q-name q-message-string)))
  ([exchange-name q-name q-message-string]
     (let [channel (*rabbitmq-multiplexer* :new-channel)]
       (drop-on-channel channel exchange-name q-name q-message-string))))  

