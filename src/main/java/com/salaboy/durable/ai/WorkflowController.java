package com.salaboy.durable.ai;

import com.salaboy.durable.ai.chain.ChainWorkflow;
import com.salaboy.durable.ai.evaluator.EvaluatorOptimizer;
import com.salaboy.durable.ai.evaluator.EvaluatorOptimizerWorkflow;
import com.salaboy.durable.ai.orchestrator.OrchestratorWorkersWorkflow;
import com.salaboy.durable.ai.parallel.ParallelWorkflow;
import com.salaboy.durable.ai.routing.RoutingWorkflow;
import io.dapr.workflows.client.DaprWorkflowClient;
import io.dapr.workflows.client.WorkflowInstanceStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
