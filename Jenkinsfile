pipeline {
    agent any
    environment {
        DOCKERHUB_CREDENTIALS = credentials('DockerAuth')
    }
    stages {
        stage('Pull the project form GitHub') {
            steps {
                echo 'Getting the project from GitHub'
                git 'https://github.com/HasanAliOzen/devops-aws'
            }
        }
        stage('Building the jar file') {
            steps {
                echo 'Start building the jar'
                sh 'gradle clean bootJar'
            }
        }
        stage('Create the Docker image of the application') {
            steps {
                echo 'Image has been built'
                sh 'docker build -t travula00/devops2 .'
            }
        }
        stage('Login to DockerHub') {
            steps {
                sh 'echo $DOCKERHUB_CREDENTIALS_PSW | docker login -u $DOCKERHUB_CREDENTIALS_USR --password-stdin'
                echo 'Logged in'
            }
        }
        stage('Push the image to DockerHub') {
            steps {
                sh 'docker push travula00/devops2'
                echo 'The image is pushed'
            }
        }
    }
}