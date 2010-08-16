(ns org.rathore.amit.utils.config
  (:use org.rathore.amit.utils.process)
  (:import [org.productivity.java.syslog4j SyslogConstants]))

(def *clj-utils-config*)
;; config is a hash with the following keys
;; :log-to-console -> boolean, if true, logger will echo messages to the console
;; :logs-dir -> points to the directory where log files must be kept
;; :log-filename-prefix -> to prepend to the log filename, used as a visual identifier when there are lots of log files
;; :exception-notifier -> a hash containing :enabled (boolean), :from, :to, :subject-prefix
;; :syslog-facility -> one of the syslog facilities

;(defmacro with-notifier-config [& exprs]
;  `(if (nil? (:exception-notifier *clj-utils-config*))
;     (throw (Exception. "exception-notifier not configured inside *clj-utils-config*"))
;     (do ~@exprs)))

(defn should-log-to-console? []
  (:log-to-console *clj-utils-config*))

(defn notify-on-exception? []
  (let [en-settings (:exception-notifier *clj-utils-config*)]
    (:enabled en-settings)))

(defn error-notification-from []
  (let [en-settings (:exception-notifier *clj-utils-config*)]
    (:from en-settings)))

(defn error-notification-to []
  (let [en-settings (:exception-notifier *clj-utils-config*)]
    (:to en-settings)))

(defn error-notification-subject-prefix []
  (let [en-settings (:exception-notifier *clj-utils-config*)]
    (:subject-prefix en-settings)))

(defn log-filename-prefix []
  (:log-filename-prefix *clj-utils-config*))

(defn log-file []
  (let [dirname (:logs-dir *clj-utils-config*)
        prefix (log-filename-prefix)]
    (str  dirname "/"  prefix "_" (process-pid) ".log")))

(defn syslog-enabled? []
  (:syslog-enabled *clj-utils-config*))

(def SYSLOG-FACILITIES {
  :local0   SyslogConstants/FACILITY_LOCAL0
  :local1   SyslogConstants/FACILITY_LOCAL1
  :local2   SyslogConstants/FACILITY_LOCAL2
  :local3   SyslogConstants/FACILITY_LOCAL3
  :local4   SyslogConstants/FACILITY_LOCAL4
  :local5   SyslogConstants/FACILITY_LOCAL5
  :local6   SyslogConstants/FACILITY_LOCAL6
  :local7   SyslogConstants/FACILITY_LOCAL7
  :auth     SyslogConstants/FACILITY_AUTH
  :authpriv SyslogConstants/FACILITY_AUTHPRIV
  :cron     SyslogConstants/FACILITY_CRON
  :daemon   SyslogConstants/FACILITY_DAEMON
  :ftp      SyslogConstants/FACILITY_FTP
  :kern     SyslogConstants/FACILITY_KERN
  :lpr      SyslogConstants/FACILITY_LPR
  :mail     SyslogConstants/FACILITY_MAIL
  :news     SyslogConstants/FACILITY_NEWS
  :syslog   SyslogConstants/FACILITY_SYSLOG
  :user     SyslogConstants/FACILITY_USER
  :uucp     SyslogConstants/FACILITY_UUCP
})

(defn syslog-facility []
  ((:syslog-facility *clj-utils-config*) SYSLOG-FACILITIES))