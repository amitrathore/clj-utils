(ns org.rathore.amit.utils.mailer
  (:use com.draines.postal.core))

(defn send-email-async [from to subject body]
  (let [email {:from from
	       :to to
	       :subject subject
	       :body body}
	mailer-agent (agent email)]
    (send mailer-agent send-message)))