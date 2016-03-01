#!/bin/sh
mvn dependency:copy-dependencies -DoutputDirectory=target/lib  
mvn package
mv -t ./target/lib ./target/classes/
mv -t ./target/lib ./target/maven-archiver/
mv -t ./target/lib ./target/maven-status/
mv ./target/middleware-mom-1.0.jar ./target/lib
