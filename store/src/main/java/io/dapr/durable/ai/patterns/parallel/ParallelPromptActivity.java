package io.dapr.durable.ai.patterns.parallel;

import io.dapr.durable.ai.ActivityCallCounter;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class ParallelPromptActivity implements WorkflowActivity {

  private final ChatClient chatClient;
  private final ActivityCallCounter callCounter;

  public ParallelPromptActivity(ChatClient.Builder chatClientBuilder, ActivityCallCounter callCounter) {
    this.chatClient = chatClientBuilder.build();
    this.callCounter = callCounter;
  }

  @Override
  public Object run(WorkflowActivityContext workflowActivityContext) {
    callCounter.increment(ParallelPromptActivity.class.getName());
    ParallelWorkflow.PromptInput promptInput = workflowActivityContext.getInput(ParallelWorkflow.PromptInput.class);
    return chatClient.prompt(promptInput.prompt() + "\nInput: " + promptInput.input()).call().content();
  }
}
