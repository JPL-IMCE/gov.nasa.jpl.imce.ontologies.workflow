import sbt.Keys._
import sbt._

import scala.io.Source
import spray.json.{DefaultJsonProtocol, _}
import complete.DefaultParsers._

import scala.language.postfixOps
import gov.nasa.jpl.imce.sbt._
import gov.nasa.jpl.imce.sbt.ProjectHelper._
import java.io.File
import java.nio.file.Files

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

lazy val mdInstallDirectory = SettingKey[File]("md-install-directory", "MagicDraw Installation Directory")

mdInstallDirectory in Global :=
  baseDirectory.value / "target" / "md.package"

lazy val testsInputsDir = SettingKey[File]("tests-inputs-dir", "Directory to scan for input *.json tests")

lazy val testsResultDir = SettingKey[File]("tests-result-dir", "Directory for the tests results to archive as the test resource artifact")

lazy val testsResultsSetupTask = taskKey[Unit]("Create the tests results directory")

lazy val mdJVMFlags = SettingKey[Seq[String]]("md-jvm-flags", "Extra JVM flags for running MD (e.g., debugging)")

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
          % "0.7.0"
          artifacts
          Artifact("gov.nasa.jpl.imce.ontologies.tools", "zip", "zip", "resource"),

        "gov.nasa.jpl.imce"
          % "gov.nasa.jpl.imce.ontologies.public"
          % sys.env.getOrElse("PUBLIC_ONTOLOGIES_VERSION", "1.2+")
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
          % "2.5.4"
          artifacts
          Artifact("gov.nasa.jpl.imce.profileGenerator.application", "zip", "zip", "resource"),

        "gov.nasa.jpl.imce"
          %% "gov.nasa.jpl.imce.profileGenerator.batch"
          % "0.2.2"
          % "test" classifier "tests"
      ),

      unmanagedClasspath in Compile ++= (unmanagedJars in Compile).value,

      // Extract jars
      extractArchives := {
        val base = baseDirectory.value
        val up = update.value
        val s = streams.value
        val showDownloadProgress = true // does not compile: logLevel.value <= Level.Debug

        val mdInstallDir = (mdInstallDirectory in ThisBuild).value
        if (!mdInstallDir.exists) {

          IO.createDirectory(mdInstallDir)

          MagicDrawDownloader.fetchMagicDraw(
            s.log, showDownloadProgress,
            up,
            credentials.value,
            mdInstallDir, base / "target" / "no_install.zip"
          )

          MagicDrawDownloader.fetchSysMLPlugin(
            s.log, showDownloadProgress,
            up,
            credentials.value,
            mdInstallDir, base / "target" / "sysml_plugin.zip"
          )

          val pfilter: DependencyFilter = new DependencyFilter {
            def apply(c: String, m: ModuleID, a: Artifact): Boolean =
              (a.`type` == "zip" || a.`type` == "resource") &&
                a.extension == "zip" &&
                (m.organization.startsWith("gov.nasa.jpl") || m.organization.startsWith("com.nomagic")) &&
                (m.name.startsWith("cae_md") ||
                  m.name.startsWith("gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker") ||
                  m.name.startsWith("imce.dynamic_scripts.magicdraw.plugin") ||
                  m.name.startsWith("com.nomagic.magicdraw.package") ||
                  m.name.startsWith("gov.nasa.jpl.imce.metrology.isoiec80000.magicdraw.library"))
          }
          val ps: Seq[File] = up.matching(pfilter)
          ps.foreach { zip =>
            // Use unzipURL to download & extract
            val files = IO.unzip(zip, mdInstallDir)
            s.log.info(
              s"=> created md.install.dir=$mdInstallDir with ${files.size} " +
                s"files extracted from zip: ${zip.getName}")
          }

          val mdDynamicScriptsDir = mdInstallDir / "dynamicScripts"
          IO.createDirectory(mdDynamicScriptsDir)

          val zfilter: DependencyFilter = new DependencyFilter {
            def apply(c: String, m: ModuleID, a: Artifact): Boolean =
              (a.`type` == "zip" || a.`type` == "resource" || true) &&
                a.extension == "zip" &&
                (m.organization.startsWith("gov.nasa.jpl") || m.organization.startsWith("org.omg.tiwg")) &&
                !(m.name.startsWith("cae_md") ||
                  m.name.startsWith("gov.nasa.jpl.magicdraw.projectUsageIntegrityChecker") ||
                  m.name.startsWith("imce.dynamic_scripts.magicdraw.plugin") ||
                  m.name.startsWith("imce.third_party") ||
                  m.name.startsWith("gov.nasa.jpl.imce.metrology.isoiec80000.magicdraw.library"))
          }
          val zs: Seq[File] = up.matching(zfilter)
          zs.foreach { zip =>
            val files = IO.unzip(zip, mdDynamicScriptsDir)
            s.log.info(
              s"=> extracted ${files.size} DynamicScripts files from zip: ${zip.getName}")
          }

          val imceSetup = mdInstallDir / "bin" / "magicdraw.imce.setup.sh"
          if (imceSetup.exists()) {
            val setup = sbt.Process(command = "/bin/bash", arguments = Seq[String](imceSetup.getAbsolutePath)).!
            require(0 == setup, s"IMCE MD Setup error! ($setup)")
            s.log.info(s"*** Executed bin/magicdraw.imce.setup.sh script")
          } else {
            s.log.info(s"*** No bin/magicdraw.imce.setup.sh script found!")
          }
        } else
          s.log.info(
            s"=> use existing md.install.dir=$mdInstallDir")
      },

      unmanagedJars in Compile := {
        val prev = (unmanagedJars in Compile).value
        val base = baseDirectory.value
        val s = streams.value
        val _ = extractArchives.value

        val mdInstallDir = base / "target" / "md.package"

        //val depJars = ((base / "lib") ** "*").filter{f => f.isDirectory && ((f) * "*.jar").get.nonEmpty}.get.map(Attributed.blank)
        val depJars = ((base / "lib") ** "*.jar").get.map(Attributed.blank)

        //val mdLibJars = (mdInstallDir ** "*").filter{f => f.isDirectory && ((f) * "*.jar").get.nonEmpty}.get.map(Attributed.blank)
        val mdLibJars = ((mdInstallDir / "lib") ** "*.jar").get.map(Attributed.blank)
        val mdPluginLibJars = ((mdInstallDir / "plugins") ** "*.jar").get.map(Attributed.blank)
        val mdDynScLibJars = ((mdInstallDir / "dynamicScripts") ** "*.jar").get.map(Attributed.blank)

        val allJars = mdLibJars ++ mdPluginLibJars ++ mdDynScLibJars ++ depJars ++ prev

        s.log.info(s"=> Adding ${allJars.size} unmanaged jars")

        allJars
      },

      unmanagedJars in Test := (unmanagedJars in Compile).value,

      unmanagedClasspath in Test := (unmanagedJars in Test).value,

      compile in Compile := (compile in Compile).dependsOn(extractArchives).value,

      compile in Test := {
        val _ = extractArchives.value
        (compile in Test).value
      },

      mdJVMFlags := Seq("-Xmx8G"), //
      // for debugging: Seq("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005"),

      testsInputsDir := baseDirectory.value / "resources" / "tests",

      testsResultDir := baseDirectory.value / "target" / "md.testResults",

      scalaSource in Test := baseDirectory.value / "src" / "test" / "scala",

      testsResultsSetupTask := {

        val s = streams.value

        // Wipe any existing tests results directory and create a fresh one
        val resultsDir = testsResultDir.value
        if (resultsDir.exists) {
          s.log.warn(s"# Deleting existing results directory: $resultsDir")
          IO.delete(resultsDir)
        }
        s.log.warn(s"# Creating results directory: $resultsDir")
        IO.createDirectory(resultsDir)
        require(
          resultsDir.exists && resultsDir.canWrite,
          s"The created results directory should exist and be writeable: $resultsDir")

      },

      test in Test := (test in Test).dependsOn(testsResultsSetupTask).value,

      testOptions in testOnly += Tests.Argument("-digest", "../../project-bundle.json"),

      parallelExecution in Test := false,

      fork in Test := true,

      testGrouping in Test := {
        val original = (testGrouping in Test).value
        val tests_dir = testsInputsDir.value
        val md_install_dir = mdInstallDirectory.value
        val tests_results_dir = testsResultDir.value
        val pas = (packageBin in Universal).value
        val jvmFlags = mdJVMFlags.value
        val jHome = javaHome.value
        val cInput = connectInput.value
        val jOpts = javaOptions.value
        val env = envVars.value
        val s = streams.value

        val testOutputFile = tests_results_dir.toPath.resolve("output.log").toFile

        val xlogger = new xsbti.Logger {

          def debug(msg: xsbti.F0[String]): Unit = append(msg())
          def error(msg: xsbti.F0[String]): Unit = append(msg())
          def info(msg: xsbti.F0[String]): Unit = append(msg())
          def warn(msg: xsbti.F0[String]): Unit = append(msg())
          def trace(exception: xsbti.F0[Throwable]): Unit = {
            val t = exception()
            append(t.getMessage)
            append(t.getStackTraceString)
          }

          def append(msg: String): Unit = {
            val pw = new java.io.PrintWriter(new java.io.FileWriter(testOutputFile, true))
            pw.println(msg)
            pw.flush()
            pw.close()
          }

        }

        val logger = new FullLogger(xlogger)

        val ds_dir = md_install_dir / "dynamicScripts"

        val files = IO.unzip(pas, ds_dir)
        s.log.warn(
          s"=> Installed ${files.size} " +
            s"files extracted from zip: $pas")

        val mdProperties = new java.util.Properties()
        IO.load(mdProperties, md_install_dir / "bin" / "magicdraw.properties")

        s.log.warn(
          s"=> Read properties file from ${md_install_dir / "bin" / "magicdraw.properties"}")

        val mdBoot =
          mdProperties
            .getProperty("BOOT_CLASSPATH")
            .split(":")
            .map(md_install_dir / _)
            .toSeq
        s.log.warn(s"# MD BOOT CLASSPATH: ${mdBoot.mkString("\n", "\n", "\n")}")

        val mdClasspath =
          mdProperties
            .getProperty("CLASSPATH")
            .split(":")
            .map(md_install_dir / _)
            .toSeq
        s.log.warn(s"# MD CLASSPATH: ${mdClasspath.mkString("\n", "\n", "\n")}")

        val imceSetupProperties = IO.readLines(md_install_dir / "bin" / "magicdraw.imce.setup.sh")

        val imceBoot =
          imceSetupProperties
            .find(_.startsWith("IMCE_BOOT_CLASSPATH_PREFIX"))
            .getOrElse("")
            .stripPrefix("IMCE_BOOT_CLASSPATH_PREFIX=\"")
            .stripSuffix("\"")
            .split("\\\\+:")
            .map(md_install_dir / _)
            .toSeq
        s.log.warn(s"# IMCE BOOT: ${imceBoot.mkString("\n", "\n", "\n")}")

        val imcePrefix =
          imceSetupProperties
            .find(_.startsWith("IMCE_CLASSPATH_PREFIX"))
            .getOrElse("")
            .stripPrefix("IMCE_CLASSPATH_PREFIX=\"")
            .stripSuffix("\"")
            .split("\\\\+:")
            .map(md_install_dir / _)
            .toSeq
        s.log.warn(s"# IMCE CLASSPATH Prefix: ${imcePrefix.mkString("\n", "\n", "\n")}")

        original.map { group =>

          s.log.warn(s"# ${env.size} env properties")
          env.keySet.toList.sorted.foreach { k =>
            s.log.warn(s"env[$k]=${env.get(k)}")
          }
          s.log.warn(s"# ------")

          s.log.warn(s"# ${jOpts.size} java options")
          s.log.warn(jOpts.mkString("\n"))
          s.log.warn(s"# ------")

          s.log.warn(s"# ${jvmFlags.size} jvm flags")
          s.log.warn(jvmFlags.mkString("\n"))
          s.log.warn(s"# ------")

          val testPropertiesFile =
            md_install_dir.toPath.resolve("data/imce.properties").toFile

          val out = new java.io.PrintWriter(new java.io.FileWriter(testPropertiesFile))
          val in = Source.fromFile(md_install_dir.toPath.resolve("data/test.properties").toFile)
          for (line <- in.getLines) {
            if (line.startsWith("log4j.appender.R.File="))
              out.println(s"log4j.appender.R.File=$tests_results_dir/tests.log")
            else if (line.startsWith("log4j.appender.SO=")) {
              out.println(s"log4j.appender.SO=org.apache.log4j.RollingFileAppender")
              out.println(s"log4j.appender.SO.File=$tests_results_dir/console.log")
            }
            else
              out.println(line)
          }
          out.close()

          val forkOptions = ForkOptions(
            bootJars = imceBoot ++ mdBoot,
            javaHome = jHome,
            connectInput = cInput,
            outputStrategy = Some(LoggedOutput(logger)),
            runJVMOptions = jOpts ++ Seq(
              "-DLOCALCONFIG=false",
              "-DWINCONFIG=false",
              "-DHOME=" + md_install_dir.getAbsolutePath,
              s"-Ddebug.properties=$testPropertiesFile",
              "-Ddebug.properties.file=imce.properties",
              "-DFL_FORCE_USAGE=true",
              "-DFL_SERVER_ADDRESS=cae-lic04.jpl.nasa.gov",
              "-DFL_SERVER_PORT=1101",
              "-DFL_EDITION=enterprise",
              "-classpath", (imcePrefix ++ mdClasspath).mkString(File.pathSeparator)
            ) ++ jvmFlags,
            workingDirectory = Some(md_install_dir),
            envVars = env +
              ("debug.dir" -> md_install_dir.getAbsolutePath) +
              ("FL_FORCE_USAGE" -> "true") +
              ("FL_SERVER_ADDRESS" -> "cae-lic04.jpl.nasa.gov") +
              ("FL_SERVER_PORT" -> "1101") +
              ("FL_EDITION" -> "enterprise") +
              ("DYNAMIC_SCRIPTS_TESTS_DIR" -> tests_dir.getAbsolutePath) +
              ("DYNAMIC_SCRIPTS_RESULTS_DIR" -> tests_results_dir.getAbsolutePath)
          )

          s.log.warn(s"# working directory: $md_install_dir")

          group.copy(runPolicy = Tests.SubProcess(forkOptions))
        }
      },

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

        val imceDependencies = (root / "md.package" / "profiles" / "IMCE" ** "*.mdzip").pair(relativeTo(root / "md.package")).sortBy(_._2)

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
        val profiles = (profilesDir ** "*.mdzip").pair(relativeTo(root)).sortBy(_._2) ++ imceDependencies

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

      addArtifact(Artifact("gov.nasa.jpl.imce.ontologies.workflow", "zip", "zip", Some("profiles"), Seq(), None, Map()), packageProfiles),

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

      addArtifact(Artifact("gov.nasa.jpl.imce.ontologies.workflow", "zip", "zip", Some("digests"), Seq(), None, Map()), artifactZipFile),

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
