package io.dapr.durable.ai.patterns.evaluator;

import io.dapr.durable.ai.ActivityCallCounter;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class GenerateActivity implements WorkflowActivity {

  private final ChatClient chatClient;
  private final ActivityCallCounter callCounter;

  public GenerateActivity(ChatClient.Builder chatClientBuilder, ActivityCallCounter callCounter) {
    this.chatClient = chatClientBuilder.build();
    this.callCounter = callCounter;
  }

  @Override
  public Object run(WorkflowActivityContext workflowActivityContext) {
    callCounter.increment(GenerateActivity.class.getName());
    EvaluatorOptimizerWorkflow.ActivityInput input =
            workflowActivityContext.getInput(EvaluatorOptimizerWorkflow.ActivityInput.class);

    EvaluatorOptimizer.Generation generationResponse = chatClient.prompt()
            .user(u -> u.text("{prompt}\n{context}\nTask: {task}")
                    .param("prompt", EvaluatorOptimizer.DEFAULT_GENERATOR_PROMPT)
                    .param("context", input.context())
                    .param("task", input.task()))
            .call()
            .entity(EvaluatorOptimizer.Generation.class);

    System.out.println(String.format("\n=== GENERATOR OUTPUT ===\nTHOUGHTS: %s\n\nRESPONSE:\n %s\n",
            generationResponse.thoughts(), generationResponse.response()));
    return generationResponse;
  }
}
