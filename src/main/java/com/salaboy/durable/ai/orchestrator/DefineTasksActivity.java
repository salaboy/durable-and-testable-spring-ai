package com.salaboy.durable.ai.orchestrator;

import com.salaboy.durable.ai.ActivityCallCounter;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static com.salaboy.durable.ai.orchestrator.OrchestratorWorkersWorkflow.DEFAULT_ORCHESTRATOR_PROMPT;

@Component
public class DefineTasksActivity implements WorkflowActivity {

  @Autowired
  private ChatClient chatClient;

  @Autowired
  private ActivityCallCounter callCounter;

  @Override
  public Object run(WorkflowActivityContext workflowActivityContext) {
    callCounter.increment(DefineTasksActivity.class.getName());
    var taskDescription = workflowActivityContext.getInput(String.class);
    return this.chatClient.prompt()
            .user(u -> u.text(DEFAULT_ORCHESTRATOR_PROMPT)
                    .param("task", taskDescription))
            .call()
            .entity(OrchestratorWorkersWorkflow.OrchestratorResponse.class);
  }
}
