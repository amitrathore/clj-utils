(ns org.rathore.amit.utils.rabbit-pool
  (:use org.rathore.amit.utils.clojure
        org.rathore.amit.utils.logger)
  (:import [com.rabbitmq.client ConnectionParameters ConnectionFactory MessageProperties QueueingConsumer]
           [com.rabbitmq.client.impl AMQConnection]
           [org.apache.commons.pool.impl GenericObjectPool]
           [org.apache.commons.pool BasePoolableObjectFactory]
           [com.rabbitmq.client AlreadyClosedException]))


(def *POOL* (atom nil))
(def *MAX-POOL-SIZE* 10)
(def *POOL-EVICTION-RUN-EVERY-MILLIS* 60000)

(defn new-rabbit-connection [host username password]
  (let [params (doto (ConnectionParameters.)
                 (.setVirtualHost "/")
                 (.setUsername username)
                 (.setPassword password))]
    (.newConnection (ConnectionFactory. params) host)))

(defn connection-valid? [c]
  (try
   (.ensureIsOpen c)
   true
   (catch AlreadyClosedException ace
     false)))

(defn connection-factory [host username password]
  (proxy [BasePoolableObjectFactory] []
    (makeObject []
      (new-rabbit-connection host username password))
    (validateObject [c]
      (connection-valid? c))
    (destroyObject [c]
      (try
       (.close #^AMQConnection c)
       (catch Exception e)))))

(defrunonce init-pool [host username password]
  (let [factory (connection-factory host username password)
        p (doto (GenericObjectPool. factory)
            (.setMaxActive *MAX-POOL-SIZE*)
            (.setLifo false)
            (.setTimeBetweenEvictionRunsMillis *POOL-EVICTION-RUN-EVERY-MILLIS*)
            (.setWhenExhaustedAction GenericObjectPool/WHEN_EXHAUSTED_BLOCK)
            (.setTestWhileIdle true)
            ;(.setTestOnBorrow true)
            )]
    (reset! *POOL* p)))

(defn pool-status []
  [(.getNumActive @*POOL*) (.getNumIdle @*POOL*) *MAX-POOL-SIZE*])

(defn get-connection-from-pool []
  ;(log-message "Pool stats [active idle max]:" (pool-status))
  (.borrowObject #^GenericObjectPool @*POOL*))

(defn return-connection-to-pool [c]
  (.returnObject #^GenericObjectPool @*POOL* c))

(defn invalidate-connection [c]
  (.invalidateObject @*POOL* c))

(defn create-channel [c]
  ;(log-message "Creating channel with" c)
  (try 
   (let [ch (.createChannel c)]
     (return-connection-to-pool c)
     (.basicQos ch 1)
     ch)
   (catch Exception e
       (log-exception e)
       (invalidate-connection c)
       (create-channel (get-connection-from-pool)))))

