(ns deps-library.release
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [deps-deploy.deps-deploy :as deps-deploy]
            [garamond.main :as garamond]
            [garamond.git :as git]
            [garamond.pom :as pom]
            [taoensso.timbre :as timbre]
            [hf.depstar.jar :as jar]))

(def default-opts
  {:jar-dir "target"
   :jar-name "project.jar"
   :repository {"clojars" {:url "https://clojars.org/repo"
                           :username (System/getenv "CLOJARS_USERNAME")
                           :password (System/getenv "CLOJARS_PASSWORD")}}})

(defn garamond-args [{:keys [group-id
                             artifact-id
                             scm-url]}]
  ["--group-id" group-id
   "--artifact-id" artifact-id
   "--scm-url" scm-url])

(defn jar [{:keys [jar-dir
                   jar-name]} & args]
  (jar/-main (str jar-dir "/" jar-name))
  #_(apply skinny/-main (concat args ["--no-libs" "--project-path" (str jar-dir "/" jar-name)])))

(defn pom [opts & args]
  (apply garamond/-main (concat (garamond-args opts) ["--pom"] args)))

(defn tag [opts & args]
  (apply garamond/-main (concat (garamond-args opts) ["--tag"] args)))

(defn -main [& [command & args]]
  (let [{:as opts
         :keys [jar-dir
                jar-name]} (-> (io/file "release.edn")
                               (slurp)
                               (edn/read-string)
                               (->> (merge default-opts)))]
    (case command
      "build-deploy" (do
                       (timbre/set-level! :warn)
                       (pom/generate! (:version (git/current-status)) opts)
                       (jar opts)
                       (sh/sh "cp" "pom.xml" jar-dir)
                       (apply deps-deploy/-main ["deploy" (str jar-dir "/" jar-name)])
                       (System/exit 0))
      "jar" (apply jar opts args)
      "pom" (apply pom opts args)
      "tag" (apply tag opts args)
      "deploy" (apply deps-deploy/-main (concat ["deploy"] args [(str jar-dir "/" jar-name)]))
      nil (-main "build-deploy"))))
