pipeline {
	/* Agent directive is required. */
	agent any

	tools {
		sbt 'default-sbt'
	}

	parameters {
		string(name: 'VERSION_ONTOLOGIES', defaultValue: '1.+', description: '')

		/* Unfortunately, SCM environment variables are currently not available in Jenkinsfile. */
		string(name: 'VERSION_PROFILES', defaultValue: '{env.BUILD_TAG}', description: 'The version of the profile resource to produce.')

		/* What to perform during build */
		string(name: 'VALIDATE', defaultValue: 'TRUE', description: 'Whether to run validation. This may be forced if not done previously before other, dependent steps (such as digest generation of profile generation).')
		string(name: 'BUILD_DIGESTS', defaultValue: 'TRUE', description: 'Whether or not to build digests. This may be forced if not done previously before other, dependent steps (i.e., profile generation).')
		string(name: 'BUILD_PROFILES', defaultValue: 'TRUE', description: 'Whether or not to generate profiles and build the profile resource.')
	}

	stages {
		stage('Checkout') {
			steps {
				/* This will clone the specific revision which triggered this Pipeline run. */
				checkout scm
			}
		}
		stage('Compile') {
			steps {
				echo "Compiling workflow unit..."

				sh 'sbt compile test:compile'
				archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
			}
		}
		stage('Setup') {
			steps {
				echo "Setting up environment..."

				// setup Fuseki, ontologies, tools, environment
			}
		}
		stage('Validate-Ontologies') {
			steps {
				echo "Validating ontologies..."

				// run makefile command, same for others below
			}
		}
		stage('Build-Digests') {
			steps {
				echo "Generating digests..."
			}
		}
		stage('Build-Profiles') {
			steps {
				echo "Building profiles..."

				/*
				 * The following inline shell conditional ensures that the shell
				 * step always sees a zero exit code, giving the later junit step
				 * an opportunity to capture and process the test reports.
				 */
				//sh ' || true'
				junit '**/target/*.xml'
			}
		}
		stage('Build-Profile-Resource') {
			steps {
				echo "Building profile resource..."

				sh 'sbt packageProfiles'
			}
		}
		stage('Deploy') {
			when {
				expression {
					currentBuild.result == null || currentBuild.result == 'SUCCESS' 
				}
			}
			steps {
				echo "Deploying artifacts..."

				//sh 'scripts/jenkins-publish.sh'
			}
		}
	}
}

