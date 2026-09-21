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
        stage('Unit Tests') {
            parallel {
                stage('Test App') {
                    when {
                        expression { fileExists('app/pom.xml') }
                    }
                    steps {
                        sh 'mvn -B -f app/pom.xml test'
                    }
                    post {
                        always {
                            junit allowEmptyResults: true, testResults: 'app/target/surefire-reports/*.xml'
                        }
                    }
                }

                stage('Test Order Service') {
                    when {
                        expression { fileExists('order-service/pom.xml') }
                    }
                    steps {
                        sh 'mvn -B -f order-service/pom.xml test'
                    }
                    post {
                        always {
                            junit allowEmptyResults: true, testResults: 'order-service/target/surefire-reports/*.xml'
                        }
                    }
                }

                stage('Test Instrument Service') {
                    when {
                        expression { fileExists('instrument-service/pom.xml') }
                    }
                    steps {
                        sh 'mvn -B -f instrument-service/pom.xml test'
                    }
                    post {
                        always {
                            junit allowEmptyResults: true, testResults: 'instrument-service/target/surefire-reports/*.xml'
                        }
                    }
                }

                stage('Test Account Service') {
                    when {
                        expression { fileExists('account-service/pom.xml') }
                    }
                    steps {
                        sh 'mvn -B -f account-service/pom.xml test'
                    }
                    post {
                        always {
                            junit allowEmptyResults: true, testResults: 'account-service/target/surefire-reports/*.xml'
                        }
                    }
                }

                stage('Test Position Service') {
                    when {
                        expression { fileExists('position-service/pom.xml') }
                    }
                    steps {
                        sh 'mvn -B -f position-service/pom.xml test'
                    }
                    post {
                        always {
                            junit allowEmptyResults: true, testResults: 'position-service/target/surefire-reports/*.xml'
                        }
                    }
                }
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
