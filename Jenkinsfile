def pipelineParams = [  projectName: "esjc" ]
pipeline {
    agent {
        node {
            label "${if(pipelineParams.nodeLabel?.trim()) pipelineParams.nodeLabel else 'linux' }"
        }
    }

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '40', artifactNumToKeepStr: '40'))
        timeout(time: 1, unit: 'HOURS')
     }

    environment {
        JACOCO_VERSION = "0.8.7"
    }

    tools {
        maven 'maven'
        jdk 'openjdk8'
    }

    parameters {
        booleanParam(name: 'isRelease', defaultValue: false, description: 'Skal prosjektet releases? Alle andre parametere ignoreres ved snapshot-bygg.')
        string(name: "specifiedVersion", defaultValue: "", description: "Hva er det nye versjonsnummeret (X.X.X)? Som default releases snapshot-versjonen")
        text(name: "releaseNotes", defaultValue: "Ingen endringer utført", description: "Hva er endret i denne releasen?")
        text(name: "securityReview", defaultValue: "Endringene har ingen sikkerhetskonsekvenser", description: "Har endringene sikkerhetsmessige konsekvenser, og hvilke tiltak er i så fall iverksatt?")
        string(name: "reviewer", defaultValue: "Endringene krever ikke review", description: "Hvem har gjort review?")
    }

    stages {
        stage('Initialize') {
            steps {
                print "Params: ${params}"
                print "Pipeline params: ${pipelineParams}"
                script {
                    wrap([$class: 'BuildUser']) {
                        env.user = sh(script: 'echo "${BUILD_USER}"', returnStdout: true).trim()
                    }
                    env.GIT_SHA = sh(returnStdout: true, script: 'git rev-parse HEAD').substring(0, 7)
                    env.REPO_NAME = scm.getUserRemoteConfigs()[0].getUrl().tokenize('/').last().split("\\.")[0]
                }
                sh '''
                 echo "PATH = ${PATH}"
                 echo "M2_HOME = ${M2_HOME}"
                 echo "REPO_NAME = ${REPO_NAME}"
                '''
                rtMavenDeployer(
                        id: "MAVEN_DEPLOYER",
                        serverId: "KS Artifactory",
                        releaseRepo: "ks-maven",
                        snapshotRepo: "maven-all"
                )
                rtMavenResolver(
                        id: "MAVEN_RESOLVER",
                        serverId: "KS Artifactory",
                        releaseRepo: "maven-all",
                        snapshotRepo: "maven-all"
                )
                sh 'mvn --version'
                rtBuildInfo(
                        captureEnv: true
                )
            }
        }
        stage('Opprett sertifikat') {
            script {
                sh script: "./scripts/generate-ssl-cert.sh", label: "Opprette sertifikat til Eventstore"
            }
        }
        stage('Build') {
            steps {
                environment {
                 UID = "${sh(script: 'echo $(id -u)', returnStdout: true, label: 'Finn UID')}"
                 GID = "${sh(script: 'echo $(id -g)', returnStdout: true, label: 'Finn GID')}"
                }
                script {
                     docker.withRegistry('https://docker-all.artifactory.fiks.ks.no', 'artifactory-token-based') {
                         sh script: "docker-compose --no-ansi build", label: "Build docker images"
                         sh script: "UID=${UID} GID=${GID} docker-compose --no-ansi up -d --remove-orphans"
                    }
                    def pom = readMavenPom file: 'pom.xml'
                    env.POM_VERSION = pom.version
                }
                rtMavenRun (
                     pom: 'pom.xml',
                     goals: '-D jib.console=plain -Dkotlin.environment.keepalive=true -T0.5C -U -B clean install',
                     resolverId: 'MAVEN_RESOLVER',
                     deployerId: "MAVEN_DEPLOYER",
                     tool: 'maven'
                )
                post {
                 success {
                     recordIssues enabledForFailure: true, tools: [java(), mavenConsole(), kotlin()]
                     rtMavenRun(
                         pom: 'pom.xml',
                         goals: "org.jacoco:jacoco-maven-plugin:${JACOCO_VERSION}:report",
                         resolverId: 'MAVEN_RESOLVER',
                         tool: 'maven'
                     )
                     jacoco(execPattern: '**/*.exec')
                 }
                 always {
                     junit testResults:'**/surefire-reports/*.xml', allowEmptyResults: true
                 }
                 failure {
                     sh "docker-compose logs --no-color -t > docker.logs"
                     archiveArtifacts artifacts: 'docker.logs', fingerprint: true
                     script {
                         if(fileExists("component-test/build/reports/tests")) {
                             zip zipFile: 'buildReports.zip', archive: true, glob: 'component-test/build/reports/tests/*.*'
                         }
                     }
                 }
                 cleanup {
                     sh script: "docker-compose --no-ansi down -v", label: "Stop all docker images"
                 }
            }
        }
        stage('Security check') {
            steps {
                withSonarQubeEnv('SonarCloud') {
                    rtMavenRun (
                            pom: 'pom.xml',
                            goals: '-B sonar:sonar',
                            resolverId: "MAVEN_RESOLVER",
                            opts: "-Dsonar.organization=ks-no")
                }
            }
        }

        stage('Release: Set version and tag') {
            when {
                expression { params.isRelease }
            }

            steps {
                script {
                    docker.withRegistry('https://docker-all.artifactory.fiks.ks.no', 'artifactory-token-based') {
                         sh script: "docker-compose --no-ansi build", label: "Build docker images"
                         sh script: "UID=${UID} GID=${GID} docker-compose --no-ansi up -d --remove-orphans"
                    }

                    if (params.specifiedVersion == null || params.specifiedVersion == "")
                        env.version = env.POM_VERSION.replace("-SNAPSHOT", "")
                    else
                        env.version = params.specifiedVersion

                    env.IMAGE_TAG = env.version

                    println("Version: ${env.version}")

                    currentBuild.description = "${env.user} released version ${env.version}"
                }

                gitCheckout()
                prepareReleaseNoBuild env.version
                rtMavenRun(
                    pom: 'pom.xml',
                    goals: '-T0.5C -U -B clean install',
                    opts: '-DskipTests=true',
                    deployerId: "MAVEN_DEPLOYER",
                    resolverId: 'MAVEN_RESOLVER',
                    tool: 'maven'
                )
                gitTag(isRelease, env.version)
            }
        }

        stage('Snapshot: Set version and tag') {
            when {
                expression { !params.isRelease }
            }

            steps {
                script {
                    env.version = env.POM_VERSION.replace("SNAPSHOT", env.GIT_SHA)
                    env.IMAGE_TAG = env.version
                    println("Version: ${env.version}")
                }
                rtMavenRun(
                    resolverId: 'MAVEN_RESOLVER',
                    pom: 'pom.xml',
                    goals: '-T0.5C enforcer:enforce@validate-snap',
                    tool: 'maven'
                )
            }
        }

        stage('Deploy artifacts') {
            when {
                anyOf {
                    branch 'master'
                    branch 'main'
                }
            }


        stage('Release: Set new snapshot') {
           when {
               expression { params.isRelease }
           }
           steps {
               setSnapshotRT(env.version, 'MAVEN_RESOLVER')
               gitPush()
           }
           post {
               success {
                   createGithubRelease env.REPO_NAME, params.reviewer, params.releaseNotes, env.version, env.user, params.securityReview
               }
           }
       }
    }
    post {
        always {
            rtPublishBuildInfo(
                    serverId: "KS Artifactory"
            )
            archiveArtifacts artifacts: '**/build.log', fingerprint: true, allowEmptyArchive: true
            deleteDir()
        }
    }
}
