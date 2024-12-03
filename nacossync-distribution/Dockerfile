FROM openjdk:17-jdk-oracle

WORKDIR /nacos-sync

COPY ./target/nacos-sync-0.5.0.tar.gz /nacos-sync/nacos-sync.tar.gz

RUN tar xf nacos-sync.tar.gz --strip-components 1 \
 && rm -f nacos-sync.tar.gz 


CMD [ "/nacos-sync/bin/startup.sh", "dockerstart"]
