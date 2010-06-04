(ns org.rathore.amit.utils.config
  (:use org.rathore.amit.utils.process))

(def *clj-utils-config*)
;; config is a hash with the following keys
;; :log-to-console -> boolean, if true, logger will echo messages to the console
;; :logs-dir -> points to the directory where log files must be kept
;; :log-filename-prefix -> to prepend to the log filename, used as a visual identifier when there are lots of log files
;; :exception-notifier -> a hash containing :enabled (boolean), :from, :to, :subject-prefix

(defmacro with-notifier-config [& exprs]
  `(if (nil? (:exception-notifier *clj-utils-config*))
     (throw (Exception. "exception-notifier not configured inside *clj-utils-config*"))
     (do ~@exprs)))

(defn should-log-to-console? []
  (:log-to-console *clj-utils-config*))

(defn notify-on-exception? []
  (with-notifier-config
   (let [en-settings (:exception-notifier *clj-utils-config*)]
     (:enabled en-settings))))

(defn error-notification-from []
  (with-notifier-config
   (let [en-settings (:exception-notifier *clj-utils-config*)]
     (:from en-settings))))

(defn error-notification-to []
  (with-notifier-config
   (let [en-settings (:exception-notifier *clj-utils-config*)]
     (:to en-settings))))

(defn error-notification-subject-prefix []
  (with-notifier-config
   (let [en-settings (:exception-notifier *clj-utils-config*)]
     (:subject-prefix en-settings))))

(defn log-file []
  (let [dirname (:logs-dir *clj-utils-config*)
        prefix (:log-filename-prefix *clj-utils-config*)]
    (str  dirname "/"  prefix "_" (process-pid) ".log")))