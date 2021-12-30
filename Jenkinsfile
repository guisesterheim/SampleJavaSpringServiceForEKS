pipeline {
    agent any

    options {
        disableConcurrentBuilds()
    }
    environment {
        GIT_ACCESS_TOKEN = credentials('GIT_ACCESS_TOKEN')
        AZ_USERNAME = credentials('AZ_USERNAME')
        AZ_TF_STORAGE_ACCOUNT = credentials('AZ_TF_STORAGE_ACCOUNT')
        AZ_SUBSCRIPTION_ID = credentials('AZ_SUBSCRIPTION_ID')
        AZ_CLIENT_ID = credentials('AZ_CLIENT_ID')
        AZ_TENANT_ID = credentials('AZ_TENANT_ID')
        AZ_TF_ROOT_RG_NAME = credentials('AZ_TF_ROOT_RG_NAME')
        AZ_STORAGE_CONNECTION_STRING = credentials('AZ_STORAGE_CONNECTION_STRING')
        AZ_DNS_ZONE_NAME = credentials('AZ_DNS_ZONE_NAME')
        AZ_TF_BASE_APP_RG_NAME = credentials('AZ_TF_BASE_APP_RG_NAME')

        // Azure Container Registry variables
        ACR_NAME = "mySampleACRDev"
        ACR_PATH_URL = "https://,.azurecr.io"

        // General Variables
        ENVIRONMENT = "dev"
        SPRING_PROFILE_TO_ACTIVATE = "$ENVIRONMENT"

        PLATFORM = "platform"
        SERVICE = "sampleJavaSpringService"
        SERVICE_PATH_TO_HEALTH_CHECK = "api/health"

        // Docker aux variables
        DOCKER_IMAGE_NAME = "$PLATFORM/$SERVICE"

        // Terraform aux variables
        TIMESTAMP = sh(script: 'echo `date +%s`', returnStdout: true).trim()
        TF_FILE_EXTENSION = ".tfstate"
        TF_CONTAINER_NAME = "$ENVIRONMENT-tf-blue-green"
        TF_VARIABLES_FILE_EXTENSION = ".tfvars"
        TF_VARIABLES_FILE = "rootVars-$ENVIRONMENT$TF_VARIABLES_FILE_EXTENSION"
        TF_CODE_FOLDER = "blue_green_code/tf_aks_infrastructure"
    }

    stages {
        stage('Test, Build & Prepare'){
            parallel {
                stage('Prepare'){
                    steps {
                        script {
                            env.TF_FILE_NAME = "$PLATFORM-$SERVICE-tf_file-$ENVIRONMENT-$TIMESTAMP$TF_FILE_EXTENSION"
                        }

                        sh 'docker system prune -f'
                    }
                }
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
                stage('Sonar'){
                    steps{
                        echo 'Waiting for Sonar...'
                    }
                }
            }
        }
        stage('Spin up Blue'){
            parallel {
                stage('Spin up Blue'){
                    steps{
                        // Creates file to store the state
                        sh '''
                            touch $TF_FILE_NAME
                            az storage blob upload \
                                --account-name $AZ_TF_STORAGE_ACCOUNT_PSW \
                                --container-name "$TF_CONTAINER_NAME" \
                                --name "$TF_FILE_NAME" \
                                --file "$TF_FILE_NAME" \
                                --connection-string "$AZ_STORAGE_CONNECTION_STRING_PSW"
                            '''

                        sh '''
                            cd $TF_CODE_FOLDER

                            terraform init \
                                -backend-config "container_name=$TF_CONTAINER_NAME" \
                                -backend-config "storage_account_name=$AZ_TF_STORAGE_ACCOUNT_PSW" \
                                -backend-config "key=$TF_FILE_NAME" \
                                -backend-config "subscription_id=$AZ_SUBSCRIPTION_ID_PSW" \
                                -backend-config "client_id=$AZ_CLIENT_ID_USR" \
                                -backend-config "client_secret=$AZ_CLIENT_ID_PSW" \
                                -backend-config "tenant_id=$AZ_TENANT_ID_PSW" \
                                -backend-config "resource_group_name=$AZ_TF_ROOT_RG_NAME_PSW"
                            '''

                        retry(2){
                            sh '''
                                cd $TF_CODE_FOLDER

                                terraform plan -var "client_id=$AZ_CLIENT_ID_USR" \
                                    -var "client_secret=$AZ_CLIENT_ID_PSW" \
                                    -var "subscription_id=$AZ_SUBSCRIPTION_ID_PSW" \
                                    -var "tenant_id=$AZ_TENANT_ID_PSW" \
                                    -var "timestamp=$TIMESTAMP" \
                                    -var-file="$TF_VARIABLES_FILE" \
                                    -out $TF_FILE_NAME.log
                                '''
                            
                            sh '''
                                cd $TF_CODE_FOLDER

                                terraform apply $TF_FILE_NAME.log
                                '''
                        }
                    }
                }
                stage('Push temp image'){
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
            }
        }
        stage('Config K8S'){
            steps{
                sh '''
                    cd $TF_CODE_FOLDER

                    CREATED_AKS_NAME=$(terraform output aks_name)
                    CREATED_RESOURCE_GROUP_NAME=$(terraform output resource_group_name)

                    az aks get-credentials --name $CREATED_AKS_NAME --resource-group $CREATED_RESOURCE_GROUP_NAME --overwrite-existing
                '''

                // Replaces docker image latest to grab the current one
                sh '''
                    PROFILE=$ENVIRONMENT

                    kubectl apply -f blue_green_code/kube_config/namespaces.yaml
                    kubectl apply -f blue_green_code/kube_config/sampleJavaSpringService.yaml
                    kubectl apply -f blue_green_code/kube_config/ingress.yaml
                    kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/controller-v0.34.1/deploy/static/provider/cloud/deploy.yaml
                '''

                timeout(time: 10, unit: 'MINUTES'){ // This is intended to control the success of grabing the generated IP Address
                    retry(36){ // Retries for 3 minutes
                        sleep(time: 5, unit: 'SECONDS')

                        sh '''
                            echo "Grabing generated IP..."
                            IP=$(kubectl get services ingress-nginx-controller -n ingress-nginx --output jsonpath='{.status.loadBalancer.ingress[0].ip}')
                            if [ -z "$IP" ]; then
                                exit 1
                            fi

                            #PUBLICIPID=$(az network public-ip list --query "[?ipAddress!=null]|[?contains(ipAddress, '$IP')].[id]" --output tsv)
                            #if [ -z "$PUBLICIPID" ]; then
                            #    exit 1
                            #fi
                        ''' 
                    }
                }
            }
            post{
                unsuccessful {
                    // Destroy blue environment
                    // TODO: notificar slack
                    build job: 'Platform/Infrastructure/DEV/Shutdown-App', wait: false, parameters: [string(name: 'ENVIRONMENT_TO_DESTROY', value: env.TF_FILE_NAME)]
                }
            }
        }
        stage('Test Blue') {
            steps {
                echo 'Waiting for tests...'
            }
        }
        stage('Health Check'){
            steps {
                retry(36){ // Retries for 3 minutes
                    sleep(time: 5, unit: 'SECONDS')
                    sh '''
                        IP=$(kubectl get services ingress-nginx-controller -n ingress-nginx --output jsonpath='{.status.loadBalancer.ingress[0].ip}')

                        ENV_UP=$(curl -s $IP/$SERVICE_PATH_TO_HEALTH_CHECK --max-time 5)
                        if [ $ENV_UP != "OK" ]; then
                            # Notify slack and rollback
                            exit 1
                        fi
                    '''
                }
            }
        }
        stage('Turn Green'){
            steps{
                sh 'rm -rf jenkins-helper'
                sh 'git clone -n https://github.com/guisesterheim/jenkins_helper.git'
                
                sh '''
                    cd jenkins-helper
                    git checkout HEAD build/libs/jenkins-helper-1.0-RELEASE.jar
                '''

                sh '''
                    IP=$(kubectl get services ingress-nginx-controller -n ingress-nginx --output jsonpath='{.status.loadBalancer.ingress[0].ip}')
                    
                    envVar=$(echo $ENVIRONMENT | tr '[:lower:]' '[:upper:]')

                    java -jar jenkins-helper/build/libs/jenkins-helper-1.0-RELEASE.jar \
                        oper="turn_green" \
                        subscription_id="$AZ_SUBSCRIPTION_ID_PSW" \
                        client_id="$AZ_CLIENT_ID_USR" \
                        secret_key="$AZ_CLIENT_ID_PSW" \
                        tenant_id="$AZ_TENANT_ID_PSW" \
                        resource_group_name="$AZ_TF_BASE_APP_RG_NAME_PSW$envVar" \
                        dns_zone_name="$AZ_DNS_ZONE_NAME_PSW" \
                        a_name="$ENVIRONMENT" \
                        ip_address="$IP" \
                        ttl="30"
                '''
            }
        }
        stage('Promote temp to latest & Destroy old'){
            steps {
                // Here you call another job on Jenkins to destroy you old environment
                // build job: 'Platform/Infrastructure/DEV/Shutdown-App', wait: false, parameters: [string(name: 'DESTROY_ALL_BUT_THIS', value: env.TF_FILE_NAME)]
                
                script {
                    docker.withRegistry(env.ACR_PATH_URL){
                        def image = docker.build('$DOCKER_IMAGE_NAME:latest')
                        image.push()
                    }
                }
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