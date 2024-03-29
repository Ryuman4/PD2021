pipeline {
    agent any

    options {
        buildDiscarder(
            logRotator(
                numToKeepStr:'5', // number of build logs to keep
                //daysToKeepStr: '15', // history to keep in days
                //artifactDaysToKeepStr: '15', // artifacts are kept for days
                artifactNumToKeepStr: '5' // number of builds have their artifacts kept
            )
        )
    }

    parameters {
        booleanParam (
            defaultValue: true,
            description: 'run tests',
            name : 'RUN_TESTS')
        booleanParam (
            defaultValue: true,
            description: 'publish images to dockerhub',
            name : 'PUBLISH_IMAGES')
        string (
            defaultValue: 'djocraveiro/pd_2021_pg',
            description: 'dockerhub public repository',
            name : 'DOCKERHUB_REP_DB')
        string (
            defaultValue: 'djocraveiro/pd_2021',
            description: 'dockerhub repository',
            name : 'DOCKERHUB_REP')
        string (
            defaultValue: 'dockerhub',
            description: 'dockerhub credentials',
            name : 'DOCKERHUB_CREDENTIALS')
        string (
            defaultValue: 'df_inventory.yml',
            description: 'ansible inventory',
            name : 'ANSIBLE_INVENTORY')
    }

    environment {
        APP_NAME = "vstore"
        APP_LISTENING_PORT = "8080"
        PG_CONTAINER_NAME = "postgres_vstore_test"
        GIT_COMMIT_REV=''
    }

    stages {
        stage('Build') {
            agent {
                docker {
                    image 'maven:3.8.1-adoptopenjdk-11'
                    args '--network host -v $HOME/.m2:/root/.m2'
                    reuseNode true
                }
            }
            steps {
                echo "=== building ==="
                sh 'mvn -B -DskipTests clean compile --file ./stock/pom.xml'
            }
        }

        stage('Package') {
            agent {
                docker {
                    image 'maven:3.8.1-adoptopenjdk-11'
                    args '--network host -v $HOME/.m2:/root/.m2'
                    reuseNode true
                }
            }
            steps {
                echo "=== packaging project ==="
                sh "mvn package -DskipTests --file ./stock/pom.xml"
                archiveArtifacts artifacts: 'stock/target/*.jar', fingerprint: true
            }
        }

        stage('Build Images') {
            steps {
                echo "=== building images==="
                script {    
                    GIT_COMMIT_REV = sh(returnStdout: true, script: "git log -n 1 --pretty=format:'%h'").trim()
                }
                
                sh "docker build -t ${params.DOCKERHUB_REP_DB}:${GIT_COMMIT_REV} -t ${params.DOCKERHUB_REP_DB}:latest ./docker/postgres"

                sh "docker build -t ${params.DOCKERHUB_REP}:${GIT_COMMIT_REV} -t ${params.DOCKERHUB_REP}:latest ."
            }
        }

        stage('Test Preparation') {
            when {
                expression { params.RUN_TESTS == true }
            }
            steps {
                echo "=== prepare for testing ==="
                sh "docker run --rm --network host --name $PG_CONTAINER_NAME -p 5432:5432 -e POSTGRES_PASSWORD=admin -e POSTGRES_USER=admin -v '$PG_CONTAINER_NAME:/var/lib/postgresql/data' -d ${params.DOCKERHUB_REP_DB}:${GIT_COMMIT_REV}"
            }
        }

        stage('Test') {
            when {
                expression { params.RUN_TESTS == true }
            }
            agent {
                docker {
                    image 'maven:3.8.1-adoptopenjdk-11'
                    args '--network host -v $HOME/.m2:/root/.m2'
                    reuseNode true
                }
            }
            steps {
                echo "=== testing ==="
                sh 'mvn test --file ./stock/pom.xml'
            }
            post {
                always {
                    junit 'stock/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Test Cleaning') {
            when {
                expression { params.RUN_TESTS == true }
            }
            steps {
                echo "=== cleaning test env ==="
                sh 'docker stop $PG_CONTAINER_NAME'
                sh 'docker volume rm $PG_CONTAINER_NAME'
            }
        }

        stage('Push to DockerHub') { 
            when {
                expression { params.PUBLISH_IMAGES == true }
            }
            steps {
                echo "=== push docker postgres image ==="
                withDockerRegistry([ credentialsId: params.DOCKERHUB_CREDENTIALS, url: "" ]) {
                    sh "docker push ${params.DOCKERHUB_REP_DB}:${GIT_COMMIT_REV}"
                    sh "docker tag ${params.DOCKERHUB_REP_DB}:${GIT_COMMIT_REV} ${params.DOCKERHUB_REP_DB}:latest"
                }

                echo "=== build and push docker webapp image ==="
                withDockerRegistry([ credentialsId: params.DOCKERHUB_CREDENTIALS, url: "" ]) {
                    sh "docker push ${params.DOCKERHUB_REP}:${GIT_COMMIT_REV}"
                    sh "docker tag ${params.DOCKERHUB_REP}:${GIT_COMMIT_REV} ${params.DOCKERHUB_REP}:latest"
                }
            }
        }

        stage('Deploy') {
            steps {
                echo "=== deploy ==="
                sh "docker run -i --network host --rm -w /work -v \"\$(pwd)\":/work cicd-ansible:latest ansible-playbook -i ${params.ANSIBLE_INVENTORY} ansible-playbook.yml -e 'DB_IMAGE=${params.DOCKERHUB_REP_DB}:${GIT_COMMIT_REV} WEB_IMAGE=${params.DOCKERHUB_REP}:${GIT_COMMIT_REV}'"
            }
        }
    }

    post {
        always {
            echo 'Lets clean up this mess -.-'
            deleteDir() /* clean up our workspace */
        }
        success {
            echo 'I have succeeded :D'
        }
        unstable {
            echo 'I am unstable :/'
        }
        failure {
            echo 'I have failed :('
        }
        changed {
            echo 'Things were different than before :o'
        }
    }
}