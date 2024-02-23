# Accessing Kafka using Skupper

[![main](https://github.com/skupperproject/skupper-example-kafka/actions/workflows/main.yaml/badge.svg)](https://github.com/skupperproject/skupper-example-kafka/actions/workflows/main.yaml)

#### Use public cloud resources to process data from a private Kafka cluster

This example is part of a [suite of examples][examples] showing the
different ways you can use [Skupper][website] to connect services
across cloud providers, data centers, and edge sites.

[website]: https://skupper.io/
[examples]: https://skupper.io/examples/index.html

#### Contents

* [Overview](#overview)
* [Prerequisites](#prerequisites)
* [Step 1: Install the Skupper command-line tool](#step-1-install-the-skupper-command-line-tool)
* [Step 2: Set up your namespaces](#step-2-set-up-your-namespaces)
* [Step 3: Deploy the Kafka cluster](#step-3-deploy-the-kafka-cluster)
* [Step 4: Create your sites](#step-4-create-your-sites)
* [Step 5: Link your sites](#step-5-link-your-sites)
* [Step 6: Expose the Kafka cluster](#step-6-expose-the-kafka-cluster)
* [Step 7: Run the client](#step-7-run-the-client)
* [Cleaning up](#cleaning-up)
* [Summary](#summary)
* [Next steps](#next-steps)
* [About this example](#about-this-example)

## Overview

This example is a simple Kafka application that shows how you can
use Skupper to access a Kafka cluster at a remote site without
exposing it to the public internet.

It contains two services:

* A Kafka cluster named "cluster1" running in a private data center.
  The cluster has a topic named "topic1".

* A Kafka client running in the public cloud.  It sends 10 messages
  to "topic1" and then receives them back.

To set up the Kafka cluster, this example uses the Kubernetes
operator from the [Strimzi][strimzi] project.  The Kafka client is a
Java application built using [Quarkus][quarkus].

The example uses two Kubernetes namespaces, "private" and "public",
to represent the private data center and public cloud.

[strimzi]: https://strimzi.io/
[quarkus]: https://quarkus.io/

## Prerequisites

* The `kubectl` command-line tool, version 1.15 or later
  ([installation guide][install-kubectl])

* Access to at least one Kubernetes cluster, from [any provider you
  choose][kube-providers]

[install-kubectl]: https://kubernetes.io/docs/tasks/tools/install-kubectl/
[kube-providers]: https://skupper.io/start/kubernetes.html

## Step 1: Install the Skupper command-line tool

This example uses the Skupper command-line tool to deploy Skupper.
You need to install the `skupper` command only once for each
development environment.

On Linux or Mac, you can use the install script (inspect it
[here][install-script]) to download and extract the command:

~~~ shell
curl https://skupper.io/install.sh | sh
~~~

The script installs the command under your home directory.  It
prompts you to add the command to your path if necessary.

For Windows and other installation options, see [Installing
Skupper][install-docs].

[install-script]: https://github.com/skupperproject/skupper-website/blob/main/input/install.sh
[install-docs]: https://skupper.io/install/

## Step 2: Set up your namespaces

Skupper is designed for use with multiple Kubernetes namespaces,
usually on different clusters.  The `skupper` and `kubectl`
commands use your [kubeconfig][kubeconfig] and current context to
select the namespace where they operate.

[kubeconfig]: https://kubernetes.io/docs/concepts/configuration/organize-cluster-access-kubeconfig/

Your kubeconfig is stored in a file in your home directory.  The
`skupper` and `kubectl` commands use the `KUBECONFIG` environment
variable to locate it.

A single kubeconfig supports only one active context per user.
Since you will be using multiple contexts at once in this
exercise, you need to create distinct kubeconfigs.

For each namespace, open a new terminal window.  In each terminal,
set the `KUBECONFIG` environment variable to a different path and
log in to your cluster.  Then create the namespace you wish to use
and set the namespace on your current context.

**Note:** The login procedure varies by provider.  See the
documentation for yours:

* [Minikube](https://skupper.io/start/minikube.html#cluster-access)
* [Amazon Elastic Kubernetes Service (EKS)](https://skupper.io/start/eks.html#cluster-access)
* [Azure Kubernetes Service (AKS)](https://skupper.io/start/aks.html#cluster-access)
* [Google Kubernetes Engine (GKE)](https://skupper.io/start/gke.html#cluster-access)
* [IBM Kubernetes Service](https://skupper.io/start/ibmks.html#cluster-access)
* [OpenShift](https://skupper.io/start/openshift.html#cluster-access)

_**Public:**_

~~~ shell
export KUBECONFIG=~/.kube/config-public
# Enter your provider-specific login command
kubectl create namespace public
kubectl config set-context --current --namespace public
~~~

_**Private:**_

~~~ shell
export KUBECONFIG=~/.kube/config-private
# Enter your provider-specific login command
kubectl create namespace private
kubectl config set-context --current --namespace private
~~~

## Step 3: Deploy the Kafka cluster

In Private, use the `kubectl create` and `kubectl apply`
commands with the listed YAML files to install the operator and
deploy the cluster and topic.

_**Private:**_

~~~ shell
kubectl create -f server/strimzi.yaml
kubectl apply -f server/cluster1.yaml
kubectl wait --for condition=ready --timeout 900s kafka/cluster1
~~~

_Sample output:_

~~~ console
$ kubectl create -f server/strimzi.yaml
customresourcedefinition.apiextensions.k8s.io/kafkas.kafka.strimzi.io created
rolebinding.rbac.authorization.k8s.io/strimzi-cluster-operator-entity-operator-delegation created
clusterrolebinding.rbac.authorization.k8s.io/strimzi-cluster-operator created
rolebinding.rbac.authorization.k8s.io/strimzi-cluster-operator-topic-operator-delegation created
customresourcedefinition.apiextensions.k8s.io/kafkausers.kafka.strimzi.io created
customresourcedefinition.apiextensions.k8s.io/kafkarebalances.kafka.strimzi.io created
deployment.apps/strimzi-cluster-operator created
customresourcedefinition.apiextensions.k8s.io/kafkamirrormaker2s.kafka.strimzi.io created
clusterrole.rbac.authorization.k8s.io/strimzi-entity-operator created
clusterrole.rbac.authorization.k8s.io/strimzi-cluster-operator-global created
clusterrolebinding.rbac.authorization.k8s.io/strimzi-cluster-operator-kafka-broker-delegation created
rolebinding.rbac.authorization.k8s.io/strimzi-cluster-operator created
clusterrole.rbac.authorization.k8s.io/strimzi-cluster-operator-namespaced created
clusterrole.rbac.authorization.k8s.io/strimzi-topic-operator created
clusterrolebinding.rbac.authorization.k8s.io/strimzi-cluster-operator-kafka-client-delegation created
clusterrole.rbac.authorization.k8s.io/strimzi-kafka-client created
serviceaccount/strimzi-cluster-operator created
clusterrole.rbac.authorization.k8s.io/strimzi-kafka-broker created
customresourcedefinition.apiextensions.k8s.io/kafkatopics.kafka.strimzi.io created
customresourcedefinition.apiextensions.k8s.io/kafkabridges.kafka.strimzi.io created
customresourcedefinition.apiextensions.k8s.io/kafkaconnectors.kafka.strimzi.io created
customresourcedefinition.apiextensions.k8s.io/kafkaconnects2is.kafka.strimzi.io created
customresourcedefinition.apiextensions.k8s.io/kafkaconnects.kafka.strimzi.io created
customresourcedefinition.apiextensions.k8s.io/kafkamirrormakers.kafka.strimzi.io created
configmap/strimzi-cluster-operator created

$ kubectl apply -f server/cluster1.yaml
kafka.kafka.strimzi.io/cluster1 created
kafkatopic.kafka.strimzi.io/topic1 created

$ kubectl wait --for condition=ready --timeout 900s kafka/cluster1
kafka.kafka.strimzi.io/cluster1 condition met
~~~

**Note:**

By default, the Kafka bootstrap server returns broker addresses
that include the Kubernetes namespace in their domain name.
When, as in this example, the Kafka client is running in a
namespace with a different name from that of the Kafka cluster,
this prevents the client from resolving the Kafka brokers.

To make the Kafka brokers reachable, set the `advertisedHost`
property of each broker to a domain name that the Kafka client
can resolve at the remote site.  In this example, this is
achieved with the following listener configuration:

~~~ yaml
spec:
  kafka:
    listeners:
      - name: plain
        port: 9092
        type: internal
        tls: false
        configuration:
          brokers:
            - broker: 0
              advertisedHost: cluster1-kafka-0.cluster1-kafka-brokers
~~~

See [Advertised addresses for brokers][advertised-addresses] for
more information.

[advertised-addresses]: https://strimzi.io/docs/operators/in-development/configuring.html#property-listener-config-broker-reference

## Step 4: Create your sites

A Skupper _site_ is a location where components of your
application are running.  Sites are linked together to form a
network for your application.  In Kubernetes, a site is associated
with a namespace.

For each namespace, use `skupper init` to create a site.  This
deploys the Skupper router and controller.  Then use `skupper
status` to see the outcome.

**Note:** If you are using Minikube, you need to [start minikube
tunnel][minikube-tunnel] before you run `skupper init`.

[minikube-tunnel]: https://skupper.io/start/minikube.html#running-minikube-tunnel

_**Public:**_

~~~ shell
skupper init
skupper status
~~~

_Sample output:_

~~~ console
$ skupper init
Waiting for LoadBalancer IP or hostname...
Waiting for status...
Skupper is now installed in namespace 'public'.  Use 'skupper status' to get more information.

$ skupper status
Skupper is enabled for namespace "public". It is not connected to any other sites. It has no exposed services.
~~~

_**Private:**_

~~~ shell
skupper init
skupper status
~~~

_Sample output:_

~~~ console
$ skupper init
Waiting for LoadBalancer IP or hostname...
Waiting for status...
Skupper is now installed in namespace 'private'.  Use 'skupper status' to get more information.

$ skupper status
Skupper is enabled for namespace "private". It is not connected to any other sites. It has no exposed services.
~~~

As you move through the steps below, you can use `skupper status` at
any time to check your progress.

## Step 5: Link your sites

A Skupper _link_ is a channel for communication between two sites.
Links serve as a transport for application connections and
requests.

Creating a link requires use of two `skupper` commands in
conjunction, `skupper token create` and `skupper link create`.

The `skupper token create` command generates a secret token that
signifies permission to create a link.  The token also carries the
link details.  Then, in a remote site, The `skupper link
create` command uses the token to create a link to the site
that generated it.

**Note:** The link token is truly a *secret*.  Anyone who has the
token can link to your site.  Make sure that only those you trust
have access to it.

First, use `skupper token create` in site Public to generate the
token.  Then, use `skupper link create` in site Private to link
the sites.

_**Public:**_

~~~ shell
skupper token create ~/secret.token
~~~

_Sample output:_

~~~ console
$ skupper token create ~/secret.token
Token written to ~/secret.token
~~~

_**Private:**_

~~~ shell
skupper link create ~/secret.token
~~~

_Sample output:_

~~~ console
$ skupper link create ~/secret.token
Site configured to link to https://10.105.193.154:8081/ed9c37f6-d78a-11ec-a8c7-04421a4c5042 (name=link1)
Check the status of the link using 'skupper link status'.
~~~

If your terminal sessions are on different machines, you may need
to use `scp` or a similar tool to transfer the token securely.  By
default, tokens expire after a single use or 15 minutes after
creation.

## Step 6: Expose the Kafka cluster

In Private, use `skupper expose` with the `--headless` option to
expose the Kafka cluster as a headless service on the Skupper
network.

Then, in Public, use the `kubectl get service` command to check
that the `cluster1-kafka-brokers` service appears after a
moment.

_**Private:**_

~~~ shell
skupper expose statefulset/cluster1-kafka --headless --port 9092
~~~

_Sample output:_

~~~ console
$ skupper expose statefulset/cluster1-kafka --headless --port 9092
statefulset cluster1-kafka exposed as cluster1-kafka-brokers
~~~

_**Public:**_

~~~ shell
kubectl get service/cluster1-kafka-brokers
~~~

_Sample output:_

~~~ console
$ kubectl get service/cluster1-kafka-brokers
NAME                     TYPE        CLUSTER-IP   EXTERNAL-IP   PORT(S)    AGE
cluster1-kafka-brokers   ClusterIP   None         <none>        9092/TCP   2s
~~~

## Step 7: Run the client

Use the `kubectl run` command to execute the client program in
Public.

_**Public:**_

~~~ shell
kubectl run client --attach --rm --restart Never --image quay.io/skupper/kafka-example-client --env BOOTSTRAP_SERVERS=cluster1-kafka-brokers:9092
~~~

_Sample output:_

~~~ console
$ kubectl run client --attach --rm --restart Never --image quay.io/skupper/kafka-example-client --env BOOTSTRAP_SERVERS=cluster1-kafka-brokers:9092
[...]
Received message 1
Received message 2
Received message 3
Received message 4
Received message 5
Received message 6
Received message 7
Received message 8
Received message 9
Received message 10
Result: OK
[...]
~~~

To see the client code, look in the [client directory](client)
of this project.

## Cleaning up

To remove Skupper and the other resources from this exercise, use
the following commands.

_**Private:**_

~~~ shell
skupper delete
kubectl delete -f server/cluster1.yaml
kubectl delete -f server/strimzi.yaml
~~~

_**Public:**_

~~~ shell
skupper delete
~~~

## Next steps

Check out the other [examples][examples] on the Skupper website.

## About this example

This example was produced using [Skewer][skewer], a library for
documenting and testing Skupper examples.

[skewer]: https://github.com/skupperproject/skewer

Skewer provides utility functions for generating the README and
running the example steps.  Use the `./plano` command in the project
root to see what is available.

To quickly stand up the example using Minikube, try the `./plano demo`
command.
