package io.dapr.durable.ai.patterns.orchestrator;

import io.dapr.durable.ai.ActivityCallCounter;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProcessTaskActivity implements WorkflowActivity {

  public static final String DEFAULT_WORKER_PROMPT = """
          Generate content based on:
          Task: {original_task}
          Style: {task_type}
          Guidelines: {task_description}
          """;

  @Autowired
  private ChatClient chatClient;

  @Autowired
  private ActivityCallCounter callCounter;

  @Override
  public Object run(WorkflowActivityContext workflowActivityContext) {
    callCounter.increment(ProcessTaskActivity.class.getName());
    var task = workflowActivityContext.getInput(OrchestratorWorkersWorkflow.SubTask.class);

    return this.chatClient.prompt()
            .user(u -> u.text(DEFAULT_WORKER_PROMPT)
                    .param("original_task", "Write a product description for a new eco-friendly water bottle")
                    .param("task_type", task.type())
                    .param("task_description", task.description()))
            .call()
            .content();
  }
}
