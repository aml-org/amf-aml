#!groovy

pipeline {
  agent {
    dockerfile true
  }
  environment {
    NEXUS = credentials('exchange-nexus')
  }
  stages {
    stage('Test') {
      steps {
        wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'XTerm']) {
          withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'sonarqube-official', passwordVariable: 'SONAR_SERVER_TOKEN', usernameVariable: 'SONAR_SERVER_URL']]) {
            sh 'sbt -mem 4096 -Dfile.encoding=UTF-8 clean coverage test coverageReport sonarMe'
          }
        }
      }
    }
    stage('Publish') {
      when {
        anyOf {
          branch 'master'
          branch 'build/develop'
          branch 'release/*'
        }
      }
      steps {
        wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'XTerm']) {
          sh 'sbt publish'
        }
      }
    }
    stage('Trigger amf projects') {
      when {
        branch 'build/develop'
      }
      steps {
        echo "Starting TCKutor Applications/AMF/amf/amf-core/build/develop"
        build job: 'application/AMF/amf/amf-core/build/develop', wait: false
      }
    }
  }
}