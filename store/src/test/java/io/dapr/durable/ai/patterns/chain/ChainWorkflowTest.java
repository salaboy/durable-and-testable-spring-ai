package io.dapr.durable.ai.patterns.chain;

import io.dapr.durable.ai.ActivityCallCounter;
import io.dapr.durable.ai.Application;
import io.dapr.durable.ai.DaprTestContainersConfig;
import io.dapr.durable.ai.TestChatModelConfig;
import io.dapr.durable.ai.patterns.chain.ChainPromptActivity;
import io.dapr.durable.ai.patterns.chain.ChainWorkflow;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.dapr.workflows.client.WorkflowState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = Application.class, properties = {
        "spring.autoconfigure.exclude=org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration"
})
@Import({DaprTestContainersConfig.class, TestChatModelConfig.class})
class ChainWorkflowTest {

  @Autowired
  private DaprWorkflowClient daprWorkflowClient;

  @Autowired
  private ActivityCallCounter activityCallCounter;

  @BeforeEach
  void setUp() {
    activityCallCounter.resetAll();
  }

  @Test
  void chainWorkflowExecutesFourActivities() throws TimeoutException {
    String input = "Q3 revenue grew 45%. Customer satisfaction hit 92 points. Employee retention at 89%.";

    String instanceId = daprWorkflowClient.scheduleNewWorkflow(ChainWorkflow.class, input);

    WorkflowState status = daprWorkflowClient.waitForWorkflowCompletion(instanceId,
            Duration.ofSeconds(30), true);

    assertThat(status.isCompleted()).isTrue();
    assertThat(activityCallCounter.getCount(ChainPromptActivity.class.getName())).isEqualTo(4);
  }
}
