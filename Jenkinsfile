pipeline {
    // Single-node setup (Mac Mini built-in node has no labels) — run on any available node.
    agent any

    triggers {
        // Nightly at ~2am (H = Jenkins-jittered minute, prevents thundering-herd)
        cron('H 2 * * *')
    }

    parameters {
        choice(
            name: 'PLATFORMS',
            choices: ['both', 'android', 'ios'],
            description: 'Which platform(s) to run. Nightly uses "both" (Android then iOS, SEQUENTIALLY — the BrowserStack plan allows only 1 parallel session). Manual runs can pick a single platform.'
        )
        choice(
            name: 'SUITE',
            choices: [
                'e2e-happy-path',
                'regression-e2e',
                'smoke',
                'sanity',
                'regression-full',
                'e2e-happy-path-ios',
                'e2e-flow-only-ios',
                'settings-loggedin',
                'med-android-en',
                'content-verification'
            ],
            description: 'TestNG suite file under src/test/resources/suites/<SUITE>.xml. Applied to BOTH platforms (default e2e-happy-path gives Android/iOS parity).'
        )
        string(
            name: 'ANDROID_APP_URL',
            defaultValue: 'med-android-latest',
            description: 'BrowserStack Android app reference — bare custom_id (e.g. med-android-latest) or bs:// hash.'
        )
        string(
            name: 'IOS_APP_URL',
            defaultValue: 'med-ios-latest',
            description: 'BrowserStack iOS app reference — bare custom_id (e.g. med-ios-latest, the device-lock-bypass build) or bs:// hash.'
        )
        string(
            name: 'ACTIVATION_CODE',
            defaultValue: '77AAAAAAAAAAAAAX',
            description: 'DiGA activation code (reusable test code — overrides Mock HI API call, no VPN needed).'
        )
        booleanParam(
            name: 'NOTIFY_SLACK',
            defaultValue: true,
            description: 'Post the run summary to Slack. UNCHECK for manual/ad-hoc runs so #qa-automation-nightly is not pinged.'
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

        stage('Test') {
            steps {
                script {
                    // SEQUENTIAL, not a parallel matrix: the BrowserStack App Automate plan has
                    // Parallels = 1, so only ONE session can run at a time. A parallel Android+iOS
                    // matrix made the second platform's session queue past the create-session
                    // timeout (build #52: iOS SessionNotCreated after ~10min). Running the platforms
                    // one after another keeps us within the single-session limit.
                    // Each platform tags + stashes its Allure/surefire results BEFORE the next
                    // platform's `mvn clean` wipes target/; the outer post block merges both.
                    def platforms = (params.PLATFORMS == 'both') ? ['android', 'ios'] : [params.PLATFORMS]
                    for (p in platforms) {
                        stage("Test - ${p}") {
                            def appUrl    = (p == 'ios') ? params.IOS_APP_URL : params.ANDROID_APP_URL
                            def buildName = "Jenkins-${env.BUILD_NUMBER}-${params.SUITE}-${p}"
                            // Fresh compile per platform (clean wipes the prior platform's target/,
                            // whose results are already stashed below).
                            sh 'mvn -B -e -ntp clean test-compile'
                            // A platform failure marks the build FAILURE but does NOT abort — the
                            // other platform still runs, and both get tagged/stashed/reported.
                            catchError(buildResult: 'FAILURE', stageResult: 'FAILURE') {
                                browserstack(credentialsId: 'c380e328-98ad-4aa9-9360-b930ee6db5bf') {
                                    withCredentials([
                                        string(credentialsId: 'IMAP_PASSWORD', variable: 'IMAP_PASSWORD')
                                    ]) {
                                        try {
                                            withEnv(["BROWSERSTACK_APP_URL=${appUrl}"]) {
                                                sh """
                                                    mvn -B -e -ntp test \
                                                      -Dbrowserstack.enabled=true \
                                                      -Dapp.type=med \
                                                      -Dplatform=${p} \
                                                      -DsuiteFile=src/test/resources/suites/${params.SUITE}.xml \
                                                      -Dactivation.code=${params.ACTIVATION_CODE} \
                                                      -Dbrowserstack.build.name='${buildName}'
                                                """
                                            }
                                        } finally {
                                            // Resolve this platform's BrowserStack dashboard URL while BS
                                            // credentials are in scope. Best-effort — never fails the build.
                                            withEnv(["BS_BUILD_NAME=${buildName}"]) {
                                                sh '''
                                                    mkdir -p target
                                                    curl -s -u "$BROWSERSTACK_USERNAME:$BROWSERSTACK_ACCESS_KEY" \
                                                      "https://api-cloud.browserstack.com/app-automate/builds.json?limit=50" \
                                                    | python3 -c "import sys,json,os; name=os.environ.get('BS_BUILD_NAME',''); d=json.load(sys.stdin); print(next(('https://app-automate.browserstack.com/dashboard/v2/builds/'+b['automation_build']['hashed_id'] for b in d if b.get('automation_build',{}).get('name')==name),''))" \
                                                      > target/bs_build_url.txt 2>/dev/null || true
                                                '''
                                            }
                                        }
                                    }
                                }
                            }
                            // Tag this platform's Allure results + stash BEFORE the next platform's
                            // `mvn clean` wipes target/. Runs even if the mvn above failed (catchError).
                            withEnv(["PLATFORM=${p}"]) {
                                sh '''
                                    python3 - "$PLATFORM" <<'PY'
import json, glob, sys
platform = sys.argv[1]
disp = 'iOS' if platform == 'ios' else 'Android'
for f in glob.glob('target/allure-results/*-result.json'):
    try:
        with open(f) as fh: data = json.load(fh)
    except Exception:
        continue
    labels = [l for l in data.get('labels', []) if l.get('name') not in ('parentSuite',)]
    labels.append({'name': 'parentSuite', 'value': disp})
    labels.append({'name': 'tag', 'value': platform})
    data['labels'] = labels
    # Make test identity platform-unique. Both platforms run the IDENTICAL suite, so
    # every result would otherwise share one historyId/testCaseId and Allure would
    # collapse Android + iOS into a single test (showing only the latest).
    if data.get('historyId'):
        data['historyId'] = platform + '-' + str(data['historyId'])
    if data.get('testCaseId'):
        data['testCaseId'] = platform + '-' + str(data['testCaseId'])
    params = [pr for pr in data.get('parameters', []) if pr.get('name') != 'platform']
    params.append({'name': 'platform', 'value': platform})
    data['parameters'] = params
    with open(f, 'w') as fh: json.dump(data, fh)
PY
                                '''
                            }
                            stash name: "results-${p}", allowEmpty: true,
                                includes: 'target/allure-results/**,target/surefire-reports/**,target/bs_build_url.txt,target/known-issues-report.json'
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            // Pull each platform's stashed results into its own subdir (best-effort — a
            // skipped platform simply has no stash).
            script {
                // Wipe last build's aggregation dir first — the main workspace is reused
                // and `agg/` is not under SCM, so stale results from a prior run would
                // otherwise be reported for a platform that did not run this build.
                dir('agg') { deleteDir() }
                ['android', 'ios'].each { p ->
                    dir("agg/${p}") {
                        try { unstash "results-${p}" }
                        catch (ignored) { echo "No stashed results for ${p} (platform not run)." }
                    }
                }
            }

            // TestNG / Surefire results from both platforms.
            junit allowEmptyResults: true,
                testResults: 'agg/*/target/surefire-reports/junitreports/*.xml,agg/*/target/surefire-reports/*.xml'

            // Seed Allure CI metadata for each platform's results dir that exists.
            script {
                ['android', 'ios'].each { p ->
                    def dirPath = "agg/${p}/target/allure-results"
                    if (fileExists(dirPath)) {
                        // Known issues → shown on the Allure Overview 'Environment' widget (known
                        // issues ride on passing tests, so they don't appear in the failure widgets).
                        def kiEnv = 'none'
                        def kiFile = "agg/${p}/target/known-issues-report.json"
                        if (fileExists(kiFile)) {
                            try {
                                def s = sh(returnStdout: true, script: """python3 - <<'PY'
import json
d = json.load(open('${kiFile}'))
items = d.get('knownIssues', [])
if items:
    parts = []
    for i in items:
        t = 'hit' if i.get('encountered') else 'not seen'
        if i.get('aging'): t += ', aging'
        parts.append(i['key'] + ' (' + t + ')')
    print('; '.join(parts) + '  -  ' + str(d.get('encounteredCount', 0)) + ' hit, '
          + str(len(d.get('agingKeys', []) or [])) + ' aging')
PY""").trim()
                                if (s) { kiEnv = s }
                            } catch (ignored) { }
                        }
                        writeFile file: "${dirPath}/environment.properties", text: """
Platform=${p}
Suite=${params.SUITE}
App=${p == 'ios' ? params.IOS_APP_URL : params.ANDROID_APP_URL}
ActivationCode=${params.ACTIVATION_CODE}
BrowserStack.Build=Jenkins-${env.BUILD_NUMBER}-${params.SUITE}-${p}
KnownIssues=${kiEnv}
""".stripIndent().trim()
                        writeFile file: "${dirPath}/executor.json", text: """
{"name":"Jenkins","type":"jenkins","buildName":"#${env.BUILD_NUMBER}","buildUrl":"${env.BUILD_URL}","reportUrl":"${env.BUILD_URL}allure"}
""".stripIndent().trim()
                        // Bucket failures in Allure: unexpected passes (fixed known issues) vs
                        // real regressions vs infra defects. Known-issue expected-fails are
                        // swallowed by knownIssue() so they never appear as failures here.
                        if (fileExists('src/test/resources/allure/categories.json')) {
                            sh "cp src/test/resources/allure/categories.json ${dirPath}/categories.json"
                        }
                    }
                }
            }

            // One merged Allure report from BOTH platforms' results. The parentSuite labels
            // (set in the Test stage) split Android vs iOS cleanly in the Suites tab.
            allure commandline: 'Allure', includeProperties: false,
                results: [[path: 'agg/android/target/allure-results'], [path: 'agg/ios/target/allure-results']]

            // Raw artifacts for debugging.
            archiveArtifacts allowEmptyArchive: true, fingerprint: false,
                artifacts: 'agg/**/target/allure-results/**, agg/**/target/surefire-reports/**'

            // Single Slack summary: one line per platform that ran (counts + inline
            // BrowserStack link), shared footer. Sandbox-safe (RunWrapper props only).
            script {
                if (!params.NOTIFY_SLACK) {
                    echo 'Slack notification skipped (NOTIFY_SLACK=false).'
                    return   // exits this script block only — junit/allure/archive above still ran
                }
                def result  = currentBuild.currentResult                 // SUCCESS / UNSTABLE / FAILURE
                def color   = result == 'SUCCESS' ? 'good' : (result == 'FAILURE' ? 'danger' : 'warning')
                def icon    = result == 'SUCCESS' ? ':large_green_circle:' : (result == 'FAILURE' ? ':red_circle:' : ':large_yellow_circle:')
                def mention = result == 'SUCCESS' ? '' : '<!here> '       // ping only when not green

                // Build one line per platform whose results are present.
                def lines = []
                ['android', 'ios'].each { p ->
                    def sfGlob = "agg/${p}/target/surefire-reports"
                    if (!fileExists(sfGlob)) { return }   // platform didn't run
                    def passed = '–', failed = '–', skipped = '–'
                    try {
                        def c = sh(returnStdout: true, script: """awk -F'[ ,]+' '/^Tests run:/{r+=\$3;f+=\$5;e+=\$7;s+=\$9} END{printf "%d,%d,%d", r-f-e-s, f+e, s}' ${sfGlob}/*.txt 2>/dev/null""").trim()
                        def parts = c.tokenize(',')
                        if (parts.size() == 3) { passed = parts[0]; failed = parts[1]; skipped = parts[2] }
                    } catch (ignored) { }

                    def bsLink = ''
                    def bsFile = "agg/${p}/target/bs_build_url.txt"
                    if (fileExists(bsFile)) {
                        def u = readFile(bsFile).trim()
                        if (u) { bsLink = "     :iphone: <${u}|BrowserStack>" }
                    }
                    // Known-issue summary (green-with-known-issues, not counted as failures).
                    def knownSeg = ''
                    def kiFile = "agg/${p}/target/known-issues-report.json"
                    if (fileExists(kiFile)) {
                        try {
                            def out = sh(returnStdout: true, script: """python3 - <<'PY'
import json
try:
    d = json.load(open('${kiFile}'))
    keys = [i['key'] for i in d.get('knownIssues', []) if i.get('encountered')]
    print(d.get('encounteredCount', 0))
    print(','.join(keys))
    print(len(d.get('agingKeys', []) or []))
except Exception:
    print(0); print(''); print(0)
PY""").trim()
                            def parts = out.split('\n')
                            def ec = parts.length > 0 ? parts[0].trim() : '0'
                            def keys = parts.length > 1 ? parts[1].trim() : ''
                            def agingN = parts.length > 2 ? parts[2].trim() : '0'
                            if (ec && ec != '0') {
                                knownSeg = "   :large_yellow_circle: ${ec} known" + (keys ? " (${keys})" : '')
                                if (agingN && agingN != '0') { knownSeg += "   :hourglass: ${agingN} aging" }
                            }
                        } catch (ignored) { }
                    }
                    def label = (p == 'ios') ? 'iOS' : 'Android'
                    lines << "${label} · ${params.SUITE}:   :white_check_mark: ${passed}   :x: ${failed}   :fast_forward: ${skipped}${knownSeg}${bsLink}"
                }

                def body = lines.join('\n')
                slackSend channel: env.SLACK_CHANNEL, color: color,
                    message: """${mention}${icon}  *MED nightly*   —   *${result}*
${body}
:bar_chart: <${env.BUILD_URL}allure|Allure Report>      :bricks: <${env.BUILD_URL}|Build #${env.BUILD_NUMBER}>      :scroll: <${env.BUILD_URL}console|Console>"""
            }
        }
    }
}
