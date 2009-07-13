(ns org.rathore.amit.utils.logger
  (:import (java.io FileWriter BufferedWriter File)
	    (org.apache.commons.io FileUtils))
  (:use org.rathore.amit.utils.config))

(defn spit [f content] 
  (let [file (File. f)]
    (if (not (.exists file))
      (FileUtils/touch file))
    (with-open [#^FileWriter fw (FileWriter. f true)]
      (with-open [#^BufferedWriter bw (BufferedWriter. fw)]
	(.write bw (str content "\n"))))))

(defn log-message [& message-tokens]
  (let [message (apply str (interleave message-tokens (repeat " ")))]
    (if (log-to-console?) 
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
  (log-message (stacktrace e)))