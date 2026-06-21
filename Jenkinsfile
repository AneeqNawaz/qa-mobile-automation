pipeline {
    // Single-node setup (Mac Mini built-in node has no labels) — run on any available node.
    agent any

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
            defaultValue: 'med-android-latest',
            description: 'BrowserStack app reference — accepts BOTH forms: (1) a bare custom_id for the always-latest build, e.g. med-android-latest (NO bs:// prefix); or (2) a hashed app_url for a specific build, e.g. bs://66552da01496cf88c003dc93a681daab065005a5. Default is the custom_id; re-upload with the same custom_id to refresh.'
        )
        string(
            name: 'ACTIVATION_CODE',
            defaultValue: '77AAAAAAAAAAAAAX',
            description: 'DiGA activation code (reusable test code — overrides Mock HI API call, no VPN needed)'
        )
    }

    options {
        timeout(time: 90, unit: 'MINUTES')
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '30', artifactNumToKeepStr: '10'))
    }

    environment {
        JAVA_HOME  = "${tool name: 'JDK-21',    type: 'jdk'}"
        MAVEN_HOME = "${tool name: 'Maven-3.9', type: 'maven'}"
        PATH       = "${env.MAVEN_HOME}/bin:${env.JAVA_HOME}/bin:${env.PATH}"
        MAVEN_OPTS = '-Xmx2g -Dfile.encoding=UTF-8'
        // Slack target channel — change this to your channel name (keep the leading '#').
        SLACK_CHANNEL = '#qa-automation-nightly'
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
                // BrowserStack-typed credential — the browserstack{} wrapper injects
                // BROWSERSTACK_USERNAME / BROWSERSTACK_ACCESS_KEY. Using aneeqnawaz's
                // account so it matches the account the MED app is uploaded under.
                browserstack(credentialsId: 'c380e328-98ad-4aa9-9360-b930ee6db5bf') {
                    withCredentials([
                        string(credentialsId: 'IMAP_PASSWORD', variable: 'IMAP_PASSWORD')
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
    }

    post {
        always {
            // TestNG / Surefire results
            junit allowEmptyResults: true, testResults: 'target/surefire-reports/junitreports/*.xml,target/surefire-reports/*.xml'

            // Seed Allure CI metadata (Environment widget + build-link) before generating
            // the report. Written into allure-results so the Allure plugin picks them up.
            script {
                if (fileExists('target/allure-results')) {
                    writeFile file: 'target/allure-results/environment.properties', text: """
Platform=${params.PLATFORM}
Suite=${params.SUITE}
App=${params.BROWSERSTACK_APP_URL}
ActivationCode=${params.ACTIVATION_CODE}
BrowserStack.Build=Jenkins-${env.BUILD_NUMBER}-${params.SUITE}-${params.PLATFORM}
""".stripIndent().trim()
                    writeFile file: 'target/allure-results/executor.json', text: """
{"name":"Jenkins","type":"jenkins","buildName":"#${env.BUILD_NUMBER}","buildUrl":"${env.BUILD_URL}","reportUrl":"${env.BUILD_URL}allure"}
""".stripIndent().trim()
                }
            }

            // Generate + publish the Allure report (link on the build page + cross-build
            // trend). 'commandline' matches the Allure Commandline tool name in
            // Manage Jenkins -> Tools.
            allure commandline: 'Allure', includeProperties: false,
                results: [[path: 'target/allure-results']]

            // Raw artifacts (screenshots, allure json, surefire xml) for debugging.
            archiveArtifacts allowEmptyArchive: true, fingerprint: false,
                artifacts: 'target/allure-results/**, target/surefire-reports/**'

            // Slack summary: compact, color-coded, links straight to the Allure report.
            // We post here (not in success/failure) so there is exactly one message per run.
            script {
                def t       = currentBuild.rawBuild.getAction(hudson.tasks.junit.TestResultAction.class)
                def total   = t ? t.totalCount : 0
                def failed  = t ? t.failCount  : 0
                def skipped = t ? t.skipCount  : 0
                def passed  = total - failed - skipped
                def result  = currentBuild.currentResult                 // SUCCESS / UNSTABLE / FAILURE
                def color   = result == 'SUCCESS' ? 'good' : (result == 'FAILURE' ? 'danger' : 'warning')
                def icon    = result == 'SUCCESS' ? ':white_check_mark:' : ':x:'
                def mention = result == 'SUCCESS' ? '' : '<!here> '       // ping only when not green
                def dur     = currentBuild.durationString.replace(' and counting', '')

                slackSend channel: env.SLACK_CHANNEL, color: color,
                    message: """${mention}${icon} *MED ${params.SUITE} / ${params.PLATFORM}* — ${result}
:white_check_mark: ${passed}   :x: ${failed}   :fast_forward: ${skipped}   (total ${total})   •   :stopwatch: ${dur}
:bar_chart: <${env.BUILD_URL}allure|Allure report>   •   :bricks: <${env.BUILD_URL}|Jenkins #${env.BUILD_NUMBER}>   •   :scroll: <${env.BUILD_URL}console|Console>"""
            }
        }
    }
}
