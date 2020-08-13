#!groovy
/**
 * Jenkins pipeline to build Business Networks Extensions
 */

/**
 * Kill already started job.
 * Assume new commit takes precendence and results from previous
 * unfinished builds are not required.
 * This feature doesn't play well with disableConcurrentBuilds() option
 */
@Library('existing-build-control')
import static com.r3.build.BuildControl.killAllExistingBuildsForJob

killAllExistingBuildsForJob(env.JOB_NAME, env.BUILD_NUMBER.toInteger())

/**
 * Sense environment
 */
boolean isReleaseBranch = (env.BRANCH_NAME =~ /^releases\/.*/)
boolean isRelease = (env.TAG_NAME =~ /^release_.*/)

pipeline {
    agent { label 'standard' }

    parameters {
        booleanParam defaultValue: (isReleaseBranch || isRelease), description: 'Publish artifacts to Artifactory?', name: 'DO_PUBLISH'
    }

    options { timestamps() }

    environment {
        ARTIFACTORY_CREDENTIALS = credentials('artifactory-credentials')
        CORDA_ARTIFACTORY_USERNAME = "${env.ARTIFACTORY_CREDENTIALS_USR}"
        CORDA_ARTIFACTORY_PASSWORD = "${env.ARTIFACTORY_CREDENTIALS_PSW}"
        ARTIFACTORY_BUILD_NAME = "DisasterRecovery/Jenkins/${!isRelease?"snapshot/":""}${env.BRANCH_NAME}".replaceAll("/", " :: ")
        PUBLISH_REPO = "${!isRelease?"r3-corda-releases":"r3-corda-dev"}"
        EXECUTOR_NUMBER = "${env.EXECUTOR_NUMBER}"
    }

    stages {
        stage('Detekt') {
            steps {
                sh "./gradlew clean detekt --info"
            }
        }
        stage('Unit Tests') {
            steps {
                sh "./gradlew clean test --info"
            }
        }
        stage('Integration Tests') {
            steps {
                sh "./gradlew clean integrationTest --info"
            }
        }
        stage('Publish to Artifactory') {
            when {
                expression { params.DO_PUBLISH }
                beforeAgent true
            }
            steps {
                rtServer(
                        id: 'R3-Artifactory',
                        url: 'https://software.r3.com/artifactory',
                        credentialsId: 'artifactory-credentials'
                )
                rtGradleDeployer(
                        id: 'deployer',
                        serverId: 'R3-Artifactory',
                        repo: env.PUBLISH_REPO
                )
                rtGradleRun(
                        usesPlugin: true,
                        useWrapper: true,
                        switches: '-s --info',
                        tasks: 'artifactoryPublish',
                        deployerId: 'deployer',
                        buildName: env.ARTIFACTORY_BUILD_NAME
                )
                rtPublishBuildInfo(
                        serverId: 'R3-Artifactory',
                        buildName: env.ARTIFACTORY_BUILD_NAME
                )
            }
        }
    }

    post {
        always {
            junit '**/build/test-results/**/*.xml'
        }
        cleanup {
            deleteDir() /* clean up our workspace */
        }
    }
}