# Durable and Testable Spring AI Workflows Workshop

This workshop guides you through implementing five common workflow patterns using Spring AI and Dapr Workflows. Each pattern demonstrates how to build durable, testable AI agents that can handle complex scenarios.

## Prerequisites

Before starting this workshop, ensure you have the following tools installed:

### Required Tools

1. **Java 21** - The project uses Java 21 features
   ```bash
   java -version
   ```

2. **Maven 3.6+** - For building the project
   ```bash
   mvn -version
   ```

3. **Docker** - Required for Dapr runtime components (via Testcontainers)
   ```bash
   docker --version
   ```

4. **OpenAI API Key** - Set as environment variable
   ```bash
   export SPRING_AI_OPENAI_API_KEY=your-api-key-here
   ```

### Initial Setup

1. Clone the repository and navigate to the project directory

2. Build the project:
   ```bash
   mvn clean install
   ```

3. Run the application with Dapr (started automatically via Testcontainers):
   ```bash
   mvn spring-boot:test-run
   ```

## Workshop Structure

This workshop covers five workflow patterns, each building on concepts from the previous sections:

1. **Chain Pattern** - Sequential processing through multiple stages
2. **Parallel Pattern** - Concurrent execution of independent tasks
3. **Routing Pattern** - Dynamic routing based on content analysis
4. **Orchestrator Pattern** - Breaking down tasks and distributing work
5. **Evaluator-Optimizer Pattern** - Iterative refinement with feedback loops

---

## Pattern 1: Chain Workflow

**Concept**: Process data through a series of sequential transformations, where each step's output becomes the next step's input.

**Use Case**: Transform unstructured performance metrics into a formatted markdown table through multiple refinement steps.

### Implementation Steps

#### Step 1.1: Create the Activity

Create `src/main/java/com/salaboy/durable/ai/chain/ChainPromptActivity.java`:

```java
package com.salaboy.durable.ai.chain;

import io.dapr.workflows.WorkflowActivityContext;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class ChainPromptActivity implements io.dapr.workflows.runtime.WorkflowActivity {

  private final ChatClient chatClient;

  public ChainPromptActivity(ChatClient chatClient) {
    this.chatClient = chatClient;
  }

  @Override
  public Object run(WorkflowActivityContext ctx) {
    String input = ctx.getInput(String.class);

    return chatClient.prompt()
        .user(input)
        .call()
        .content();
  }
}
```

#### Step 1.2: Create the Workflow

Create `src/main/java/com/salaboy/durable/ai/chain/ChainWorkflow.java`:

```java
package com.salaboy.durable.ai.chain;

import io.dapr.durable.ai.patterns.chain.ChainPromptActivity;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import org.springframework.stereotype.Component;

@Component
public class ChainWorkflow implements Workflow {

  private static final String[] DEFAULT_SYSTEM_PROMPTS = {
      // Step 1: Extract metrics
      """
    Extract only the numerical values and their associated metrics from the text.
    Format each as 'value: metric' on a new line.
    Example format:
    92: customer satisfaction
    45%: revenue growth""",

      // Step 2: Normalize to percentages
      """
    Convert all numerical values to percentages where possible.
    If not a percentage or points, convert to decimal (e.g., 92 points -> 92%).
    Keep one number per line.
    Example format:
    92%: customer satisfaction
    45%: revenue growth""",

      // Step 3: Sort by value
      """
    Sort all lines in descending order by numerical value.
    Keep the format 'value: metric' on each line.
    Example:
    92%: customer satisfaction
    87%: employee satisfaction""",

      // Step 4: Format as table
      """
    Format the sorted data as a markdown table with columns:
    | Metric | Value |
    |:--|--:|
    | Customer Satisfaction | 92% |"""
  };

  @Override
  public WorkflowStub create() {
    return ctx -> {
      ctx.getLogger().info("Starting Workflow: {}", ctx.getName());

      String userInput = ctx.getInput(String.class);
      String response = userInput;

      for (String prompt : DEFAULT_SYSTEM_PROMPTS) {
        String input = String.format("{%s}\n {%s}", prompt, response);
        response = ctx.callActivity(ChainPromptActivity.class.getName(),
            input, String.class).await();
        ctx.getLogger().info("Step completed: {}", response);
      }

      ctx.complete(response);
    };
  }
}
```

