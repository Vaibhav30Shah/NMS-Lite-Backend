package com.motadata;

import com.motadata.server.ApiServer;
import io.vertx.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Bootstrap
{


    private static final Logger LOGGER = LoggerFactory.getLogger(Bootstrap.class);

    public static void main(String[] args)
    {
        Vertx vertx = Vertx.vertx();

        vertx.deployVerticle(new ApiServer()).onComplete(event -> {
            if (event.succeeded())
            {
               LOGGER.info("Server started");
            }

            else
            {
               LOGGER.error("Server failed to start");
            }
        });
    }
}
