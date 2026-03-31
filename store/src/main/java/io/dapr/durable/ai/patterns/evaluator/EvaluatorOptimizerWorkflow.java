package io.dapr.durable.ai.patterns.evaluator;

import io.dapr.durabletask.TaskFailedException;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EvaluatorOptimizerWorkflow implements Workflow {

  @Override
  public WorkflowStub create() {
    return ctx -> {
      ctx.getLogger().info("Starting Workflow: {}", ctx.getName());

      IteractionContext iteractionContext = ctx.getInput(IteractionContext.class);

      EvaluatorOptimizer.Generation generation = null;
      try {
        generation = ctx.callActivity(GenerateActivity.class.getName(),
                new ActivityInput(iteractionContext.task(), iteractionContext.context()),
                EvaluatorOptimizer.Generation.class).await();
      } catch (TaskFailedException tfe) {
        tfe.printStackTrace();
      }

      iteractionContext.memory().add(generation.response());
      iteractionContext.chainOfThought().add(generation);

      EvaluatorOptimizer.EvaluationResponse evaluationResponse = null;
      try {
        evaluationResponse = ctx.callActivity(EvaluateActivity.class.getName(),
                new ActivityInput(iteractionContext.task(), iteractionContext.context()),
                EvaluatorOptimizer.EvaluationResponse.class).await();
      } catch (TaskFailedException tfe) {
        tfe.printStackTrace();
      }

      if (evaluationResponse.evaluation().equals(EvaluatorOptimizer.EvaluationResponse.Evaluation.PASS)) {
        ctx.complete(new EvaluatorOptimizer.RefinedResponse(generation.response(), iteractionContext.chainOfThought()));
        return;
      }

      StringBuilder newContext = new StringBuilder();
      newContext.append("Previous attempts:");
      for (String m : iteractionContext.memory()) {
        newContext.append("\n- ").append(m);
      }
      newContext.append("\nFeedback: ").append(evaluationResponse.feedback());

      ctx.continueAsNew(new IteractionContext(iteractionContext.task(), newContext.toString(),
              iteractionContext.memory(), iteractionContext.chainOfThought()));
    };
  }

  public record IteractionContext(String task, String context, List<String> memory,
                                  List<EvaluatorOptimizer.Generation> chainOfThought) {}

  public record ActivityInput(String task, String context) {}
}
