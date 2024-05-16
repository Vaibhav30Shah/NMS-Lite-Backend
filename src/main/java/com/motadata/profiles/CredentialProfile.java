package com.motadata.profiles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.http.HttpServer;
import io.vertx.ext.web.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CredentialProfile extends AbstractVerticle
{
    private static final Logger CREDENTIAL_LOGGER = LoggerFactory.getLogger(CredentialProfile.class);

    @Override
    public void start(Promise<Void> startPromise) throws Exception
    {
        HttpServer server = vertx.createHttpServer();

        EventBus eventBus = vertx.eventBus();

        Router mainRouter = Router.router(vertx);

        Router subRouterCredential = Router.router(vertx);

        mainRouter.route("/").handler(routingContext -> {
           routingContext.response().setStatusCode(200).end("Welcome To NMS Lite");
        });


    }
}
