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
        stage('Prepare, Test, Build & Sec'){
            parallel {
                stage('Build, Test & Push'){
                    steps {
                        script {
                            env.CB_BUILD_ID = sh(script: 'aws codebuild start-build --project-name docker-build | jq -r .build.id', returnStdout: true).trim()
                            echo env.CB_BUILD_ID

                            def retryAttempt = 0
                            retry(100) {
                                sleep 5

                                env.CB_BUILD_STATUS = sh(script: 'aws codebuild batch-get-builds --ids $CB_BUILD_ID | jq -r .builds[0].buildStatus', returnStdout: true).trim()
                                echo env.CB_BUILD_STATUS
                                if(env.CB_BUILD_STATUS == "SUCCEEDED"){
                                    env.JOB_STATUS="SUCCESS"
                                    return 0
                                }else if(env.CB_BUILD_STATUS == "FAILED"){
                                    env.JOB_STATUS="FAILURE"
                                    return 0
                                }else{
                                    return 1
                                }

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