pipeline {
    agent any
    tools {
        gradle 8.7
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
                sh 'gradle bootJar'
            }
        }
        stage('Create the Docker image of the application') {
            agent any
            steps {
                echo 'Image has been built'
                sh 'docker build -t travula00/devops .'
            }
        }
        stage('Login to DockerHub') {
            steps{
                withCredentials([usernamePassword(credentialsId: 'DockerAuth', passwordVariable: 'DockerAuthPassword', usernameVariable: 'DockerAuthUser')]) {
                    sh "docker login -u ${env.DockerAuthUser} -p ${env.DockerAuthPassword}"
                }
            }
        }
        stage('Push the image to DockerHub') {
            steps {
                sh 'docker push travula00/devops'
                echo 'The image is pushed'
            }
        }
    }
}