package com.tencent.weblancer.web.conf;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

/**
 * @author fishzhao
 * @since 2021-01-12
 */
@Getter
@ToString
public final class AppMetaConfig {

  private final int serverPort;
  private final List<ObjectNode> dataSources;
  private final List<String> interfaceDefinitionPath;

  @JsonCreator
  public AppMetaConfig(
      @JsonProperty("serverPort") int serverPort,
      @JsonProperty("dataSources") @JsonAlias("dataSource") List<ObjectNode> dataSources,
      @JsonProperty("interfaceDefinitionPath") @JsonAlias("interfaceDefinitionPaths")
          List<String> interfaceDefinitionPath) {
    this.serverPort = serverPort;
    this.dataSources = dataSources;
    this.interfaceDefinitionPath = interfaceDefinitionPath;
  }

  public void validate() {
    Preconditions.checkArgument(
        serverPort > 0 && serverPort <= 65535, "illegal serverPort: %s", serverPort);
    Preconditions.checkArgument(
        dataSources != null && !dataSources.isEmpty(), "empty dataSources!");
    for (ObjectNode objectNode : dataSources) {
      Preconditions.checkArgument(
          objectNode.hasNonNull("id"), "`id` is required in dataSource: %s", objectNode);
    }
    Preconditions.checkArgument(
        interfaceDefinitionPath != null && !interfaceDefinitionPath.isEmpty(),
        "empty interfaceDefinitionPath!");
  }
}
