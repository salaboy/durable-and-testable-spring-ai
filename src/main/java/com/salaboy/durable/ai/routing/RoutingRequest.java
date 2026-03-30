package com.salaboy.durable.ai.routing;

public record RoutingRequest(String input, Iterable<String> availableRoutes) {
}
