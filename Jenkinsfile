pipeline {
    agent any

    environment {
        DOCKER_HUB_CREDENTIALS = credentials('DockerAuth')
        DOCKER_IMAGE = "travula00/devops"
        GIT_REPO = "https://github.com/HasanAliOzen/devops-aws.git"
    }

    stages {
        stage('Pull the project form GitHub') {
            steps {
                echo 'Getting the project from GitHub'
                git url: "${env.GIT_REPO}", branch: 'main'
            }
        }

        stage('Building the jar file') {
            steps {
                echo 'Start building the jar'
                sh './gradlew clean bootJar'
            }
        }

        stage('Build Docker Image') {
            steps {
                script {
                    dockerImage = docker.build("${env.DOCKER_IMAGE}:${env.BUILD_ID}")
                }
                echo 'Image has been built'
            }
        }

        stage('Push Docker Image') {
            steps {
                script {
                    docker.withRegistry('https://index.docker.io/v1/', "${env.DOCKER_HUB_CREDENTIALS}") {
                        dockerImage.push()
                        dockerImage.push('latest')
                    }
                }
                echo 'The image is pushed'
            }
        }
    }

    post {
        always {
            cleanWs()
        }
    }
}
