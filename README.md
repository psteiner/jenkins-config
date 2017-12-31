# jenkins-config

Jenkins config as code for local development.

### How to build and run

```
docker build -t cdongsi/jenkins:latest .

docker run -p 8080:8080 -p 50000:50000 -v /data/mydata:/var/jenkins_home \
-v /Users/tdongsi/Mycode:/var/jenkins_home/code cdongsi/jenkins:latest
```

**Reference**:

* [Base Docker image](https://hub.docker.com/r/jenkins/jenkins/)
* [Usage instruction](https://hub.docker.com/_/jenkins/). Note that the image is deprecated.

### Installing plugins

Construct the list of plugins and versions you want to use in the file `plugins.txt`.
It is recommended to keep the plugin in alphabetical order for easy record keeping and code review.
For an existing Jenkins instance, you can obtain its list of installed plugins by running the following Groovy script in its Script Console (accessed via `Manage Jenkins` > `Script Console`):

``` groovy Get list of installed plugins
Jenkins.instance.pluginManager.plugins.sort { it.shortName }.each {
  plugin -> 
    println "${plugin.shortName}:${plugin.version}"
}
```

Other methods to get similar list can be found in [this Stackoverflow thread](https://stackoverflow.com/questions/9815273/how-to-get-a-list-of-installed-jenkins-plugins-with-name-and-version-pair).
The `install-plugins.sh` script and supporting files are already in latest Jenkins's Docker image.
Otherwise, they can be obtained from [this Github](https://github.com/jenkinsci/docker).

#### Kubernetes plugin

For Kubernetes plugin, we have to add provisioning flags, based on [its recommendation](https://github.com/jenkinsci/kubernetes-plugin#over-provisioning-flags).
By default, Jenkins spawns agents conservatively. 
Say, if there are 2 builds in queue, it won't spawn 2 executors immediately. 
It will spawn one executor and wait for sometime for the first executor to be freed before deciding to spawn the second executor.
If you want to override this behaviour and spawn an executor for each build in queue immediately without waiting, you can use these flags during Jenkins startup:

```
-Dhudson.slaves.NodeProvisioner.initialDelay=0
-Dhudson.slaves.NodeProvisioner.MARGIN=50
-Dhudson.slaves.NodeProvisioner.MARGIN0=0.85
```

See [here](https://support.cloudbees.com/hc/en-us/articles/115000060512-New-agents-are-not-being-provisioned-for-my-jobs-in-the-queue-when-I-have-agents-that-are-suspended) for meaning of the provision flags above.

### Local Kubernetes

#### Installing Minikube
Install `kubectl` and `minikube`, both from [kubernetes on Github](https://github.com/kubernetes/).

```plain Install minikube
tdongsi$ curl -Lo minikube https://storage.googleapis.com/minikube/releases/v0.24.1/minikube-darwin-amd64 &&\
 chmod +x minikube && mv minikube /usr/local/bin/
  % Total    % Received % Xferd  Average Speed   Time    Time     Time  Current
                                 Dload  Upload   Total   Spent    Left  Speed
100 39.3M  100 39.3M    0     0  5401k      0  0:00:07  0:00:07 --:--:-- 7069k

tdongsi$ minikube start
Starting local Kubernetes v1.8.0 cluster...
Starting VM...
Downloading Minikube ISO
 140.01 MB / 140.01 MB [============================================] 100.00% 0s
Getting VM IP address...
Moving files into cluster...
Downloading localkube binary
 148.25 MB / 148.25 MB [============================================] 100.00% 0s
 0 B / 65 B [----------------------------------------------------------]   0.00%
 65 B / 65 B [======================================================] 100.00% 0sSetting up certs...
Connecting to cluster...
Setting up kubeconfig...
Starting cluster components...
Kubectl is now configured to use the cluster.
Loading cached images from config file.

tdongsi$ kubectl version
Client Version: version.Info{Major:"1", Minor:"5", GitVersion:"v1.5.4", GitCommit:"7243c69eb523aa4377bce883e7c0dd76b84709a1", 
GitTreeState:"clean", BuildDate:"2017-03-07T23:53:09Z", GoVersion:"go1.7.4", Compiler:"gc", Platform:"darwin/amd64"}
Server Version: version.Info{Major:"1", Minor:"8", GitVersion:"v1.8.0", GitCommit:"0b9efaeb34a2fc51ff8e4d34ad9bc6375459c4a4", 
GitTreeState:"clean", BuildDate:"2017-11-29T22:43:34Z", GoVersion:"go1.9.1", Compiler:"gc", Platform:"linux/amd64"}
```

##### Troubleshooting: Minikube and VPN

If you are connected to corporate VPN, you might have problem with starting Minikube.

```
tdongsi$ minikube start --disk-size=50g --kubernetes-version=v1.8.0
Starting local Kubernetes v1.8.0 cluster...
Starting VM...
Downloading Minikube ISO
 140.01 MB / 140.01 MB [============================================] 100.00% 0s

^C
```

I have attempted different approaches for this issue, but none are consistently working.

1. Use OpenConnect for VPN access rather than Cisco's AnyConnect client.
1. Set port forwarding for the minikube VM to forward port 8443 on 127.0.0.1 to port 8443 in the VM.
1. Use `--host-only-cidr` option in `minikube start`.

In addition, [this pull request](https://github.com/kubernetes/minikube/pull/1329) supposedly fixes the issue, in v0.19.0 release.

#### Tips and Tricks

Some good-to-know commands for minikube:

```text
# This will get k8s dashboard URL
tdongsi$ minikube dashboard --url
http://192.168.99.100:30000

tdongsi$ minikube service <your_service> --url

# This will allow minikube to reuse local Docker image without uploading
tdongsi$ eval $(minikube docker-env)
# The Docker client/context is now switched to minikube's Docker daemon
tdongsi$ docker images
REPOSITORY                                             TAG                 IMAGE ID            CREATED             SIZE
gcr.io/google_containers/kubernetes-dashboard-amd64    v1.8.0              55dbc28356f2        4 weeks ago         119MB
gcr.io/k8s-minikube/storage-provisioner                v1.8.0              4689081edb10        7 weeks ago         80.8MB
gcr.io/k8s-minikube/storage-provisioner                v1.8.1              4689081edb10        7 weeks ago         80.8MB
gcr.io/google_containers/k8s-dns-sidecar-amd64         1.14.5              fed89e8b4248        3 months ago        41.8MB
gcr.io/google_containers/k8s-dns-kube-dns-amd64        1.14.5              512cd7425a73        3 months ago        49.4MB
gcr.io/google_containers/k8s-dns-dnsmasq-nanny-amd64   1.14.5              459944ce8cc4        3 months ago        41.4MB
gcr.io/google_containers/kubernetes-dashboard-amd64    v1.6.3              691a82db1ecd        5 months ago        139MB
gcr.io/google-containers/kube-addon-manager            v6.4-beta.2         0a951668696f        6 months ago        79.2MB
gcr.io/google_containers/pause-amd64                   3.0                 99e59f495ffa        20 months ago       747kB
```

If you have problems with minikube’s Docker daemon building your images, you can also copy the image from your local daemon into minikube like this: 
`docker save <image> | minikube ssh docker load` 
(currently not working due to [this bug](https://github.com/kubernetes/minikube/issues/1957)).

The alternative is to save and load manually:

```text
tdongsi$ docker save myjenkins:0.1 -o myjenkins.tar

tdongsi$ scp -i $(minikube ssh-key) myjenkins.tar docker@$(minikube ip):/home/docker

tdongsi$ minikube ssh "docker load --input /home/docker/myjenkins.tar"
a85f35566a26: Loading layer [==================================================>]  196.8MB/196.8MB
...
```

#### Deploy Jenkins

Use the pre-defined YAML file.

```text
tdongsi$ kubectl create -f k8s/jenkins.yaml
deployment "jenkins" created
service "jenkins" created
tdongsi$ kubectl get pods
NAME                       READY     STATUS    RESTARTS   AGE
jenkins-7874759567-5pbwv   1/1       Running   0          2s

tdongsi$ minikube service jenkins --url
http://192.168.99.100:30080
```

Note that the volumes are specifically defined in `jenkins.yaml` to map `JENKINS_HOME` to `/data/mydata`
and `JENKINS_HOME/code` to `/Users/tdongsi/Mycode`.
Such mapping is not coincidental and one must know before modifying to fit his setup.
Specifically, `/data/...` is chosen for `JENKINS_HOME` since it is one of few [persistent folders in Minikube](https://kubernetes.io/docs/getting-started-guides/minikube/#persistent-volumes). 
`/Users/tdongsi/...` is chosen since it is the [only mounted host folder for OSX](https://kubernetes.io/docs/getting-started-guides/minikube/#mounted-host-folders).
These are not configurable at the moment and different for the driver and OS you are using.

**Troubleshooting**:

You may get the following errors:

```text
tdongsi$ kubectl get pods
NAME                       READY     STATUS             RESTARTS   AGE
jenkins-7874759567-jqkww   0/1       CrashLoopBackOff   1          23s

tdongsi$ kubectl logs jenkins-7874759567-jqkww
touch: cannot touch ‘/var/jenkins_home/copy_reference_file.log’: Permission denied
Can not write to /var/jenkins_home/copy_reference_file.log. Wrong volume permissions?
```

If your hostPath is `/your/home`, it will store the jenkins data in `/your/home` on the host. 
Ensure that `/your/home` is accessible by the `jenkins` user in container (uid 1000) or use `-u some_other_user` parameter with `docker run`. 
To fix it, you must set the correct permissions in the host before you mount volumes.

```text
tdongsi$ minikube ssh sudo chown 1000 /data/mydata
```

Note that since `JENKINS_HOME` is intentionally persistent in the default setup, remember to clear the `/data` folder or 
change volume mapping when working on Docker image.