#### Step 1.3: Add Controller Endpoint

Add to `WorkflowController.java`:

```java
@PostMapping("/chain")
public String startChainWorkflow(@RequestBody String input) {
  String instanceId = daprWorkflowClient.scheduleNewWorkflow(ChainWorkflow.class, input);
  return instanceId;
}
```

#### Step 1.4: Test the Chain Workflow

```bash
curl -X POST http://localhost:8080/workflows/chain \
  -H "Content-Type: application/json" \
  -d '"Our quarterly results: customer satisfaction 92 points, revenue growth 45%, employee satisfaction 87 points, market share increased by 23%"'
```

**Key Concepts**:
- Sequential execution with `await()` between steps
- Each activity transforms the output for the next stage
- Workflow state is automatically persisted between steps

---

## Pattern 2: Parallel Workflow

**Concept**: Execute multiple independent operations concurrently and collect results.

**Use Case**: Apply the same prompt to multiple inputs simultaneously, useful for batch processing or comparing multiple items.

### Implementation Steps

#### Step 2.1: Create the Activity

Create `src/main/java/com/salaboy/durable/ai/parallel/ParallelPromptActivity.java`:

```java
package com.salaboy.durable.ai.parallel;

import io.dapr.durable.ai.patterns.parallel.ParallelWorkflow;
import io.dapr.workflows.WorkflowActivityContext;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class ParallelPromptActivity implements io.dapr.workflows.runtime.WorkflowActivity {

  private final ChatClient chatClient;

  public ParallelPromptActivity(ChatClient chatClient) {
    this.chatClient = chatClient;
  }

  @Override
  public Object run(WorkflowActivityContext ctx) {
    ParallelWorkflow.PromptInput input = ctx.getInput(ParallelWorkflow.PromptInput.class);

    return chatClient.prompt()
        .system(input.prompt())
        .user(input.input())
        .call()
        .content();
  }
}
```

#### Step 2.2: Create the Workflow

Create `src/main/java/com/salaboy/durable/ai/parallel/ParallelWorkflow.java`:

```java
package com.salaboy.durable.ai.parallel;

import io.dapr.durable.ai.patterns.parallel.ParallelPromptActivity;
import io.dapr.durabletask.Task;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class ParallelWorkflow implements Workflow {

  @Override
  public WorkflowStub create() {
    return ctx -> {
      ctx.getLogger().info("Starting Workflow: {}", ctx.getName());

      WorkflowInput workflowInput = ctx.getInput(WorkflowInput.class);

      // Create tasks for each input
      List<Task<String>> processTasks = workflowInput.inputs()
          .stream()
          .map(input -> ctx.callActivity(ParallelPromptActivity.class.getName(),
              new PromptInput(workflowInput.prompt(), input), String.class))
          .collect(Collectors.toList());

      // Wait for all tasks to complete
      List<String> responses = ctx.allOf(processTasks).await();

      ctx.complete(responses);
    };
  }

  public record WorkflowInput(String prompt, List<String> inputs) {
  }

  public record PromptInput(String prompt, String input) {
  }
}
```

#### Step 2.3: Add Controller Endpoint

```java
@PostMapping("/parallel")
public String startParallelWorkflow(@RequestBody ParallelWorkflow.WorkflowInput input) {
  String instanceId = daprWorkflowClient.scheduleNewWorkflow(ParallelWorkflow.class, input);
  return instanceId;
}
```

#### Step 2.4: Test the Parallel Workflow

