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
                sh 'for s in accounts instruments orders positions; do mvn -B -f app/$s-service/pom.xml clean package -Dmaven.test.skip=true || exit 1; done'
            }
        }
        
        stage('Database Validation Test') {
            steps {
                sh 'docker rm -f accounts-db instruments-db orders-db positions-db underfrog-python || true'
                sh 'chmod +x ./db/scripts/data_validation_test.sh'
                sh './db/scripts/data_validation_test.sh'
            }
            post {
                always {
                    sh 'docker-compose down --remove-orphans --volumes || true'
                    sh 'docker rm -f accounts-db instruments-db orders-db positions-db underfrog-python || true'
                }
            }
        }
    }
}
