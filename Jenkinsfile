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
      steps {
        withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'github-exchange', passwordVariable: 'GITHUB_PASS', usernameVariable: 'GITHUB_USER']]) {
          sh '''#!/bin/bash
                version=$(sbt version | tail -n 1 | grep -o '[0-9].[0-9].[0-9].*')
                commit=$(git log -1 | grep -o '[a-zA-Z0-9]\\{40\\}')
                msg="tagging release commit with it's release version"
                url="https://${GITHUB_USER}:${GITHUB_PASS}@github.com/mulesoft/amf-aml"
                
                git config user.email 'amirra@mulesoft.com\'
                git config user.name 'arielmirra\'
                
                echo "delete remote tag:"
                git push $url -d $version
                
                echo "tag:"
                git tag -f $version
                
                echo "push tag:"
                git push $url $version
         '''
        }
      }
    }
  }
}
