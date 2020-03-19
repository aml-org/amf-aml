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
                commit=$(git log -1 | grep -o '[a-zA-Z0-9]\\{40\\}')
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
