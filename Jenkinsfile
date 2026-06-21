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
                            env.BS_BUILD_NAME = "Jenkins-${env.BUILD_NUMBER}-${params.SUITE}-${params.PLATFORM}"

                            try {
                                sh """
                                    mvn -B -e -ntp test \
                                      -Dbrowserstack.enabled=true \
                                      -Dapp.type=med \
                                      -Dplatform=${params.PLATFORM} \
                                      -DsuiteFile=src/test/resources/suites/${params.SUITE}.xml \
                                      -Dactivation.code=${params.ACTIVATION_CODE} \
                                      -Dbrowserstack.build.name='${env.BS_BUILD_NAME}'
                                """
                            } finally {
                                // Resolve the BrowserStack App Automate dashboard URL here, where the
                                // BS credentials are in scope (post{} runs outside this wrapper). Stash
                                // it to a file for the Slack step. Best-effort — never fails the build.
                                try {
                                    sh '''
                                        mkdir -p target
                                        curl -s -u "$BROWSERSTACK_USERNAME:$BROWSERSTACK_ACCESS_KEY" \
                                          "https://api-cloud.browserstack.com/app-automate/builds.json?limit=50" \
                                        | python3 -c "import sys,json,os; name=os.environ.get('BS_BUILD_NAME',''); d=json.load(sys.stdin); print(next(('https://app-automate.browserstack.com/dashboard/v2/builds/'+b['automation_build']['hashed_id'] for b in d if b.get('automation_build',{}).get('name')==name),''))" \
                                          > target/bs_build_url.txt 2>/dev/null || true
                                    '''
                                } catch (ignored) { /* link is optional */ }
                            }
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

            // Slack summary: clean, color-coded, deep links to Allure + BrowserStack.
            // Sandbox-safe by design — uses only whitelisted RunWrapper props (no rawBuild,
            // which script-security blocks). Counts are parsed from surefire in the shell
            // and everything optional is wrapped so the message ALWAYS sends.
            script {
                def result  = currentBuild.currentResult                 // SUCCESS / UNSTABLE / FAILURE
                def color   = result == 'SUCCESS' ? 'good' : (result == 'FAILURE' ? 'danger' : 'warning')
                def icon    = result == 'SUCCESS' ? ':large_green_circle:' : (result == 'FAILURE' ? ':red_circle:' : ':large_yellow_circle:')
                def mention = result == 'SUCCESS' ? '' : '<!here> '       // ping only when not green
                def dur     = currentBuild.durationString.replace(' and counting', '')

                // Test counts (passed, failed+errors, skipped) parsed from surefire.
                def passed = '–', failed = '–', skipped = '–'
                try {
                    def c = sh(returnStdout: true, script: '''awk -F'[ ,]+' '/^Tests run:/{r+=$3;f+=$5;e+=$7;s+=$9} END{printf "%d,%d,%d", r-f-e-s, f+e, s}' target/surefire-reports/*.txt 2>/dev/null''').trim()
                    def p = c.tokenize(',')
                    if (p.size() == 3) { passed = p[0]; failed = p[1]; skipped = p[2] }
                } catch (ignored) { }

                // Optional BrowserStack dashboard link (resolved in the Test stage).
                def bsLink = ''
                if (fileExists('target/bs_build_url.txt')) {
                    def u = readFile('target/bs_build_url.txt').trim()
                    if (u) { bsLink = ":iphone: <${u}|BrowserStack>      " }
                }

                slackSend channel: env.SLACK_CHANNEL, color: color,
                    message: """${mention}${icon}  *MED*   ·   ${params.SUITE}   ·   ${params.PLATFORM}   —   *${result}*
:white_check_mark: ${passed} passed      :x: ${failed} failed      :fast_forward: ${skipped} skipped      :stopwatch: ${dur}
:bar_chart: <${env.BUILD_URL}allure|Allure Report>      ${bsLink}:bricks: <${env.BUILD_URL}|Build #${env.BUILD_NUMBER}>      :scroll: <${env.BUILD_URL}console|Console>"""
            }
        }
    }
}