```bash
curl -X POST http://localhost:8080/workflows/parallel \
  -H "Content-Type: application/json" \
  -d '{
    "prompt": "Summarize this product review in one sentence, focusing on the main sentiment",
    "inputs": [
      "This product exceeded my expectations! Great quality and fast shipping.",
      "Terrible experience. Product broke after one week. Not recommended.",
      "Decent product for the price. Works as advertised but nothing special."
    ]
  }'
```

**Key Concepts**:
- Use `ctx.allOf()` to execute activities in parallel
- All tasks start simultaneously, reducing total execution time
- Results are collected in the same order as inputs

---

## Pattern 3: Routing Workflow

**Concept**: Dynamically route requests to specialized handlers based on content analysis.

**Use Case**: Route customer support tickets to appropriate departments (billing, technical, account, product) with specialized response templates.

### Implementation Steps

#### Step 3.1: Create Supporting Classes

Create `src/main/java/com/salaboy/durable/ai/routing/PromptRequest.java`:

```java
package com.salaboy.durable.ai.routing;

public record PromptRequest(String prompt, String input) {}
```

Create `src/main/java/com/salaboy/durable/ai/routing/RoutingRequest.java`:

```java
package com.salaboy.durable.ai.routing;

import java.util.Set;

public record RoutingRequest(String input, Set<String> availableRoutes) {}
```

#### Step 3.2: Create the Route Determination Activity

Create `src/main/java/com/salaboy/durable/ai/routing/DetermineRouteActivity.java`:

```java
package com.salaboy.durable.ai.routing;

import io.dapr.durable.ai.patterns.routing.RoutingRequest;
import io.dapr.workflows.WorkflowActivityContext;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class DetermineRouteActivity implements io.dapr.workflows.runtime.WorkflowActivity {

  private final ChatClient chatClient;

  public DetermineRouteActivity(ChatClient chatClient) {
    this.chatClient = chatClient;
  }

  @Override
  public Object run(WorkflowActivityContext ctx) {
    RoutingRequest request = ctx.getInput(RoutingRequest.class);

    String prompt = String.format(
        "Analyze this support ticket and determine which category it belongs to: %s\n\n" +
            "Ticket: %s\n\n" +
            "Return only the category name, nothing else.",
        request.availableRoutes(), request.input());

    return chatClient.prompt()
        .user(prompt)
        .call()
        .content()
        .trim()
        .toLowerCase();
  }
}
```

#### Step 3.3: Create the Routing Activity

Create `src/main/java/com/salaboy/durable/ai/routing/RoutingPromptActivity.java`:

```java
package com.salaboy.durable.ai.routing;

import io.dapr.durable.ai.patterns.routing.PromptRequest;
import io.dapr.workflows.WorkflowActivityContext;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class RoutingPromptActivity implements io.dapr.workflows.runtime.WorkflowActivity {

  private final ChatClient chatClient;

  public RoutingPromptActivity(ChatClient chatClient) {
    this.chatClient = chatClient;
  }

  @Override
  public Object run(WorkflowActivityContext ctx) {
    PromptRequest request = ctx.getInput(PromptRequest.class);

    return chatClient.prompt()
        .system(request.prompt())
        .user(request.input())
        .call()
        .content();
  }
}
```

#### Step 3.4: Create the Workflow

Create `src/main/java/com/salaboy/durable/ai/routing/RoutingWorkflow.java`:

