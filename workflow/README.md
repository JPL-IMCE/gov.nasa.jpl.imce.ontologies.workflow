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
sbt setupTools setupOntologies setupFuseki
cd workflow
. env.sh
make bootstrap
make dependencies
make validate
make loadprod
make profiles
sbt packageProfiles
```

or:

```shell
sbt setupTools setupOntologies setupFuseki
cd workflow
. env.sh
make profiles
cd ..
sbt packageProfiles
```

# TODO:

[Makefile.erb](Makefile.erb) needs to be updated.
Currently, the paths to the ontologies are relative paths.
This worked when the current directory was the ontologies/ folder.

1) The ontologies should be read from an absolute path: `$(ONTOLOGIES)` (see [env.sh](env.sh))

2) The ontologies folder, `$(ONTOLOGIES)`, should be treated as read-only. Derived artifacts should go
in the `$(WORKFLOW)` directory somewhere (see [env.sh](env.sh))
