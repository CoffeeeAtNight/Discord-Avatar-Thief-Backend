FROM vertx/vertx3

ENV VERTICLE_NAME com.discord.avatar.thief.master.MainVerticle
ENV VERTICLE_FILE target/master-1.0.0-SNAPSHOT-fat.jar

ENV VERTICLE_HOME /usr/verticles

EXPOSE 8080

COPY $VERTICLE_FILE $VERTICLE_HOME/

WORKDIR $VERTICLE_HOME
ENTRYPOINT ["sh", "-c"]
CMD ["exec vertx run $VERTICLE_NAME -cp $VERTICLE_HOME/*"]
