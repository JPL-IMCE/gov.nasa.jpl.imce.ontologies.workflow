[ ![Download](https://api.bintray.com/packages/jpl-imce/gov.nasa.jpl.imce/gov.nasa.jpl.imce.ontologies.workflow/images/download.svg) ](https://bintray.com/jpl-imce/gov.nasa.jpl.imce/gov.nasa.jpl.imce.ontologies.workflow/_latestVersion)

# IMCE Ontology Workflow

## Downloadable Artifacts

Follow the link above to download the latest release of the profiles (as a NoMagic MagicDraw installable resource).

## Setup

```sbt
sbt setupTools
```

This will fetch the IMCE ontology tools from Maven 
(See the `libraryDependencies` in [build.sbt](build.sbt))
and extract them in a new folder: `target/tools`

```sbt
sbt clean
```

Deletes everything in the `target` folder.

