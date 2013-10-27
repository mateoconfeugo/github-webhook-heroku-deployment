(ns dmgr.test.core
  ^{:author "Matthew Burns"
    :doc "Acceptance test revolving around proving a user can trigger a heroku app install from a github webhook
         using a third heroku app running  in the clould"}
  (:use  [dmgr.core])
 (:import [java.io FileNotFoundException File]
          [org.eclipse.jgit.lib RepositoryBuilder]
          [org.eclipse.jgit.internal.storage.file FileRepository]
          [org.eclipse.jgit.transport RemoteConfig RefSpec URIish UsernamePasswordCredentialsProvider]
           [org.eclipse.jgit.api Git InitCommand StatusCommand AddCommand
            ListBranchCommand PullCommand MergeCommand LogCommand PushCommand
            LsRemoteCommand Status ResetCommand$ResetType FetchCommand])
 (:require [clj-webdriver.taxi :as scraper :refer [set-driver! to click exists? input-text submit quit]]
           [clj-jgit.porcelain :refer [with-identity]]
            [clojure.core]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [conch.core]
            [expectations :refer [expect]]

            [fs.core :refer [delete-dir]]
            [lstoll.utils :refer [env]]
            [ring.mock.request :refer [request]]
            [tentacles.repos :refer [create-repo]]
            [tentacles.core :refer [api-call]]))

;;=========================================================================
;; TESTS SETUP
;;=========================================================================
(def test-opts {:github-repo-uri "github.com/mateoconfeugo/lhg.git"
                :heroku-repo-uri "git@heroku.com"
                :ssh-public-key "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDxhl4HNWTmtANOfHDjFJvZsCv2zQ53dNXpQvR5hbyIdWA6X7bhTYOhrOlAuOBp55wR4vwbRg6zQ9AhMeCAuuh4ZjO5K68gyxRtNgCEYu/TIiKq5ob2BGT1KlczqD4LpvQVnvHyH17U+aCFhO8Xl35jZ3GMr2YZc9mxh8iNqjUUUZsj0IotfQA3bzTPC3ecM+m4H4sQ0k+pUvhdB3SAvE/bM6nbRgwzPHhp5YI/oMEpIhls8TAyo3iVR6YkbHEcMvbrHUOArVoSa1uHVkoWdn0ZH/5IG7Y6msY9/QMgJmwiBvH5qKWLomtZ8vCdtn13NlSli7Ek6+PE1dktCxJudjst matthewburns@mattsmac.local"
                :access-key "test123"})

;;=========================================================================
;; UNIT TEST:  Get the correct path
;;=========================================================================
(def test-app-name "lhg")
(def expected-path  (str (System/getProperty "user.dir") "/lhg"))
(expect true (= expected-path (expand-path test-app-name)))
(def test-params (get-deployment-parameters test-app-name test-opts))
(def test-params (get-deployment-parameters test-app-name ))
(env "lhg_GITHUB_REPO")
(def test-git-deploy-cmds (git-deploy-cmds test-params))
(def test-stageing-area (prepare-staging-area (assoc test-params :app-path (expand-path test-app-name))))

(get-deployment-parameters )

;;=========================================================================
;; UNIT TEST:  Make sure the external process runs the proper shell commands
;;=========================================================================
(deploy test-params)

;;========================================================================
;; ACCEPTANCE TEST: Ring Compojure clojure web app installed from github
;; when the web hook is triggered
;;========================================================================
(def test-webhook-uri "/deploy?app=lhg&key=test123")

(def github-repo-uri (get-github-repo-uri "lhg"))
(def heroku-repo-uri  (get-heroku-repo-uri "lhg"))
(def private-key (get-ssh-private-key "lhg"))

(git-push-heroku-master {:app-name "lhg"
                         :staging-parent-dir-path "test"
                         :src-repo-uri "https://github.com/mateoconfeugo/lhg.git"
                         :src-repo-remote "origin"
                         :branch-name "master"})

(expect true (= (:status (app (request :post test-webhook-uri))) 200))
(def working-dir (File. (:working-copy-tmp-dir test-remote-git)))
(delete-dir working-dir)
