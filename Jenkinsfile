pipeline {
    agent any
    environment {
         //Variables are securely managed via Jenkins Credentials security settings 
        POSTGRES_DB = credentials('postgres-db')
        POSTGRES_USER = credentials('postgres-user')
        POSTGRES_PASSWORD = credentials('postgres-password') 
    }

    triggers {
        cron('*/15 * * * *')
    }
    tools {
        jdk 'JDK21'
        maven 'maven'
    }
    stages {
        stage('Checkout') {
            steps {
                sh 'sudo chown -R jenkins:jenkins . || true'  // Ensure permissions
                checkout scm
            }
        }
        stage('Build Image') {
            steps {
                sh 'mvn -B clean package -DskipTests'
                sh 'docker build -t team-skeleton:latest .'
            }
        }
        
        stage('Database Validation Test') {
            steps {
                sh 'chmod +x ./scripts/data_validation_test.sh'
                sh './scripts/data_validation_test.sh'
            }
            post {
                always {
                    sh 'docker-compose down || true'
                }
            }
        }
    }
}
