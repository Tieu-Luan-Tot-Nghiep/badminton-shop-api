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
                        # Đăng nhập AWS ECR
                        aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $ECR_REGISTRY
                        
                        # Build Docker image từ file .jar vừa tạo
                        docker build -t $LOCAL_IMAGE .
                        
                        # Gắn tag image theo bản Latest và Build Number
                        docker tag $LOCAL_IMAGE:latest $ECR_IMAGE_LATEST
                        docker tag $LOCAL_IMAGE:latest $ECR_IMAGE_VERSION
                        
                        # Đẩy image lên kho lưu trữ AWS
                        docker push $ECR_IMAGE_LATEST
                        docker push $ECR_IMAGE_VERSION
                    '''
                }
            }
        }

        stage('Deploy') {
            when { expression { currentBuild.currentResult == null || currentBuild.currentResult == 'SUCCESS' } }
            steps {
                withCredentials([file(credentialsId: 'badminton-shop-env', variable: 'ENV_FILE')]) {
                    sh '''
                        set -e
                        cp "$ENV_FILE" .env
                        trap 'rm -f .env' EXIT

                        aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $ECR_REGISTRY

                        # TẮT IN LOG TRƯỚC KHI ĐỌC FILE NHẠY CẢM
                        set +x
                        echo "Loading environment variables..."
                        set -a
                        [ -f .env ] && . ./.env
                        set +a
                        # BẬT LẠI IN LOG SAU KHI NẠP XONG
                        set -x

                        export APP_IMAGE="$ECR_IMAGE_VERSION"

                        echo 'Starting services with Docker Compose...'
                        
                        # SỬA Ở ĐÂY: Thêm dấu gạch ngang vào giữa docker-compose
                        docker-compose -f "$DOCKER_COMPOSE_FILE" pull app
                        docker-compose -f "$DOCKER_COMPOSE_FILE" up -d --remove-orphans

                        echo 'Waiting for postgres to become healthy...'
                        for i in {1..30}; do
                            # SỬA CẢ Ở ĐÂY NẾU CẦN
                            STATUS=$(docker inspect -f "{{.State.Health.Status}}" postgres 2>/dev/null || echo "starting")
                            if [ "$STATUS" == "healthy" ]; then
                                echo "PostgreSQL is ready."
                                break
                            fi
                            sleep 2
                        done

                        # SỬA Ở ĐÂY: Thêm dấu gạch ngang
                        docker-compose -f "$DOCKER_COMPOSE_FILE" ps
                    '''
                }
            }
        }
    }

    post {
        success { echo 'Deployment completed successfully!' }
        failure { echo 'Deployment failed. Please check the logs above.' }
    }
}