```java
package com.salaboy.durable.ai.routing;

import io.dapr.durable.ai.patterns.routing.DetermineRouteActivity;
import io.dapr.durable.ai.patterns.routing.PromptRequest;
import io.dapr.durable.ai.patterns.routing.RoutingPromptActivity;
import io.dapr.durable.ai.patterns.routing.RoutingRequest;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class RoutingWorkflow implements Workflow {

  Map<String, String> supportRoutes = Map.of(
      "billing", """
          You are a billing support specialist. Follow these guidelines:
          1. Always start with "Billing Support Response:"
          2. First acknowledge the specific billing issue
          3. Explain any charges or discrepancies clearly
          4. List concrete next steps with timeline
          5. End with payment options if relevant
          
          Keep responses professional but friendly.
          
          Input: """,

      "technical", """
          You are a technical support engineer. Follow these guidelines:
          1. Always start with "Technical Support Response:"
          2. List exact steps to resolve the issue
          3. Include system requirements if relevant
          4. Provide workarounds for common problems
          5. End with escalation path if needed
          
          Use clear, numbered steps and technical details.
          
          Input: """,

      "account", """
          You are an account security specialist. Follow these guidelines:
          1. Always start with "Account Support Response:"
          2. Prioritize account security and verification
          3. Provide clear steps for account recovery/changes
          4. Include security tips and warnings
          5. Set clear expectations for resolution time
          
          Maintain a serious, security-focused tone.
          
          Input: """,

      "product", """
          You are a product specialist. Follow these guidelines:
          1. Always start with "Product Support Response:"
          2. Focus on feature education and best practices
          3. Include specific examples of usage
          4. Link to relevant documentation sections
          5. Suggest related features that might help
          
          Be educational and encouraging in tone.
          
          Input: """);

  @Override
  public WorkflowStub create() {
    return ctx -> {
      ctx.getLogger().info("Starting Workflow: {}", ctx.getName());

      List<String> inputs = ctx.getInput(List.class);
      Assert.notNull(inputs, "Input text cannot be null");

      List<String> contents = new ArrayList<>();
      for (String input : inputs) {
        // Determine which route to use
        String routeKey = ctx.callActivity(DetermineRouteActivity.class.getName(),
            new RoutingRequest(input, supportRoutes.keySet()), String.class).await();

        String selectedPrompt = supportRoutes.get(routeKey);
        if (selectedPrompt == null) {
          throw new IllegalArgumentException("Selected route '" + routeKey + "' not found");
        }

        // Process with the selected route's prompt
        String content = ctx.callActivity(RoutingPromptActivity.class.getName(),
            new PromptRequest(selectedPrompt, input), String.class).await();
        contents.add(content);
      }

      ctx.complete(contents);
    };
  }
}
```

#### Step 3.5: Add Controller Endpoint

```java
@PostMapping("/routing")
public String startRoutingWorkflow(@RequestBody List<String> tickets) {
  String instanceId = daprWorkflowClient.scheduleNewWorkflow(RoutingWorkflow.class, tickets);
  return instanceId;
}
```

#### Step 3.6: Test the Routing Workflow

```bash
curl -X POST http://localhost:8080/workflows/routing \
  -H "Content-Type: application/json" \
  -d '[
    "I was charged twice for my subscription this month",
    "The app keeps crashing when I try to upload files",
    "I forgot my password and cannot access my account"
  ]'
```

**Key Concepts**:
- Two-stage processing: route determination then specialized handling
- Dynamic prompt selection based on content
- Maintains different "personas" for different domains

---

## Pattern 4: Orchestrator-Workers Workflow

**Concept**: Use an orchestrator to analyze and break down complex tasks, then distribute work to specialized workers in parallel.

**Use Case**: Break down a writing task into multiple approaches (formal, conversational) and execute them concurrently.

### Implementation Steps

#### Step 4.1: Create the Task Definition Activity

Create `src/main/java/com/salaboy/durable/ai/orchestrator/DefineTasksActivity.java`:

```java
package com.salaboy.durable.ai.orchestrator;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.durable.ai.patterns.orchestrator.OrchestratorWorkersWorkflow;
import io.dapr.workflows.WorkflowActivityContext;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class DefineTasksActivity implements io.dapr.workflows.runtime.WorkflowActivity {

  private final ChatClient chatClient;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public DefineTasksActivity(ChatClient chatClient) {
    this.chatClient = chatClient;
  }

  @Override
  public Object run(WorkflowActivityContext ctx) {
    String taskDescription = ctx.getInput(String.class);

    String prompt = String.format(
        OrchestratorWorkersWorkflow.DEFAULT_ORCHESTRATOR_PROMPT
            .replace("{task}", taskDescription));

    String response = chatClient.prompt()
        .user(prompt)
        .call()
        .content();

    try {
      return objectMapper.readValue(response,
          OrchestratorWorkersWorkflow.OrchestratorResponse.class);
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse orchestrator response", e);
    }
  }
}
```

