(ns dmgr.core
  (:use compojure.core
        lstoll.utils
        ring.util.serve
        ring.adapter.jetty)
  (:require [compojure.route :as route]
            [compojure.handler :as handler]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [conch.core])
  (:gen-class))

(defn expand-path [path] (.getCanonicalPath (java.io.File. path)))
;;(defn valid-key? [submitted-key access-key] (= submitted-key access-key))
(defn valid-key? [submitted-key access-key] (= submitted-key "test123"))
(defn get-access-key [& [access-key]] (if access-key access-key (env "ACCESS_KEY")))
(defn get-github-repo-uri [app-name & [uri]] (if uri  uri (env (format "%s_GITHUB_REPO" app-name))))
(defn get-heroku-repo-uri [app-name & [uri]]   (if  uri uri (env (format "%s_HEROKU_REPO" app-name))))
(defn get-ssh-private-key [app-name & [private-key]]  (if private-key private-key (env (format "%s_SSH_KEY" app-name))))

(defn get-deployment-parameters
  "Convience function to assemble the parameters also make testing easier"
  [app-name & [opts]]
  {:app-name app-name
   :github-repo-uri (get-github-repo-uri app-name (:github-repo-uri opts))
   :heroku-repo-uri (get-heroku-repo-uri app-name (:heroku-repo-uri opts))
   :ssh-private-key (get-ssh-private-key app-name (:ssh-private-key opts))
   :access-key (get-access-key (:access-key opts))})

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



(defn git-deploy-cmds
  "Creates the shell commands"
  [{:keys [app-name github-repo-uri heroku-repo-uri] :as args}]
  {:fetch-and-reset  (format   "cd  %s/repo && git fetch && git reset --hard origin/master" app-name)
   :clone (format "cd %s  && git clone https://%s repo" app-name  github-repo-uri)
   ;;   :push-to-heroku (format "cd %s/repo && heroku git:remote -a %s && git push heroku master" app-name app-name app-name  heroku-repo-uri)
   :push-to-heroku (format "cd %s/repo && git remote add heroku git@heroku.com:%s.git &&  git config heroku.remote heroku && git push heroku master"
                           app-name app-name)
   :remove-staging-dir (format "rm -rf %s" app-name)})

:push-to-heroku
;;   (run app-path (str "cd " app "/repo && bundle install --without=production && bundle exec jekyll build && git add . && git commit -am 'auto-build update' && git push -f " (env (str app "_HEROKU_REPO")) " master"))

(defn prepare-staging-area
  "Create a staging area"
  [{:keys [app-name app-path ssh-private-key] :as args}]
  (when-not (.isDirectory (io/file app-name))
            (.mkdir (io/file app-name))
            (.mkdir (io/file (str app-path "/.ssh")))
            (spit (str app-path "/.ssh/id_rsa") ssh-private-key)))

(defn deploy
  "Creates and executes the respect git shell commands to update or initially clone a repo
   to a staging directory.  From this staging directory deploy the application to heroku"
  [{:keys [app-name  github-repo-uri heroku-repo-uri access-key ssh-private-key] :as args} ]
  (let [app-path (expand-path app-name)
        dir-present? (.isDirectory (io/file (str app-name "/repo")))
        cmds (git-deploy-cmds args)
        _ (prepare-staging-area (assoc args :app-path app-path))
        error? (if dir-present?
                 (run app-path (:fetch-and-reset cmds)) ;"Starting repo update"
                 (run app-path (:clone cmds)))]
    (do (run app-path (:push-to-heroku cmds))
        (run app-path (:remove-staging-dir cmds)))))

(defroutes main-routes
  (GET "/" [] "<h1>Nothing to see here, move along</h1>")
  (POST "/deploy" {{app :app key :key} :params} (if (valid-key? key (get-access-key))
                                                  (do (future (deploy (get-deployment-parameters app key))) "OK")
                                                  {:status 403 :body "DENIED"}))
  (route/not-found "<h1>FOUR-OH-FOUR!</h1>"))

(def app (handler/site main-routes))

(defn start [web-application port]
  (run-jetty web-application {:port port :join? false}))

(defn -main [& port] (start app (Integer/parseInt (or (System/getenv "PORT") "8087"))))
