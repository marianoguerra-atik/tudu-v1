tudu
====

An example project that builds an end to end todo list using
clojure/immutant on the backend and clojurescript/om.next on the frontend

Setup
-----

First you need `leiningen <http://leiningen.org/>`_ installed and running,
follow the link for installation instructions.

We need a local version of om.next installed since it's still in alpha, we
will use alpha30 to start with, we may upgrade it later in the development.

To make it easy just run once, it will install the required version of om.next locally for you::

    make setup

Get up and running
------------------

To get the app up and running in dev mode, you'll need to start the front end
and the backend.

To start the front end, run::

    lein figwheel

To start the backend, run::

    lein run

The app will now be accessible at http://localhost:8080/index.html

Test the backend from the command line
--------------------------------------

for this you will need to install `transito <https://pypi.python.org/pypi/transito>`_::

    sudo pip install transito

and then run::

    echo '[:tudu/items]' | transito http post http://localhost:8080/query e2t -
    echo '[(tudu.item/create {:value {:title "My Task"}})]' | transito http post http://localhost:8080/query e2t -
    echo '[(tudu.item/close {:id 2})]' | transito http post http://localhost:8080/query e2t -

Interactive Backend Development
-------------------------------

start a repl::

    lein repl

in the repl::

    (require 'tudu.core :reload-all)
    (def sys (tudu.core/start "localhost" 8080 "/"))
    (tudu.core/stop sys)

if you want to be really sure a namespace was recompiled run::

    ; change the path to the path of your module
    (load-file "src/tudu/web.clj")
    (load-file "src/tudu/api.clj")

every time you want to reload changes evaluate again the first line and stop
start the system if the changes involve the system.

To use the repl from vim I use `vim-slime <https://github.com/jpalardy/vim-slime>`_

Compiling the Frontend
----------------------

To compile the frontend and have live reload capabilities run::

    lein figwheel

You should also be running the backend on another shell, then visit:

http://localhost:8080/index.html

Note that http://localhost:8080/ won't work, it has to be http://localhost:8080/index.html

You can now make changes on the frontend and it will live reload.

Build for Deployment
--------------------

To build::

    lein uberjar

To run the build (stop the backend if you are running it for development to avoid port collisions)::

    cd target
    java -jar tudu-0.1.0-SNAPSHOT-standalone.jar

License
-------

Copyright © 2016 marianoguerra

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