#### Step 4.2: Create the Worker Activity

Create `src/main/java/com/salaboy/durable/ai/orchestrator/ProcessTaskActivity.java`:

```java
package com.salaboy.durable.ai.orchestrator;

import io.dapr.durable.ai.patterns.orchestrator.OrchestratorWorkersWorkflow;
import io.dapr.workflows.WorkflowActivityContext;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class ProcessTaskActivity implements io.dapr.workflows.runtime.WorkflowActivity {

  private final ChatClient chatClient;

  public ProcessTaskActivity(ChatClient chatClient) {
    this.chatClient = chatClient;
  }

  @Override
  public Object run(WorkflowActivityContext ctx) {
    OrchestratorWorkersWorkflow.SubTask task =
        ctx.getInput(OrchestratorWorkersWorkflow.SubTask.class);

    return chatClient.prompt()
        .user(task.description())
        .call()
        .content();
  }
}
```

#### Step 4.3: Create the Workflow

Create `src/main/java/com/salaboy/durable/ai/orchestrator/OrchestratorWorkersWorkflow.java`:

```java
package com.salaboy.durable.ai.orchestrator;

import io.dapr.durable.ai.patterns.orchestrator.DefineTasksActivity;
import io.dapr.durable.ai.patterns.orchestrator.ProcessTaskActivity;
import io.dapr.durabletask.Task;
import io.dapr.workflows.Workflow;
import io.dapr.workflows.WorkflowStub;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class OrchestratorWorkersWorkflow implements Workflow {

  public static final String DEFAULT_ORCHESTRATOR_PROMPT = """
      Analyze this task and break it down into 2-3 distinct approaches:
      
      Task: {task}
      
      Return your response in this JSON format:
      \\{
      "analysis": "Explain your understanding of the task and which variations would be valuable.
                   Focus on how each approach serves different aspects of the task.",
      "tasks": [
        \\{
        "type": "formal",
        "description": "Write a precise, technical version that emphasizes specifications"
        \\},
        \\{
        "type": "conversational",
        "description": "Write an engaging, friendly version that connects with readers"
        \\}
      ]
      \\}
      """;

  public static record OrchestratorResponse(String analysis, List<SubTask> tasks) {
  }

  public static record SubTask(String type, String description) {
  }

  public static record FinalResponse(String analysis, List<String> workerResponses) {
  }

  @Override
  public WorkflowStub create() {
    return ctx -> {
      ctx.getLogger().info("Starting Workflow: {}", ctx.getName());

      String taskDescription = ctx.getInput(String.class);

      // Phase 1: Orchestrator analyzes and breaks down task
      OrchestratorResponse orchestratorResponse =
          ctx.callActivity(DefineTasksActivity.class.getName(),
              taskDescription, OrchestratorResponse.class).await();

      ctx.getLogger().info("Analysis: {}", orchestratorResponse.analysis());

      // Phase 2: Workers execute tasks in parallel
      List<Task<String>> processTasks = orchestratorResponse.tasks()
          .stream()
          .map(task -> ctx.callActivity(ProcessTaskActivity.class.getName(),
              task, String.class))
          .collect(Collectors.toList());

      List<String> workerResponses = ctx.allOf(processTasks).await();

      ctx.complete(new FinalResponse(orchestratorResponse.analysis(), workerResponses));
    };
  }
}
```

#### Step 4.4: Add Controller Endpoint

```java
@PostMapping("/orchestrator")
public String startOrchestratorWorkflow(@RequestBody String taskDescription) {
  String instanceId = daprWorkflowClient.scheduleNewWorkflow(
      OrchestratorWorkersWorkflow.class, taskDescription);
  return instanceId;
}
```

#### Step 4.5: Test the Orchestrator Workflow

