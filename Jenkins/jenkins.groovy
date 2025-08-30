pipeline {
    agent { label 'local' }

    parameters {
        choice(name: 'OS', choices: ['linux', 'apple', 'windows'], description: 'Pick OS')
        choice(name: 'ARCH', choices: ['amd64', 'arm'], description: 'Pick ARCH')
        booleanParam(name: 'SKIP_TESTS', defaultValue: false, description: 'Skip running tests')
        booleanParam(name: 'SKIP_LINT', defaultValue: false, description: 'Skip running linter')
    }

    environment {
        REPO = 'https://github.com/dkedrovskyi/tbot.git'
        BRANCH = 'main'
    }

    stages {

        stage('clone') {
            steps {
                echo 'Clone Repository'
                git branch: "${BRANCH}", url: "${REPO}"
            }
        }

        stage('lint') {
            when {
                expression { return !params.SKIP_LINT }
            }
            steps {
                echo 'Running linter'
                sh 'make lint'
            }
        }

        stage('test') {
            when {
                expression { return !params.SKIP_TESTS }
            }
            steps {
                echo 'Testing started'
                sh 'make test'
            }
        }

        stage('build') {
            steps {
                echo "Building binary for platform ${params.OS} on ${params.ARCH} started"
                sh "make ${params.OS} ${params.ARCH}"
            }
        }

        stage('image') {
            steps {
                echo "Building image for platform ${params.OS} on ${params.ARCH} started"
                sh "make image ${params.OS} ${params.ARCH}"
            }
        }

        stage('docker-login') {
            steps {
                sh "echo $GITHUB_TOKEN | docker login ghcr.io -u $GITHUB_USER --password-stdin"
            }
        }

        stage('push image') {
            steps {
                sh "make ${params.OS} ${params.ARCH} image push"
            }
        }

    }

    post {
    always {
        script {
            sh 'docker logout'
        }
    }
    }
}
