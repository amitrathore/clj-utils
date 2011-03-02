(ns org.rathore.amit.utils.rabbit-pool
  (:use org.rathore.amit.utils.clojure
        org.rathore.amit.utils.logger
        clojure.contrib.str-utils)
  (:import [com.rabbitmq.client ConnectionFactory MessageProperties QueueingConsumer]
           [com.rabbitmq.client.impl AMQConnection]
           [org.apache.commons.pool.impl GenericObjectPool]
           [org.apache.commons.pool BasePoolableObjectFactory]
           [com.rabbitmq.client AlreadyClosedException]))

(def *POOL* (atom nil))
(def *MAX-POOL-SIZE* 10)
(def *MAX-IDLE-SIZE* 10)

(declare connection-valid?)

(defn new-rabbit-connection [host username password]
  (.newConnection
   (doto (ConnectionFactory.)
     (.setVirtualHost "/")
     (.setUsername username)
     (.setPassword password)
     (.setHost host))))

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
            (.setWhenExhaustedAction GenericObjectPool/WHEN_EXHAUSTED_BLOCK)
            (.setMaxIdle *MAX-IDLE-SIZE*)
            (.setTestWhileIdle true))]
    (reset! *POOL* p)))

(defn pool-status []
  [(.getNumActive @*POOL*) (.getNumIdle @*POOL*) *MAX-POOL-SIZE*])

(defn get-connection-from-pool []
  (.borrowObject #^GenericObjectPool @*POOL*))

(defn return-connection-to-pool [c]
  (.returnObject #^GenericObjectPool @*POOL* c))

(defn invalidate-connection [c]
  (.invalidateObject @*POOL* c))

