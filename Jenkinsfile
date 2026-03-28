pipeline {
    agent any

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    environment {
        APP_NAME = 'badminton-shop'
        AWS_REGION = 'us-east-1'
        ECR_REGISTRY = '283209027400.dkr.ecr.us-east-1.amazonaws.com'
        LOCAL_IMAGE = 'badmintion-shop'
        ECR_IMAGE_LATEST = '283209027400.dkr.ecr.us-east-1.amazonaws.com/badmintion-shop:latest'
        ECR_IMAGE_VERSION = "283209027400.dkr.ecr.us-east-1.amazonaws.com/badmintion-shop:${BUILD_NUMBER}"
        CONTAINER_NAME = 'badmintion-shop'
        HOST_PORT = '80'
        CONTAINER_PORT = '80'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build') {
            steps {
                sh 'chmod +x ./gradlew'
                sh './gradlew clean bootJar -x test'
            }
        }

        stage('Archive Artifact') {
            steps {
                archiveArtifacts artifacts: 'build/libs/*.jar', fingerprint: true
            }
        }

        stage('Run Tools app.py') {
            when {
                expression { currentBuild.currentResult == null || currentBuild.currentResult == 'SUCCESS' }
            }
            steps {
                sh '''
                    set -e
                    timeout 60s python3 -m uvicorn --app-dir tools/clip-local-service app:app --host 0.0.0.0 --port 8001 || true
                '''
            }
        }

        stage('Verify Env File') {
            when {
                expression { currentBuild.currentResult == null || currentBuild.currentResult == 'SUCCESS' }
            }
            steps {
                withCredentials([file(credentialsId: 'badminton-shop-env', variable: 'ENV_FILE')]) {
                    sh '''
                        set -e

                        cp "$ENV_FILE" .env
                        ls -la .env

                        # Show only variable names for safety.
                        head -n 3 .env | sed 's/=.*$/=***masked***/'

                        if grep -q '^AWS_REGION=' .env; then
                            echo 'Bien AWS_REGION da ton tai trong file .env'
                        else
                            echo 'KHONG tim thay AWS_REGION trong file .env'
                        fi

                        rm -f .env
                    '''
                }
            }
        }

        stage('Push Image To ECR') {
            when {
                expression { currentBuild.currentResult == null || currentBuild.currentResult == 'SUCCESS' }
            }
            steps {
                echo 'Build passed. Login ECR and push Docker image...'
                withCredentials([file(credentialsId: 'badminton-shop-env', variable: 'ENV_FILE')]) {
                    sh '''
                        set -e

                        cp "$ENV_FILE" .env
                        trap 'rm -f .env' EXIT

                        aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $ECR_REGISTRY
                        docker build -t $LOCAL_IMAGE .
                        docker tag $LOCAL_IMAGE:latest $ECR_IMAGE_LATEST
                        docker tag $LOCAL_IMAGE:latest $ECR_IMAGE_VERSION
                        docker push $ECR_IMAGE_LATEST
                        docker push $ECR_IMAGE_VERSION
                    '''
                }
                echo "Push image ${ECR_IMAGE_LATEST} and ${ECR_IMAGE_VERSION} successfully."
            }
        }

        stage('Deploy Latest Container') {
            when {
                expression { currentBuild.currentResult == null || currentBuild.currentResult == 'SUCCESS' }
            }
            steps {
                echo 'Pull and run the newest image version...'
                withCredentials([file(credentialsId: 'badminton-shop-env', variable: 'ENV_FILE')]) {
                    sh '''
                        set -e

                        cp "$ENV_FILE" .env
                        trap 'rm -f .env' EXIT

                        aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $ECR_REGISTRY
                        docker pull $ECR_IMAGE_VERSION

                        if docker ps -a --format '{{.Names}}' | grep -w "$CONTAINER_NAME" >/dev/null 2>&1; then
                            docker rm -f "$CONTAINER_NAME"
                        fi

                        docker run -d \
                            --name "$CONTAINER_NAME" \
                            --restart unless-stopped \
                            --env-file .env \
                            -p "$HOST_PORT:$CONTAINER_PORT" \
                            "$ECR_IMAGE_VERSION"
                    '''
                }
                echo "Container ${CONTAINER_NAME} is running with image ${ECR_IMAGE_VERSION}."
            }
        }
    }

    post {
        success {
            echo 'Pipeline completed: build and deploy succeeded.'
        }
        failure {
            echo 'Pipeline failed. Deploy stage was skipped because build did not pass.'
        }
    }
}