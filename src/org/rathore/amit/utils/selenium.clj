(ns org.rathore.amit.utils.selenium
  (:import (com.thoughtworks.selenium DefaultSelenium))
  (:require (org.danlarkin [json :as json])))

(def PAUSE-TIME-MILLIS 20000)

(defn sauce-labs-start-command [username-string access-key-string]
  (json/encode-to-str
   {"username" username-string
    "access-key" access-key-string
    "os" "Windows 2003"
    "browser" "iexplore"
    "browser-version" "7."}))

(defn selenium-client [rc-server start-url]
  (DefaultSelenium. rc-server 4444 "*chrome" start-url))

(defn saucy-client [start-url]
  (DefaultSelenium. "saucelabs.com" 4444 (sauce-labs-start-command "amit" "c80c708c-5c7b-420c-b58c-032774a75d17") start-url))

(defn pause-test []
  (Thread/sleep PAUSE-TIME-MILLIS))

(defmacro with-selenium [[sel-name local rc-server start-url] & body]
  `(let [~sel-name (if ~local
		     (selenium-client ~rc-server ~start-url)
		     (saucy-client ~start-url))]
     (.start ~sel-name)
     ~@body
     (.stop ~sel-name)
     (println "Done.")))

(defn run-via-se-agent [se-test-fn]
  (let [a (agent :__ignore__)]
    (send-off a se-test-fn)))