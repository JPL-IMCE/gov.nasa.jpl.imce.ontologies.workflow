# IMCE Ontology Workflow

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

