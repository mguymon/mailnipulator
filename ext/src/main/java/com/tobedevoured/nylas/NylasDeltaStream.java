package com.tobedevoured.nylas;

import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;
import io.vertx.rxjava.core.AbstractVerticle;
import io.vertx.rxjava.core.eventbus.EventBus;
import io.vertx.rxjava.core.eventbus.Message;
import io.vertx.rxjava.core.http.HttpClient;
import io.vertx.rxjava.core.http.HttpClientRequest;
import io.vertx.rxjava.core.http.HttpClientResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Base64;


/**
 * Created by mguymon on 3/19/16.
 */
public class NylasDeltaStream extends AbstractVerticle {

    private static final Logger logger = LoggerFactory.getLogger(NylasDeltaStream.class);

    public void start() throws Exception {
        logger.info("Starting {}", this.getClass().getName());

        EventBus eventBus = vertx.eventBus();
        eventBus.consumer("nylas.delta.endpoint", new Handler<Message<Object>>() {
            @Override
            public void handle(Message<Object> event) {
                logger.info("Received new nylas delta endpoint: {}", event.body());
                HttpClient httpClient = vertx.createHttpClient();

                JsonObject config = (JsonObject)event.body();

                String encoding = Base64.getEncoder().encodeToString(config.getString("auth").getBytes(StandardCharsets.UTF_8));

                HttpClientRequest request = httpClient
                    .getAbs(config.getString("url"))
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .putHeader("Authorization", "Basic " + encoding);
                request.toObservable()
                    .flatMap(HttpClientResponse::toObservable)
                    .forEach(
                        buffer -> {
                            if (buffer.toString().trim().length() > 0 ) {
                                logger.info("delta stream: {}", buffer.toString());
                                eventBus.publish("nylas.delta.stream", buffer.toString());
                            }
                        }
                    );
                request.end();
            }
        });

    }
}
