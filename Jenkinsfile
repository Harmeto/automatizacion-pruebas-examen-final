// =============================================================================
// PIPELINE DE INTEGRACIÓN CONTINUA Y DESPLIEGUE — declaración para Jenkins
//
// Equivalente al pipeline definido en .github/workflows. Se versiona junto al
// código, que es el punto de "pipeline as code": la definición del proceso
// cambia por los mismos canales que el software, queda en el historial y se
// puede revisar y revertir igual que cualquier otro archivo.
//
// Se mantiene deliberadamente en paralelo al de GitHub Actions para mostrar
// que el proceso no depende de la herramienta: las mismas etapas, los mismos
// gates y los mismos scripts.
// =============================================================================

pipeline {

    // Agente con Docker disponible: cada ejecución parte de un entorno limpio,
    // de modo que un build no puede contaminar al siguiente.
    agent {
        label 'docker'
    }

    options {
        timeout(time: 30, unit: 'MINUTES')
        buildDiscarder(logRotator(numToKeepStr: '20'))
        timestamps()
    }

    parameters {
        string(
            name: 'VERSION',
            defaultValue: '1.1.0',
            description: 'Versión a desplegar en el slot inactivo'
        )
        booleanParam(
            name: 'DESPLEGAR',
            defaultValue: false,
            description: 'Ejecutar además el deployment pipeline Blue-Green'
        )
    }

    environment {
        JAVA_HOME = tool name: 'jdk-21', type: 'jdk'
        PATH = "${JAVA_HOME}/bin:${env.PATH}"
    }

    stages {

        // =====================================================================
        // ETAPA DE COMMIT
        // Las validaciones más rápidas primero. Si algo falla aquí, el pipeline
        // se detiene antes de gastar tiempo en etapas costosas.
        // =====================================================================

        stage('Checkout') {
            steps {
                checkout scm
                script {
                    env.COMMIT_SHA = sh(
                        script: 'git rev-parse --short HEAD',
                        returnStdout: true
                    ).trim()
                }
                echo "Construyendo el commit ${env.COMMIT_SHA}"
            }
        }

        stage('Build') {
            steps {
                // Compilación sin pruebas: verifica que el código esté sano
                // antes de invertir en ejecutarlas.
                sh './mvnw -B compile'
            }
        }

        stage('Análisis estático') {
            steps {
                // Ocupa el lugar que tendrían SonarQube o Snyk en un entorno
                // corporativo. Criterio de bloqueo: cualquier hallazgo detiene
                // el pipeline.
                sh './mvnw -B spotbugs:check'
            }
            post {
                always {
                    archiveArtifacts artifacts: 'target/spotbugsXml.xml',
                                     allowEmptyArchive: true
                }
            }
        }

        stage('Pruebas unitarias') {
            steps {
                // Incluye el gate de cobertura configurado en el pom: si cae
                // bajo el 80 %, este stage falla.
                sh './mvnw -B test'
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/*.xml',
                          allowEmptyResults: false
                    publishHTML(target: [
                        reportDir:  'target/site/jacoco',
                        reportFiles: 'index.html',
                        reportName: 'Cobertura de código',
                        keepAll: true,
                        alwaysLinkToLastBuild: true,
                        allowMissing: true
                    ])
                }
            }
        }

        // =====================================================================
        // PRUEBAS DE INTEGRACIÓN
        // Levantan el contexto y la base de datos: más lentas, por eso van
        // después de que lo barato ya pasó.
        // =====================================================================

        stage('Pruebas de integración') {
            steps {
                sh './mvnw -B verify'
            }
            post {
                always {
                    junit testResults: 'target/failsafe-reports/*.xml',
                          allowEmptyResults: true
                }
            }
        }

        stage('Empaquetado') {
            steps {
                sh './mvnw -B package -DskipTests -Djacoco.skip=true'
                // El artefacto se guarda versionado: es la pieza concreta que
                // se desplegará y a la que se puede volver ante un fallo.
                archiveArtifacts artifacts: 'target/gestor-tareas-*.jar',
                                 fingerprint: true
            }
        }

        // =====================================================================
        // DEPLOYMENT PIPELINE
        // Solo bajo petición explícita: levanta contenedores y un navegador.
        // =====================================================================

        stage('Despliegue Blue-Green') {
            when {
                expression { return params.DESPLEGAR }
            }
            steps {
                sh './scripts/levantar-ambiente.sh 1.0.0'
                sh './scripts/estado.sh'

                // Este comando contiene el Acceptance Test Gate: si los
                // escenarios BDD no pasan, sale con error, el tráfico no se
                // promueve y el stage falla.
                sh "./scripts/desplegar.sh ${params.VERSION}"

                sh './scripts/estado.sh'
            }
            post {
                always {
                    archiveArtifacts artifacts: 'docs/evidencias/**,target/cucumber-reports/**',
                                     allowEmptyArchive: true
                }
                failure {
                    // Si el despliegue falló después de promover, se revierte.
                    // Cuando el rechazo vino del gate, el tráfico nunca se
                    // movió y rollback.sh lo detecta por sí mismo.
                    sh './scripts/rollback.sh "el stage de despliegue falló en Jenkins" || true'
                    sh 'docker compose logs --tail=80 || true'
                }
                cleanup {
                    sh 'docker compose down -v || true'
                }
            }
        }
    }

    post {
        success {
            echo "Pipeline correcto para el commit ${env.COMMIT_SHA}"
        }
        failure {
            // En un entorno real, aquí iría la notificación al canal del
            // equipo y al autor del commit.
            echo "Pipeline fallido para el commit ${env.COMMIT_SHA}"
        }
        cleanup {
            cleanWs()
        }
    }
}
