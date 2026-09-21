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
                checkout scm
            }
        }
        stage('Build Image') {
            steps {
                sh 'mvn -B -f app/pom.xml clean package -DskipTests'
                sh 'docker build -t team-skeleton:latest ./app'
            }
        }
        
        stage('Database Validation Test') {
            steps {
                sh 'docker rm -f underfrog-postgres underfrog-app underfrog-notebooks underfrog-python || true'
                sh 'chmod +x ./db/scripts/data_validation_test.sh'
                sh './db/scripts/data_validation_test.sh'
            }
            post {
                always {
                    sh 'docker-compose down --remove-orphans --volumes || true'
                    sh 'docker rm -f underfrog-postgres underfrog-app underfrog-notebooks underfrog-python || true'
                }
            }
        }
    }
}
