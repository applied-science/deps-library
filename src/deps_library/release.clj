(ns deps-library.release
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [deps-deploy.deps-deploy :as deps-deploy]
            [garamond.main :as garamond]
            [garamond.git :as git]
            [garamond.pom :as pom]
            [garamond.version :as v]
            [garamond.util :refer [exit]]
            [taoensso.timbre :as timbre]
            [hf.depstar.uberjar :as uberjar]))

(defn default-opts []
  {:jar/path "target/project.jar"
   :jar/type :thin

   :version (:version (git/current-status))

   :repository {"clojars" {:url "https://clojars.org/repo"
                           :username (System/getenv "CLOJARS_USERNAME")
                           :password (System/getenv "CLOJARS_PASSWORD")}}})

(defn garamond-args [{:keys [group-id
                             artifact-id
                             scm-url]}]
  ["--group-id" group-id
   "--artifact-id" artifact-id
   "--scm-url" scm-url])

(defn tag [args]
  (let [{:keys [incr-type options exit-message ok?]} (garamond/validate-args args)
        status (git/current-status)
        opts (assoc options :prefix (or (:prefix options) (:prefix status) "v"))
        new-version (cond-> (:version status)
                            incr-type (v/increment incr-type))]

    (cond exit-message
          (exit (if ok? 0 1) exit-message)

          (and (:tag opts) (-> status :git :dirty?))
          (exit 1 "Current repository is dirty, will not create a tag. Please commit your changes and retry."))

    (git/tag! new-version opts status)

    (timbre/infof "Created new git tag %s from %s increment of %s"
                  (str (:prefix opts) new-version)
                  (name incr-type)
                  (:current status))))

(defn -main [& [command & args]]
  (let [opts (-> (io/file "release.edn")
                 (slurp)
                 (edn/read-string)
                 (->> (merge (default-opts))))

        ;; commands
        pom #(pom/generate! (:version opts) opts)
        jar #(uberjar/uber-main {:dest (:jar/path opts) :jar (:jar/type opts)}
                                (:depstar/uber-main opts []))
        deploy #(deps-deploy/-main "deploy" (:jar/path opts))]
    (case command
      ("patch"
        "minor"
        "major") (do (tag (conj (garamond-args opts) "patch"))
                     (-main "build-deploy"))
      "tag" (tag (conj (garamond-args opts) (or (first args) "patch")))
      "build-deploy" (do
                       (timbre/set-level! :warn)
                       (pom)
                       (jar)
                       (deploy))
      "pom" (pom)
      "jar" (jar)
      "deploy" (deploy)
      nil (-main "build-deploy"))
    (System/exit 0)))
