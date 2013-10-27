(defproject github-webhook-heroku-deployment "1.0.0"
  :description "Web hook events to repo trigger a heroku deployment"
  :heroku {:app-name "github-heroku-deploy" :app-url "http://github-heroku-deploy.herokuapp.com"}
  :ring {:handler dmgr.core/app :auto-reload? true :auto-refresh true}
  :uberjar-name "github-heroku-deploy-standalone.jar"
  :min-lein-version "2.0.0"
  :main dmgr.core
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [clj-webdriver "0.6.0"]
                 [compojure "1.1.5"]
                 [ring-serve "0.1.2"]
                 [conch "0.3.1"]
                 [tentacles "0.2.6"]
                 [clj-jgit "0.6.1"]
                 [me.raynes/fs "1.4.4"]
                 [org.eclipse.jgit "3.1.0.201310021548-r"]
                 [net.lstoll/utils "0.3.2"]]
  :profiles  {:dev {:dependencies [[expectations "1.4.56"]
                                   [org.clojure/tools.trace "0.7.6"]
                                   [ring-mock "0.1.5"]
                                   [ring/ring-devel "1.2.0"]
                                   [vmfest "0.3.0-beta.3"]]}}
  :plugins [[lein-ring "0.8.7"]
            [lein-heroku-deploy "0.1.0"]
            [lein-marginalia "0.7.1"] ; literate programming
            [lein-expectations "0.0.7"] ; run expect test
            [lein-autoexpect "0.2.5"] ; run expect tests when files change
            [configleaf "0.4.6"] ; access this file from the application
            ]

)
