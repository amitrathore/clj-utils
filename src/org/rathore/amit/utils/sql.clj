(ns org.rathore.amit.utils.sql
  (:import (java.sql Date Time)))

(defn timestamp-for-sql [time-in-millis]
  (str (.toString (Date. time-in-millis)) " " (.toString (Time. time-in-millis))))

(defn timestamp-for-now []
  (timestamp-for-sql (System/currentTimeMillis)))