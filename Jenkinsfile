pipeline {
    agent any

    options {
        disableConcurrentBuilds()
    }
    environment {
        GIT_ACCESS_TOKEN = credentials('GIT_ACCESS_TOKEN')
        
    }
    
    stages {
        stage('Test, Build & Sec'){
            parallel {
                stage('Build & Test'){
                    steps {
                        withGradle {
                            sh '''
                                export SPRING_PROFILES_ACTIVE=$SPRING_PROFILE_TO_ACTIVATE
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
                sh 'az login -u $AZ_USERNAME_USR -p $AZ_USERNAME_PSW'
                sh '''
                    TOKEN=$(az acr login --name $ACR_NAME --expose-token --output tsv --query accessToken)
                    docker login -u 00000000-0000-0000-0000-000000000000 -p $TOKEN $ACR_PATH_URL
                '''

                script {
                    docker.withRegistry(env.ACR_PATH_URL){
                        def image = docker.build('$DOCKER_IMAGE_NAME:tmp')
                        image.push()
                    }
                }
            }
        }
        stage('Deploy'){
            steps{
                sh '''
                    echo waiting
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