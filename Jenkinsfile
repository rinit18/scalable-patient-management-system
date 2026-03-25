pipeline {
    agent any

    stages {

        stage('Build') {
            steps {
                sh './mvnw clean install -DskipTests'
            }
        }

        stage('Test') {
            steps {
                sh './mvnw test'
            }
        }

        stage('Docker Build') {
            steps {
                sh 'docker-compose build'
            }
        }

        stage('Run App') {
            steps {
                sh 'docker-compose up -d'
                sh 'sleep 20'
            }
        }
    }
}