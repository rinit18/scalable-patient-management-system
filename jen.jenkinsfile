pipeline {
    agent any

    stages {

        stage('Checkout') {
            steps {
                git 'https://github.com/rinit18/scalable-patient-management-system'
            }
        }

        stage('Build') {
            steps {
                sh 'mvn clean install -DskipTests'
            }
        }

        stage('Test') {
            steps {
                sh 'mvn test'
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