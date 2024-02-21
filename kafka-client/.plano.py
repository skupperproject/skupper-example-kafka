from plano import *

@command
def build():
    run("mvn package")

@command(name="run")
def run_():
    build()
    run("java -jar target/quarkus-app/quarkus-run.jar")

@command
def clean():
    run("mvn clean")

@command
def build_image():
    build()
    run("podman build -t quay.io/skupper/kafka-example-client .")

@command
def push_image():
    build_image()
    run("podman push quay.io/skupper/kafka-example-client")