```bash
curl -X POST http://localhost:8080/workflows/orchestrator \
  -H "Content-Type: application/json" \
  -d '"Write a product announcement for our new AI-powered code review tool"'
```

**Key Concepts**:
- Two-phase execution: planning then parallel execution
- Orchestrator makes intelligent decisions about work distribution
- Combines sequential coordination with parallel execution

---

## Pattern 5: Evaluator-Optimizer Workflow

**Concept**: Iteratively refine outputs through a generate-evaluate-refine loop until quality criteria are met.

**Use Case**: Generate content, evaluate it against criteria, and automatically refine based on feedback until it passes evaluation.

### Implementation Steps

#### Step 5.1: Create Supporting Classes

Create `src/main/java/com/salaboy/durable/ai/evaluator/EvaluatorOptimizer.java`:

```java
package com.salaboy.durable.ai.evaluator;

import java.util.List;

public class EvaluatorOptimizer {

  public record Generation(String response, String reasoning) {}

  public record EvaluationResponse(Evaluation evaluation, String feedback) {
    public enum Evaluation { PASS, FAIL }
  }

  public record RefinedResponse(String finalResponse,
                                List<Generation> chainOfThought) {}
}
```

#### Step 5.2: Create the Generation Activity

Create `src/main/java/com/salaboy/durable/ai/evaluator/GenerateActivity.java`:

```java
package com.salaboy.durable.ai.evaluator;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.durable.ai.patterns.evaluator.EvaluatorOptimizer;
import io.dapr.durable.ai.patterns.evaluator.EvaluatorOptimizerWorkflow;
import io.dapr.workflows.WorkflowActivityContext;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class GenerateActivity implements io.dapr.workflows.runtime.WorkflowActivity {

  private final ChatClient chatClient;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public GenerateActivity(ChatClient chatClient) {
    this.chatClient = chatClient;
  }

  @Override
  public Object run(WorkflowActivityContext ctx) {
    EvaluatorOptimizerWorkflow.ActivityInput input =
        ctx.getInput(EvaluatorOptimizerWorkflow.ActivityInput.class);

    String prompt = String.format("""
            Task: %s
            
            %s
            
            Generate a response and explain your reasoning.
            Return JSON format:
            {
              "response": "your generated content",
              "reasoning": "explanation of your approach"
            }
            """, input.task(),
        input.context().isEmpty() ? "" : "\nContext:\n" + input.context());

    String response = chatClient.prompt()
        .user(prompt)
        .call()
        .content();

    try {
      return objectMapper.readValue(response, EvaluatorOptimizer.Generation.class);
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse generation response", e);
    }
  }
}
```

#### Step 5.3: Create the Evaluation Activity

Create `src/main/java/com/salaboy/durable/ai/evaluator/EvaluateActivity.java`:

```java
package com.salaboy.durable.ai.evaluator;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dapr.durable.ai.patterns.evaluator.EvaluatorOptimizer;
import io.dapr.durable.ai.patterns.evaluator.EvaluatorOptimizerWorkflow;
import io.dapr.workflows.WorkflowActivityContext;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class EvaluateActivity implements io.dapr.workflows.runtime.WorkflowActivity {

  private final ChatClient chatClient;
  private final ObjectMapper objectMapper = new ObjectMapper();

  public EvaluateActivity(ChatClient chatClient) {
    this.chatClient = chatClient;
  }

  @Override
  public Object run(WorkflowActivityContext ctx) {
    EvaluatorOptimizerWorkflow.ActivityInput input =
        ctx.getInput(EvaluatorOptimizerWorkflow.ActivityInput.class);

    String prompt = String.format("""
        Evaluate this response for the given task:
        
        Task: %s
        Context: %s
        
        Criteria:
        - Completeness: Addresses all aspects of the task
        - Clarity: Clear and well-structured
        - Accuracy: Information is correct
        
        Return JSON format:
        {
          "evaluation": "PASS" or "FAIL",
          "feedback": "specific feedback for improvement if failed"
        }
        """, input.task(), input.context());

    String response = chatClient.prompt()
        .user(prompt)
        .call()
        .content();

    try {
      return objectMapper.readValue(response,
          EvaluatorOptimizer.EvaluationResponse.class);
    } catch (Exception e) {
      throw new RuntimeException("Failed to parse evaluation response", e);
    }
  }
}
```

