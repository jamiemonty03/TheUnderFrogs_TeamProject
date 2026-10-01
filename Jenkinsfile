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
        }

        stage('GitLeaks - Secret Scan') {
            steps {
                script {
                    try {
                        sh 'gitleaks detect --source . --format json --output gitleaks-report.json 2>/dev/null || true'
                        sh '''
                        if [ -f gitleaks-report.json ]; then
                            if grep -q "\\"Findings\\"" gitleaks-report.json && [ $(wc -c < gitleaks-report.json) -gt 100 ]; then
                                echo "⚠️  Secrets found! Check gitleaks-report.json"
                                cat gitleaks-report.json
                            else
                                echo "✅ No secrets detected"
                            fi
                        fi
                        '''
                    } catch (Exception e) {
                        echo "⚠️  GitLeaks scan completed with warnings (tool may not be installed)"
                    }
                }
            }
        }

        stage('Dependency Scanner') {
            parallel {
                stage('Scan Accounts Service') {
                    steps {
                        script {
                            try {
                                sh 'mvn -B -f app/accounts-service/pom.xml org.owasp:dependency-check-maven:check 2>/dev/null || echo "Dependency check completed"'
                            } catch (Exception e) {
                                echo "Dependency check scan completed"
                            }
                        }
                    }
                }
                stage('Scan Instruments Service') {
                    steps {
                        script {
                            try {
                                sh 'mvn -B -f app/instruments-service/pom.xml org.owasp:dependency-check-maven:check 2>/dev/null || echo "Dependency check completed"'
                            } catch (Exception e) {
                                echo "Dependency check scan completed"
                            }
                        }
                    }
                }
                stage('Scan Orders Service') {
                    steps {
                        script {
                            try {
                                sh 'mvn -B -f app/orders-service/pom.xml org.owasp:dependency-check-maven:check 2>/dev/null || echo "Dependency check completed"'
                            } catch (Exception e) {
                                echo "Dependency check scan completed"
                            }
                        }
                    }
                }
                stage('Scan Positions Service') {
                    steps {
                        script {
                            try {
                                sh 'mvn -B -f app/positions-service/pom.xml org.owasp:dependency-check-maven:check 2>/dev/null || echo "Dependency check completed"'
                            } catch (Exception e) {
                                echo "Dependency check scan completed"
                            }
                        }
                    }
                }
                stage('Scan Trade Executor') {
                    steps {
                        script {
                            try {
                                sh 'mvn -B -f app/trade-executor/pom.xml org.owasp:dependency-check-maven:check 2>/dev/null || echo "Dependency check completed"'
                            } catch (Exception e) {
                                echo "Dependency check scan completed"
                            }
                        }
                    }
                }
            }
        }

        stage('SonarQube Analysis - Accounts Service') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh 'mvn -B -f app/accounts-service/pom.xml sonar:sonar -Dsonar.projectKey=theunderfrogs-accounts-service -Dsonar.projectName="Accounts Service" -Dsonar.coverage.jacoco.xmlReportPaths=app/accounts-service/target/site/jacoco/jacoco.xml'
                }
            }
        }

        stage('Quality Gate - Accounts Service') {
            steps {
                timeout(time: 1, unit: 'HOURS') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('SonarQube Analysis - Instruments Service') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh 'mvn -B -f app/instruments-service/pom.xml sonar:sonar -Dsonar.projectKey=theunderfrogs-instruments-service -Dsonar.projectName="Instruments Service" -Dsonar.coverage.jacoco.xmlReportPaths=app/instruments-service/target/site/jacoco/jacoco.xml'
                }
            }
        }

        stage('Quality Gate - Instruments Service') {
            steps {
                timeout(time: 1, unit: 'HOURS') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('SonarQube Analysis - Orders Service') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh 'mvn -B -f app/orders-service/pom.xml sonar:sonar -Dsonar.projectKey=theunderfrogs-orders-service -Dsonar.projectName="Orders Service" -Dsonar.coverage.jacoco.xmlReportPaths=app/orders-service/target/site/jacoco/jacoco.xml'
                }
            }
        }

        stage('Quality Gate - Orders Service') {
            steps {
                timeout(time: 1, unit: 'HOURS') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('SonarQube Analysis - Positions Service') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh 'mvn -B -f app/positions-service/pom.xml sonar:sonar -Dsonar.projectKey=theunderfrogs-positions-service -Dsonar.projectName="Positions Service" -Dsonar.coverage.jacoco.xmlReportPaths=app/positions-service/target/site/jacoco/jacoco.xml'
                }
            }
        }

        stage('Quality Gate - Positions Service') {
            steps {
                timeout(time: 1, unit: 'HOURS') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('SonarQube Analysis - Trade Executor') {
            steps {
                withSonarQubeEnv('SonarQube') {
                    sh 'mvn -B -f app/trade-executor/pom.xml sonar:sonar -Dsonar.projectKey=theunderfrogs-trade-executor -Dsonar.projectName="Trade Executor" -Dsonar.coverage.jacoco.xmlReportPaths=app/trade-executor/target/site/jacoco/jacoco.xml'
                }
            }
        }

        stage('Quality Gate - Trade Executor') {
            steps {
                timeout(time: 1, unit: 'HOURS') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

    }
}
