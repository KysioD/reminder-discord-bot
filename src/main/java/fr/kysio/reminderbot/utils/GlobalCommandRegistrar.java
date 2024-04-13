package fr.kysio.reminderbot.utils;

import discord4j.common.JacksonResources;
import discord4j.discordjson.json.ApplicationCommandRequest;
import discord4j.rest.RestClient;
import discord4j.rest.service.ApplicationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Based on https://github.com/Discord4J/example-projects/blob/master/gradle-simple-bot/src/main/java/com/novamaday/d4j/gradle/simplebot/GlobalCommandRegistrar.java
 */
public class GlobalCommandRegistrar {
    private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private final RestClient restClient;

    // The name of the folder the commands json is in, inside our resources folder
    private static final String commandsFolderName = "commands/";

    private static final Boolean IS_DEV_ENV = System.getenv("DISCORD_TOKEN") == null;
    private static final Long GUILD_ID = 1192419670922362880L;

    public GlobalCommandRegistrar(RestClient restClient) {
        this.restClient = restClient;
    }

    //Since this will only run once on startup, blocking is okay.
    public void registerCommands(List<String> fileNames) throws IOException {
        //Create an ObjectMapper that supports Discord4J classes
        final JacksonResources d4jMapper = JacksonResources.create();

        // Convenience variables for the sake of easier to read code below
        final ApplicationService applicationService = restClient.getApplicationService();
        final long applicationId = restClient.getApplicationId().block();

        //Get our commands json from resources as command data
        List<ApplicationCommandRequest> commands = new ArrayList<>();
        for (String json : getCommandsJson(fileNames)) {
            ApplicationCommandRequest request = d4jMapper.getObjectMapper()
                .readValue(json, ApplicationCommandRequest.class);

            commands.add(request); //Add to our array list
        }

        if (IS_DEV_ENV) {
            applicationService.bulkOverwriteGuildApplicationCommand(applicationId, GUILD_ID, commands)
                    .doOnNext(cmd -> LOGGER.debug("Successfully registered Guild Command " + cmd.name()))
                    .doOnError(e -> LOGGER.error("Failed to register guild commands", e))
                    .subscribe();
            return;
        }

        /* Bulk overwrite commands. This is now idempotent, so it is safe to use this even when only 1 command
        is changed/added/removed
        */
        applicationService.bulkOverwriteGlobalApplicationCommand(applicationId, commands)
            .doOnNext(cmd -> LOGGER.debug("Successfully registered Global Command " + cmd.name()))
            .doOnError(e -> LOGGER.error("Failed to register global commands", e))
            .subscribe();
    }

    /* The two below methods are boilerplate that can be completely removed when using Spring Boot */

    private static List<String> getCommandsJson(List<String> fileNames) throws IOException {
        // Confirm that the commands folder exists
        URL url = GlobalCommandRegistrar.class.getClassLoader().getResource(commandsFolderName);
        Objects.requireNonNull(url, commandsFolderName + " could not be found");

        //Get all the files inside this folder and return the contents of the files as a list of strings
        List<String> list = new ArrayList<>();
        for (String file : fileNames) {
            String resourceFileAsString = getResourceFileAsString(commandsFolderName + file);
            list.add(Objects.requireNonNull(resourceFileAsString, "Command file not found: " + file));
        }
        return list;
    }

    /**
     * Gets a specific resource file as String
     *
     * @param fileName The file path omitting "resources/"
     * @return The contents of the file as a String, otherwise throws an exception
     */
    private static String getResourceFileAsString(String fileName) throws IOException {
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        try (InputStream resourceAsStream = classLoader.getResourceAsStream(fileName)) {
            if (resourceAsStream == null) return null;
            try (InputStreamReader inputStreamReader = new InputStreamReader(resourceAsStream);
                 BufferedReader reader = new BufferedReader(inputStreamReader)) {
                return reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }
        }
    }
}