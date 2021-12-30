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
        stage('Prepare, Test, Build & Sec'){
            parallel {
                stage('Prepare'){
                    steps {
                        sh '''
                            aws configure set default.aws_access_key_id $AWS_CREDS_USR
                            aws configure set default.aws_secret_access_key $AWS_CREDS_PSW
                            aws configure set default.region $REGION
                        '''
                    }
                }
                stage('Build & Test'){
                    steps {
                        withGradle {
                            sh '''
                                export SPRING_PROFILES_ACTIVE=dev
                                ./gradlew build
                                '''
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
        stage('Push Docker'){
            steps {
                sh '''
                    aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 594483618195.dkr.ecr.us-east-1.amazonaws.com
                    docker build -t samplemsforeks .
                    docker tag samplemsforeks:latest 594483618195.dkr.ecr.us-east-1.amazonaws.com/samplemsforeks:latest

                    docker push 594483618195.dkr.ecr.us-east-1.amazonaws.com/samplemsforeks:latest
                '''
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