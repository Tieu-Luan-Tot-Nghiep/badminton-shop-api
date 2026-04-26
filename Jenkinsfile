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
        LOCAL_IMAGE = 'badminton-shop'
        ECR_IMAGE_LATEST = "283209027400.dkr.ecr.us-east-1.amazonaws.com/${APP_NAME}:latest"
        ECR_IMAGE_VERSION = "283209027400.dkr.ecr.us-east-1.amazonaws.com/${APP_NAME}:${BUILD_NUMBER}"
        CONTAINER_NAME = 'badminton-shop'
        HOST_PORT = '80'
        CONTAINER_PORT = '8080'
        DOCKER_COMPOSE_FILE = 'docker-compose.yml'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Disk Preflight') {
            steps {
                sh '''
                    set -e
                    echo '=== Cleaning up disk space ==='
                    docker container prune -f || true
                    docker image prune -af --filter "until=24h" || true
                    docker builder prune -af || true
                    rm -rf "$WORKSPACE/.gradle" "$WORKSPACE/build" || true
                    
                    AVAIL_KB=$(df --output=avail / | tail -1 | tr -d ' ')
                    if [ "$AVAIL_KB" -lt 2097152 ]; then
                        echo 'ERROR: Less than 2GB free disk space.'
                        exit 1
                    fi
                '''
            }
        }

        stage('Build') {
            steps {
                sh 'chmod +x ./gradlew'
                sh './gradlew clean bootJar -x test --no-daemon'
            }
        }

        stage('Verify & Push Image') {
            when { expression { currentBuild.currentResult == null || currentBuild.currentResult == 'SUCCESS' } }
            steps {
                withCredentials([file(credentialsId: 'badminton-shop-env', variable: 'ENV_FILE')]) {
                    sh '''
                        set -e
                        cp "$ENV_FILE" .env
                        trap 'rm -f .env' EXIT

                        # Login ECR
                        aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $ECR_REGISTRY
                        
                        # Build and Tag
                        docker build -t $LOCAL_IMAGE .
                        docker tag $LOCAL_IMAGE:latest $ECR_IMAGE_LATEST
                        docker tag $LOCAL_IMAGE:latest $ECR_IMAGE_VERSION
                        
                        # Push
                        docker push $ECR_IMAGE_LATEST
                        docker push $ECR_IMAGE_VERSION
                    '''
                }
            }
        }

        stage('Deploy') {
            when { expression { currentBuild.currentResult == null || currentBuild.currentResult == 'SUCCESS' } }
            steps {
                // Bổ sung credentials cho Database tại đây
                withCredentials([
                    file(credentialsId: 'badminton-shop-env', variable: 'ENV_FILE'),
                    string(credentialsId: 'db-name', variable: 'DB_NAME'),
                    string(credentialsId: 'db-user', variable: 'DB_USERNAME'),
                    string(credentialsId: 'db-pass', variable: 'DB_PASSWORD')
                ]) {
                    sh '''
                        set -e
                        cp "$ENV_FILE" .env
                        trap 'rm -f .env' EXIT

                        aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $ECR_REGISTRY

                        export APP_IMAGE="$ECR_IMAGE_VERSION"
                        export POSTGRES_DB="$DB_NAME"
                        export POSTGRES_USER="$DB_USERNAME"
                        export POSTGRES_PASSWORD="$DB_PASSWORD"

                        docker compose -f "$DOCKER_COMPOSE_FILE" pull app
                        docker compose -f "$DOCKER_COMPOSE_FILE" up -d --remove-orphans

                        echo 'Waiting for postgres to become healthy...'
                        for i in {1..30}; do
                            if [ "$(docker inspect -f "{{.State.Health.Status}}" postgres 2>/dev/null)" == "healthy" ]; then
                                echo "PostgreSQL is ready."
                                break
                            fi
                            sleep 2
                        done
                    '''
                }
            }
        }
    }

    post {
        success { echo 'Deployment successful!' }
        failure { echo 'Pipeline failed. Check logs for details.' }
    }
}