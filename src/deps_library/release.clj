(ns deps-library.release
  (:require [clojure.edn :as edn]
            [clojure.tools.cli :as cli]
            [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [deps-deploy.deps-deploy :as deps-deploy]
            [garamond.git :as git]
            [garamond.pom :as pom]
            [garamond.version :as v]
            [garamond.util :refer [exit]]
            [hf.depstar.uberjar :as uberjar]
            [clojure.string :as str]
            [taoensso.timbre :as timbre]))

(defn default-opts []
  (let [{:as status :keys [version prefix]} (git/current-status)]
    {:jar/path "target/project.jar"
     :jar/type :thin
     :git/status status
     :version version
     :prefix prefix
     :repository {"clojars" {:url "https://clojars.org/repo"
                             :username (System/getenv "CLOJARS_USERNAME")
                             :password (System/getenv "CLOJARS_PASSWORD")}}}))

(defn parse-version [version prefix]
  (-> version
      (str/replace-first prefix "")
      (v/parse)))

(defn tag [{:as options :keys [version git/status]}]
  (let [tag (v/to-string version options)]
    (println (str "TAG... " tag))
    (if (-> status :git :dirty?)
      (let [msg "Current repository is dirty, will not create a tag. Please commit your changes and retry."]
        (if (:dry-run options)
          (println (str " - [ERROR] " msg))
          (throw (ex-info msg options))))
      (when-not (:dry-run options)
        (try (git/tag! version options status)
             (catch Exception e
               (println :tag-error (ex-data e))
               (case (:code (ex-data e))
                 128 (println (str " - tag already exists (" tag "), continuing"))
                 (throw e)))))))
  options)

(defn pom [{:as options :keys [version]}]
  (println (str "POM... " (str (:group-id options) "/" (:artifact-id options)
                               " {:mvn/version \"" (:version options) "\"}")))
  (when-not (:dry-run options)
    (pom/generate! version options))
  options)

(defn jar [{:as options :keys [jar/path jar/type]}]
  (println (str "JAR... " path " (" (name type) ")"))
  (when-not (:dry-run options)
    (uberjar/uber-main {:dest path :jar type}
                       (:depstar/uber-main options)))
  options)

(defn deploy [{:as options :keys [jar/path]}]
  (println (str "DEPLOY... " (-> options :repository ffirst)))
  (when-not (:dry-run options)
    (deps-deploy/-main "deploy" path))
  options)

(def cmd-opts
  [["-v" "--version VERSION" "Specify a custom version to tag/publish"
    :parse-fn v/parse]
   ["-i" "--incr INCREMENT" "Specify how to increment the current version"]
   [nil "--patch" "Specifies patch increment"]
   [nil "--minor" "Specifies minor increment"]
   [nil "--major" "Specifies major increment"]
   [nil "--prefix" "Specifies version prefix"
    :default "v"]
   [nil "--dry-run" "Print expected actions, avoiding any side effects"]])

(defn main [& [command & args]]
  (let [cli-opts (:options (cli/parse-opts args cmd-opts))
        file-opts (-> (io/file "release.edn")
                      (slurp)
                      (edn/read-string))
        options (merge (default-opts)
                       file-opts
                       cli-opts)
        incr-type (cond (:incr options) (keyword (:incr options))
                        (:patch options) :patch
                        (:minor options) :minor
                        (:major options) :major)
        options (-> options
                    (update :prefix #(or % "v"))
                    (update :version #(cond-> %
                                              (string? %) (parse-version (:prefix options))
                                              incr-type (v/increment incr-type))))]
    (when (and (:version cli-opts) incr-type)
      (throw (ex-info
               (str "Cannot specify both a version (" (:version options)
                    ") and increment (" incr-type ")") options)))
    (when (:dry-run options) (println "DRY RUN"))
    (timbre/set-level! :warn)

    (case command
      "tag" (tag options)
      "release" (-> options
                    (tag)
                    (pom)
                    (jar)
                    (deploy))
      "pom" (pom options)
      "jar" (jar options)
      "deploy" (deploy options)
      (apply main "release" (cons command args)))))

(defn -main [& args]
  (apply main args)
  (System/exit 0))

;; Examples
;;
;; TAG
;;
;; clj -A:release tag                 # current version
;;
;; clj -A:release tag --patch         # incremented version
;; clj -A:release tag --minor
;; clj -A:release tag --major
;;
;; clj -A:release tag -v 0.1.2-alpha  # force version
;;
;; RELEASE
;;
;; clj -A:release                     # current version
;; clj -A:release --patch             # incremented version
;; clj -A:release -v 0.1.2            # force version

