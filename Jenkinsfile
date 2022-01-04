pipeline {
    agent any

    options {
        disableConcurrentBuilds()
    }
    environment {
        AWS_CREDS = credentials('AWS_ACCOUNT_CREDENTIALS')
        EKS_NAME = credentials('EKS_NAME')
        REGION = "us-east-1"
    }

    stages {
        stage('Prepare'){
            steps {
                sh '''
                    aws configure set default.aws_access_key_id $AWS_CREDS_USR
                    aws configure set default.aws_secret_access_key $AWS_CREDS_PSW
                    aws configure set default.region $REGION
                '''
            }
        }
        stage('Test, Build, Push & Sec'){
            parallel {
                stage('Build, Test & Push'){
                    steps {
                        script {
                            env.CB_BUILD_ID = sh(script: 'aws codebuild start-build --project-name docker-build | jq -r .build.id', returnStdout: true).trim()
                            echo env.CB_BUILD_ID
                        }

                        timeout(time: 3, unit: 'MINUTES') {
                            retry(100) {
                                sh '''
                                    #!/bin/bash

                                    sleep 5
                                    status=$(aws codebuild batch-get-builds --ids $CB_BUILD_ID | jq -r .builds[0].buildStatus)
                                    if [ "$status" = "SUCCEEDED" ]; then
                                        exit 0
                                    elif [ "$status" = "FAILED" ]; then
                                        exit 0
                                    else
                                        exit 1
                                    fi
                                '''
                            }
                        }
                    }
                }
                stage('SonarQube'){
                    steps{
                        echo 'Waiting for Sonar...'
                    }
                }
            }
        }
        stage('Deploy'){
            steps{
                sh '''
                    aws eks update-kubeconfig --region $REGION --name $EKS_NAME
                    kubectl apply -f k8s_deploy.yaml
                    kubectl apply -f k8s_service.yaml
                '''
            }
        }
        stage('Health Check'){
            steps {
                sh '''
                    echo waiting for health check
                '''
            }
        }
        stage('Metrics'){
            steps{
                sh '''
                    echo waiting for metrics
                '''
            }
        }
    }
    post {
        unsuccessful {
            // TODO: Notify slack
            echo 'Notify Slack'
            echo 'Add metrics'
        }
        success {
            echo 'Notify Slack'
            echo 'Add metrics'
        }
    }
}