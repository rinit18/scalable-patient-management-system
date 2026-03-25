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

        stage('Build Services (Skip Tests)') {
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

        stage('Docker Build') {
            steps {
                sh 'docker-compose build'
            }
        }

        stage('Start System') {
            steps {
                sh '''
                docker-compose down
                docker-compose up -d
                sleep 40
                '''
            }
        }

        stage('Run Integration Tests') {
            steps {
                sh '''
                cd integration-tests
                mvn test
                '''
            }
        }

        stage('Stop System') {
            steps {
                sh 'docker-compose down'
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