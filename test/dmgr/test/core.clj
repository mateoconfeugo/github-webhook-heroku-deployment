(ns dmgr.test.core
  ^{:author "Matthew Burns"
    :doc "Acceptance test revolving around proving a user can trigger a heroku app install from a github webhook
         using a third heroku app running  in the clould"}
  (:use  [dmgr.core])
  (:require [clj-webdriver.taxi :as scraper :refer [set-driver! to click exists? input-text submit quit]]
            [clojure.core]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [conch.core]
            [expectations :refer [expect]]
            [lstoll.utils :refer [env]]
            [ring.mock.request :refer [request]]
            [tentacles.repos :refer [create-repo]]
            [tentacles.core :refer [api-call]]))

;;=========================================================================
;; TESTS SETUP
;;=========================================================================
(def test-opts {:github-repo-uri "github.com/mateoconfeugo/lhg.git"
                :heroku-repo-uri "git@heroku.com"
                :ssh-private-key "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQDxhl4HNWTmtANOfHDjFJvZsCv2zQ53dNXpQvR5hbyIdWA6X7bhTYOhrOlAuOBp55wR4vwbRg6zQ9AhMeCAuuh4ZjO5K68gyxRtNgCEYu/TIiKq5ob2BGT1KlczqD4LpvQVnvHyH17U+aCFhO8Xl35jZ3GMr2YZc9mxh8iNqjUUUZsj0IotfQA3bzTPC3ecM+m4H4sQ0k+pUvhdB3SAvE/bM6nbRgwzPHhp5YI/oMEpIhls8TAyo3iVR6YkbHEcMvbrHUOArVoSa1uHVkoWdn0ZH/5IG7Y6msY9/QMgJmwiBvH5qKWLomtZ8vCdtn13NlSli7Ek6+PE1dktCxJudjst matthewburns@mattsmac.local"
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

;;=========================================================================
;; UNIT TEST:  Make sure the external process runs the proper shell commands
;;=========================================================================
(def git-fetch-test-cmd nil)
(def git-clone-test-cmd nil)
(:push-to-heroku test-git-deploy-cmds)
;(deploy test-params)

;;========================================================================
;; ACCEPTANCE TEST: Ring Compojure clojure web app installed from github
;; when the web hook is triggered
;;========================================================================
(def test-webhook-uri "/deploy?app=lhg&key=test123")

(get-github-repo-uri "lhg" "blarg")
(get-ssh-private-key "lhg")


(expect true (= (:status (app (request :post test-webhook-uri))) 200))
(keys (System/getenv ))
