pipeline {
    agent { label params.AGENT_LABEL }

    triggers {
        // Nightly at ~2am (H = Jenkins-jittered minute, prevents thundering-herd)
        cron('H 2 * * *')
    }

    parameters {
        choice(
            name: 'SUITE',
            choices: [
                'regression-e2e',
                'smoke',
                'sanity',
                'regression-full',
                'e2e-happy-path',
                'e2e-happy-path-ios',
                'med-android-en',
                'content-verification'
            ],
            description: 'TestNG suite file under src/test/resources/suites/<SUITE>.xml'
        )
        choice(
            name: 'PLATFORM',
            choices: ['android', 'ios'],
            description: 'Mobile platform — selects driver capabilities'
        )
        string(
            name: 'BROWSERSTACK_APP_URL',
            defaultValue: 'bs://med-android-latest',
            description: 'BrowserStack app URL. Use a custom_id for a stable URL (e.g. bs://med-android-latest, bs://med-ios-latest). Re-upload the APK/IPA with the same custom_id to refresh — URL stays valid forever.'
        )
        string(
            name: 'ACTIVATION_CODE',
            defaultValue: '77AAAAAAAAAAAAAX',
            description: 'DiGA activation code (reusable test code — overrides Mock HI API call, no VPN needed)'
        )
        string(
            name: 'AGENT_LABEL',
            defaultValue: 'linux && jdk17',
            description: 'Jenkins agent label (the runner needs JDK 17 + Maven, NOT a real device since tests run on BrowserStack)'
        )
        booleanParam(
            name: 'PUBLISH_ALLURE',
            defaultValue: true,
            description: 'Publish Allure report (requires Allure Jenkins plugin)'
        )
    }

    options {
        timeout(time: 90, unit: 'MINUTES')
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '10'))
    }

    environment {
        JAVA_HOME  = "${tool name: 'JDK-17',    type: 'jdk'}"
        MAVEN_HOME = "${tool name: 'Maven-3.9', type: 'maven'}"
        PATH       = "${env.MAVEN_HOME}/bin:${env.JAVA_HOME}/bin:${env.PATH}"
        MAVEN_OPTS = '-Xmx2g -Dfile.encoding=UTF-8'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                sh 'git log --oneline -1'
            }
        }

        stage('Compile') {
            steps {
                sh 'mvn -B -e -ntp clean test-compile'
            }
        }

        stage('Test') {
            steps {
                withCredentials([
                    string(credentialsId: 'BROWSERSTACK_USERNAME',   variable: 'BROWSERSTACK_USERNAME'),
                    string(credentialsId: 'BROWSERSTACK_ACCESS_KEY', variable: 'BROWSERSTACK_ACCESS_KEY'),
                    string(credentialsId: 'IMAP_PASSWORD',           variable: 'IMAP_PASSWORD')
                ]) {
                    script {
                        env.BROWSERSTACK_APP_URL = params.BROWSERSTACK_APP_URL

                        sh """
                            mvn -B -e -ntp test \
                              -Dbrowserstack.enabled=true \
                              -Dapp.type=med \
                              -Dplatform=${params.PLATFORM} \
                              -DsuiteFile=src/test/resources/suites/${params.SUITE}.xml \
                              -Dactivation.code=${params.ACTIVATION_CODE} \
                              -Dbrowserstack.build.name='Jenkins-${env.BUILD_NUMBER}-${params.SUITE}-${params.PLATFORM}'
                        """
                    }
                }
            }
        }
    }

    post {
        always {
            // TestNG / Surefire results
            junit allowEmptyResults: true, testResults: 'target/surefire-reports/junitreports/*.xml,target/surefire-reports/*.xml'

            // Raw artifacts (screenshots, allure json, surefire xml) for debugging
            archiveArtifacts allowEmptyArchive: true, fingerprint: false,
                artifacts: 'target/allure-results/**, target/surefire-reports/**'

            // Allure report (requires Allure Jenkins plugin)
            script {
                if (params.PUBLISH_ALLURE) {
                    allure includeProperties: false, jdk: '', results: [[path: 'target/allure-results']]
                }
            }
        }
        failure {
            echo "Pipeline FAILED. BrowserStack session: build name 'Jenkins-${env.BUILD_NUMBER}-${params.SUITE}-${params.PLATFORM}'."
            // TODO add slack/email notify once destination is decided
        }
        success {
            echo 'Pipeline PASSED.'
        }
    }
}
