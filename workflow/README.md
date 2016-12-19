# JPL IMCE-jenkins workflow:

```shell
export CI_BUILD_KEY=${JOB_NAME} CI_BUILD_NUMBER=${BUILD_NUMBER} CI_REVISION_NUMBER=${GIT_COMMIT}
cd gov.nasa.jpl.imce.ontologies/ontologies
make -f Makefile.bootstrap Makefile
nice make ${PARALLEL_MAKE_OPTS} dependencies
nice make ${PARALLEL_MAKE_OPTS} validate-xml validate-owl
nice make ${PARALLEL_MAKE_OPTS} load-production artifacts
/home/jenkins/sbt-cae-publish.sh $JOB_NAME $WORKSPACE ${GIT_BRANCH#*/}
```

becomes:

```shell
cd workflow
. env.sh
make bootstrap
make dependencies
make validate
make loadprod
```

or:

```shell
cd workflow
. env.sh
make loadprod
```