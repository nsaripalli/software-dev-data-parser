ARG SBT_VERSION=1.3.7
FROM mozilla/sbt

RUN apt update
RUN apt install g++ valgrind cmake git wget -y

WORKDIR /test

RUN wget -c https://downloads.lightbend.com/scala/2.13.1/scala-2.13.1.deb; dpkg --force-all -i scala-2.13.1.deb;
ADD build.sbt build.sbt
ADD project/build.properties project/build.properties
RUN sbt compile