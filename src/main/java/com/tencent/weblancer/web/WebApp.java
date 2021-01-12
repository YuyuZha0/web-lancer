package com.tencent.weblancer.web;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencent.weblancer.web.conf.AppMetaConfig;
import com.tencent.weblancer.web.conf.DynamicInterfaceConfig;
import com.tencent.weblancer.web.conf.JsonInterfaceConfig;
import com.tencent.weblancer.web.repo.DataSourceRegistry;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
public final class WebApp implements AutoCloseable {

  private final AppMetaConfig metaConfig;
  private final ObjectMapper configMapper;

  public static void main(String[] args) throws Exception {

    ObjectMapper configMapper =
        new ObjectMapper()
            .enable(MapperFeature.ALLOW_COERCION_OF_SCALARS)
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
    List<DynamicInterfaceConfig> dynamicInterfaceConfigs = new ArrayList<>();
    for (String strPath : metaConfig.getInterfaceConfigPath()) {
      Path path = Paths.get(strPath);
      if (!Files.isReadable(path)) {
        log.warn("Path [{}] is not readable ank will be skipped!", path);
        continue;
      }
      if (Files.isDirectory(path)) {
        Files.walk(path)
            .filter(Files::isReadable)
            .forEach(p -> addInterfaceConfig(dynamicInterfaceConfigs, p));
      } else {
        addInterfaceConfig(dynamicInterfaceConfigs, path);
      }
    }
    log.info("[{}] interface(s) detected.", dynamicInterfaceConfigs.size());

    Vertx vertx = Vertx.vertx(new VertxOptions().setPreferNativeTransport(true));
    HttpRequestHandlerBuilder httpRequestHandlerBuilder =
        new HttpRequestHandlerBuilder(vertx, dataSourceRegistry, dynamicInterfaceConfigs);

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
                                })));
  }

  private void addInterfaceConfig(List<DynamicInterfaceConfig> dynamicInterfaceConfigs, Path path) {
    try (InputStream in = Files.newInputStream(path, StandardOpenOption.READ)) {
      List<JsonInterfaceConfig> jsonInterfaceConfigList =
          configMapper.readValue(
              in,
              configMapper
                  .getTypeFactory()
                  .constructCollectionType(ArrayList.class, JsonInterfaceConfig.class));
      if (jsonInterfaceConfigList != null && !jsonInterfaceConfigList.isEmpty()) {
        log.info(
            "Found [{}] interface config item(s) in file: {}",
            jsonInterfaceConfigList.size(),
            path);
        dynamicInterfaceConfigs.addAll(jsonInterfaceConfigList);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void close() {}
}
