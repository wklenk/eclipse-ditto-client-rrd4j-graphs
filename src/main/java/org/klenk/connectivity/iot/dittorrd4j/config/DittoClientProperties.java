package org.klenk.connectivity.iot.dittorrd4j.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "ditto-client")
@ConstructorBinding
@Data
public class DittoClientProperties {

    @Data
    public static class Authentication {
        private String clientId;
        private String clientSecret;
        private List<String> scopes = new ArrayList<>();
        private String tokenEndpoint;
    }

    private Authentication authentication;
    private String webServiceEndpoint;
}
