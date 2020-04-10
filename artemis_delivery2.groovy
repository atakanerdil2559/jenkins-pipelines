node {
properties(
	[parameters(
		[choice(choices: 
			[
				'0.1', 
				'0.2', 
				'0.3', 
				'0.4', 
				'0.5'], 
		description: 'Which version of the app should I deploy? ', 
		name: 'Version'), 
	choice(choices: 
	[
        
		'dev1.atakanerdil.com', 
		'qa1.atakanerdil.com', 
		'stage1.atakanerdil.com', 
		'prod1.atakanerdil.com'], 
	description: 'Please provide an environment to build the application', 
	name: 'ENVIR')])])


		stage("Stage1"){
			timestamps {
				ws {
                    checkout([$class: 'GitSCM', branches: [[name: '${Version}']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[url: 'https://github.com/farrukh90/artemis.git']]])			}
		}
		stage("Get Credentials"){
			timestamps {
				ws{
					sh '''
						aws ecr get-login-password --region eu-west-2 | docker login --username AWS --password-stdin 337879762040.dkr.ecr.eu-west-2.amazonaws.com/artemis
						'''
				}
			}
		}
		stage("Build Docker Image"){
			timestamps {
				ws {
					sh '''
						docker build -t artemis:${Version} .
						'''
				}
			}
		}
		stage("Tag Image"){
			timestamps {
				ws {
					sh '''
						docker tag artemis:${Version} 337879762040.dkr.ecr.eu-west-2.amazonaws.com/artemis:${Version} 
						'''
				}
			}
		}
		stage("Push Image"){
			timestamps {
				ws {
					sh '''
						docker push 337879762040.dkr.ecr.eu-west-2.amazonaws.com/artemis:${Version}
						'''
				}
			}
		}
		stage("Send slack notifications"){
			timestamps {
				ws {
					echo "Slack"
					//slackSend color: '#BADA55', message: 'Hello, World!'
            }
        }
    
    }
    		stage("Clean Up"){
			timestamps {
				ws {
					try {
						sh '''
							#!/bin/bash
							IMAGES=$(ssh centos@${ENVIR} docker ps -aq) 
							for i in \$IMAGES; do
								ssh centos@${ENVIR} docker stop \$i
								ssh centos@${ENVIR} docker rm \$i
							done 
							'''
					} catch(e) {
						println("Script failed with error: ${e}")
						}
					}
				}
			}

    	stage("Run Container"){
		timestamps {
			ws {
				sh '''
					ssh centos@${ENVIR} docker run -dti -p 5001:5000 337879762040.dkr.ecr.eu-west-2.amazonaws.com/artemis:${Version}
					'''
            }
        }
    }
}