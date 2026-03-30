package com.salaboy.durable.ai.routing;

import com.salaboy.durable.ai.ActivityCallCounter;
import io.dapr.workflows.WorkflowActivity;
import io.dapr.workflows.WorkflowActivityContext;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component
public class DetermineRouteActivity implements WorkflowActivity {

  private final ChatClient chatClient;
  private final ActivityCallCounter callCounter;

  public DetermineRouteActivity(ChatClient.Builder chatClientBuilder, ActivityCallCounter callCounter) {
    this.chatClient = chatClientBuilder.build();
    this.callCounter = callCounter;
  }

  @Override
  public Object run(WorkflowActivityContext ctx) {
    callCounter.increment(DetermineRouteActivity.class.getName());
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

    RoutingResponse routingResponse = chatClient.prompt(selectorPrompt).call().entity(RoutingResponse.class);

    return routingResponse.selection();
  }
}
