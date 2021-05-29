(ns applied-science.deps-library
  (:require [clojure.edn :as edn]
            [clojure.tools.cli :as cli]
            [clojure.java.io :as io]
            [deps-deploy.deps-deploy :as deps-deploy]
            [garamond.git :as git]
            [garamond.pom :as pom]
            [garamond.version :as v]
            [garamond.util :refer [exit]]
            [hf.depstar.uberjar :as uberjar]
            [hf.depstar :as depstar]
            [clojure.string :as str]
            [taoensso.timbre :as timbre]))

(defn default-options [{:as cli-options
                        :keys [clojars-username clojars-password]}]
  (let [{:as status :keys [version prefix]} (try (git/current-status)
                                                 (catch java.lang.NullPointerException e nil))]
    {:jar/path "target/project.jar"
     :jar/type :thin
     :git/status status
     :version version
     :prefix (or prefix "v")
     :skip-tag false
     :repository {"clojars" {:url "https://clojars.org/repo"
                             :username (or clojars-username (System/getenv "CLOJARS_USERNAME"))
                             :password (or clojars-password (System/getenv "CLOJARS_PASSWORD"))}}}))

(defn sanitize-options
  "Removes sensitive details from options (for logging)"
  [options]
  (let [assoc-if (fn [m k v] (if (m k) (assoc m k v) m))]
    (-> options
        (assoc-if :clojars-password "XXXX")
        (update-in [:repository "clojars"] assoc-if :password "XXXX"))))

(defn fail! [message options]
  (throw (ex-info (str "\n" message) (sanitize-options options))))

(defn parse-version [version prefix]
  (-> version
      (str/replace-first prefix "")
      (v/parse)))

(defn tag [{:as options :keys [version git/status]}]
  (let [tag (v/to-string version options)]
    (println (str "Tag... " tag))
    (when-not (:dry-run options)
      (try (git/tag! version options status)
           (catch Exception e
             (case (:code (ex-data e))
               128 (println (str " - tag already exists (" tag "), continuing"))
               (throw e))))))
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
    (depstar/jar {:jar path}))
  options)

(defn deploy [{:as options :keys [jar/path]}]
  (println (str "Deploy... " (-> options sanitize-options :repository ffirst)))
  (when-not (:dry-run options)
    (deps-deploy/-main "deploy" path))
  options)

(defn install [{:as options :keys [jar/path]}]
  (println (str "Install... "))
  (when-not (:dry-run options)
    (deps-deploy/-main "install" path))
  options)

(def cmd-opts
  [["-v" "--version VERSION" "Specify a fixed version"]
   ["-i" "--incr INCREMENT" "Increment the current version"]
   [nil "--skip-tag" "Do not create a git tag for this version"
    :default-desc false]
   [nil "--prefix PREFIX" "Version prefix for git tag"
    :default-desc "v"]
   [nil "--patch" "Increment patch version"]
   [nil "--minor" "Increment minor version"]
   [nil "--major" "Increment major version"]
   [nil "--config CONFIG" "Path to EDN options file"
    :default "release.edn"]
   [nil "--group-id GROUP-ID"]
   [nil "--artifact-id ARTIFACT-ID"]
   [nil "--scm-url SCM-URL" "The source control management URL (eg. github url)"]
   [nil "--clojars-username CLOJARS-USERNAME" "Your Clojars username"
    :default-desc "environment variable"]
   [nil "--clojars-password CLOJARS-PASSWORD" "Your Clojars password"
    :default-desc "environment variable"]
   [nil "--dry-run" "Print expected actions, avoiding any side effects"]
   ["-h" "--help" "Print CLI options"]])

(defn main [& args]
  (let [{cli-options :options
         :keys [summary]} (cli/parse-opts args cmd-opts)
        file-options (some-> (:config cli-options)
                             (io/file)
                             (as-> file (when (.exists file) file))
                             (slurp)
                             (edn/read-string))
        options (merge (default-options cli-options)
                       file-options
                       cli-options)
        incr-type (cond (:incr options) (keyword (:incr options))
                        (:patch options) :patch
                        (:minor options) :minor
                        (:major options) :major)
        options (-> options
                    (update :prefix #(or % "v"))
                    (update :version #(cond-> %
                                              (string? %) (parse-version (:prefix options))
                                              incr-type (v/increment incr-type))))
        force-static-version (or (:version cli-options)
                                 (:version file-options))
        COMMAND (first args)]
    (if (:help options)
      (println summary)
      (do
        (some-> (cond (nil? (:version options))
                      (if force-static-version
                        (format "Invalid version: %s" force-static-version)
                        (format (str "No version specified. "
                                     "Add a starting tag (eg. git tag v0.1.0), "
                                     "pass a --version argument, or include a :version "
                                     "in %s.")
                                (:config cli-options)))

                      (and incr-type force-static-version)
                      (format "Cannot increment a version specified via CLI or file (%s - %s)"
                              force-static-version incr-type)

                      (and (-> options :git/status :git :dirty?)
                           (not (:dry-run options))
                           (not (#{"install"
                                   "version"} COMMAND)))
                      "Current repository has uncommitted work. Please commit your changes and retry.")
                (fail! options))

        (when (:dry-run options) (println "DRY RUN"))

        (timbre/set-level! :warn)

        (case COMMAND
          "version" (println (v/to-string (:version options) options)
                             (str "(" (or (some-> incr-type name) "no change") ")"))
          "tag" (tag options)
          "release" (-> options
                        (cond-> (not (:skip-tag options)) (tag))
                        (pom)
                        (jar)
                        (deploy))
          "pom" (pom options)
          "jar" (jar options)
          "deploy" (deploy options)
          "install" (-> options
                        (pom)
                        (jar)
                        (install))
          (apply main "release" args))))))

(defn -main [& args]
  (apply main args)
  (System/exit 0))

(comment
  (main "--dry-run"))

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

