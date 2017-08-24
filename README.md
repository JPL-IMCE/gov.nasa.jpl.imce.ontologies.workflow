[ ![Download](https://api.bintray.com/packages/jpl-imce/gov.nasa.jpl.imce/gov.nasa.jpl.imce.ontologies.workflow/images/download.svg) ](https://bintray.com/jpl-imce/gov.nasa.jpl.imce/gov.nasa.jpl.imce.ontologies.workflow/_latestVersion)

# Ontology Processing and Profile Generation Workflow

The ontology processing and profile generation workflow is intended for validating and generating MagicDraw-compatible SysML profiles for core IMCE ontologies. It uses OWL ontologies (*TODO: OML AS INPUT*) as input. The following will walk through the environment setup, and give instructions on how to run the workflow (a) locally and (b) on a CI system such as Jenkins.

## Environment Setup and Prerequisites

The directory where this project is located must *NOT* contain space characters, parentheses, slash (forward or backwards), or other characters that could
be interpreted in Makefiles or in a shell as a something that is not part of a filename.

Running the workflow requires a number of tools to be configured: JRuby, SBT, Java 8 (JDK), Make. When using a CI server (such as Jenkins), a number of plugins are recommended to be installed. This is mentioned at the bottom of this section.

### JRuby
Ensure that JRuby 1.7.24 is installed. **Note:** it is important to install version 1.7.24, since there have been incompatible changes to the syntax and supported libraries in later versions.

JRuby 1.7.24 can be fetched from [here](http://jruby.org/files/downloads/1.7.24/index.html). Ensure that an environment variable `JRUBY` exists, or that the command `which jruby` directs to the correct installation. 

### Java 8 JDK
Install the latest version of the Java 8 JDK from [Oracle's website](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html). It is recommended **not** to use OpenJDK. 

Java 8 JDK may already be pre-installed on your system. To check if this is the case, run `java -version` in a terminal window.

### SBT 0.13.x
Install the latest version of SBT 0.13.x from the [Scala SBT website](http://www.scala-sbt.org/). **Note:** do NOT use SBT 1.x (currently a pre-release / beta) - there are known differences in syntax that will break part of the workflow.

### Make
Ensure that "make" is installed on the system. This is typically already the case for most Unix-based systems. Make is part of the [GNU Utils](https://www.gnu.org/software/make/). On MacOSX, this may require installing Xcode.

### Valid License Server Connection for MagicDraw 18.0 sp6
Ensure that a valid connection to a license server is available. This is only required if profiles are to be produced.

### Note For Running Under Continuous Integration
When running in a CI system (such as Jenkins), the agent may need to be configured appropriately. For Jenkins:

* Add an environment variable JRUBY under "Manage Jenkins" > "Manage Nodes" > (node name) > "Configure". The value should be *the absolute path to the jruby executable*: e.g., /usr/local/jruby/jruby-1.7.24/bin/jruby . **Note:** this is particularly important when using Jenkins 2.4.x, since Jenkins may NOT be aware of the system's environment settings.
* Install the SBT plugin (simply called "sbt plugin"), and configure an installation of SBT 0.13.x .

The above assumes a recommended standard installation of Jenkins 2.4.x, with the recommended plugins installed (e.g., GitHub plugin, JUnit plugin)

## Running

Before running the workflow, ensure that all encrypted data is decrypted locally. 

There are two variations depending on which ontologies are to be processed.

### Processing the official ontologies

The official ontologies are retrieved from an SBT dependency.

```sh
sbt setupOntologies 
```

### With other ontologies

`target/ontologies` must be either a folder with IMCE ontologies or a symlink to such a folder.

### Converting the ontologies

Regardless of their provenance, IMCE ontologies are represented as textual `*.oml` files 
that need to be converted to `*.owl` for the ontology workflow.

```sh
sbt convertOntologies
```

### Processing the ontologies

To run the pre-population and validation of ontologies, execute the following commands in a terminal, starting at the project root:

```sh
sbt setupTools
cd workflow
. env.sh
make bootstrap
make validation-dependencies
make validate-xml
make validate-owl
make validate-groups
make validate-bundles

cd ..
```

To generate profiles, execute the following additional steps:

```sh
sbt setupProfileGenerator
cd workflow
make digests
make profiles
cd ..
```

This will first produce the *digest* artifacts by querying the running Fuseki instance, and then pass the digests on to the profile generation workflow. Note that profiles are generated using MagicDraw, which will require an active license server connection (license server is specified in the `build.sbt` file). This will also require credentials to be decrypted.

To produce an installable resource for MagicDraw, use the following command:

```sh
sbt packageProfiles
```

This will produce a MD-compatible resource / plugin in the "target" folder. Running `sbt publish` or `sbt publishSigned` will then upload this archive to bintray.

### Running Under CI
For Jenkins, a pipeline script can be found in the root directory. This file is called `Jenkinsfile`. **Note:** this file is currently under development.

To decrypt encrypted files automatically, use openssl as described in the scripts/travis-decode.sh file. Note that this requires setting an environment variable within the CI system (e.g., in Jenkins: "Manage Jenkins" > "Manage Nodes" > (node name) > "Configure"; then set the environment variable ENCRYPTION_PASSWORD).
