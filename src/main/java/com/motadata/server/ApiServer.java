package com.motadata.server;

import com.motadata.Bootstrap;
import com.motadata.util.Constants;
import com.motadata.util.HelperFunctions;
import com.motadata.util.IOOperations;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

public class ApiServer extends AbstractVerticle
{
    private static final Logger LOGGER = LoggerFactory.getLogger(ApiServer.class);

    private static final Map<Integer, JsonObject> credentialProfiles = new HashMap<>();

    private static final AtomicInteger credentalIdCounter = new AtomicInteger(1);

    private static final Map<Integer, JsonObject> discoveryProfiles = new HashMap<>();

    private static final AtomicInteger discoveryProfileIdCounter = new AtomicInteger(1);

    private static final JsonArray discoveryProfileArray= new JsonArray();

    @Override
    public void start(Promise<Void> startPromise) throws Exception
    {
        HttpServer server = vertx.createHttpServer();

        Router router = Router.router(vertx);

        /* Main Routing */
        router.route("/").handler(routingContext ->
        {
            HttpServerResponse response = routingContext.response();

            response.setChunked(true);

            response.end("Welcome to NMS Lite");

        });

        /* ----------------------------------------------------------------- Credential Profile -------------------------------------------------------------------------- */

        //create credential profile
        router.post(Constants.CREDENTIAL_ROUTE).handler(routingContext -> routingContext.request().bodyHandler(body ->
        {
            {
                try
                {
                    LOGGER.debug("Received request: {}", routingContext.request().uri());

                    JsonObject credentialProfile = body.toJsonObject();

                    int credProfileId = credentalIdCounter.getAndIncrement();

                    credentialProfiles.put(credProfileId, credentialProfile);

                    routingContext.response()
                            .setStatusCode(Constants.SUCCESS_STATUS)
                            .putHeader("Content-Type", "Text/plain")
                            .end("Credential Profile created successfully");
                }
                catch (Exception e)
                {
                    var resp = errorHandler(routingContext);

                    routingContext.json(resp);

                    LOGGER.error("Error creating credential profile:", e);
                }
            }
        }));

        //get particular credentials
        router.get(Constants.CREDENTIAL_ROUTE + ":" + Constants.CREDENTIAL_PROFILE_ID).handler(routingContext ->
        {
            try
            {
                LOGGER.debug("Received request: {}", routingContext.request().uri());

                int credProfileId = Integer.parseInt(routingContext.request().getParam(Constants.CREDENTIAL_PROFILE_ID));

                JsonObject credentialProfile = credentialProfiles.get(credProfileId);

                if (credentialProfile != null)
                {
                    routingContext.response()
                            .putHeader("Content-Type", "application/json")
                            .end(credentialProfile.encode());
                }
                else
                {
                    routingContext.response().setStatusCode(Constants.NOT_FOUND_STATUS).end("Credential profile not found");
                }
            }
            catch (Exception e)
            {
                var resp = errorHandler(routingContext);

                routingContext.json(resp);

                LOGGER.error("Error creating credential profile:", e);
            }
        });

        //get all credentials
        router.get(Constants.CREDENTIAL_ROUTE).handler(routingContext ->
        {
            try
            {
                LOGGER.debug("Received request: {}", routingContext.request().uri());

                JsonObject allProfiles = new JsonObject();

                for (Map.Entry<Integer, JsonObject> entry : credentialProfiles.entrySet())
                {
                    allProfiles.put(String.valueOf(entry.getKey()), entry.getValue());
                }

                routingContext.response()
                        .putHeader("Content-Type", "application/json")
                        .end(allProfiles.encode());
            }

            catch (Exception e)
            {
                var resp = errorHandler(routingContext);

                routingContext.json(resp);

                LOGGER.error("Error creating credential profile:", e);
            }
        });

        //Update credential
        router.put(Constants.CREDENTIAL_ROUTE + ":" + Constants.CREDENTIAL_PROFILE_ID).handler(routingContext -> routingContext.request().bodyHandler(body ->
        {
            try
            {
                LOGGER.debug("Received request: {}", routingContext.request().uri());

                int credProfileId = Integer.parseInt(routingContext.request().getParam(Constants.CREDENTIAL_PROFILE_ID));

                JsonObject updatedCredentialProfile = body.toJsonObject();

                if (credentialProfiles.containsKey(credProfileId))
                {
                    credentialProfiles.put(credProfileId, updatedCredentialProfile);

                    routingContext.response().setStatusCode(Constants.SUCCESS_STATUS).end("Credential Profile updated successfully");
                }

                else
                {
                    routingContext.response().setStatusCode(Constants.NOT_FOUND_STATUS).end("Credential profile not found");
                }
            }
            catch (Exception e)
            {
                var resp = errorHandler(routingContext);

                routingContext.json(resp);

                LOGGER.error("Error creating credential profile:", e);
            }
        }));

        //delete credential
        router.delete(Constants.CREDENTIAL_ROUTE + ":" + Constants.CREDENTIAL_PROFILE_ID).handler(routingContext ->
        {
            try
            {
                LOGGER.debug("Received request: {}", routingContext.request().uri());

                int credProfileId = Integer.parseInt(routingContext.request().getParam(Constants.CREDENTIAL_PROFILE_ID));

                if (credentialProfiles.containsKey(credProfileId))
                {
                    credentialProfiles.remove(credProfileId);

                    routingContext.response().setStatusCode(Constants.SUCCESS_STATUS).end("Credential Profile Deleted successfully");
                }

                else
                {
                    routingContext.response().setStatusCode(Constants.NOT_FOUND_STATUS).end("Credential profile not found");
                }

            }
            catch (Exception e)
            {
                var resp = errorHandler(routingContext);

                routingContext.json(resp);

                LOGGER.error("Error creating credential profile:", e);
            }
        });


        /* --------------------------------------------------------------------- Discovery Profile ---------------------------------------------------------------------------- */

        // create a new discovery profile
        router.post(Constants.DISCOVERY_ROUTE).handler(routingContext -> routingContext.request().bodyHandler(body ->
        {
            try
            {
                LOGGER.debug("Received request: {}", routingContext.request().uri());

                JsonObject discoveryProfile = body.toJsonObject();

                int discoveryProfileId = discoveryProfileIdCounter.getAndIncrement();

                // Process credential profiles
                JsonArray credProfileIds = discoveryProfile.getJsonArray(Constants.CREDENTIAL_PROFILE);

                JsonArray processedCredProfiles = new JsonArray();

                for (Object credProfileObj : credProfileIds)
                {
                    if (credProfileObj instanceof JsonObject)
                    {
                        JsonObject credProfileJson = (JsonObject) credProfileObj;

                        Integer id = credProfileJson.getInteger(Constants.KEY_CREDENTIAL_ID);

                        if (id != null)
                        {
                            JsonObject credProfile = credentialProfiles.get(id);

                            if (credProfile != null)
                            {
                                processedCredProfiles.add(credProfile);
                            }
                        }
                    }
                }

                // Update the discovery profile with processed credential profiles
                discoveryProfile.put(Constants.CREDENTIAL_PROFILE, processedCredProfiles);

                discoveryProfiles.put(discoveryProfileId, discoveryProfile);

                routingContext.response()
                        .setStatusCode(Constants.SUCCESS_STATUS)
                        .putHeader("Content-Type", "text/plain")
                        .end("Discovery Profile Created successfully");
            }

            catch (Exception e)
            {
                var resp = errorHandler(routingContext);

                routingContext.json(resp);

                LOGGER.error("Error creating credential profile:", e);
            }
        }));

        //get all discovery profiles
        router.get(Constants.DISCOVERY_ROUTE).handler(routingContext ->
        {
            try
            {
                LOGGER.debug("Received request: {}", routingContext.request().uri());

                JsonObject allProfiles = new JsonObject();

                for (Map.Entry<Integer, JsonObject> entry : discoveryProfiles.entrySet())
                {
                    allProfiles.put(entry.getKey().toString(), entry.getValue());
                }

                routingContext.response()
                        .putHeader("Content-Type", "application/json")
                        .end(allProfiles.encode());
            }

            catch (Exception e)
            {
                var resp = errorHandler(routingContext);

                routingContext.json(resp);

                LOGGER.error("Error creating credential profile:", e);
            }
        });

        //get a specific discovery profile
        router.get(Constants.DISCOVERY_ROUTE + ":" + Constants.DISCOVERY_PROFILE_ID).handler(routingContext ->
        {
            try
            {
                LOGGER.debug("Received request: {}", routingContext.request().uri());

                int discoveryProfileId = Integer.parseInt(routingContext.request().getParam(Constants.DISCOVERY_PROFILE_ID));

                JsonObject discoveryProfile = discoveryProfiles.get(discoveryProfileId);

                if (discoveryProfile != null)
                {
                    routingContext.response()
                            .putHeader("Content-Type", "application/json")
                            .end(discoveryProfile.encode());
                }

                else
                {
                    routingContext.response().setStatusCode(Constants.NOT_FOUND_STATUS).end("Discovery profile not found");
                }
            }
            catch (Exception e)
            {
                var resp = errorHandler(routingContext);

                routingContext.json(resp);

                LOGGER.error("Error creating credential profile:", e);
            }
        });

        //update a discovery profile
        router.put(Constants.DISCOVERY_ROUTE + ":" + Constants.DISCOVERY_PROFILE_ID).handler(routingContext -> routingContext.request().bodyHandler(body ->
        {
            try
            {
                LOGGER.debug("Received request: {}", routingContext.request().uri());

                int discoveryProfileId = Integer.parseInt(routingContext.request().getParam(Constants.DISCOVERY_PROFILE_ID));

                JsonObject updatedDiscoveryProfile = body.toJsonObject();

                if (discoveryProfiles.containsKey(discoveryProfileId))
                {
                    // Process credential profiles
                    JsonArray credProfileIds = updatedDiscoveryProfile.getJsonArray(Constants.CREDENTIAL_PROFILE);

                    JsonArray processedCredProfiles = new JsonArray();

                    for (Object credProfileObj : credProfileIds)
                    {
                        if (credProfileObj instanceof JsonObject)
                        {
                            JsonObject credProfileJson = (JsonObject) credProfileObj;

                            Integer id = credProfileJson.getInteger(Constants.KEY_CREDENTIAL_ID);

                            if (id != null)
                            {
                                JsonObject credProfile = credentialProfiles.get(id);

                                if (credProfile != null)
                                {
                                    processedCredProfiles.add(credProfile);
                                }
                            }
                        }
                    }

                    // Update the discovery profile with processed credential profiles
                    updatedDiscoveryProfile.put(Constants.CREDENTIAL_PROFILE, processedCredProfiles);

                    discoveryProfiles.put(discoveryProfileId, updatedDiscoveryProfile);

                    routingContext.response().setStatusCode(Constants.SUCCESS_STATUS).end("Discovery Profile Updated Successfully");
                }
                else
                {
                    routingContext.response().setStatusCode(Constants.NOT_FOUND_STATUS).end("Discovery profile not found");
                }
            }
            catch (Exception e)
            {
                var resp = errorHandler(routingContext);

                routingContext.json(resp);

                LOGGER.error("Error creating credential profile:", e);
            }
        }));

        //delete a discovery profile
        router.delete(Constants.DISCOVERY_ROUTE + ":" + Constants.DISCOVERY_PROFILE_ID).handler(routingContext ->
        {
            try
            {
                LOGGER.debug("Received request: {}", routingContext.request().uri());

                int discoveryProfileId = Integer.parseInt(routingContext.request().getParam(Constants.DISCOVERY_PROFILE_ID));

                if (discoveryProfiles.containsKey(discoveryProfileId))
                {
                    discoveryProfiles.remove(discoveryProfileId);

                    routingContext.response().setStatusCode(Constants.SUCCESS_STATUS).end("Discovery Profile Updated Successfully");
                }
                else
                {
                    routingContext.response().setStatusCode(Constants.NOT_FOUND_STATUS).end("Discovery profile not found");
                }
            }
            catch (Exception e)
            {
                var resp = errorHandler(routingContext);

                routingContext.json(resp);

                LOGGER.error("Error creating credential profile:", e);
            }
        });


        // Run discovery route
        router.post(Constants.RUN_DISCOVERY + ":" + Constants.DISCOVERY_PROFILE_ID).handler(routingContext ->
        {
            try
            {
                LOGGER.debug("Received request: {}", routingContext.request().uri());

                // Get the discovery profile ID from the request path
                int discoveryProfileId = Integer.parseInt(routingContext.request().getParam(Constants.DISCOVERY_PROFILE_ID));

                // Get the discovery profile from the map
                JsonObject discoveryProfile = discoveryProfiles.get(discoveryProfileId);

                System.out.println("Discovery Profile: " + discoveryProfile);

                if (discoveryProfile != null)
                {
                    JsonArray outputArray = new JsonArray();

                    String ip = discoveryProfile.getString("ip");

                    // Check if the IP is alive using fping
                    if (HelperFunctions.isPingSuccessful(ip))
                    {
                        LOGGER.info("PING Check Successful " + ip+" is alive.");

                        discoveryProfile.put("plugin.type", "Discover");

                        discoveryProfileArray.add(discoveryProfile);

                        // Encode the JSON and send it to the plugin engine
                        var encodedJson = Base64.getEncoder().encodeToString(discoveryProfileArray.encode().getBytes());

                        Process process = new ProcessBuilder(Constants.PLUGIN_ENGINE_PATH, encodedJson).start();

                        // Read the output from the plugin engine
                        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                        StringBuilder outputBuilder = new StringBuilder();

                        String line;

                        while ((line = reader.readLine()) != null)
                        {
                            outputBuilder.append(line);
                        }

                        // Decode the output JSON array
                        var decodedBytes= Base64.getDecoder().decode(outputBuilder.toString());

                        String decodedString = new String(decodedBytes, StandardCharsets.UTF_8);

                        LOGGER.debug("Decoded Result: " + decodedString);

                        JsonArray contextOutput = new JsonArray(decodedString.toString());

                        outputArray.addAll(contextOutput);
                    }

                    // Send the output array as the response
                    routingContext.response()
                            .setStatusCode(Constants.SUCCESS_STATUS)
                            .putHeader("Content-Type", "application/json")
                            .end(outputArray.encode());
                }
                else
                {
                    // Discovery profile not found
                    routingContext.response().setStatusCode(Constants.NOT_FOUND_STATUS).end("Discovery profile not found.");
                }
            }
            catch (Exception e)
            {
                LOGGER.error("Error during discovery process:", e);

                routingContext.response().setStatusCode(Constants.ERROR_STATUS).end("Error during discovery process.");
            }
        });

        /* Server starting and error handling */
        server.requestHandler(router).listen(8080).onComplete(event ->
        {
            if (event.succeeded())
            {
                LOGGER.info("Server started on port 8080");
            }
            else
            {
                LOGGER.error("Server started on port 8080");
            }
        });
    }

    private JsonObject errorHandler(RoutingContext ctx)
    {
        var response = new JsonObject();

        response.put(Constants.ERROR_NAME, "Invalid JSON Format")
                .put(Constants.ERROR_MESSAGE, "Provide Valid JSON Format ");

        return response;
    }
}
