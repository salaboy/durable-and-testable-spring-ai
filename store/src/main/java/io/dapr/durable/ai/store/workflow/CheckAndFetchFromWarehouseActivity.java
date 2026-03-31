package io.dapr.durable.ai.store.workflow;

import io.dapr.durable.ai.ActivityCallCounter;
import io.dapr.durable.ai.patterns.routing.RoutingRequest;
import io.dapr.durable.ai.patterns.routing.RoutingResponse;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.stereotype.Component;

@Component
public class CheckAndFetchFromWarehouseActivity implements WorkflowActivity {

  private final ChatClient chatClient;
  private final ActivityCallCounter callCounter;
  private final ToolCallbackProvider mcpTools;

  public CheckAndFetchFromWarehouseActivity(ChatClient.Builder chatClientBuilder,
                                            ActivityCallCounter callCounter,
                                            ToolCallbackProvider mcpTools) {
    this.chatClient = chatClientBuilder.build();
    this.callCounter = callCounter;
    this.mcpTools = mcpTools;
  }

  @Override
  public Object run(WorkflowActivityContext ctx) {
    callCounter.increment(CheckAndFetchFromWarehouseActivity.class.getName());
    RoutingRequest routingRequest = ctx.getInput(RoutingRequest.class);

    String selectorPrompt = String.format("""
                Analyze the input and select the most appropriate support team from these options: %s
                First explain your reasoning, then provide your selection in this JSON format:

                \\{
                    "reasoning": "Brief explanation of why this ticket should be routed to a specific team.
                                Consider key terms, user intent, and urgency level.",
                    "selection": "The chosen team name"
                \\}

                Input: %s""", routingRequest.availableRoutes(), routingRequest.input());

    RoutingResponse routingResponse = chatClient
        .prompt(selectorPrompt)
        .toolCallbacks(mcpTools)
        .call()
        .entity(RoutingResponse.class);

    return routingResponse.selection();
  }
}
