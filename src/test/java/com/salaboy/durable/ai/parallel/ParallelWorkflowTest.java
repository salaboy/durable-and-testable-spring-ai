package com.salaboy.durable.ai.parallel;

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
import java.util.List;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = Application.class, properties = {
        "spring.autoconfigure.exclude=org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration"
})
@Import({DaprTestContainersConfig.class, TestChatModelConfig.class})
class ParallelWorkflowTest {

  @Autowired
  private DaprWorkflowClient daprWorkflowClient;

  @Autowired
  private ActivityCallCounter activityCallCounter;

  @BeforeEach
  void setUp() {
    activityCallCounter.resetAll();
  }

  @Test
  void parallelWorkflowExecutesOneActivityPerInput() throws TimeoutException {
    List<String> inputs = List.of(
            "Summarize the history of Rome",
            "Summarize the history of Greece",
            "Summarize the history of Egypt"
    );
    ParallelWorkflow.WorkflowInput workflowInput =
            new ParallelWorkflow.WorkflowInput("Summarize the following topic:", inputs);

    String instanceId = daprWorkflowClient.scheduleNewWorkflow(ParallelWorkflow.class, workflowInput);

    WorkflowState status = daprWorkflowClient.waitForWorkflowCompletion(instanceId,
            Duration.ofSeconds(30), true);

    assertThat(status.isCompleted()).isTrue();
    assertThat(activityCallCounter.getCount(ParallelPromptActivity.class.getName())).isEqualTo(3);
  }
}
