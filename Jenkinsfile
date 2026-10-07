pipeline {
    agent any
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
                stage('Build Trade Executor') {
                    steps {
                        sh 'mvn -B -f app/trade-executor/pom.xml clean package -Dmaven.test.skip=true'
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
                stage('Test Trade Executor') {
                    steps {
                        sh 'mvn -B -f app/trade-executor/pom.xml test'
                    }
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'app/*/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Prepare Environment') {
            steps {
                sh '''
                    DB_PASSWORD=$(openssl rand -hex 16)
                    JWT_SECRET=$(openssl rand -hex 32)
                    for s in accounts-service instruments-service orders-service positions-service trade-executor trade-analytics-service; do
                        if [ ! -f app/$s/.env ]; then
                            sed -e "s/ENTER_PASSWORD_HERE/$DB_PASSWORD/" -e "s/your-generated-secret-token-here/$JWT_SECRET/" \
                                app/$s/.env.example > app/$s/.env
                        fi
                    done
                '''
            }
        }

        stage('Python Tests') {
            steps {
                sh '''
                    docker-compose run --rm --no-deps -e PYTHONDONTWRITEBYTECODE=1 python sh -c \
                        "python -m pytest /db/tests -p no:cacheprovider --junitxml=/db/tests/reports/pytest-db.xml; s=\\$?; chown -R $(id -u):$(id -g) /db/tests/reports; exit \\$s"
                    docker-compose build trade-analytics-service
                    docker-compose run --rm --no-deps -e PYTHONDONTWRITEBYTECODE=1 -v "$PWD/app/trade-analytics-service/tests:/app/tests" trade-analytics-service sh -c \
                        "python -m pytest tests -p no:cacheprovider --junitxml=tests/reports/pytest-analytics.xml; s=\\$?; chown -R $(id -u):$(id -g) tests/reports; exit \\$s"
                '''
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'db/tests/reports/*.xml, app/trade-analytics-service/tests/reports/*.xml'
                }
            }
        }

        stage('End-to-end Smoke Test') {
            steps {
                sh './scripts/smoke-test-e2e.sh'
            }
            post {
                always {
                    sh 'docker-compose down --volumes --remove-orphans || true'
                }
            }
        }

        stage('Dependency Scanner') {
            parallel {
                stage('Scan Accounts Service') {
                    steps {
                        sh 'mvn -B -f app/accounts-service/pom.xml dependency:analyze'
                    }
                }
                stage('Scan Instruments Service') {
                    steps {
                        sh 'mvn -B -f app/instruments-service/pom.xml dependency:analyze'
                    }
                }
                stage('Scan Orders Service') {
                    steps {
                        sh 'mvn -B -f app/orders-service/pom.xml dependency:analyze'
                    }
                }
                stage('Scan Positions Service') {
                    steps {
                        sh 'mvn -B -f app/positions-service/pom.xml dependency:analyze'
                    }
                }
                stage('Scan Trade Executor') {
                    steps {
                        sh 'mvn -B -f app/trade-executor/pom.xml dependency:analyze'
                    }
                }
            }
        }

        stage('SonarQube Analysis') {
            parallel {
                stage('Accounts Service') {
                    steps {
                        withSonarQubeEnv('SonarQube') {
                            sh 'mvn -B -f app/accounts-service/pom.xml sonar:sonar -Dsonar.projectKey=theunderfrogs-accounts-service -Dsonar.projectName="Accounts Service" -Dsonar.coverage.jacoco.xmlReportPaths=app/accounts-service/target/site/jacoco/jacoco.xml'
                        }
                    }
                }
                stage('Instruments Service') {
                    steps {
                        withSonarQubeEnv('SonarQube') {
                            sh 'mvn -B -f app/instruments-service/pom.xml sonar:sonar -Dsonar.projectKey=theunderfrogs-instruments-service -Dsonar.projectName="Instruments Service" -Dsonar.coverage.jacoco.xmlReportPaths=app/instruments-service/target/site/jacoco/jacoco.xml'
                        }
                    }
                }
                stage('Orders Service') {
                    steps {
                        withSonarQubeEnv('SonarQube') {
                            sh 'mvn -B -f app/orders-service/pom.xml sonar:sonar -Dsonar.projectKey=theunderfrogs-orders-service -Dsonar.projectName="Orders Service" -Dsonar.coverage.jacoco.xmlReportPaths=app/orders-service/target/site/jacoco/jacoco.xml'
                        }
                    }
                }
                stage('Positions Service') {
                    steps {
                        withSonarQubeEnv('SonarQube') {
                            sh 'mvn -B -f app/positions-service/pom.xml sonar:sonar -Dsonar.projectKey=theunderfrogs-positions-service -Dsonar.projectName="Positions Service" -Dsonar.coverage.jacoco.xmlReportPaths=app/positions-service/target/site/jacoco/jacoco.xml'
                        }
                    }
                }
                stage('Trade Executor') {
                    steps {
                        withSonarQubeEnv('SonarQube') {
                            sh 'mvn -B -f app/trade-executor/pom.xml sonar:sonar -Dsonar.projectKey=theunderfrogs-trade-executor -Dsonar.projectName="Trade Executor" -Dsonar.coverage.jacoco.xmlReportPaths=app/trade-executor/target/site/jacoco/jacoco.xml'
                        }
                    }
                }
            }
        }

        stage('Quality Gates') {
            parallel {
                stage('Accounts Service') {
                    steps {
                        timeout(time: 1, unit: 'HOURS') {
                            waitForQualityGate abortPipeline: true
                        }
                    }
                }
                stage('Instruments Service') {
                    steps {
                        timeout(time: 1, unit: 'HOURS') {
                            waitForQualityGate abortPipeline: true
                        }
                    }
                }
                stage('Orders Service') {
                    steps {
                        timeout(time: 1, unit: 'HOURS') {
                            waitForQualityGate abortPipeline: true
                        }
                    }
                }
                stage('Positions Service') {
                    steps {
                        timeout(time: 1, unit: 'HOURS') {
                            waitForQualityGate abortPipeline: true
                        }
                    }
                }
                stage('Trade Executor') {
                    steps {
                        timeout(time: 1, unit: 'HOURS') {
                            waitForQualityGate abortPipeline: true
                        }
                    }
                }
            }
        }

    }
}
