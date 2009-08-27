(ns org.rathore.amit.utils.redis-helper
  (:require redis))

(def *redis-config* nil)

(defmacro with-redis [& exprs]
  `(if (nil? *redis-config*)
     (throw (Exception. "*redis-config* is not bound appropriately!"))
     (redis/with-server *redis-config*
       (do
	 ~@exprs))))


