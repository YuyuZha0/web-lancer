package com.tencent.weblancer.web.conf;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * @author fishzhao
 * @since 2021-02-08
 */
@Slf4j
public final class FileInterfaceDefinitionSupplier
    implements Supplier<List<DynamicInterfaceDefinition>> {

  private final AppMetaConfig appMetaConfig;
  private final ObjectMapper configMapper;

  public FileInterfaceDefinitionSupplier(
      @NonNull AppMetaConfig appMetaConfig, @NonNull ObjectMapper configMapper) {
    this.appMetaConfig = appMetaConfig;
    this.configMapper = configMapper;
  }

  @Override
  public List<DynamicInterfaceDefinition> get() {
    List<DynamicInterfaceDefinition> dynamicInterfaceDefinitions = new ArrayList<>();
    for (String strPath : appMetaConfig.getInterfaceDefinitionPath()) {
      Path path = Paths.get(strPath);
      if (Files.isDirectory(path)) {
        try (Stream<Path> pathStream = Files.walk(path)) {
          pathStream
              .filter(Files::isRegularFile)
              .forEach(p -> addInterfaceDefinition(dynamicInterfaceDefinitions, p));
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      } else if (Files.isRegularFile(path)) {
        addInterfaceDefinition(dynamicInterfaceDefinitions, path);
      } else {
        log.warn("Path [{}] is not valid and will be skipped!", path);
      }
    }
    log.info("[{}] interface(s) detected.", dynamicInterfaceDefinitions.size());
    return dynamicInterfaceDefinitions;
  }

  private void addInterfaceDefinition(
      List<DynamicInterfaceDefinition> dynamicInterfaceDefinitions, Path path) {
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
