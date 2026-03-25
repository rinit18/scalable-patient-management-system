pipeline {
    agent any

    stages {

        stage('Fix Permissions') {
            steps {
                sh '''
                chmod +x auth-service/mvnw
                chmod +x patient-service/mvnw
                chmod +x billing-service/mvnw
                chmod +x analytics-service/mvnw
                chmod +x api-gateway/mvnw
                '''
            }
        }

        stage('Build Services') {
            steps {
                sh '''
                cd auth-service && ./mvnw clean install -DskipTests
                cd ../patient-service && ./mvnw clean install -DskipTests
                cd ../billing-service && ./mvnw clean install -DskipTests
                cd ../analytics-service && ./mvnw clean install -DskipTests
                cd ../api-gateway && ./mvnw clean install -DskipTests
                '''
            }
        }

        stage('Run Tests') {
            steps {
                sh '''
                cd auth-service && ./mvnw test
                cd ../patient-service && ./mvnw test
                cd ../billing-service && ./mvnw test
                cd ../analytics-service && ./mvnw test
                cd ../api-gateway && ./mvnw test
                '''
            }
        }

        stage('Docker Build') {
            steps {
                sh 'docker-compose build'
            }
        }

        stage('Run Full System') {
            steps {
                sh '''
                docker-compose down
                docker-compose up -d
                sleep 30
                '''
            }
        }
    }

    post {
        always {
            echo 'Pipeline Finished'
        }
        success {
            echo 'Build Success ✅'
        }
        failure {
            echo 'Build Failed ❌'
        }
    }
}