(ns org.rathore.amit.utils.clojure
  (:import (java.io PushbackReader StringReader)))

(defn var-ize [var-vals]
  (loop [ret [] vvs (seq var-vals)]
    (if vvs
      (recur  (conj (conj ret `(var ~(first vvs))) (second vvs))
	      (next (next vvs)))
      (seq ret))))

(defmacro run-and-measure-timing [expr]
  `(let [start-time# (System/currentTimeMillis)
	 response# ~expr
	 end-time# (System/currentTimeMillis)]
     {:time-taken (- end-time# start-time#) :response response# :start-time start-time# :end-time end-time#}))

(defn destructured-hash [attribs]
  (let [d-pair (fn [attrib]
		 (list attrib (.replace (name attrib) "-" "_")))]		 
  (apply hash-map (mapcat d-pair attribs))))

(defmacro def-hash-method [method-name params & exprs]
  `(defn ~method-name [~(destructured-hash params)]
     (do
       ~@exprs)))

;(defn read-clojure-str [object-str]
;  (read (PushbackReader. (StringReader. object-str))))

(defmacro defmemoized [fn-name args & body]
  `(def ~fn-name (memoize (fn ~args 
			    (do
			      ~@body)))))

(defn create-runonce [function] 
  (let [sentinel (Object.)
        result (atom sentinel)] 
    (fn [& args]
      (locking sentinel 
        (if (= @result sentinel)
          (reset! result (apply function args)) 
          @result)))))

(defmacro defrunonce [fn-name args & body]
  `(def ~fn-name (create-runonce (fn ~args ~@body))))