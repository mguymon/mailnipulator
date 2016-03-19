package com.tobedevoured.nylas;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;


public class Mailnipulator {
    private static final Logger logger = LoggerFactory.getLogger(Mailnipulator.class);

    Vertx vertx;
    String gemPath;

    public Mailnipulator() {
        vertx = Vertx.vertx();
    }

    public void deploy() {

        File file = new File("vendor/bundle/jruby/2.2.0");
        gemPath = file.getAbsolutePath();
        logger.info("GEM_PATH={}", gemPath);

        vertx.deployVerticle(NylasDeltaStream.class.getName());

        EventBus eb = vertx.eventBus();

        MessageConsumer<String> consumer = eb.consumer("mailnipulator.deploy.server", message -> {
            deployNylas(vertx, gemPath);
        });

        consumer.completionHandler(res -> {
            if (res.succeeded()) {
                deployServer(vertx, gemPath);
            } else {
                logger.error("Registration failed!", res.cause());
            }
        });
    }

    public void deployServer(Vertx vertx, String gemPath) {
        final String serverVerticle = "mailnipulator/server.rb";

        logger.debug("Deploying {}", serverVerticle);

        final DeploymentOptions serverOptions =
                new DeploymentOptions().setConfig(new JsonObject().put("GEM_PATH", gemPath));
        vertx.deployVerticle(serverVerticle, serverOptions, ar -> {
            if (ar.succeeded()) {
                logger.info("Deployed {}", serverVerticle);
                EventBus eb = vertx.eventBus();
                eb.publish("mailnipulator.deploy.server", "success");

            } else {
                logger.info("Deployed failed: {}", serverVerticle,  ar.cause());
            }
        });
    }

    public void deployNylas(Vertx vertx, String gemPath) {
        final String nylasVerticle = "mailnipulator/nylas.rb";

        logger.debug("Deploying {}", nylasVerticle);

        final DeploymentOptions nylasOptions =
                new DeploymentOptions().setConfig(
                        new JsonObject().put("GEM_PATH", gemPath)
                ).setWorker(true);
        vertx.deployVerticle(nylasVerticle, nylasOptions, status -> {
            if (status.succeeded()) {
                logger.info("Deployed {}", nylasVerticle);
            } else {
                logger.info("Deployed failed: {}", nylasVerticle,  status.cause());
            }
        });

        EventBus eb = vertx.eventBus();
        eb.publish("mailnipulator.deploy.nylas", "success");
    }

    public static void main(String[] args) {
        Mailnipulator mailnipulator = new Mailnipulator();
        mailnipulator.deploy();
    }
}
