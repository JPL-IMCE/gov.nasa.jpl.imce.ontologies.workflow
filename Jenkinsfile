#!/usr/bin/env groovy

pipeline {
	/* Agent directive is required. */
	agent any

	/* The following is NOT supported currently! (see below for a workaround) */
//	tools {
//		sbt 'default-sbt'	// Even specifying the suggested org.jvnet.hudson.plugins.SbtPluginBuilder$SbtInstallation does not work
//	}

	parameters {
		string(name: 'VERSION_ONTOLOGIES', defaultValue: '1.+', description: '')

		/* Unfortunately, SCM environment variables are currently not available in Jenkinsfile. */
		string(name: 'VERSION_PROFILES', defaultValue: '{env.BUILD_TAG}', description: 'The version of the profile resource to produce.')

		/* What to perform during build */
		string(name: 'LOAD_FUSEKI', defaultValue: 'TRUE', description: 'Whether or not to load (validated) ontologies into a running Fuseki instance. This is equivalent to running loadprod.')
		string(name: 'BUILD_DIGESTS', defaultValue: 'TRUE', description: 'Whether or not to build digests. This may be forced if not done previously before other, dependent steps (i.e., profile generation).')
		string(name: 'BUILD_PROFILES', defaultValue: 'TRUE', description: 'Whether or not to generate profiles and build the profile resource.')
		string(name: 'OML_REPO', defaultValue: 'undefined', description: 'Repository where OML data to be converted is stored.')
	}

	stages {
		stage('Checkout') {
			steps {
				/* This will clone the specific revision which triggered this Pipeline run. */
				checkout scm
			}
		}

		stage('Setup') {
			steps {
				echo "Setting up environment..."

				sh "env"

				sh "${tool name: 'default-sbt', type: 'org.jvnet.hudson.plugins.SbtPluginBuilder$SbtInstallation'}/bin/sbt -Dproject.version=${params.VERSION_PROFILES} setupTools"

				// Decrypt files
				// TODO Add OpenSSL installation as prerequisite to readme?
				// TODO Add environment variable?
				withCredentials([string(credentialsId: 'ENCRYPTION_PASSWORD', variable: 'ENCRYPTION_PASSWORD')]) {
					sh "scripts/jenkins-decode.sh"
				}

				// setup Fuseki, ontologies, tools, environment
			}
		}

		stage('Compile') {
			steps {
				echo "Compiling workflow unit..."

				// Thanks to https://gist.github.com/muuki88/e2824008b653ac0fc5ba749fdf249616 for this one!
				sh "${tool name: 'default-sbt', type: 'org.jvnet.hudson.plugins.SbtPluginBuilder$SbtInstallation'}/bin/sbt -Dproject.version=${params.VERSION_PROFILES} compile test:compile"
				//archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
			}
		}

		stage('OML to OWL') {
			when {
				expression { params.OML_REPO != 'undefined' }
			}
			steps {
				echo "Converting OML to OWL..."

				// TODO Move cloning to checkout stage?
				sh "rm -rf target/ontologies"	// Need to make sure it's empty before cloning
				sh "mkdir -p target/ontologies; cd target/ontologies; git clone ${OML_REPO} ."

				// Invoke the convertOntologies SBT task
				//sh "${tool name: 'default-sbt', type: 'org.jvnet.hudson.plugins.SbtPluginBuilder$SbtInstallation'}/bin/sbt -Dproject.version=${params.VERSION_PROFILES} setupOMLConverter"
				//sh "${tool name: 'default-sbt', type: 'org.jvnet.hudson.plugins.SbtPluginBuilder$SbtInstallation'}/bin/sbt -Dproject.version=${params.VERSION_PROFILES} convertOntologies"
			}
		}

		stage('Pre-Process Ontologies') {
			when {
				expression { params.LOAD_FUSEKI == 'TRUE' }
			}
			steps {
				echo "Validating ontologies and loading into Fuseki..."

				sh "cd workflow; . env.sh; /usr/bin/make dependencies"
				sh "cd workflow; . env.sh; /usr/bin/make loadprod"
				// run makefile command, same for others below
			}
		}

		stage('Build Digests') {
			when {
				expression { params.BUILD_DIGESTS == 'TRUE' }
			}
			steps {
				echo "Generating digests..."

				sh "cd workflow; . env.sh; /usr/bin/make digests"
			}
		}

		stage('Build Profiles') {
			/*
			 * Note: this step requires digests to exist already!
			 */
			when {
				expression { params.BUILD_PROFILES == 'TRUE' }
			}
			steps {
				echo "Building profiles..."

				/*
				 * The following inline shell conditional ensures that the shell
				 * step always sees a zero exit code, giving the later junit step
				 * an opportunity to capture and process the test reports.
				 */
				//sh ' || true'

				sh "cd workflow; . env.sh; /usr/bin/make profiles"
				junit '**/target/*.xml'
			}
		}

		stage('Build Profile Resource') {
			steps {
				echo "Building profile resource..."

				sh "${tool name: 'default-sbt', type: 'org.jvnet.hudson.plugins.SbtPluginBuilder$SbtInstallation'}/bin/sbt -Dproject.version=${params.VERSION_PROFILES} packageProfiles"
			}
		}

		stage('Deploy') {
			/*
			 * In addition to the below guard, jenkins-deploy.sh will check whether
			 * or not a tag is associated with the current commit. *Note*:
			 * unfortunately, Jenkins currently does not make any parameters for git
			 * available within pipeline scripts - hence the externalization of the
			 * logic.
			 */
			when {
				expression {
					currentBuild.result == null || currentBuild.result == 'SUCCESS' 
				}
			}
			steps {
				sh 'scripts/jenkins-deploy.sh'
				//sh "${tool name: 'default-sbt', type: 'org.jvnet.hudson.plugins.SbtPluginBuilder$SbtInstallation'}/bin/sbt -Dproject.version=${params.VERSION_PROFILES} publish"
				//sh 'scripts/jenkins-publish.sh'
			}
		}
	}
}
