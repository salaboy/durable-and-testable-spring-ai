package com.salaboy.durable.ai.evaluator;

import com.salaboy.durable.ai.ActivityCallCounter;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class EvaluateActivity implements WorkflowActivity {

  private final ChatClient chatClient;
  private final ActivityCallCounter callCounter;

  public EvaluateActivity(ChatClient.Builder chatClientBuilder, ActivityCallCounter callCounter) {
    this.chatClient = chatClientBuilder.build();
    this.callCounter = callCounter;
  }

  @Override
  public Object run(WorkflowActivityContext workflowActivityContext) {
    callCounter.increment(EvaluateActivity.class.getName());
    EvaluatorOptimizerWorkflow.ActivityInput input =
            workflowActivityContext.getInput(EvaluatorOptimizerWorkflow.ActivityInput.class);

    EvaluatorOptimizer.EvaluationResponse evaluationResponse = chatClient.prompt()
            .user(u -> u.text("{prompt}\nOriginal task: {task}\nContent to evaluate: {content}")
                    .param("prompt", EvaluatorOptimizer.DEFAULT_EVALUATOR_PROMPT)
                    .param("task", input.task())
                    .param("content", input.context()))
            .call()
            .entity(EvaluatorOptimizer.EvaluationResponse.class);

    System.out.println(String.format("\n=== EVALUATOR OUTPUT ===\nEVALUATION: %s\n\nFEEDBACK: %s\n",
            evaluationResponse.evaluation(), evaluationResponse.feedback()));
    return evaluationResponse;
  }
}
