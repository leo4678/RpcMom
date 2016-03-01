#!/bin/sh
mvn dependency:copy-dependencies -DoutputDirectory=target/lib   -DincludeScope=compile
mvn install
mv -t ./target/lib ./target/classes/
mv -t ./target/lib ./target/maven-archiver/
mv -t ./target/lib ./target/maven-status/
mv ./target/rpc-api-1.0.jar ./target/lib