# start transactor

export CODEQ_PATH=/Users/reborg/prj/3rdparties/codeq-playground
$CODEQ_PATH/deps/datomic-pro-0.9.5561.62/bin/transactor \
$CODEQ_PATH/deps/datomic-pro-0.9.5561.62/config/samples/dev-transactor-template.properties &

# analyze repos

cd /Users/reborg/prj/3rdparties/codeq
java -server -Xmx1g -jar $CODEQ_PATH/deps/codeq/target/codeq-0.1.0-SNAPSHOT-standalone.jar \
datomic:free://localhost:4334/codeq
cd -

cd /Users/reborg/prj/3rdparties/clojure
java -server -Xmx1g -jar $CODEQ_PATH/deps/codeq/target/codeq-0.1.0-SNAPSHOT-standalone.jar \
datomic:free://localhost:4334/clojure
cd -

# start console

$CODEQ_PATH/deps/datomic-pro-0.9.5561.62/bin/console -p 9256 dev datomic:dev://localhost:4334/ &
