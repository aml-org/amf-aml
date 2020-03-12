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
        bash '''#!/bin/bash
                VERSION=$(sbt version | tail -n 1 | grep -o '[0-9].[0-9].[0-9].*')
                COMMIT=$(git log -1 | grep -o '[a-zA-Z0-9]\\{40\\}')
                1>&2 echo $VERSION
                echo $COMMIT
                git tag -a $VERSION $COMMIT
                git push origin $VERSION
         '''
      }
    }
  }
}
