package io.dapr.durable.ai;

import io.dapr.testcontainers.*;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@TestConfiguration()
public class DaprTestContainersConfig {

  Map<String, String> postgreSQLDetails = new HashMap<>();

  {{
    postgreSQLDetails.put("host", "postgresql");
    postgreSQLDetails.put("user", "postgres");
    postgreSQLDetails.put("password", "postgres");
    postgreSQLDetails.put("database", "dapr");
    postgreSQLDetails.put("port", "5432");
    postgreSQLDetails.put("actorStateStore", String.valueOf(true));

  }}

  private Component stateStoreComponent = new Component("kvstore",
      "state.postgresql", "v2", postgreSQLDetails);



  @Bean
  public PostgreSQLContainer postgreSQLContainer(Network network) {
    return new PostgreSQLContainer(DockerImageName.parse("postgres"))
        .withNetworkAliases("postgresql")
        .withDatabaseName("dapr")
        .withUsername("postgres")
        .withPassword("postgres")
//        .withReuse(true)
        .withNetwork(network);
  }


  @Bean
  @ServiceConnection
  public DaprContainer daprContainer(Network daprNetwork,
                                     PostgreSQLContainer postgresqlContainer) {
    return new DaprContainer("daprio/daprd:1.17.0")
            .withAppName("durable-and-testable-ai")
            .withNetwork(daprNetwork)
//        .withReusableScheduler(true)
//        .withReusablePlacement(true)
            .withComponent(stateStoreComponent)
            .dependsOn(postgresqlContainer);
  }

  @Bean
  public WorkflowDashboardContainer workflowDashboard(Network network, PostgreSQLContainer postgreSQLContainer) {
    return new WorkflowDashboardContainer(WorkflowDashboardContainer.getDefaultImageName())
        .withNetwork(network)
        .withStateStoreComponent(stateStoreComponent)
        //.withReuse(true)
        .withExposedPorts(8080)
        .dependsOn(postgreSQLContainer);
  }


  @Bean
  public Network getDaprNetwork(Environment env) {
//    boolean reuse = env.getProperty("reuse", Boolean.class, false);
//    if (reuse) {
      Network defaultDaprNetwork = new Network() {
        @Override
        public String getId() {
          return "dapr-network";
        }

        @Override
        public void close() {
        }

      };

      List<com.github.dockerjava.api.model.Network> networks = DockerClientFactory.instance().client()
              .listNetworksCmd()
              .withNameFilter("dapr-network").exec();
      if (networks.isEmpty()) {
        Network.builder().createNetworkCmdModifier(cmd -> cmd.withName("dapr-network")).build().getId();
      }
      return defaultDaprNetwork;
//    } else {
//      return Network.newNetwork();
//    }
  }
}
