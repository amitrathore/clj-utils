(ns org.rathore.amit.utils.logger
  (:import (java.io FileWriter BufferedWriter File)
	    (org.apache.commons.io FileUtils))
  (:use org.rathore.amit.utils.config)
  (:use org.rathore.amit.utils.sql)
  (:use clojure.contrib.str-utils)
  (:use org.rathore.amit.utils.mailer))

(declare email-exception)

(defn spit [f content] 
  (let [file (File. f)]
    (if (not (.exists file))
      (FileUtils/touch file))
    (with-open [#^FileWriter fw (FileWriter. f true)]
      (with-open [#^BufferedWriter bw (BufferedWriter. fw)]
	(.write bw (str content "\n"))))))

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

(defn log-exception [e]
  (log-message (stacktrace e))
  (if (notify-on-exception?)
    (email-exception e)))

(defn email-exception [e]
  (let [subject (str (error-notification-subject-prefix) " " (.getMessage e))
	body (str-join "\n" [(timestamp-for-now) (stacktrace e) (str "\nAlso logged to" (log-file))])]
    (send-email-async (error-notification-from) (error-notification-to) subject body)))

