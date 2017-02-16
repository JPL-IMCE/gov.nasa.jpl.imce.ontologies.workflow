
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

lazy val setupProfileGenerator = taskKey[File]("Location of the profile generator directory extracted from dependencies")

lazy val packageProfiles = taskKey[File]("Location of the generated profiles")

lazy val imce_ontologies_workflow =
  Project("gov-nasa-jpl-imce-ontologies-workflow", file("."))
    .enablePlugins(AetherPlugin)
    .enablePlugins(GitVersioning)
    .enablePlugins(UniversalPlugin)
    .settings(
      resolvers += Resolver.bintrayRepo("tiwg", "org.omg.tiwg"),

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
          Artifact("docboox-xsl", url("https://repo1.maven.org/maven2/net/sf/docbook/docbook-xsl/1.79.1/docbook-xsl-1.79.1-resources.zip")),

        "gov.nasa.jpl.imce"
          %% "gov.nasa.jpl.imce.profileGenerator.application"
          % "2.5.3"
          artifacts
          Artifact("gov.nasa.jpl.imce.profileGenerator.application", "zip", "zip", "resource"),

        "gov.nasa.jpl.imce"
          %% "gov.nasa.jpl.imce.profileGenerator.batch"
          % "0.2.0"
          % "test" classifier "tests"
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

      // TODO This must be extracted over a MD install as a dynamic script
      setupProfileGenerator := {

        val slog = streams.value.log

        val profileGeneratorDir = baseDirectory.value / "target" / "profileGenerator"

        if (profileGeneratorDir.exists()) {
          slog.warn(s"Profile generator is already extracted in $profileGeneratorDir")
        }  else {
          IO.createDirectory(profileGeneratorDir)

          val tfilter: DependencyFilter = new DependencyFilter {
            def apply(c: String, m: ModuleID, a: Artifact): Boolean =
              a.extension == "zip" &&
                m.organization.startsWith("gov.nasa.jpl.imce") &&
                m.name.startsWith("gov.nasa.jpl.imce.profileGenerator")
          }

          update.value
            .matching(tfilter)
            .headOption
            .fold[Unit] {
            slog.error("Cannot find the profile generator resource zip!")
          } { zip =>
            IO.unzip(zip, profileGeneratorDir)
            slog.warn(s"Extracted profile generator from ${zip.name}")
            slog.warn(s"Profile generator in: $profileGeneratorDir")
          }
        }

        profileGeneratorDir
      },

      packageProfiles := {
        // Outputs
        val root = baseDirectory.value / "target"
        val profilesDir = root / "profiles"

        val d = {
          import java.util.{ Date, TimeZone }
          val formatter = new java.text.SimpleDateFormat("yyyy-MM-dd-HH:mm")
          formatter.setTimeZone(TimeZone.getTimeZone("UTC"))
          formatter.format(new Date)
        }

        val ver = version.value

        // Collect a list of all files in a particular subdirectory
        def collectFiles(dir : File) : Array[File] = {
          val these = dir.listFiles
          these ++ these.filter(_.isDirectory).flatMap(collectFiles)
        }

        // Filter the list of files in a subdirectory by the extension used by digests (here: json)
        //val profiles = collectFiles(profilesDir).filter(f => f.getAbsoluteFile.toString.endsWith(".mdzip"))
        val profiles = (profilesDir ** "*.mdzip").pair(relativeTo(root)).sortBy(_._2)

        // Create the various profiles, and package
        val resourceManager = root / "data" / "resourcemanager"
        IO.createDirectory(resourceManager)

        val resourceDescriptorFile = resourceManager / "MDR_Profile_gov_nasa_jpl_imce_ontologies_public_77563_descriptor.xml"
        val resourceDescriptorInfo =
          <resourceDescriptor critical="false" date={d}
                              description="IMCE Ontology Embedding as SysML Profiles for MagicDraw"
                              group="IMCE Resource"
                              homePage="https://github.com/JPL-IMCE/gov.nasa.jpl.imce.ontologies.public"
                              id="77563"
                              mdVersionMax="higher"
                              mdVersionMin="18.0"
                              name="IMCE Profiles"
                              product="IMCE Profiles"
                              restartMagicdraw="false"
                              type="Profile">
            <version human={ver} internal={ver} resource={ver + "0"}/>
            <provider email="sebastian.j.herzig@jpl.nasa.gov"
                      homePage="https://github.com/sjiherzig"
                      name="IMCE"/>
            <edition>Reader</edition>
            <edition>Community</edition>
            <edition>Standard</edition>
            <edition>Professional Java</edition>
            <edition>Professional C++</edition>
            <edition>Professional C#</edition>
            <edition>Professional ArcStyler</edition>
            <edition>Professional EFFS ArcStyler</edition>
            <edition>OptimalJ</edition>
            <edition>Professional</edition>
            <edition>Architect</edition>
            <edition>Enterprise</edition>
            <installation>
              {profiles.map { case (_, path) =>
              <file
              from={path}
              to={path}>
              </file>
            }}
            </installation>
          </resourceDescriptor>

        xml.XML.save(
          filename = resourceDescriptorFile.getAbsolutePath,
          node = resourceDescriptorInfo,
          enc = "UTF-8")

        val resourceManagerFiles = (resourceManager ** "*").pair(relativeTo(root)).sortBy(_._2)

        val zipFile: File = baseDirectory.value / "target" / s"imce-omf_ontologies-profiles-${version.value}-resource.zip"

        IO.zip(profiles ++ resourceManagerFiles, zipFile)

        zipFile
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

        //ZipHelper.zipNative(fileMappings, zipFile)
        IO.zip(fileMappings, zipFile)

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
