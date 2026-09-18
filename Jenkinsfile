pipeline {
    agent any

    environment {
        // Database credentials are managed in Jenkins Credentials.
        DB_HOST = credentials('team-database')
        DB_PORT = '5432'
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

        // stage('Build Image') {
        //     steps {
        //         sh 'mvn -B clean package -DskipTests'
        //         sh 'docker build -t team-skeleton:latest .'
        //     }
        // }

        stage('Database Connection Test') {
            steps {
                sh '''
                    docker run --rm \\
                        -e PGPASSWORD="$POSTGRES_PASSWORD" \\
                        postgres:15-alpine \\
                        psql \\
                        -h "$DB_HOST" \\
                        -p "$DB_PORT" \\
                        -U "$POSTGRES_USER" \\
                        -d "$POSTGRES_DB" \\
                        -c 'SELECT 1;'
                '''
            }
        }

        /*
        stage('Database Validation Test') {
            steps {
                sh 'chmod +x ./scripts/data_validation_test.sh'
                sh './scripts/data_validation_test.sh'
            }
        }
        */
    }
}
