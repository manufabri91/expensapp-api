package com.manuelfabri.expenses.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

class ConfigServerRetryTest {

  // Generated with Cloud Context 4.3.3 / Security Crypto 6.5.11 and the test-only bootstrap key.
  private static final String LEGACY_CIPHERTEXT =
      "fe9cce2ae663716be03fa24f19259be42631aa762a35eeb11ae9c2d3ee70610f5"
          + "cd25ab3d212c5967db92028e24d0481";

  private HttpServer server;
  private final AtomicInteger requests = new AtomicInteger();

  @AfterEach
  void stopServer() {
    if (server != null) {
      server.stop(0);
    }
  }

  @Test
  void temporaryConfigServerFailureRetriesAndDecryptsLegacyProperties() throws IOException {
    startServer(2);

    try (ConfigurableApplicationContext context = application().run(arguments())) {
      assertThat(context.getEnvironment().getProperty("retry.fixture")).isEqualTo("loaded");
      assertThat(context.getEnvironment().getProperty("retry.encrypted")).isEqualTo("legacy-config-fixture");
      assertThat(requests.get()).isGreaterThanOrEqualTo(3);
    }
  }

  @Test
  void unavailableConfigServerStopsStartupAfterConfiguredAttempts() throws IOException {
    startServer(Integer.MAX_VALUE);

    assertThatThrownBy(() -> application().run(arguments())).isInstanceOf(IllegalStateException.class);
    assertThat(requests.get()).isEqualTo(3);
  }

  private void startServer(int failures) throws IOException {
    server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext("/", exchange -> {
      boolean unavailable = requests.incrementAndGet() <= failures;
      byte[] body = (unavailable ? "unavailable" : """
          {"name":"retry-fixture","profiles":["default"],"propertySources":[
            {"name":"fixture","source":{"retry.fixture":"loaded","retry.encrypted":"{cipher}%s"}}
          ]}
          """.formatted(LEGACY_CIPHERTEXT)).getBytes(StandardCharsets.UTF_8);
      exchange.getResponseHeaders().set("Content-Type", "application/json");
      exchange.sendResponseHeaders(unavailable ? 503 : 200, body.length);
      try (var response = exchange.getResponseBody()) {
        response.write(body);
      }
      exchange.close();
    });
    server.start();
  }

  private SpringApplicationBuilder application() {
    return new SpringApplicationBuilder(RetryFixture.class).web(WebApplicationType.NONE).logStartupInfo(false);
  }

  private String[] arguments() {
    return new String[] {
        "--spring.application.name=retry-fixture",
        "--spring.cloud.config.enabled=true",
        "--spring.cloud.config.uri=http://127.0.0.1:" + server.getAddress().getPort(),
        "--spring.cloud.config.fail-fast=true",
        "--spring.cloud.config.retry.max-attempts=3",
        "--spring.cloud.config.retry.initial-interval=1",
        "--spring.cloud.config.retry.max-interval=2",
        "--spring.main.banner-mode=off"
    };
  }

  @Configuration(proxyBeanMethods = false)
  static class RetryFixture {
  }
}
