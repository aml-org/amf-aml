#!groovy

pipeline {
  agent {
    dockerfile true
  }
  environment {
    NEXUS = credentials('exchange-nexus')
  }
  stages {
    stage('autotag') {
//      steps {
//        wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'XTerm']) {
//
//        }
//      }
      steps {
        bash '''#!/bin/bash
                VERSION=$(sbt version | tail -n 1 | grep -o '[0-9].[0-9].[0-9].*')
                COMMIT=$(git log -1 | grep -o '[a-zA-Z0-9]\\{40\\}')
                echo $VERSION
                echo $COMMIT
                &2 echo `git tag -a $VERSION $COMMIT`
                &2 echo `git push origin $VERSION`
         '''
      }
    }
//    stage('Test') {
//      steps {
//        wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'XTerm']) {
//          sh 'sbt -mem 4096 -Dfile.encoding=UTF-8 clean coverage test coverageReport'
//        }
//      }
//    }
//    stage('Coverage') {
//      when {
//         branch 'master'
//      }
//      steps {
//        wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'XTerm']) {
//          withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'sonarqube-official', passwordVariable: 'SONAR_SERVER_TOKEN', usernameVariable: 'SONAR_SERVER_URL']]) {
//            sh 'sbt -Dsonar.host.url=${SONAR_SERVER_URL} sonarScan'
//          }
//        }
//      }
//    }
//    stage('Publish') {
//      when {
//        anyOf {
//          branch 'master'
//          branch 'new_model'
//        }
//      }
//      steps {
//        wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'XTerm']) {
//          sh 'sbt publish'
//        }
//      }
//    }
  }
}
