package com.salaboy.durable.ai.orchestrator;

import com.salaboy.durable.ai.ActivityCallCounter;
import com.salaboy.durable.ai.Application;
import com.salaboy.durable.ai.DaprTestContainersConfig;
import com.salaboy.durable.ai.TestChatModelConfig;
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
class OrchestratorWorkflowTest {

  @Autowired
  private DaprWorkflowClient daprWorkflowClient;

  @Autowired
  private ActivityCallCounter activityCallCounter;

  @BeforeEach
  void setUp() {
    activityCallCounter.resetAll();
  }

  @Test
  void orchestratorWorkflowExecutesDefineAndProcessActivities() throws TimeoutException {
    String taskDescription = "Write a product description for a new eco-friendly water bottle";

    String instanceId = daprWorkflowClient.scheduleNewWorkflow(OrchestratorWorkersWorkflow.class, taskDescription);

    WorkflowState status = daprWorkflowClient.waitForWorkflowCompletion(instanceId,
            Duration.ofSeconds(30), true);

    assertThat(status.isCompleted()).isTrue();
    assertThat(activityCallCounter.getCount(DefineTasksActivity.class.getName())).isEqualTo(1);
    assertThat(activityCallCounter.getCount(ProcessTaskActivity.class.getName())).isEqualTo(2);
  }
}