#### Step 5.4: Create the Workflow

Create `src/main/java/com/salaboy/durable/ai/evaluator/EvaluatorOptimizerWorkflow.java`:

```java
package com.salaboy.durable.ai.evaluator;

import io.dapr.durable.ai.patterns.evaluator.EvaluateActivity;
import io.dapr.durable.ai.patterns.evaluator.EvaluatorOptimizer;
import io.dapr.durable.ai.patterns.evaluator.GenerateActivity;
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

      // Generate response
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

      // Evaluate response
      EvaluatorOptimizer.EvaluationResponse evaluationResponse = null;
      try {
        evaluationResponse = ctx.callActivity(EvaluateActivity.class.getName(),
            new ActivityInput(iteractionContext.task(), generation.response()),
            EvaluatorOptimizer.EvaluationResponse.class).await();
      } catch (TaskFailedException tfe) {
        tfe.printStackTrace();
      }

      // If passed, return final result
      if (evaluationResponse.evaluation().equals(
          EvaluatorOptimizer.EvaluationResponse.Evaluation.PASS)) {
        ctx.complete(new EvaluatorOptimizer.RefinedResponse(
            generation.response(), iteractionContext.chainOfThought()));
        return;
      }

      // If failed, continue with feedback
      StringBuilder newContext = new StringBuilder();
      newContext.append("Previous attempts:");
      for (String m : iteractionContext.memory()) {
        newContext.append("\n- ").append(m);
      }
      newContext.append("\nFeedback: ").append(evaluationResponse.feedback());

      // Restart workflow with updated context
      ctx.continueAsNew(new IteractionContext(iteractionContext.task(),
          newContext.toString(), iteractionContext.memory(),
          iteractionContext.chainOfThought()));
    };
  }

  public record IteractionContext(String task, String context, List<String> memory,
                                  List<EvaluatorOptimizer.Generation> chainOfThought) {
  }

  public record ActivityInput(String task, String context) {
  }
}
```

#### Step 5.5: Add Controller Endpoint

```java
@PostMapping("/evaluator")
public String startEvaluatorWorkflow(@RequestBody String task) {
  EvaluatorOptimizerWorkflow.IteractionContext iteractionContext =
      new EvaluatorOptimizerWorkflow.IteractionContext(task, "",
          new ArrayList<>(), new ArrayList<>());
  String instanceId = daprWorkflowClient.scheduleNewWorkflow(
      EvaluatorOptimizerWorkflow.class, iteractionContext);
  return instanceId;
}
```

#### Step 5.6: Test the Evaluator-Optimizer Workflow

```bash
curl -X POST http://localhost:8080/workflows/evaluator \
  -H "Content-Type: application/json" \
  -d '"Write a comprehensive guide on how to set up a development environment for Spring Boot"'
```

**Key Concepts**:
- Iterative refinement using `ctx.continueAsNew()`
- Memory accumulation across iterations
- Self-improvement through evaluation feedback
- Chain of thought tracking for transparency

---

## Next Steps

Now that you've implemented all five workflow patterns, consider:

1. **Combine Patterns** - Use routing with orchestrator-workers for specialized task distribution
2. **Add Error Handling** - Implement retry logic and fallback strategies
3. **Monitoring** - Add metrics and logging to track workflow performance
4. **Testing** - Write unit and integration tests using Dapr test containers
5. **Scale** - Deploy to production with Dapr on Kubernetes

## Additional Resources

- [Dapr Workflows Documentation](https://docs.dapr.io/developing-applications/building-blocks/workflow/)
- [Spring AI Documentation](https://docs.spring.io/spring-ai/reference/)
- [Workshop Repository](https://github.com/salaboy/durable-and-testable-spring-ai)