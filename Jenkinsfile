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
        sh '''#!/bin/bash
                version=$(sbt version | tail -n 1 | grep -o '[0-9].[0-9].[0-9].*')
                commit=$(git log -1 | grep -o '[a-zA-Z0-9]\\{40\\}')
                msg="tagging release commit with it's release version"
                url="https://\\${GIT_USERNAME}:\\${GIT_PASSWORD}@github.com/mulesoft/amf-aml"
                echo $GIT_USERNAME $GIT_PASSWORD $version $commit
                git remote show origin
                git config user.email 'amirra@mulesoft.com\'
                git config user.name 'Ariel Mirra\'
                git tag -fa -m $msg $version $commit
                git push $url refs/tags/$version
         '''
      }
    }
  }
}
