pipeline{
    agent any
    
    triggers{
        pollSCM('* * * * *')
    }

    tools{
        maven 'maven-3'
        jdk 'java11'
    }

    stages{
        stage('maven-clean'){
            steps{
                script{
                    last_started=env.STAGE_NAME
                }
                bat 'mvn clean'
            }
        }
         stage('maven-compile'){
            steps{
                script{
                    last_started=env.STAGE_NAME
                }
                bat 'mvn compile'
            }
        }
        stage('maven-test'){
            steps{
                script{
                    last_started=env.STAGE_NAME
                }
                bat 'mvn test'
            }
        }
        stage('maven-package'){
            steps{
                script{
                    last_started=env.STAGE_NAME
                }
                bat 'mvn package'
            }
        }

        stage('sonar-analysis'){
            steps{
                script{
                    last_started=env.STAGE_NAME
                }
                withSonarQubeEnv('sonarqube4'){
                    bat 'mvn sonar:sonar'
                }
            }
        }
        stage("quality-gate") {
            steps {
                script{
                    last_started=env.STAGE_NAME
                }
              timeout(time: 1, unit: 'HOURS') {
                waitForQualityGate abortPipeline: true
              }
            }
          }
        stage ('deploy to artifactory'){
            steps{
                script{
                    last_started=env.STAGE_NAME
                }
                rtUpload(
                    serverId: 'artifactoryserver',
                    spec: '''{
                        "files":[
                            {
                              "pattern": "target/*.jar",
                                "target": "art-doc-dev-local1"
                             }
                        ]
                    }''',
                )
            }
        } 
        stage ('download to artifacts folder'){
            steps{
                script{
                    last_started=env.STAGE_NAME
                }
                rtDownload(
                    serverId: 'artifactoryserver',
                    spec: '''{
                        "files":[
                            {
                              "pattern": "art-doc-dev-local1/",
                                 "target": "artifacts/"
                             }
                        ]
                    }''',
                )
            }
        }

        stage('aws deployment'){
            steps{
                script{
                    last_started=env.STAGE_NAME
                }
                sshagent(['f17813c1-8ef1-4fee-960e-74e7c6f7efcb']){
                    bat 'scp -r C:/Windows/System32/config/systemprofile/AppData/Local/Jenkins/.jenkins/workspace/ContinuousIntegrationPipeline/artifacts/*.jar ubuntu@3.18.221.13:/home/ubuntu/artifacts'
        }
            }
        } 

    }
        post{
            success{
                mail bcc: '', body: "<b>Pipeline Success</b><br>Project: ${env.JOB_NAME} <br>Build Number: ${env.BUILD_NUMBER} <br>Last Stage Completed: $last_started <br> URL: ${env.BUILD_URL}", cc: '', charset: 'UTF-8', from: '', mimeType: 'text/html', replyTo: '', subject: "Pipeline Information System: ${env.JOB_NAME}", to: "samalatib96@gmail.com";
            }
            failure{
                mail bcc: '', body: "<b>Pipeline Failure</b><br>Project: ${env.JOB_NAME} <br>Build Number: ${env.BUILD_NUMBER} <br>Stage Name: $last_started <br> URL: ${env.BUILD_URL}", cc: '', charset: 'UTF-8', from: '', mimeType: 'text/html', replyTo: '', subject: "Pipeline Information System: ${env.JOB_NAME}", to: "samalatib96@gmail.com";
            }
        }



    }
