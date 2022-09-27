#!groovy
@Library('amf-jenkins-library') _

import groovy.transform.Field

def SLACK_CHANNEL = '#amf-jenkins'
def PRODUCT_NAME = "amf-core"
def lastStage = ""
def color = '#FF8C00'
def headerFlavour = "WARNING"
@Field AMF_JOB = "application/AMF/amf/develop"
@Field AMF_CUSTOM_VALIDATOR_SCALAJS_JOB = "application/AMF/amf-custom-validator-scalajs/develop"

pipeline {
    options {
        timeout(time: 30, unit: 'MINUTES')
        ansiColor('xterm')
    }
    agent {
        dockerfile {
            filename 'Dockerfile'
            registryCredentialsId 'github-salt'
            registryUrl 'https://ghcr.io'
        }
    }
    environment {
        NEXUS = credentials('exchange-nexus')
        GITHUB_ORG = 'aml-org'
        GITHUB_REPO = 'amf-core'
    }
    stages {
        stage('Test') {
            steps {
                script {
                    lastStage = env.STAGE_NAME
                    sh 'sbt -mem 4096 -Dfile.encoding=UTF-8 clean coverage test coverageAggregate'
                }
            }
        }
        stage('Coverage') {
            when {
                anyOf {
                    branch 'master'
                    branch 'develop'
                }
            }
            steps {
                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'sonarqube-official', passwordVariable: 'SONAR_SERVER_TOKEN', usernameVariable: 'SONAR_SERVER_URL']]) {
                    script {
                        lastStage = env.STAGE_NAME
                        sh 'sbt -Dsonar.host.url=${SONAR_SERVER_URL} -Dsonar.login=${SONAR_SERVER_TOKEN} sonarScan'
                    }
                }
            }
        }
        stage('Publish') {
            when {
                anyOf {
                    branch 'master'
                    branch 'develop'
                }
            }
            steps {
                script {
                    lastStage = env.STAGE_NAME
                    sh '''
                           echo "about to publish in sbt"
                           sbt publish
                           echo "sbt publishing successful"
                       '''
                }
            }
        }
        stage('Triggers') {
            when {
                anyOf {
                    branch 'develop'
                }
            }
            steps {
                script {
                    lastStage = env.STAGE_NAME
                    echo "Triggering amf on develop branch"
                    build job: AMF_JOB, wait: false
                    echo "Triggering amf-custom-validator-scalajs on develop branch"
                    build job: AMF_CUSTOM_VALIDATOR_SCALAJS_JOB, wait: false
                }
            }
        }
        stage('Tag version') {
            when {
                anyOf {
                    branch 'master'
                }
            }
            steps {
                withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'github-salt', passwordVariable: 'GITHUB_PASS', usernameVariable: 'GITHUB_USER']]) {
                    script {
                        lastStage = env.STAGE_NAME
                        def version = sbtArtifactVersion("amlJVM")
                        tagCommitToGithub(version)
                    }
                }
            }
        }
    }
    post {
        unsuccessful {
            script {
                if (isMaster() || isDevelop()) {
                    sendBuildErrorSlackMessage(lastStage, SLACK_CHANNEL, PRODUCT_NAME)
                } else {
                    echo "Unsuccessful build: skipping slack message notification as branch is not master or develop"
                }
            }
        }
        success {
            script {
                echo "SUCCESSFUL BUILD"
                if (isMaster()) {
                    sendSuccessfulSlackMessage(SLACK_CHANNEL, PRODUCT_NAME)
                } else {
                    echo "Successful build: skipping slack message notification as branch is not master"
                }
            }
        }
    }
}

Boolean isDevelop() {
    env.BRANCH_NAME == "develop"
}

Boolean isMaster() {
    env.BRANCH_NAME == "master"
}

def sendBuildErrorSlackMessage(String lastStage, String slackChannel, String productName) {
    def color = '#FF8C00'
    def headerFlavour = 'WARNING'
    if (isMaster()) {
        color = '#FF0000'
        headerFlavour = "RED ALERT"
    } else if (isDevelop()) {
        color = '#FFD700'
    }
    def message = """:alert: ${headerFlavour}! :alert: Build failed!.
                  |Branch: ${env.BRANCH_NAME}
                  |Stage: ${lastStage}
                  |Product: ${productName}
                  |Build URL: ${env.BUILD_URL}""".stripMargin().stripIndent()
    slackSend color: color, channel: "${slackChannel}", message: message
}

def sendSuccessfulSlackMessage(String slackChannel, String productName) {
    slackSend color: '#00FF00', channel: "${slackChannel}", message: ":ok_hand: ${productName} Master Publish OK! :ok_hand:"
}

