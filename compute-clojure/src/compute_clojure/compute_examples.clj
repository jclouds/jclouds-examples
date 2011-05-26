(ns compute-clojure.compute-examples
  (:use
    [org.jclouds.compute2]
    [clojure.java.io])
  (:import
    [org.jclouds.domain Credentials]
    [org.jclouds.scriptbuilder InitBuilder]
    [org.jclouds.compute.options TemplateOptions$Builder]
    [org.jclouds.scriptbuilder.domain Statement Statements]
    [org.jclouds.scriptbuilder.statements.login AdminAccess]))

(defn in-group? [group]
  #(= group (.getGroup %)))

(defn print-nodes-in-group [compute group]
  (let [nodes (nodes-with-details-matching compute #(= group (.getGroup %)))]
    (map #(println (format "<< node %s private ips:%s public-ips:%s%n" (.getId %) (private-ips %) (public-ips %))) nodes)))

(defn get-credentials []
  (Credentials.
    (System/getProperty "user.name")
    (slurp (str (System/getProperty "user.home") "/.ssh/id_rsa") :encoding "utf-8")))

(defn options [login]
  (-> (TemplateOptions$Builder/overrideCredentialsWith login)
    (.runAsRoot false)
    (.wrapInInitScript false)))

(defn print-exec-response [response]
  (let [node (key response)
        output (val response)]
    (println (format "<< node: %s %s %s %s%n" (.getId node) (private-ips node) (public-ips node) output))))

(defn create [compute group]
  "Create a new node in the given group"
  (do
    (println (format ">> adding node to group: %s%n" group))
    (let [response (create-node compute group (TemplateOptions$Builder/runScript (AdminAccess/standard)))]
      (println (format "<< %s%n" response))
      response)))

(defn exec [compute command group login]
  "Execute command for the given group, using login"
  (do
    (println (format ">> running [%s] on group %s as %s%n" command group (.identity login)))
    (let [responses (run-script-on-nodes-matching compute (in-group? group) (Statements/exec command) (options login))]
      (map print-exec-response responses)
      responses)))

(defn destroy [compute group]
  "Destroy all nodes in the given group"
  (do
    (println (format ">> destroying nodes in group %s%n" group))
    (let [responses (destroy-nodes-matching compute (in-group? group))]
      (map #(println (format "<< %s%n" %)) responses)
      responses)))