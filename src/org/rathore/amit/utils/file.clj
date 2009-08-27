(ns org.rathore.amit.utils.file)

(import '(java.io FileWriter BufferedWriter File))
(import '(java.io FileWriter FileReader))
(import '(org.apache.commons.io FileUtils))

(defn lines-of-file [file-name]
 (line-seq
  (java.io.BufferedReader.
   (java.io.InputStreamReader.
    (java.io.FileInputStream. file-name)))))
	
(defn spit [f content] 
  (let [file (File. f)]
    (if (not (.exists file))
      (FileUtils/touch file))
    (with-open [#^FileWriter fw (FileWriter. f true)]
      (with-open [#^BufferedWriter bw (BufferedWriter. fw)]
	(.write bw (str content "\n"))))))

