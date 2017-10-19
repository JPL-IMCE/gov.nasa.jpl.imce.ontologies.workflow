#!/usr/bin/env groovy

pipeline {
	/* Agent directive is required. */
	agent { node { label 'imce-infr-dev-01.jpl.nasa.gov' } } 

	/* The following is NOT supported currently! (see below for a workaround) */
//	tools {
//		sbt 'default-sbt'	// Even specifying the suggested org.jvnet.hudson.plugins.SbtPluginBuilder$SbtInstallation does not work
//	}

	parameters {
		string(name: 'VERSION_ONTOLOGIES', defaultValue: '1.+', description: '')

		/* Unfortunately, SCM environment variables are currently not available in Jenkinsfile. */
		string(name: 'VERSION_PROFILES', defaultValue: '{env.BUILD_TAG}', description: 'The version of the profile resource to produce.')

		/* What to perform during build */
		string(name: 'BOOTSTRAP_BUILDS', defaultValue: 'TRUE', description: 'Whether or not to bootstrap subsequent builds and calculate dependencies. It makes no sense to skip this step.')
		string(name: 'VALIDATE_ONTOLOGIES', defaultValue: 'TRUE', description: 'Whether or not to validate ontologies. It has a side effect of loading ontologies into Fuseki.')
		string(name: 'BUILD_DIGESTS', defaultValue: 'TRUE', description: 'Whether or not to build digests. This may be forced if not done previously before other, dependent steps (i.e., profile generation).')
		string(name: 'BUILD_PROFILES', defaultValue: 'TRUE', description: 'Whether or not to generate profiles and build the profile resource.')
		string(name: 'OML_REPO', defaultValue: 'undefined', description: 'Repository where OML data to be converted is stored.')
	}

	environment {
		DISPLAY = ':9999'
		GEM_HOME = '/home/jenkins/.rvm/gems/jruby-1.7.19'
		PATH = '/home/jenkins/.rvm/gems/jruby-1.7.19/bin:/home/jenkins/.rvm/gems/jruby-1.7.19@global/bin:/home/jenkins/.rvm/rubies/jruby-1.7.19/bin:/usr/lib64/qt-3.3/bin:/usr/local/bin:/bin:/usr/bin:/usr/local/sbin:/usr/sbin:/home/jenkins/.rvm/bin'
	}

	stages {
		stage('Setup') {
			steps {
				echo "Setting up environment..."

				sh "env"
				sh "sbt clean cleanFiles"
				sh "sbt -Dproject.version=${params.VERSION_PROFILES} setupTools setupOntologies"

				// Decrypt files
				// TODO Add OpenSSL installation as prerequisite to readme?
				// TODO Add environment variable?
				withCredentials([string(credentialsId: 'ENCRYPTION_PASSWORD', variable: 'ENCRYPTION_PASSWORD')]) {
					sh "scripts/jenkins-decode.sh"
				}

				// setup Fuseki, ontologies, tools, environment
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
				//sh "sbt -Dproject.version=${params.VERSION_PROFILES} setupOMLConverter"
				//sh "sbt -Dproject.version=${params.VERSION_PROFILES} convertOntologies"
			}
		}

		stage('Bootstrap Builds') {
			when {
				expression { params.BOOTSTRAP_BUILDS == 'TRUE' }
			}
			steps {
				echo "Bootstrapping builds..."

				sh "cd workflow; source ./env.sh; /usr/bin/make bootstrap"
				sh "cd workflow; source ./env.sh; /usr/bin/make validation-dependencies"
				// run makefile command, same for others below
			}
		}

		stage('Validate Ontologies') {
			when {
				expression { params.VALIDATE_ONTOLOGIES == 'TRUE' }
			}
			steps {
				echo "Validating ontologies and loading into Fuseki..."

				sh "cd workflow; source ./env.sh; /usr/bin/make validate-xml"
				sh "cd workflow; source ./env.sh; /usr/bin/make validate-owl"
				sh "cd workflow; source ./env.sh; /usr/bin/make validate-groups"
				sh "cd workflow; source ./env.sh; /usr/bin/make validate-bundles"
				// run makefile command, same for others below
			}
		}

		stage('Build Digests') {
			when {
				expression { params.BUILD_DIGESTS == 'TRUE' }
			}
			steps {
				echo "Generating digests..."

				sh "cd workflow; source ./env.sh; /usr/bin/make digests"
			}
		}

		stage('Compile Profile Generator') {
			when {
				expression { params.BUILD_PROFILES == 'TRUE' }
			}
			steps {
				echo "Compiling workflow unit..."

				// Thanks to https://gist.github.com/muuki88/e2824008b653ac0fc5ba749fdf249616 for this one!
				sh "sbt -Dproject.version=${params.VERSION_PROFILES} compile test:compile"
				//archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
			}
		}

		stage('Build Profiles') {
			/*
			 * Note: this step requires digests to exist already!
			 */
			when {
				expression { params.BUILD_PROFILES == 'TRUE' }
			}
			post {
        		always {
            	// Kill vnc display
            	sh "vncserver -kill $DISPLAY"
        		}
        	}
			steps {
				echo "Building profiles..."
				// Kill old display and start VNC Server for headless MD
				sh "vncserver -kill $DISPLAY 2> /dev/null || true; vncserver $DISPLAY -SecurityTypes None"

				/*
				 * The following inline shell conditional ensures that the shell
				 * step always sees a zero exit code, giving the later junit step
				 * an opportunity to capture and process the test reports.
				 */
				//sh ' || true'
				sh "sbt -Dproject.version=${params.VERSION_PROFILES} setupProfileGenerator"
				sh "cd workflow; source ./env.sh; /usr/bin/make profiles"
			}

    	}

		stage('Build Profile Resource') {
			when {
				expression { params.BUILD_PROFILES == 'TRUE' }
			}
			steps {
				echo "Building profile resource..."
				sh "sbt -Dproject.version=${params.VERSION_PROFILES} packageProfiles"
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
				//sh "sbt -Dproject.version=${params.VERSION_PROFILES} publish"
				//sh 'scripts/jenkins-publish.sh'
			}
		}
	}

	post {
    	always {
        	junit 'target/**/*.xml'
    	}
	}
}
