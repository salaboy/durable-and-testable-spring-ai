package com.salaboy.durable.ai.chain;

import com.salaboy.durable.ai.ActivityCallCounter;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class ChainPromptActivity implements WorkflowActivity {

  private final ChatClient chatClient;
  private final ActivityCallCounter callCounter;

  public ChainPromptActivity(ChatClient.Builder chatClientBuilder, ActivityCallCounter callCounter) {
    this.chatClient = chatClientBuilder.build();
    this.callCounter = callCounter;
  }

  @Override
  public Object run(WorkflowActivityContext workflowActivityContext) {
    callCounter.increment(ChainPromptActivity.class.getName());
    String input = workflowActivityContext.getInput(String.class);
    return chatClient.prompt(input).call().content();
  }
}
