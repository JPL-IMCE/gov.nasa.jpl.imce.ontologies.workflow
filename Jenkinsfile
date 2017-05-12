pipeline {
	/* Agent directive is required. */
	agent any

	parameters {
		string(name: 'VERSION_ONTOLOGIES', defaultValue: '1.+', description: '')

		/* Unfortunately, SCM environment variables are currently not available in Jenkinsfile. */
		string(name: 'VERSION_PROFILES', defaultValue: '{env.BUILD_TAG}', description: 'The version of the profile resource to produce.')
	}

	stages {
		stage('Checkout') {
			/* This will clone the specific revision which triggered this Pipeline run. */
			checkout scm
		}
		stage('Build') {
			steps {
				echo "Building workflow unit..."

				sh 'sbt compile test:compile'
				archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
			}
		}
		stage('Validate-Ontologies') {
			steps {
				echo "Validating ontologies..."
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
