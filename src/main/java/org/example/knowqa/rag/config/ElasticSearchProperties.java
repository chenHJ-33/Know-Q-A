package org.example.knowqa.rag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = ElasticSearchProperties.PREFIX)
@Data
public class ElasticSearchProperties {
    public static final String  PREFIX="elasticsearch";
    private String host;

    private String baseUrl;

    private String modelName;

    private String apiKey;

    private int dimensions;
}
