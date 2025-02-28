<!-- NOTE: This file is generated from skewer.yaml.  Do not edit it directly. -->

# Accessing Kafka using Skupper

[![main](https://github.com/lynnemorrison/skupper-example-kafka/actions/workflows/main.yaml/badge.svg)](https://github.com/lynnemorrison/skupper-example-kafka/actions/workflows/main.yaml)

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
* [Step 2: Access your Kubernetes clusters](#step-2-access-your-kubernetes-clusters)
* [Step 3: Install Skupper on your Kubernetes clusters](#step-3-install-skupper-on-your-kubernetes-clusters)
* [Step 4: Deploy the Kafka cluster](#step-4-deploy-the-kafka-cluster)
* [Step 5: Create your sites](#step-5-create-your-sites)
* [Step 6: Link your sites](#step-6-link-your-sites)
* [Step 7: Expose the Kafka cluster](#step-7-expose-the-kafka-cluster)
* [Step 8: Run the client](#step-8-run-the-client)
* [Step 9: Cleaning Up](#step-9-cleaning-up)
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

* Access to at least one Kubernetes cluster, from [any provider you
  choose][kube-providers].

* The `kubectl` command-line tool, version 1.15 or later
  ([installation guide][install-kubectl]).

* The `skupper` command-line tool, version 2.0 or later.  On Linux
  or Mac, you can use the install script (inspect it
  [here][cli-install-script]) to download and extract the command:

  See [Installing the Skupper CLI][cli-install-docs] for more
  information.

[kube-providers]: https://skupper.io/start/kubernetes.html
[install-kubectl]: https://kubernetes.io/docs/tasks/tools/install-kubectl/
[cli-install-script]: https://github.com/skupperproject/skupper-website/blob/main/input/install.sh
[cli-install-docs]: https://skupper.io/install/

## Step 1: Install the Skupper command-line tool

This example uses the Skupper command-line tool to create Skupper
resources.  You need to install the `skupper` command only once
for each development environment.

On Linux or Mac, you can use the install script (inspect it
[here][install-script]) to download and extract the command:

~~~ shell
curl https://skupper.io/install.sh | sh -s -- --version 2.0.0
~~~

The script installs the command under your home directory.  It
prompts you to add the command to your path if necessary.

For Windows and other installation options, see [Installing
Skupper][install-docs].

[install-script]: https://github.com/skupperproject/skupper-website/blob/main/input/install.sh
[install-docs]: https://skupper.io/install/

## Step 2: Access your Kubernetes clusters

Skupper is designed for use with multiple Kubernetes clusters.
The `skupper` and `kubectl` commands use your
[kubeconfig][kubeconfig] and current context to select the cluster
and namespace where they operate.

[kubeconfig]: https://kubernetes.io/docs/concepts/configuration/organize-cluster-access-kubeconfig/

This example uses multiple cluster contexts at once. The
`KUBECONFIG` environment variable tells `skupper` and `kubectl`
which kubeconfig to use.

For each cluster, open a new terminal window.  In each terminal,
set the `KUBECONFIG` environment variable to a different path and
log in to your cluster.

_**Public:**_

~~~ shell
export KUBECONFIG=~/.kube/config-public
#Enter provider-specific login command
kubectl create namespace public
kubectl config set-context --current --namespace public
~~~

_**Private:**_

~~~ shell
export KUBECONFIG=~/.kube/config-private
#Enter provider-specific login command
kubectl create namespace private
kubectl config set-context --current --namespace private
~~~

**Note:** The login procedure varies by provider.

## Step 3: Install Skupper on your Kubernetes clusters

Using Skupper on Kubernetes requires the installation of the
Skupper custom resource definitions (CRDs) and the Skupper
controller.

For each cluster, use `kubectl apply` with the Skupper
installation YAML to install the CRDs and controller.

_**Public:**_

~~~ shell
kubectl apply -f https://skupper.io/v2/install.yaml
~~~

_**Private:**_

~~~ shell
kubectl apply -f https://skupper.io/v2/install.yaml
~~~

## Step 4: Deploy the Kafka cluster

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
              advertisedHost: cluster1-kafka-0
            - broker: 1
              advertisedHost: cluster1-kafka-1
~~~

See [Advertised addresses for brokers][advertised-addresses] for
more information.

[advertised-addresses]: https://strimzi.io/docs/operators/in-development/configuring.html#property-listener-config-broker-reference

## Step 5: Create your sites

A Skupper _site_ is a location where components of your
application are running.  Sites are linked together to form a
network for your application.  In Kubernetes, a site is associated
with a namespace.

Use the kubectl apply command to declaratively create sites in the kubernetes
namespaces. This deploys the Skupper router. Then use kubectl get site to see
the outcome.

**Note:** If you are using Minikube, you need to [start minikube
tunnel][minikube-tunnel] before you create your sites.

[minikube-tunnel]: https://skupper.io/start/minikube.html#running-minikube-tunnel

_**Private:**_

~~~ shell
kubectl apply -f ./private-crs/site.yaml
kubectl wait --for condition=Ready --timeout=3m site/private
~~~

_Sample output:_

~~~ console
$ kubectl wait --for condition=Ready --timeout=3m site/private
site.skupper.io/private created
site.skupper.io/private condition met
~~~

_**Public:**_

~~~ shell
kubectl apply -f ./public-crs/site.yaml
kubectl wait --for condition=Ready --timeout=3m site/public
~~~

_Sample output:_

~~~ console
$ kubectl wait --for condition=Ready --timeout=3m site/public
site.skupper.io/public created
site.skupper.io/public condition met
~~~

## Step 6: Link your sites

A Skupper _link_ is a channel for communication between two sites.
Links serve as a transport for application connections and
requests.

Creating a link requires the use of two Skupper commands in
conjunction: `skupper token issue` and `skupper token redeem`.
The `skupper token issue` command generates a secret token that
can be transferred to a remote site and redeemed for a link to the
issuing site.  The `skupper token redeem` command uses the token
to create the link.

**Note:** The link token is truly a *secret*.  Anyone who has the
token can link to your site.  Make sure that only those you trust
have access to it.

First, use `skupper token issue` in @site0@ to generate the token.
Then, use `skupper token redeem` in @site1@ to link the sites.

_**Public:**_

~~~ shell
skupper token issue ~/secret.token
~~~

_Sample output:_

~~~ console
$ skupper token issue ~/secret.token
Waiting for token status ...

Grant "public-cad4f72d-2917-49b9-ab66-cdaca4d6cf9c" is ready
Token file /run/user/1000/skewer/secret.token created

Transfer this file to a remote site. At the remote site,
create a link to this site using the "skupper token redeem" command:

  skupper token redeem <file>

The token expires after 1 use(s) or after 15m0s.
~~~

_**Private:**_

~~~ shell
skupper token redeem ~/secret.token
~~~

_Sample output:_

~~~ console
$ skupper token redeem ~/secret.token
Waiting for token status ...
Token "public-cad4f72d-2917-49b9-ab66-cdaca4d6cf9c" has been redeemed
~~~

If your terminal sessions are on different machines, you may need
to use `scp` or a similar tool to transfer the token securely.  By
default, tokens expire after a single use or 15 minutes after
being issued.

## Step 7: Expose the Kafka cluster

We will create listeners and connectors to expose the kafka service
In Private, we will create a connector.

Then, in Public, we will create a listener.

_**Private:**_

~~~ shell
kubectl apply -f ./private-crs/connector.yaml
~~~

_Sample output:_

~~~ console
$ kubectl apply -f ./private-crs/connector.yaml
connector.skupper.io/cluster1-kafka created
~~~

_**Public:**_

~~~ shell
kubectl apply -f ./public-crs/listener.yaml
~~~

_Sample output:_

~~~ console
$ kubectl apply -f ./public-crs/listener.yaml
listener.skupper.io/cluster1-kafka-brokers created
~~~

## Step 8: Run the client

Use the `kubectl run` command to execute the client program in
Public sending to either broker.

_**Public:**_

~~~ shell
kubectl run client --attach --rm --restart Never --image quay.io/skupper/kafka-example-client --env BOOTSTRAP_SERVERS=cluster1-kafka-0:9092
~~~

_Sample output:_

~~~ console
$ kubectl run client --attach --rm --restart Never --image quay.io/skupper/kafka-example-client --env BOOTSTRAP_SERVERS=cluster1-kafka-0:9092
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

## Step 9: Cleaning Up

To test remove Skupper and the other resources from this exercise, use
the following commands.

_**Private:**_

~~~ shell
skupper site delete --all
kubectl delete -f server/cluster1.yaml
kubectl delete -f server/strimzi.yaml
~~~

_**Public:**_

~~~ shell
skupper site delete --all
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
