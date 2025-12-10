#!/bin/sh
cd ..
mvn clean package -Pprod
cp target/cruud-0.0.1-SNAPSHOT.jar docker/assets/
#docker build -t cruud .
