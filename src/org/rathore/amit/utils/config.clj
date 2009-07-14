(ns org.rathore.amit.utils.config
  (:use org.rathore.amit.utils.process))

(def *rathore-utils-config* nil)
;; config is a hash with the following keys
;; :log-to-console -> boolean, if true, logger will echo messages to the console
;; :logs-dir -> points to the directory where log files must be kept
;; :log-filename-prefix -> to prepend to the log filename, used as a visual identifier when there are lots of log files

(defmacro with-config [& exprs]
  `(if (nil? *rathore-utils-config*)
     (throw (Exception. "*rathore-utils-config* is not bound appropriately!"))
     (do ~@exprs)))

(defn should-log-to-console? []
  (with-config
   (:log-to-console *rathore-utils-config*)))

(defn log-file []
  (with-config
   (let [dirname (:logs-dir *rathore-utils-config*)
	 prefix (:log-filename-prefix *rathore-utils-config*)]
     (str  dirname "/"  prefix (process-pid) ".log"))))