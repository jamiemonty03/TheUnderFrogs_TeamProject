pipeline {
    agent any
    triggers {
        cron('*/45 * * * *')
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

        stage('Build Services') {
            parallel {
                stage('Build Accounts Service') {
                    steps {
                        sh 'mvn -B -f app/accounts-service/pom.xml clean package -Dmaven.test.skip=true'
                    }
                }
                stage('Build Instruments Service') {
                    steps {
                        sh 'mvn -B -f app/instruments-service/pom.xml clean package -Dmaven.test.skip=true'
                    }
                }
                stage('Build Orders Service') {
                    steps {
                        sh 'mvn -B -f app/orders-service/pom.xml clean package -Dmaven.test.skip=true'
                    }
                }
                stage('Build Positions Service') {
                    steps {
                        sh 'mvn -B -f app/positions-service/pom.xml clean package -Dmaven.test.skip=true'
                    }
                }
            }
        }

        stage('Static Analysis') {
            parallel {
                stage('Analyse Accounts Service') {
                    steps {
                        sh 'mvn -B -f app/accounts-service/pom.xml checkstyle:check'
                    }
                }
                stage('Analyse Instruments Service') {
                    steps {
                        sh 'mvn -B -f app/instruments-service/pom.xml checkstyle:check'
                    }
                }
                stage('Analyse Orders Service') {
                    steps {
                        sh 'mvn -B -f app/orders-service/pom.xml checkstyle:check'
                    }
                }
                stage('Analyse Positions Service') {
                    steps {
                        sh 'mvn -B -f app/positions-service/pom.xml checkstyle:check'
                    }
                }
            }
        }

        stage('Unit Tests') {
            parallel {
                stage('Test Accounts Service') {
                    steps {
                        sh 'mvn -B -f app/accounts-service/pom.xml test'
                    }
                }
                stage('Test Instruments Service') {
                    steps {
                        sh 'mvn -B -f app/instruments-service/pom.xml test'
                    }
                }
                stage('Test Orders Service') {
                    steps {
                        sh 'mvn -B -f app/orders-service/pom.xml test'
                    }
                }
                stage('Test Positions Service') {
                    steps {
                        sh 'mvn -B -f app/positions-service/pom.xml test'
                    }
                }
            }
        }

        stage('Database Validation Test') {
            steps {
                sh 'chmod +x ./db/scripts/data_validation_test.sh'
                sh './db/scripts/data_validation_test.sh'
            }
        }
    }
}
