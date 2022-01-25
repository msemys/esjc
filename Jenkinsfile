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
        stage('Build') {
            environment {
             UID = "${sh(script: 'echo $(id -u)', returnStdout: true, label: 'Finn UID')}"
             GID = "${sh(script: 'echo $(id -g)', returnStdout: true, label: 'Finn GID')}"
            }
            steps {
                script {
                     sh script: "./scripts/generate-ssl-cert.sh", label: "Opprette sertifikat til Eventstore"
                     docker.withRegistry('https://docker-all.artifactory.fiks.ks.no', 'artifactory-token-based') {
                         sh script: "docker-compose --no-ansi build", label: "Build docker images"
                         sh script: "UID=${UID} GID=${GID} docker-compose --no-ansi up -d --remove-orphans"
                    }
                    def pom = readMavenPom file: 'pom.xml'
                    env.POM_VERSION = pom.version
                }
                rtMavenRun (
                     pom: 'pom.xml',
                     goals: ' -T0.5C -U -B clean install',
                     resolverId: 'MAVEN_RESOLVER',
                     deployerId: "MAVEN_DEPLOYER",
                     tool: 'maven'
                )
            }
            post {
                success {
                    recordIssues enabledForFailure: true, tool: java()
                    rtMavenRun(
                        pom: 'pom.xml',
                        goals: "org.jacoco:jacoco-maven-plugin:${JACOCO_VERSION}:report",
                        tool: 'maven',
                        resolverId: "MAVEN_RESOLVER"
                    )
                    jacoco(execPattern: '**/*.exec')
                }
                cleanup {
                    sh script: "docker-compose --no-ansi down -v", label: "Stop all docker images"
                }
            }
        }

        stage('Snapshot: verify pom') {
            when {
                expression { !params.isRelease }
            }

            steps {
                rtMavenRun (
                        pom: 'pom.xml',
                        goals: "-T0.5C enforcer:enforce@validate-snap",
                        resolverId: "MAVEN_RESOLVER"
                )
            }
        }

        stage('Release: new version') {
            when {
                expression { params.isRelease }
            }

            steps {
                script {
                    if (params.specifiedVersion == null || params.specifiedVersion == "")
                        env.releaseVersion = env.POM_VERSION.replace("-SNAPSHOT", "")
                    else
                        env.releaseVersion = params.specifiedVersion

                    currentBuild.description = "${env.user} released version ${env.releaseVersion}"
                }

                gitCheckout(env.BRANCH_NAME)
                prepareReleaseNoBuildRT(releaseVersion,'MAVEN_RESOLVER')
                rtMavenRun(
                        pom: 'pom.xml',
                        goals: '-DskipTests -U -B clean install',
                        deployerId: 'MAVEN_DEPLOYER',
                        resolverId: 'MAVEN_RESOLVER'
                )
                gitTag(isRelease, releaseVersion)
            }
        }
        stage('Release: set snapshot') {
            when {
                expression { params.isRelease }
            }
            steps {
                setSnapshotRT(releaseVersion, 'MAVEN_RESOLVER')
                gitPush()
            }
            post {
                success {
                    createGithubRelease env.REPO_NAME, params.reviewer, params.releaseNotes, env.releaseVersion, env.user
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
