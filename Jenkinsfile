pipeline {
    agent any
    tools {
        jdk 'JDK21'
        maven 'maven'
    }
    environment {
        npm_config_cache = "${WORKSPACE}/.npm-cache"
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

        // One service at a time: five Spring Boot test JVMs plus Testcontainers at once runs the box out of memory.
        stage('Test: Accounts Service') {
            steps {
                sh 'mvn -B -f app/accounts-service/pom.xml test'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'app/accounts-service/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Test: Instruments Service') {
            steps {
                sh 'mvn -B -f app/instruments-service/pom.xml test'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'app/instruments-service/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Test: Orders Service') {
            steps {
                sh 'mvn -B -f app/orders-service/pom.xml test'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'app/orders-service/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Test: Positions Service') {
            steps {
                sh 'mvn -B -f app/positions-service/pom.xml test'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'app/positions-service/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Test: Trade Executor') {
            steps {
                sh 'mvn -B -f app/trade-executor/pom.xml test'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'app/trade-executor/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Prepare Environment') {
            steps {
                sh '''
                    DB_PASSWORD=$(openssl rand -hex 16)
                    JWT_SECRET=$(openssl rand -hex 32)
                    for s in accounts-service auth-service instruments-service orders-service positions-service trade-executor trade-analytics-service; do
                        if [ ! -f app/$s/.env ]; then
                            sed -e "s/ENTER_PASSWORD_HERE/$DB_PASSWORD/" -e "s/your-generated-secret-token-here/$JWT_SECRET/" \
                                app/$s/.env.example > app/$s/.env
                        fi
                    done
                '''
            }
        }

        stage('Auth Database') {
            steps {
                sh 'docker-compose up -d --wait auth-db'
            }
        }

        stage('Auth Service Tests') {
            agent {
                docker {
                    image 'node:24'
                    args '--network host'
                    reuseNode true
                }
            }
            steps {
                dir('app/auth-service') {
                    sh 'node -v'
                    sh 'npm ci'
                    sh 'npm run test:cov'
                    sh 'npm run test:e2e'
                }
            }
        }

        stage('Frontend') {
            agent {
                docker {
                    image 'node:24'
                    reuseNode true
                }
            }
            steps {
                dir('app/frontend') {
                    sh 'node -v'
                    sh 'npm ci'
                    sh 'npm run lint'
                    sh 'npm test -- --watch=false --coverage'
                    sh 'npm run build'
                }
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
                    junit allowEmptyResults: true, testResults: 'app/instruments-service/db/tests/reports/*.xml, app/trade-analytics-service/tests/reports/*.xml'
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

        // One stage per service, in order: waitForQualityGate checks the build's most recent analysis, so each gate must run right after its own scan.
        stage('Sonar: Accounts Service') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh 'mvn -B -f app/accounts-service/pom.xml sonar:sonar -Dsonar.projectKey=theunderfrogs-accounts-service -Dsonar.projectName="Accounts Service"'
                }
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Sonar: Instruments Service') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh 'mvn -B -f app/instruments-service/pom.xml sonar:sonar -Dsonar.projectKey=theunderfrogs-instruments-service -Dsonar.projectName="Instruments Service"'
                }
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Sonar: Orders Service') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh 'mvn -B -f app/orders-service/pom.xml sonar:sonar -Dsonar.projectKey=theunderfrogs-orders-service -Dsonar.projectName="Orders Service"'
                }
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Sonar: Positions Service') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh 'mvn -B -f app/positions-service/pom.xml sonar:sonar -Dsonar.projectKey=theunderfrogs-positions-service -Dsonar.projectName="Positions Service"'
                }
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Sonar: Trade Executor') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh 'mvn -B -f app/trade-executor/pom.xml sonar:sonar -Dsonar.projectKey=theunderfrogs-trade-executor -Dsonar.projectName="Trade Executor"'
                }
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

    }
    post {
        always {
            sh 'docker-compose down --volumes --remove-orphans || true'
        }
    }
}
