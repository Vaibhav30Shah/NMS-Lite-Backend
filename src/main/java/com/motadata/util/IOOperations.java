package com.motadata.util;

import io.vertx.core.json.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class IOOperations
{

    private static final Logger LOGGER = LoggerFactory.getLogger(IOOperations.class);

    public void writeCredentialToFile(String credentialData)
    {
        try
        {
            FileWriter fileWriter = new FileWriter(Constants.CREDENTIALS_FILE, true); // Append mode

            fileWriter.write(credentialData + System.lineSeparator()); // Write the credential data to the file

            fileWriter.close();
        }
        catch (IOException e)
        {
            LOGGER.error(e.getMessage());
        }
    }

    public String readCredentialFromFile() throws IOException
    {
        File file = new File(Constants.CREDENTIALS_FILE);
        return Files.readString(Paths.get(file.getAbsolutePath()));
    }

    public JsonObject findCredentialProfile(String credentialData, String credProfileId)
    {
        JsonObject credentialJson = new JsonObject(credentialData);

        if (credentialJson.containsKey(credProfileId))
        {
            return credentialJson.getJsonObject(credProfileId);
        }

        return null;
    }
}
