
import java.io.File
import java.nio.file.Files
import com.typesafe.sbt.packager.chmod
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

lazy val setupFuseki = taskKey[File]("Location of the apache jena fuseki server extracted from dependencies")

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

      scalaVersion := "2.11.8",

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

      libraryDependencies ++= Seq(
        "gov.nasa.jpl.imce"
          % "gov.nasa.jpl.imce.ontologies.tools"
          % "0.3.0"
          artifacts
          Artifact("gov.nasa.jpl.imce.ontologies.tools", "zip", "zip", "resource"),

        "gov.nasa.jpl.imce"
          % "gov.nasa.jpl.imce.ontologies.public"
          % sys.env.getOrElse("PUBLIC_ONTOLOGIES_VERSION", "1.0.+")
          artifacts
          Artifact("gov.nasa.jpl.imce.ontologies.public", "zip", "zip", "resource"),

        "gov.nasa.jpl.imce" %% "imce.third_party.jena_libraries"
          % "3.4.+"
          artifacts
          Artifact("imce.third_party.jena_libraries", "zip", "zip", "resource"),

        "org.apache.jena" % "apache-jena-fuseki" % "2.4.1"
          % "compile"
          artifacts
          Artifact("apache-jena-fuseki", "tar.gz", "tar.gz"),

        "net.sf.docbook"
          % "docbook-xsl"
          % "1.79.1"
          % "compile"
          artifacts
          // The following should work but SBT fails to download the artifact from the URL below.
          //Artifact("doxbook-xsl", "zip", "zip", "resources")
        Artifact("docboox-xsl", url("https://repo1.maven.org/maven2/net/sf/docbook/docbook-xsl/1.79.1/docbook-xsl-1.79.1-resources.zip"))

      ),

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

          val dfilter: DependencyFilter = new DependencyFilter {
            def apply(c: String, m: ModuleID, a: Artifact): Boolean =
              a.extension == "zip" &&
                m.organization.startsWith("net.sf.docbook") &&
                m.name.startsWith("docbook-xsl")
          }

          update.value
            .matching(tfilter)
            .headOption
            .fold[Unit] {
            slog.error("Cannot find the IMCE ontology tools resource zip!")
          } { zip =>
            IO.unzip(zip, toolsDir)
            slog.warn(s"Extracted IMCE ontology tools from ${zip.name}")
            slog.warn(s"Ontology tools in: $toolsDir")
          }

          update.value
            .matching(dfilter)
            .headOption
            .fold[Unit] {
            slog.error("Cannot find the docbook-xsl zip!")
          } { zip =>
            IO.unzip(zip, toolsDir)
            slog.warn(s"Extracted docbook-xsl from ${zip.name}")
            slog.warn(s"Docbook in: $toolsDir")
          }
        }

        toolsDir
      },

      setupFuseki := {

        val slog = streams.value.log

        val fusekiDir = baseDirectory.value / "target" / "fuseki"

        if (fusekiDir.exists()) {
          slog.warn(s"Apache jena fuseki already extracted in $fusekiDir")
        }  else {
          IO.createDirectory(fusekiDir)

          val jfilter: DependencyFilter = new DependencyFilter {
            def apply(c: String, m: ModuleID, a: Artifact): Boolean =
              a.extension == "tar.gz" &&
                m.organization.startsWith("org.apache.jena") &&
                m.name.startsWith("apache-jena-fuseki")
          }
          update.value
            .matching(jfilter)
            .headOption
            .fold[Unit] {
            slog.error("Cannot find apache-jena-fuseki tar.gz!")
          } { tgz =>
            slog.warn(s"found: $tgz")
            val dir = target.value / "tarball"
            Process(Seq("tar", "--strip-components", "1", "-zxf", tgz.getAbsolutePath), Some(fusekiDir)).! match {
              case 0 => ()
              case n => sys.error("Error extracting " + tgz + ". Exit code: " + n)
            }
          }
        }

        fusekiDir
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

      makePom := { artifactZipFile; makePom.value },

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
