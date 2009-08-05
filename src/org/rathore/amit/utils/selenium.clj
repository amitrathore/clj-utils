(ns org.rathore.amit.utils.selenium
  (:import (com.thoughtworks.selenium DefaultSelenium)))

(def PAUSE-TIME-MILLIS 10000)

(defn sauce-labs-start-command []
  (str "{\"username\": \"username-string\"," +
                "\"access-key\": \"access-key-string\"," +
                "\"os\": \"Linux\"," +
                "\"browser\": \"firefox\"," +
                "\"browser-version\": \"3.\"}",
                "http://saucelabs.com/"))

(defn selenium-client [rc-server start-url]
  (DefaultSelenium. rc-server 4444 "*chrome" start-url))

(defn pause-test []
  (Thread/sleep PAUSE-TIME-MILLIS))

(defmacro with-selenium [[sel-name rc-server start-url] & body]
  `(let [~sel-name (selenium-client ~rc-server ~start-url)]     
     (.start ~sel-name)
     ~@body
     (.stop ~sel-name)
     (println "Done.")))

(defn run-via-se-agent [se-test-fn]
  (let [a (agent :__ignore__)]
    (send-off a se-test-fn)))