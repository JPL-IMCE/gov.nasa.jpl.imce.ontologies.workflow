
import java.io.File
import java.nio.file.Files

import sbt.Keys._
import sbt._

licenses in GlobalScope += "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0.html")

updateOptions := updateOptions.value.withCachedResolution(true)

shellPrompt in ThisBuild := { state => Project.extract(state).currentRef.project + "> " }

resolvers := {
  val previous = resolvers.value
  if (git.gitUncommittedChanges.value)
    Seq[Resolver](Resolver.mavenLocal) ++ previous
  else
    previous
}

lazy val setupTools = taskKey[File]("Location of the imce ontology tools directory extracted from dependencies")

lazy val setupOntologies = taskKey[File]("Location of the imce ontologies, either extracted from dependencies or symlinked")

lazy val artifactZipFile = taskKey[File]("Location of the zip artifact file")

lazy val imce_ontologies_workflow =
  Project("gov-nasa-jpl-imce-ontologies-workflow", file("."))
    .enablePlugins(AetherPlugin)
    .enablePlugins(GitVersioning)
    .enablePlugins(UniversalPlugin)
    .settings(
      projectID := {
        val previous = projectID.value
        previous.extra(
          "artifact.kind" -> "workflow")
      },

      // disable automatic dependency on the Scala library
      autoScalaLibrary := false,

      // disable using the Scala version in output paths and artifacts
      crossPaths := false,

      publishMavenStyle := true,

      // do not include all repositories in the POM
      pomAllRepositories := false,

      // make sure no repositories show up in the POM file
      pomIncludeRepository := { _ => false },

      // disable publishing the main jar produced by `package`
      publishArtifact in(Compile, packageBin) := false,

      // disable publishing the main API jar
      publishArtifact in(Compile, packageDoc) := false,

      // disable publishing the main sources jar
      publishArtifact in(Compile, packageSrc) := false,

      // disable publishing the jar produced by `test:package`
      publishArtifact in(Test, packageBin) := false,

      // disable publishing the test API jar
      publishArtifact in(Test, packageDoc) := false,

      // disable publishing the test sources jar
      publishArtifact in(Test, packageSrc) := false,

      sourceGenerators in Compile := Seq(),

      managedSources in Compile := Seq(),

      libraryDependencies +=
        "gov.nasa.jpl.imce"
        % "gov.nasa.jpl.imce.ontologies.tools"
        % "0.3.0"
        artifacts
        Artifact("gov.nasa.jpl.imce.ontologies.tools", "zip", "zip", "resource"),

      libraryDependencies +=
        "gov.nasa.jpl.imce"
          % "gov.nasa.jpl.imce.ontologies.public"
          % "1.0.1"
          artifacts
          Artifact("gov.nasa.jpl.imce.ontologies.public", "zip", "zip", "resource"),

      setupTools := {

        val slog = streams.value.log

        val toolsDir = baseDirectory.value / "target" / "tools"

        if (toolsDir.exists()) {
          slog.warn(s"IMCE ontology tools already extracted in $toolsDir")
        }  else {
          IO.createDirectory(toolsDir)

          val tfilter: DependencyFilter = new DependencyFilter {
            def apply(c: String, m: ModuleID, a: Artifact): Boolean =
              a.extension == "zip" &&
                m.organization.startsWith("gov.nasa.jpl.imce") &&
                m.name.startsWith("gov.nasa.jpl.imce.ontologies.tools")
          }

          update.value
            .matching(tfilter)
            .headOption
            .fold[Unit] {
            slog.error("Cannot find the IMCE ontology tools resource zip!")
          } { zip =>
            IO.unzip(zip, toolsDir)
            slog.warn(s"Extracted IMCE ontology tools from $zip")
            slog.warn(s"IMCE ontology tools in: $toolsDir")
          }

        }

        toolsDir
      },

      setupOntologies := {

        val slog = streams.value.log

        //
        val ontologiesLink = (baseDirectory.value / "local.ontologies").toPath

        val ontologiesDir = baseDirectory.value / "target" / "ontologies"

        if (ontologiesDir.exists()) {
          slog.warn(s"IMCE ontology tools already extracted or symlinked in $ontologiesDir")
        }  else {

          if (Files.isSymbolicLink(ontologiesLink)) {
            val localOntologies = Files.readSymbolicLink(ontologiesLink)
            Files.createSymbolicLink(ontologiesDir.toPath, localOntologies.resolve("ontologies"))
            slog.warn(s"Using 'local.ontologies' link to $localOntologies")
            slog.warn(s"IMCE ontologies in: $ontologiesDir")
          } else {
            IO.createDirectory(ontologiesDir)

            val tfilter: DependencyFilter = new DependencyFilter {
              def apply(c: String, m: ModuleID, a: Artifact): Boolean =
                a.extension == "zip" &&
                  m.organization.startsWith("gov.nasa.jpl.imce") &&
                  m.name.startsWith("gov.nasa.jpl.imce.ontologies.public")
            }

            update.value
              .matching(tfilter)
              .headOption
              .fold[Unit] {
              slog.error("Cannot find the IMCE ontology public resource zip!")
            } { zip =>
              IO.unzip(zip, ontologiesDir / "..")
              slog.warn(s"Extracted IMCE ontology public from $zip")
              slog.warn(s"IMCE ontologies in: $ontologiesDir")
            }

          }
        }

        ontologiesDir
      },

      artifactZipFile := {
        import com.typesafe.sbt.packager.universal._
        val artifactsDir = baseDirectory.value / "target" / "workflow" / "artifacts"
        val targetDir = baseDirectory.value / "target"
        val ontologiesDir= targetDir / "ontologies"

        val bPath = artifactsDir / "bundles"
        val bFiles = (PathFinder(bPath).*** --- bPath) pair Path.rebase(bPath, ontologiesDir)
        IO.copy(bFiles, overwrite=true, preserveLastModified=true)

        val dPath = artifactsDir / "digests"
        val dFiles = (PathFinder(dPath).*** --- dPath) pair Path.rebase(dPath, ontologiesDir)
        IO.copy(dFiles, overwrite=true, preserveLastModified=true)

        val ePath = artifactsDir / "entailments"
        val eFiles = (PathFinder(ePath).*** --- ePath) pair Path.rebase(ePath, ontologiesDir)
        IO.copy(eFiles, overwrite=true, preserveLastModified=true)

        val oPath = artifactsDir / "ontologies"
        val oFiles = (PathFinder(oPath).*** --- oPath) pair Path.rebase(oPath, ontologiesDir)
        IO.copy(oFiles, overwrite=true, preserveLastModified=true)

        val fileMappings = ontologiesDir.*** pair relativeTo(targetDir)
        val zipFile: File = baseDirectory.value / "target" / s"imce-omf_ontologies-digests-${version.value}-resource.zip"

        ZipHelper.zipNative(fileMappings, zipFile)

        zipFile
      },

      addArtifact(Artifact("imce-omf_ontologies-digests", "zip", "zip", Some("resource"), Seq(), None, Map()), artifactZipFile),

      makePom <<= makePom dependsOn artifactZipFile,

      sourceGenerators in Compile := Seq(),

      managedSources in Compile := Seq(),

      // disable publishing the main jar produced by `package`
      publishArtifact in(Compile, packageBin) := false,

      // disable publishing the main API jar
      publishArtifact in(Compile, packageDoc) := false,

      // disable publishing the main sources jar
      publishArtifact in(Compile, packageSrc) := false,

      // disable publishing the jar produced by `test:package`
      publishArtifact in(Test, packageBin) := false,

      // disable publishing the test API jar
      publishArtifact in(Test, packageDoc) := false,

      // disable publishing the test sources jar
      publishArtifact in(Test, packageSrc) := false
    )
