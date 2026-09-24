pipeline {
    agent any

    environment {
        // Jenkins Secret Text credentials. These are used only by disposable DB
        // containers in the schema validation stage.
        POSTGRES_USER = credentials('postgres-user')
        POSTGRES_PASSWORD = credentials('postgres-password')
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
                stage('Accounts') {
                    steps {
                        sh 'mvn -B -f app/accounts-service/pom.xml clean package -DskipTests'
                    }
                }
                stage('Instruments') {
                    steps {
                        sh 'mvn -B -f app/instruments-service/pom.xml clean package -DskipTests'
                    }
                }
                stage('Orders') {
                    steps {
                        sh 'mvn -B -f app/orders-service/pom.xml clean package -DskipTests'
                    }
                }
                stage('Positions') {
                    steps {
                        sh 'mvn -B -f app/positions-service/pom.xml clean package -DskipTests'
                    }
                }
            }
        }

        stage('Static Analysis') {
            parallel {
                stage('Analyse Accounts') {
                    steps {
                        sh 'mvn -B -f app/accounts-service/pom.xml checkstyle:check'
                    }
                }
                stage('Analyse Instruments') {
                    steps {
                        sh 'mvn -B -f app/instruments-service/pom.xml checkstyle:check'
                    }
                }
                stage('Analyse Orders') {
                    steps {
                        sh 'mvn -B -f app/orders-service/pom.xml checkstyle:check'
                    }
                }
                stage('Analyse Positions') {
                    steps {
                        sh 'mvn -B -f app/positions-service/pom.xml checkstyle:check'
                    }
                }
            }
        }

        stage('Unit Tests') {
            parallel {
                stage('Test Accounts') {
                    steps {
                        sh 'mvn -B -f app/accounts-service/pom.xml test'
                    }
                }
                stage('Test Instruments') {
                    steps {
                        sh 'mvn -B -f app/instruments-service/pom.xml test'
                    }
                }
                stage('Test Orders') {
                    steps {
                        sh 'mvn -B -f app/orders-service/pom.xml test'
                    }
                }
                stage('Test Positions') {
                    steps {
                        sh 'mvn -B -f app/positions-service/pom.xml test'
                    }
                }
            }
        }

        stage('Build Docker Images') {
            steps {
                sh '''#!/bin/sh
                    set -eu
                    for service in accounts instruments orders positions; do
                        docker build \\
                            --tag "theunderfrogs/${service}-service:${BUILD_NUMBER}" \\
                            "app/${service}-service"
                    done
                '''
            }
        }

        stage('Validate Database Schemas') {
            steps {
                sh '''#!/bin/sh
                    set -eu

                    # Use unique, disposable containers rather than docker-compose:
                    # this stage never touches shared compose containers or volumes.
                    suffix="${BUILD_NUMBER:-local}-$(printf '%s' "${JOB_NAME:-job}" | tr -c 'a-zA-Z0-9_.-' '-')-$$"
                    containers=""
                    cleanup() {
                        for container in $containers; do
                            docker rm -f "$container" >/dev/null 2>&1 || true
                        done
                    }
                    trap cleanup EXIT INT TERM

                    validate_schema() {
                        service="$1"
                        database="${service}_db"
                        expected_tables="$2"
                        container="schema-${service}-${suffix}"
                        schema_dir="$WORKSPACE/app/${service}-service/db/schema"
                        test -d "$schema_dir"

                        docker run --detach --rm \\
                            --name "$container" \\
                            --env POSTGRES_USER="$POSTGRES_USER" \\
                            --env POSTGRES_PASSWORD="$POSTGRES_PASSWORD" \\
                            --env POSTGRES_DB="$database" \\
                            --volume "$schema_dir:/docker-entrypoint-initdb.d:ro" \\
                            postgres:15-alpine >/dev/null
                        containers="$containers $container"

                        ready=false
                        for attempt in $(seq 1 30); do
                            if docker exec "$container" pg_isready -U "$POSTGRES_USER" -d "$database" >/dev/null 2>&1; then
                                ready=true
                                break
                            fi
                            sleep 2
                        done
                        if [ "$ready" != true ]; then
                            docker logs "$container"
                            echo "Database did not become ready: $service" >&2
                            return 1
                        fi

                        for table in $expected_tables; do
                            found=$(docker exec "$container" psql -U "$POSTGRES_USER" -d "$database" -Atc \\
                                "SELECT to_regclass('public.' || '$table') IS NOT NULL")
                            if [ "$found" != t ]; then
                                echo "Missing table '$table' in ${service}_db" >&2
                                return 1
                            fi
                            echo "Validated ${service}_db.$table"
                        done
                    }

                    validate_schema accounts 'accounts'
                    validate_schema instruments 'instruments instruments_metrics raw_prices clean_prices price_metrics raw_stocks raw_etfs raw_bonds clean_stocks clean_etfs clean_bonds'
                    validate_schema orders 'orders client_trades'
                    validate_schema positions 'positions'
                '''
            }
        }
    }

    post {
        always {
            junit allowEmptyResults: true, testResults: 'app/*-service/target/surefire-reports/*.xml'
        }
    }
}
