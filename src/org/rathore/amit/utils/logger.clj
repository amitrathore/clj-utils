(ns org.rathore.amit.utils.logger
  (:import (java.io FileWriter BufferedWriter File)
	    (java.net InetAddress))
  (:use org.rathore.amit.utils.config)
  (:use org.rathore.amit.utils.file)
  (:use org.rathore.amit.utils.sql)
  (:use clojure.contrib.str-utils)
  (:use org.rathore.amit.utils.mailer))

(declare email-exception)

(defn log-message [& message-tokens]
  (let [timestamp-prefix (str (timestamp-for-now) ": ")
	message (apply str timestamp-prefix (interleave message-tokens (repeat " ")))]
    (if (should-log-to-console?) 
      (println message))
    (spit (log-file) message)))

(defn exception-name [e]
  (.getName (.getClass e)))

(defn stacktrace [e]
  (apply str 
	 (cons (str (exception-name e) "\n")
	       (cons (str (.getMessage e) "\n")
		     (map #(str (.toString %) "\n") (.getStackTrace e))))))

(defn log-exception
  ([e additional-message]
     (let [cause (last (take-while #(not (nil? %)) (iterate #(.getCause %) e)))]
       (log-message additional-message)
       (log-message (stacktrace e))
       (when-not (= e cause)
         (log-message "The cause is:")
         (log-message (stacktrace cause)))
       (when (notify-on-exception?)
         (email-exception e additional-message)
         nil)))
  ([e]
     (log-exception e "\n")))

(defn email-exception [e additional-message]
  (let [subject (str (error-notification-subject-prefix) " " (.getMessage e))
	body (str-join "\n" [(timestamp-for-now) additional-message "\n" (stacktrace e) (str "\nAlso logged to" (log-file) " on " (InetAddress/getLocalHost))])]
    (send-email-async (error-notification-from) (error-notification-to) subject body)))

(defmacro with-exception-logging [& exprs]
  `(try
    (do ~@exprs)
    (catch Exception e#
      (log-exception e#))))
