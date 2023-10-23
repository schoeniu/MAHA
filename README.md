<h2>About</h2>

MAHA (messaging architecture-aware horizontal autoscaler) is a horizontal autoscaler specifically designed for messaging
based microservice meshes. It was created in scope of a computer science master thesis.
The autoscaler uses the number of messages in messaging queues as metric for scaling decisions and
considers the relationships of microservices. It was tested with the managed application CUP (car update provider)
and outperforms the native Kubernetes autoscaler in all test case done in regard to message processing speed and cost
efficiency.

<h2>Content</h2>
Below the most relevant high level folders and files of the repository are listed and described
to help with orientation.
<br>

```
cup
+---jmeter
|       TestingPlan.jmx -> Apache JMeter file containing test cases
|       
+---k8s -> Kubernetes deployment files
|           
+---scripts -> utility scripts folder
|   |   deploy-hpa-60.sh -> deploy CPU HPAs
|   |   init.sh -> initialization script for first installation
|   |   test-follow.sh -> test log tail (requires stern to be installed)
|   |   test-reset.sh -> test reset and log tail (requires stern to be installed)
|   |   token.sh -> print token for Kubernets dashboard access
|   |       
|   \---logs -> folder where log tail outputs are placed to
|           
+---services -> Spring Boot service implementations
|   +---cup -> CUP implemenatation
|   |                           
|   \---maha -> MAHA implemenatation
|                                   
\---testing -> testing output related files
        bootstrap_for_ci.ipynb -> confidence interval calculation python script
        bootstrap_for_ci.zip -> confidence interval calculation python script outputs
        LogAnalyzer.java -> analyzer to summarize statistics from static logs created through log tail
        testing results.zip -> test results outputs / log analyzer outputs
```

<h2>Local Setup</h2>
<h3>Prerequisites</h3>
The test cases were done on a machine with Windows 10, AMD Ryzen 5 5600X 6x3.70GHz CPU
and 48GB DDR4-3200Mhz RAM. To Run the minimal setup without scaled CUP services at least 8GB memory need to be
at the cluster node. For running all test cases delivering the test results as stored in the testing directory
40GB memory are required. Having not enough memory will pods to fail deploying with the error
"0/1 nodes are available: 1 insufficient memory".

Furthermore, an internet connection is required on the initial setup for downloading Docker images and maven
dependencies.

<h3>Installation</h3>
To run the project locally and execute a test case, the following steps are necessary:

1. Install Docker Desktop for Windows (tested with v4.14.1 and WSL2
   backend) https://docs.docker.com/desktop/install/windows-install/
2. Enable in the Docker Desktop settings Kubernetes
3. Run the init.sh script from the scripts' folder. This may take some time, since Docker images and maven dependencies
   need to be downloaded.
4. Access https://localhost:30004 through a browser with the token received from running the token.sh script. Verify all
   deployments are ready.
5. Install the log tailing tool stern https://github.com/stern/stern and add it to the PATH environment variable.
6. Install Apache JMeter https://jmeter.apache.org/download_jmeter.cgi and load the TestingPlan.jmx from the jmeter
   folder.
7. Choose the desired scaling strategy to test. Options are:
    1. No scaling: No CPU HPAs are deployed (run undeploy-hpa-60.sh if necessary) and scaling-config.yaml config map in
       CUP namespace is set to scalingEnabled: false
    2. CPU HPAs: CPU HPAs are deployed with deploy-hpa-60.sh and scaling-config.yaml is set to scalingEnabled: false
    3. MAHA without follow-up: No CPU HPAs are deployed and scaling-config.yaml is set to scalingEnabled: true,
       followUpScalingEnabled: false
    4. MAHA with follow-up: No CPU HPAs are deployed and scaling-config.yaml is set to scalingEnabled: true,
       followUpScalingEnabled: true
8. Prepare the test case execution by running test-reset.sh and wait until the log tailing with stern is reached.
9. Run the desired test case from JMeter. You can monitor messaging in ElasticMQ from http://localhost:30009 and
   from the Grafana messaging and scaling dashboard available at http://localhost:30006/dashboards
10. Once all messages are processed, the logtail can be quit with CTRL+C. Log files are available in the scripts/logs
    folder. LogAnalyzer.java may be run if desired to extract statistics from the test case execution.

<h2>HTTP URLs/Ports</h2>

| Service              | NodePort |                     URL | Endpoints                                                                                             |
|----------------------|---------:|------------------------:|-------------------------------------------------------------------------------------------------------|
| Prometheus           |    30000 |  http://localhost:30000 |                                                                                                       |
| Kubernetes dashboard |    30004 | https://localhost:30004 |                                                                                                       |
| Postgres             |    30005 |  http://localhost:30005 |                                                                                                       |
| Grafana              |    30006 |  http://localhost:30006 |                                                                                                       |
| ElasticMQ API        |    30008 | https://localhost:30008 |                                                                                                       |
| ElasticMQ UI         |    30009 | https://localhost:30009 |                                                                                                       |
| MAHA                 |    30070 |  http://localhost:30070 |                                                                                                       |
| ext-request-proxy    |    30071 |  http://localhost:30071 | /request/{vin}                                                                                        |
| cup-trigger          |    30080 |  http://localhost:30080 |                                                                                                       |
| cup-process          |    30081 |  http://localhost:30081 |                                                                                                       |
| cup-cache            |    30082 |  http://localhost:30082 |                                                                                                       |
| cup-vehicle-data     |    30083 |  http://localhost:30083 |                                                                                                       |
| cup-rollout          |    30084 |  http://localhost:30084 |                                                                                                       |
| cup-history          |    30085 |  http://localhost:30085 | /status/raw (GET)<br/>/status/summary (GET)<br/> /status/processing (GET)<br/>/status/delete (DELETE) |
