package com.tencent.weblancer.web;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.tencent.weblancer.web.conf.AppMetaConfig;
import com.tencent.weblancer.web.conf.DynamicInterfaceDefinition;
import com.tencent.weblancer.web.conf.JsonInterfaceDefinition;
import com.tencent.weblancer.web.repo.DataSourceRegistry;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;

/**
 * @author fishzhao
 * @since 2021-01-12
 */
@RequiredArgsConstructor
@Slf4j
public final class WebApp {

  private final AppMetaConfig metaConfig;
  private final ObjectMapper configMapper;

  public static void main(String[] args) throws Exception {

    Preconditions.checkArgument(args != null && StringUtils.isNotBlank(args[0]), "config file path is required at args[0]!");

    ObjectMapper configMapper =
            new ObjectMapper()
                    .enable(JsonReadFeature.ALLOW_UNESCAPED_CONTROL_CHARS.mappedFeature())
                    .enable(JsonParser.Feature.ALLOW_COMMENTS)
                    .enable(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

    AppMetaConfig metaConfig;
    try (InputStream in = Files.newInputStream(Paths.get(args[0]), StandardOpenOption.READ)) {
      metaConfig = configMapper.readValue(in, AppMetaConfig.class);
    }

    new WebApp(metaConfig, configMapper).start();
  }

  private void start() throws Exception {
    metaConfig.validate();
    DataSourceRegistry dataSourceRegistry = new DataSourceRegistry();
    metaConfig
            .getDataSources()
            .forEach(
                    objectNode ->
                            dataSourceRegistry.registerDataSourceConfig(
                                    objectNode.remove("id").asText(), objectNode));
    List<DynamicInterfaceDefinition> dynamicInterfaceDefinitions = new ArrayList<>();
    for (String strPath : metaConfig.getInterfaceDefinitionPath()) {
      Path path = Paths.get(strPath);
      if (!Files.isReadable(path)) {
        log.warn("Path [{}] is not readable ank will be skipped!", path);
        continue;
      }
      if (Files.isDirectory(path)) {
        Files.walk(path)
                .filter(Files::isReadable)
                .forEach(p -> addInterfaceDefinition(dynamicInterfaceDefinitions, p));
      } else {
        addInterfaceDefinition(dynamicInterfaceDefinitions, path);
      }
    }
    log.info("[{}] interface(s) detected.", dynamicInterfaceDefinitions.size());

    Vertx vertx = Vertx.vertx(new VertxOptions().setPreferNativeTransport(true));
    HttpRequestHandlerBuilder httpRequestHandlerBuilder =
            new HttpRequestHandlerBuilder(vertx, dataSourceRegistry, dynamicInterfaceDefinitions);

    HttpServer httpServer =
            vertx.createHttpServer(
                    new HttpServerOptions().setTcpFastOpen(true).setTcpNoDelay(true).setTcpQuickAck(true));

    httpServer
            .requestHandler(httpRequestHandlerBuilder.get())
            .listen(metaConfig.getServerPort())
            .onSuccess(
                    s -> log.info("Start httpServer successfully listening on port: {}", s.actualPort()));

    Runtime.getRuntime()
            .addShutdownHook(
                    Executors.defaultThreadFactory()
                            .newThread(
                                    () ->
                                            httpServer
                                                    .close()
                                                    .onSuccess(
                                                            v -> {
                                                              log.info("HttpServer shutdown successfully!");
                                                              vertx.close();
                                                              dataSourceRegistry.close();
                                                            })));
  }

  private void addInterfaceDefinition(List<DynamicInterfaceDefinition> dynamicInterfaceDefinitions, Path path) {
    try (InputStream in = Files.newInputStream(path, StandardOpenOption.READ)) {
      List<JsonInterfaceDefinition> jsonInterfaceDefinitions =
              configMapper.readValue(
                      in,
                      configMapper
                              .getTypeFactory()
                              .constructCollectionType(ArrayList.class, JsonInterfaceDefinition.class));
      if (jsonInterfaceDefinitions != null && !jsonInterfaceDefinitions.isEmpty()) {
        log.info(
                "Found [{}] interface config item(s) in file: {}",
                jsonInterfaceDefinitions.size(),
                path);
        dynamicInterfaceDefinitions.addAll(jsonInterfaceDefinitions);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

}
