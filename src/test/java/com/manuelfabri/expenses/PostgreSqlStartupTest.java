package com.manuelfabri.expenses;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.manuelfabri.expenses.service.FirebaseService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@EnabledIfSystemProperty(named = "expenses.postgres.url", matches = ".+")
@SpringBootTest(properties = {
    "spring.config.location=file:src/main/resources/application.yml",
    "spring.cloud.config.enabled=false",
    "firebase.admin-sdk-path=unused-test-credentials",
    "firebase.api-key=unused-test-key",
    "spring.liquibase.enabled=true",
    "spring.jpa.hibernate.ddl-auto=validate",
    "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect",
    "spring.datasource.driver-class-name=org.postgresql.Driver"
})
@AutoConfigureMockMvc
class PostgreSqlStartupTest {

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private FirebaseService firebaseService;

  @MockitoBean
  private FirebaseApp firebaseApp;

  @MockitoBean
  private FirebaseAuth firebaseAuth;

  @DynamicPropertySource
  static void databaseProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", () -> System.getProperty("expenses.postgres.url"));
    registry.add("spring.datasource.username", () -> System.getProperty("expenses.postgres.username", "postgres"));
    registry.add("spring.datasource.password", () -> System.getProperty("expenses.postgres.password", "postgres"));
  }

  @Test
  void migratedPostgresStartsWithValidatedMappingsAndSecurity() throws Exception {
    assertThat(jdbcTemplate.queryForObject("select count(*) from databasechangelog", Integer.class)).isPositive();
    mockMvc.perform(get("/actuator/health")).andExpect(status().isOk());
    mockMvc.perform(get("/transaction")).andExpect(status().isUnauthorized());
  }

  @Test
  void bootConfiguredFirebaseClientReadsSnakeCaseRefreshTokens() {
    RestTemplate client = (RestTemplate) ReflectionTestUtils.getField(firebaseService, "restTemplate");
    MockRestServiceServer server = MockRestServiceServer.bindTo(client).build();
    server.expect(requestTo("https://securetoken.googleapis.com/v1/token?key=unused-test-key"))
        .andRespond(withSuccess("""
            {"id_token":"access-fixture","refresh_token":"refresh-fixture",
             "expires_in":"3600","token_type":"Bearer","user_id":"fixture-user"}
            """, MediaType.APPLICATION_JSON));

    var response = firebaseService.refreshToken("input-fixture");

    assertThat(response.getIdToken()).isEqualTo("access-fixture");
    assertThat(response.getRefreshToken()).isEqualTo("refresh-fixture");
    assertThat(response.getExpiresIn()).isEqualTo("3600");
    server.verify();
  }
}
