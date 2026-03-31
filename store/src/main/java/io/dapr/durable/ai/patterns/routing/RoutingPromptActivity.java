package io.dapr.durable.ai.patterns.routing;

import io.dapr.durable.ai.ActivityCallCounter;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class RoutingPromptActivity implements WorkflowActivity {

  private final ChatClient chatClient;
  private final ActivityCallCounter callCounter;

  public RoutingPromptActivity(ChatClient.Builder chatClientBuilder, ActivityCallCounter callCounter) {
    this.chatClient = chatClientBuilder.build();
    this.callCounter = callCounter;
  }

  @Override
  public Object run(WorkflowActivityContext workflowActivityContext) {
    callCounter.increment(RoutingPromptActivity.class.getName());
    PromptRequest promptRequest = workflowActivityContext.getInput(PromptRequest.class);
    String prompt = promptRequest.prompt() + "\nInput: " + promptRequest.input();
    return chatClient.prompt(prompt).call().content();
  }
}
