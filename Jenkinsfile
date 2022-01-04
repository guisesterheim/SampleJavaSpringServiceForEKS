pipeline {
    agent {
        kubernetes {
            label 'kaniko'
            yaml """
apiVersion: v1
kind: Pod
metadata:
  name: kaniko
spec:
  serviceAccountName: jenkins-sa-agent
  containers:
  - name: jnlp
    image: 'public.ecr.aws/z9u4r7b2/jenkins-agent:latest'
    args: ['\$(JENKINS_SECRET)', '\$(JENKINS_NAME)']
  - name: kaniko
    image: 594483618195.dkr.ecr.us-east-1.amazonaws.com/kaniko:latest
    imagePullPolicy: Always
    command:
    - /busybox/cat
    tty: true
    resources:
      requests:
        cpu: "1"
        memory: 4Gi
  restartPolicy: Never
"""
        }    
    }

    options {
        disableConcurrentBuilds()
    }
    environment {
        AWS_CREDS = credentials('AWS_ACCOUNT_CREDENTIALS')
        EKS_NAME = credentials('EKS_NAME')
        REGION = "us-east-1"
    }

    stages {
        stage('Testing'){
            environment {
                DOCKERFILE  = "Dockerfile.v3"
                GITREPO     = "git://github.com/ollypom/mysfits.git"
                CONTEXT     = "./api"
                REGISTRY    = '594483618195.dkr.ecr.us-east-1.amazonaws.com'
                IMAGE       = 'mysfits'
                TAG         = 'latest'
            }
            steps {
                sh '''#!/busybox/sh
                    /kaniko/executor \
                    --context=${GITREPO} \
                    --context-sub-path=${CONTEXT} \
                    --dockerfile=${DOCKERFILE} \
                    --destination=${REGISTRY}/${IMAGE}:${TAG}
                '''

                container(name: 'kaniko', shell: '/busybox/sh') {
                    sh '''#!/busybox/sh
                    /kaniko/executor \
                    --context=${GITREPO} \
                    --context-sub-path=${CONTEXT} \
                    --dockerfile=${DOCKERFILE} \
                    --destination=${REGISTRY}/${IMAGE}:${TAG}
                    '''
                }
            }
        }
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
                                ./gradlew build --stacktrace
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