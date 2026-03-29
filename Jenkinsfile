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
        CONTAINER_PORT = '8080'
        CLIP_CONTAINER_NAME = 'clip-local-service'
        CLIP_HOST_PORT = '8001'
        CLIP_CONTAINER_PORT = '8001'
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

                    echo '=== Disk usage before cleanup ==='
                    df -h
                    df -ih || true
                    docker system df || true

                    # Free stale Docker artifacts to avoid Gradle/Build failures on low disk.
                    docker container prune -f || true
                    docker image prune -af --filter 'until=24h' || true
                    docker volume prune -f || true
                    docker builder prune -af || true

                    rm -rf "$WORKSPACE/.gradle" || true
                    rm -rf "$WORKSPACE/build" || true
                    rm -rf "$WORKSPACE/tools/clip-local-service/.venv" || true
                    rm -rf "$HOME/.cache/pip" || true

                    # Prevent Docker json logs from filling the root disk.
                    find /var/lib/docker/containers -name '*-json.log' -type f -size +100M -exec truncate -s 0 {} ';' 2>/dev/null || true

                    echo '=== Disk usage after cleanup ==='
                    df -h
                    df -ih || true
                    docker system df || true

                    # Require at least 2GB free before build to avoid random Gradle failures.
                    AVAIL_KB=$(df --output=avail / | tail -1 | tr -d ' ')
                    if [ "$AVAIL_KB" -lt 2097152 ]; then
                        echo 'ERROR: less than 2GB free on /. Please increase disk size or remove running containers/artifacts.'
                        exit 1
                    fi
                '''
            }
        }

        stage('Build') {
            steps {
                sh 'chmod +x ./gradlew'
                sh 'GRADLE_USER_HOME="$WORKSPACE/.gradle" ./gradlew clean bootJar -x test --no-daemon'
            }
        }

        stage('Archive Artifact') {
            steps {
                archiveArtifacts artifacts: 'build/libs/*.jar', fingerprint: true
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

                        if grep -q '^CLIP_MODEL_PATH=' .env; then
                            echo 'Bien CLIP_MODEL_PATH da ton tai trong file .env'
                        else
                            echo 'KHONG tim thay CLIP_MODEL_PATH trong file .env (se su dung gia tri rong khi run container)'
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

        stage('Deploy With Docker Compose') {
            when {
                expression { currentBuild.currentResult == null || currentBuild.currentResult == 'SUCCESS' }
            }
            steps {
                echo 'Pull and run services by Docker Compose...'
                withCredentials([file(credentialsId: 'badminton-shop-env', variable: 'ENV_FILE')]) {
                    sh '''
                        set -e

                        cp "$ENV_FILE" .env
                        trap 'rm -f .env' EXIT

                        aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $ECR_REGISTRY

                        if docker compose version >/dev/null 2>&1; then
                            COMPOSE_CMD="docker compose"
                        elif command -v docker-compose >/dev/null 2>&1; then
                            COMPOSE_CMD="docker-compose"
                        else
                            echo 'ERROR: Docker Compose is not installed on this Jenkins agent.'
                            exit 1
                        fi

                        export APP_IMAGE="$ECR_IMAGE_VERSION"
                        export HOST_PORT="$HOST_PORT"
                        export CONTAINER_PORT="$CONTAINER_PORT"
                        export CONTAINER_NAME="$CONTAINER_NAME"
                        export CLIP_CONTAINER_NAME="$CLIP_CONTAINER_NAME"
                        export CLIP_HOST_PORT="$CLIP_HOST_PORT"
                        export CLIP_CONTAINER_PORT="$CLIP_CONTAINER_PORT"

                        $COMPOSE_CMD -f "$DOCKER_COMPOSE_FILE" pull app || true
                        $COMPOSE_CMD -f "$DOCKER_COMPOSE_FILE" up -d --remove-orphans
                        $COMPOSE_CMD -f "$DOCKER_COMPOSE_FILE" ps

                        for i in $(seq 1 90); do
                            if curl -fsS "http://127.0.0.1:$HOST_PORT" >/dev/null 2>&1; then
                                echo "Application is reachable at http://127.0.0.1:$HOST_PORT"
                                exit 0
                            fi
                            sleep 2
                        done

                        echo 'Application did not become reachable in time. Recent app logs:'
                        $COMPOSE_CMD -f "$DOCKER_COMPOSE_FILE" logs --tail=120 app || true
                        exit 1
                    '''
                }
                echo "Services were deployed by Docker Compose using image ${ECR_IMAGE_VERSION}."
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