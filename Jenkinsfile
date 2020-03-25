#!groovy

pipeline {
  agent {
    dockerfile true
  }
  stages {
    stage('autotag') {
      steps {
        withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'github-exchange', passwordVariable: 'GITHUB_PASS', usernameVariable: 'GITHUB_USER']]) {
          sh '''#!/bin/bash
                version=$(sbt version | tail -n 1 | grep -o '[0-9].[0-9].[0-9].*')
                url="https://${GITHUB_USER}:${GITHUB_PASS}@github.com/mulesoft/amf-aml"
                
                echo "about to tag the commit with the new version:"
                git push $url --delete $version
                git tag -f $version
                git push $url $version
         '''
        }
      }
    }
  }
}
