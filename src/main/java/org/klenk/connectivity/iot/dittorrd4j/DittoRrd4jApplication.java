package org.klenk.connectivity.iot.dittorrd4j;

import org.eclipse.ditto.client.DittoClient;
import org.eclipse.ditto.client.DittoClients;
import org.eclipse.ditto.client.changes.ChangeAction;
import org.eclipse.ditto.client.configuration.ClientCredentialsAuthenticationConfiguration;
import org.eclipse.ditto.client.configuration.WebSocketMessagingConfiguration;
import org.eclipse.ditto.client.messaging.AuthenticationProvider;
import org.eclipse.ditto.client.messaging.AuthenticationProviders;
import org.eclipse.ditto.client.messaging.MessagingProvider;
import org.eclipse.ditto.client.messaging.MessagingProviders;
import org.eclipse.ditto.json.JsonPointer;
import org.eclipse.ditto.model.base.json.JsonSchemaVersion;
import org.klenk.connectivity.iot.dittorrd4j.config.DittoClientProperties;
import org.klenk.connectivity.iot.dittorrd4j.service.DittoRrd4jService;
import org.rrd4j.core.Util;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;

@SpringBootApplication
@EnableConfigurationProperties(DittoClientProperties.class)
public class DittoRrd4jApplication {

	public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless","true");

        SpringApplication.run(DittoRrd4jApplication.class, args);
    }

    @Bean
    public CommandLineRunner commandLineRunner(DittoRrd4jService service, DittoClientProperties dittoClientProperties) {
        return args -> {

            service.createRrdDb();

            /*
            Instant now = Instant.now();
            Instant twentyFourHoursAgo = now.minus(24, ChronoUnit.HOURS);

            Instant currentTime = twentyFourHoursAgo;

            Random random = new Random();

            double sent = 0;
            double received = 0;

            while (currentTime.isBefore(now)) {

                sent += random.nextInt(100);
                received += random.nextInt( 100);

                service.addSample(
                        Util.getTimestamp(new Date(currentTime.toEpochMilli())),
                        sent, received);

                currentTime = currentTime.plus(5, ChronoUnit.MINUTES);
            }
            */

            AuthenticationProvider authenticationProvider =
                    AuthenticationProviders.clientCredentials(ClientCredentialsAuthenticationConfiguration.newBuilder()
                            .clientId(dittoClientProperties.getAuthentication().getClientId())
                            .clientSecret(dittoClientProperties.getAuthentication().getClientSecret())
                            .scopes(dittoClientProperties.getAuthentication().getScopes())
                            .tokenEndpoint(dittoClientProperties.getAuthentication().getTokenEndpoint())
                            .build());

            MessagingProvider messagingProvider = MessagingProviders.webSocket(WebSocketMessagingConfiguration.newBuilder()
                    .endpoint(dittoClientProperties.getWebServiceEndpoint())
                    .jsonSchemaVersion(JsonSchemaVersion.V_1)
                    .build(), authenticationProvider);

            DittoClient client = DittoClients.newInstance(messagingProvider);

            client.twin().startConsumption().get();
            System.out.println("Subscribed for Twin events");
            client.twin().registerForThingChanges("my-changes", change -> {
                System.out.println("Message: " + change);

                if (change.getAction() == ChangeAction.UPDATED) {
                    if (change.getPath().equals(JsonPointer.of("/features/networkTraffic/properties"))) {

                        change.getThing().ifPresent(thing -> {
                            thing.getFeatures().ifPresent(features -> {
                                features.getFeature("networkTraffic").ifPresent(networkTraffic -> {
                                    try {
                                        service.addSample(
                                                Util.getTimestamp(new Date(System.currentTimeMillis())),
                                                networkTraffic.getProperty("totalBytesSent").get().asLong(),
                                                networkTraffic.getProperty("totalBytesReceived").get().asLong());
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                });
                            });
                        });
                    }
                }
            });
        };

    }
}
