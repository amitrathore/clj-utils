(ns org.rathore.amit.utils.process
  (:import (java.lang.management ManagementFactory)))

(defn process-pid []
  (let [m-name (.getName (ManagementFactory/getRuntimeMXBean))]
    (first (.split m-name "@"))))
