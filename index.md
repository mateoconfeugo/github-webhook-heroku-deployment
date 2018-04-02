

Simple app that lets you deploy your app to Heroku when you push to GitHub. It will fetch code on change, and force push the master branch to Heroku.

Note, this will trigger on all pushes to git. If you haven't changed master it will still run and push to Heroku, however this won't trigger an app update because the master branch hasn't changed

## Usage

### Github

First, set a key on this app to secure deploys

    $ heroku config:add ACCESS_KEY=supersecret

Create/Obtain a SSH key to use for fetching/deploying. Add this to your Github app as a deploy key, and to your Heroku account. Even better, create a Heroku account for deployment, and add it as a collaborator.



Distributed under the Eclipse Public License, the same as Clojure.
