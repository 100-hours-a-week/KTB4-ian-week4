package com.ian.community;

import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LocalDatabaseConfigurationTest {

    @Test
    void localProfileKeepsEmbeddedH2WithoutFlyway() throws Exception {
        List<PropertySource<?>> propertySources =
                new YamlPropertySourceLoader().load(
                        "application-local",
                        new ClassPathResource(
                                "application-local.yaml"
                        )
                );
        PropertySource<?> properties = propertySources.getFirst();

        assertThat(properties.getProperty(
                "spring.datasource.driver-class-name"
        )).isEqualTo("org.h2.Driver");
        assertThat(properties.getProperty(
                "spring.datasource.url"
        )).asString().startsWith("jdbc:h2:mem:");
        assertThat(properties.getProperty(
                "spring.jpa.hibernate.ddl-auto"
        )).isEqualTo("update");
        assertThat(properties.getProperty(
                "spring.flyway.enabled"
        )).isEqualTo(false);
        assertThat(properties.getProperty(
                "spring.h2.console.enabled"
        )).isEqualTo(true);
    }
}
