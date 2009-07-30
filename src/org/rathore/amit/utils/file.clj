(ns org.rathore.amit.utils.file)

(defn lines-of-file [file-name]
 (line-seq
  (java.io.BufferedReader.
   (java.io.InputStreamReader.
    (java.io.FileInputStream. file-name)))))


(def counter (atom 0))

(defn loop-test []
  (let [lots [1 2 3 4 5 6 7 8 9 10]]
    (loop [rem lots]
      (let [b (take 3 rem)]
	(println "taken" (count b))
	(swap! counter + (count b))
	(if-not (empty? rem)
	  (recur (drop 3 rem)))))))
	