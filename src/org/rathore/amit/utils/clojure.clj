(ns org.rathore.amit.utils.clojure)

(defn var-ize [var-vals]
  (loop [ret [] vvs (seq var-vals)]
    (if vvs
      (recur  (conj (conj ret `(var ~(first vvs))) (second vvs))
	      (next (next vvs)))
      (seq ret))))

(defn push-thread-bindings [bindings-map]
  (clojure.lang.Var/pushThreadBindings bindings-map))

(defn pop-thread-bindings []
  (clojure.lang.Var/popThreadBindings))

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