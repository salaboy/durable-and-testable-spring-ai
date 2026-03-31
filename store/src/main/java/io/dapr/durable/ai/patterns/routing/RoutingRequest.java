package io.dapr.durable.ai.patterns.routing;

public record RoutingRequest(String input, Iterable<String> availableRoutes) {
}
