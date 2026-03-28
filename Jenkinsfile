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
        ECR_IMAGE = '283209027400.dkr.ecr.us-east-1.amazonaws.com/badmintion-shop:latest'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Build & Test') {
            steps {
                sh 'chmod +x ./gradlew'
                sh './gradlew clean test'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'build/test-results/test/*.xml'
                    archiveArtifacts artifacts: 'build/reports/**', allowEmptyArchive: true
                }
            }
        }

        stage('Package') {
            steps {
                sh './gradlew bootJar -x test'
                archiveArtifacts artifacts: 'build/libs/*.jar', fingerprint: true
            }
        }

        stage('Push Image To ECR') {
            when {
                expression { currentBuild.currentResult == null || currentBuild.currentResult == 'SUCCESS' }
            }
            steps {
                echo 'Tests passed. Login ECR and push Docker image...'
                sh '''
                    set -e

                    aws ecr get-login-password --region $AWS_REGION | docker login --username AWS --password-stdin $ECR_REGISTRY
                    docker build -t $LOCAL_IMAGE .
                    docker tag $LOCAL_IMAGE:latest $ECR_IMAGE
                    docker push $ECR_IMAGE
                '''
                echo "Push image ${ECR_IMAGE} successfully."
            }
        }
    }

    post {
        success {
            echo 'Pipeline completed: build, test, and deploy succeeded.'
        }
        failure {
            echo 'Pipeline failed. Deploy stage was skipped because tests/build did not pass.'
        }
    }
}