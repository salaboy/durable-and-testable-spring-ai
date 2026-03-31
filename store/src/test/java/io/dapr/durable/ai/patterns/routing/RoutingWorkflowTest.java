package io.dapr.durable.ai.patterns.routing;

import io.dapr.durable.ai.ActivityCallCounter;
import io.dapr.durable.ai.Application;
import io.dapr.durable.ai.DaprTestContainersConfig;
import io.dapr.durable.ai.TestChatModelConfig;
import io.dapr.durable.ai.patterns.routing.DetermineRouteActivity;
import io.dapr.durable.ai.patterns.routing.RoutingPromptActivity;
import io.dapr.durable.ai.patterns.routing.RoutingWorkflow;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.dapr.workflows.client.WorkflowState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = Application.class, properties = {
        "spring.autoconfigure.exclude=org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration"
})
@Import({DaprTestContainersConfig.class, TestChatModelConfig.class})
class RoutingWorkflowTest {

  @Autowired
  private DaprWorkflowClient daprWorkflowClient;

  @Autowired
  private ActivityCallCounter activityCallCounter;

  @BeforeEach
  void setUp() {
    activityCallCounter.resetAll();
  }

  @Test
  void routingWorkflowExecutesTwoActivitiesPerInput() throws TimeoutException {
    List<String> tickets = List.of(
            "I was charged twice for my subscription last month",
            "I need help understanding my invoice"
    );

    String instanceId = daprWorkflowClient.scheduleNewWorkflow(RoutingWorkflow.class, tickets);

    WorkflowState status = daprWorkflowClient.waitForWorkflowCompletion(instanceId,
            Duration.ofSeconds(30), true);

    assertThat(status.isCompleted()).isTrue();
    assertThat(activityCallCounter.getCount(DetermineRouteActivity.class.getName())).isEqualTo(2);
    assertThat(activityCallCounter.getCount(RoutingPromptActivity.class.getName())).isEqualTo(2);
  }
}
