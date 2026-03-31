package com.salaboy.mcp.server;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Service;

@Service
public class WarehouseService {

  @McpTool(description = "Get current temperature for a location")
  public String getTemperature(
      @McpToolParam(description = "City name", required = true) String city) {
    return String.format("Current temperature in %s: 22°C", city);
  }
}
