package org.example;

import discord4j.core.DiscordClient;
import discord4j.core.GatewayDiscordClient;
import reactor.core.publisher.Mono;

public class Main {
    private static final String DISCORD_TOKEN = System.getenv("DISCORD_TOKEN");

    public static void main(String[] args) {
        DiscordClient client = DiscordClient.create(DISCORD_TOKEN);

        Mono<Void> login = client.withGateway((GatewayDiscordClient gateway) -> Mono.empty());

        login.block();
    }
}