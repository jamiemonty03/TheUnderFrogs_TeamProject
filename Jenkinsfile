pipeline {
    agent any
    environment {
        POSTGRES_DB='underfrog'
        POSTGRES_USER='postgres'

        //Password is securely managed via Jenkins Credentials plugin 
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
                sh 'mvn -B clean package -DskipTests'
                sh 'docker build -t team-skeleton:latest .'
            }
        }
        
        stage('Database Validation Test') {
            steps {
                sh '''

                # Clean up any leftover containers from a previous run
                docker-compose down --remove-orphans || true

                # Start services
                docker-compose up -d
                
                # Wait for PostgreSQL to be ready
                for i in {1..30}; do
                    if docker exec underfrog-postgres pg_isready -U ${POSTGRES_USER} > /dev/null 2>&1; then
                        echo "Database is ready"
                        break
                    fi
                    echo "Waiting for database... ($i/30)"
                    sleep 2
                done
                #Validate data presence
                
                docker exec underfrog-postgres psql -U ${POSTGRES_USER} -d ${POSTGRES_DB} -c "
                SELECT table_name FROM information_schema.tables 
                WHERE table_schema = 'public' 
                ORDER BY table_name;
                " > /tmp/db_validation.log

                # Check that all 4 tables exist
                if grep -q "accounts" /tmp/db_validation.log && \
                grep -q "instruments" /tmp/db_validation.log && \
                grep -q "orders" /tmp/db_validation.log && \
                grep -q "positions" /tmp/db_validation.log; then
                    echo "✓ All tables exist"
                else
                    echo "✗ Tables missing"
                    cat /tmp/db_validation.log
                    exit 1
                fi
                '''
            }
            post {
                always {
                    sh 'docker-compose down || true'
                }
            }
        }
    }
}
