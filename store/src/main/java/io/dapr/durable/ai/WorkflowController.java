package io.dapr.durable.ai;

import io.dapr.durable.ai.patterns.chain.ChainWorkflow;
import io.dapr.durable.ai.patterns.evaluator.EvaluatorOptimizerWorkflow;
import io.dapr.durable.ai.patterns.orchestrator.OrchestratorWorkersWorkflow;
import io.dapr.durable.ai.patterns.parallel.ParallelWorkflow;
import io.dapr.durable.ai.patterns.routing.RoutingWorkflow;
import io.dapr.workflows.client.DaprWorkflowClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/workflows")
public class WorkflowController {

  private final DaprWorkflowClient daprWorkflowClient;

  public WorkflowController(DaprWorkflowClient daprWorkflowClient) {
    this.daprWorkflowClient = daprWorkflowClient;
  }

  @PostMapping("/chain")
  public String startChainWorkflow(@RequestBody String input) {
    String instanceId = daprWorkflowClient.scheduleNewWorkflow(ChainWorkflow.class, input);
    return instanceId;
  }

  @PostMapping("/orchestrator")
  public String startOrchestratorWorkflow(@RequestBody String taskDescription) {
    String instanceId = daprWorkflowClient.scheduleNewWorkflow(OrchestratorWorkersWorkflow.class, taskDescription);
    return instanceId;
  }

  @PostMapping("/parallel")
  public String startParallelWorkflow(@RequestBody ParallelWorkflow.WorkflowInput input) {
    String instanceId = daprWorkflowClient.scheduleNewWorkflow(ParallelWorkflow.class, input);
    return instanceId;
  }

  @PostMapping("/routing")
  public String startRoutingWorkflow(@RequestBody List<String> tickets) {
    String instanceId = daprWorkflowClient.scheduleNewWorkflow(RoutingWorkflow.class, tickets);
    return instanceId;
  }

  @PostMapping("/evaluator")
  public String startEvaluatorWorkflow(@RequestBody String task) {
    EvaluatorOptimizerWorkflow.IteractionContext iteractionContext = new EvaluatorOptimizerWorkflow
            .IteractionContext(task, "", new ArrayList<>(), new ArrayList<>());
    String instanceId = daprWorkflowClient.scheduleNewWorkflow(EvaluatorOptimizerWorkflow.class, iteractionContext);
    return instanceId;
  }
}
