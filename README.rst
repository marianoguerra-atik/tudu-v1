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

License
-------

Copyright © 2016 marianoguerra

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
