(ns dmgr.core
  (:use compojure.core
        lstoll.utils
        ring.util.serve
        ring.adapter.jetty)
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [conch.core]
            [clj-jgit.porcelain :refer [with-identity]]
            [fs.core :refer [delete-dir]]
            [tentacles.repos :as github :refer [archive-link ]])
 (:import [java.io FileNotFoundException File]
          [org.eclipse.jgit.lib RepositoryBuilder ]
          [org.eclipse.jgit.internal.storage.file FileRepository]
           [org.eclipse.jgit.api Git InitCommand StatusCommand AddCommand
            ListBranchCommand PullCommand MergeCommand LogCommand
            LsRemoteCommand Status ResetCommand$ResetType
            FetchCommand])
  (:gen-class))

(defn expand-path [path] (.getCanonicalPath (java.io.File. path)))
;;(defn valid-key? [submitted-key access-key] (= submitted-key access-key))
(defn valid-key? [submitted-key access-key] (= submitted-key "test123"))
(defn get-access-key [& [access-key]] (if access-key access-key (env "ACCESS_KEY")))
(defn get-github-repo-uri [app-name & [uri]] (if uri  uri (env (format "%s_GITHUB_REPO" app-name))))
(defn get-heroku-repo-uri [app-name & [uri]]   (if  uri uri (env (format "%s_HEROKU_REPO" app-name))))
(defn get-ssh-private-key [app-name & [private-key]]  (if private-key private-key (env (format "%s_SSH_KEY" app-name))))

(defn run
  "Run the shell commands with the correct environment variables returning exit status code"
  [app-path cmd]
  (let [exec ["sh" "-c" cmd :env (merge (into {} (System/getenv))
                                        {"GIT_SSH" (str (expand-path ".") "/bin/git-ssh")
                                         "SSH_KEY" (str app-path "/.ssh/id_rsa")})]
        _ (log cmd)
        proc (apply conch.core/proc exec)]
    (future (conch.core/stream-to :out proc *out*))
    (future (conch.core/stream-to :err proc *out*))
    (conch.core/exit-code proc)))

(defn git-push-heroku-master
  "Push the project into heroku to trigger its build deploy"
  [{:keys [app-name staging-parent-dir-path src-repo-uri src-repo-remote branch-name] :as args} ]
  (let [app-path (expand-path (format "%s/staging" staging-parent-dir-path))
        staging-dir (File. app-path)
        _ (when (.isDirectory staging-dir) (delete-dir staging-dir))
        from-git (-> (Git/cloneRepository) (.setURI src-repo-uri) (.setDirectory staging-dir)
                     (.setBranch branch-name) (.setBare false) (.setRemote src-repo-remote)
                     (.setNoCheckout false) .call)
        from-repo (.getRepository from-git)
        repo-config (.getConfig from-repo)
        _ (.setString repo-config "remote" "heroku"  "url" (format "git@heroku.com:%s.git" app-name))
        _ (.setString repo-config "remote" "heroku" "fetch" "+refs/heads/*:refs/remotes/heroku/*")
        _ (.save repo-config)
        commit-result (-> from-git .commit (.setMessage "deployment commit to allow a push to heroku repo master branch") .call)
        shell-push-cmd (format "cd %s  && git push -f heroku master" app-path)]
    (run app-path shell-push-cmd)))

(defn prepare-staging-area
  "Create a staging area for the app"
  [{:keys [app-path ssh-private-key] :as args}]
  (when-not (.isDirectory (io/file app-path))
            (.mkdir (io/file app-path))
            (.mkdir (io/file (str app-path "/.ssh")))
            (spit (str app-path "/.ssh/id_rsa") ssh-private-key)))

(defn deploy
  "Potential interface function"
  [{:keys [app-name  github-repo-uri heroku-repo-uri access-key ssh-private-key
           staging-parent-dir-path] :as args} ]
  (let [app-path (expand-path (format "staging/%s" app-name))
        dir-present? (.isDirectory (io/file app-path))
        _ (prepare-staging-area (assoc args :app-path app-path))]
    (git-push-heroku-master {:app-name app-name :staging-parent-dir-path  staging-parent-dir-path
                             :src-repo-uri github-repo-uri :src-repo-remote "origin" :branch-name "master"})))

(defn get-deployment-parameters
  "Convience function to assemble the parameters also make testing easier"
  [app-name & [opts]]
  {:app-name app-name
   :github-repo-uri (get-github-repo-uri app-name (:github-repo-uri opts))
   :heroku-repo-uri (get-heroku-repo-uri app-name (:heroku-repo-uri opts))
   :ssh-private-key (get-ssh-private-key app-name (:ssh-private-key opts))
   :access-key (get-access-key (:access-key opts))})

(defroutes main-routes
  (GET "/" [] "<h1>Nothing to see here, move along</h1>")
  (POST "/deploy" {{app :app key :key} :params} (if (valid-key? key (get-access-key))
                                                  (do (future (deploy (get-deployment-parameters app key))) "OK")
                                                  {:status 403 :body "DENIED"}))
  (route/not-found "<h1>FOUR-OH-FOUR!</h1>"))

(def app (handler/site main-routes))

(defn start [web-application port]
  (run-jetty web-application {:port port :join? false}))

(defn -main [& port]
  (start app (Integer/parseInt (or (System/getenv "PORT") "8087"))))
