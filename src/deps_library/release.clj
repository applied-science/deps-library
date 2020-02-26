(ns deps-library.release
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [deps-deploy.deps-deploy :as deps-deploy]
            [garamond.main :as garamond]
            [garamond.git :as git]
            [garamond.pom :as pom]
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

(defn tag [opts & args]
  (apply garamond/-main (concat (garamond-args opts)
                                (:garamond-tag-args opts)
                                ["--tag"] args)))

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
      "tag" (apply tag opts (or (seq args) ["patch"]))
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
