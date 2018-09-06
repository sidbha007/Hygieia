package com.capitalone.dashboard.plugin;

import com.capitalone.dashboard.plugin.core.HygieiaPlugin;
import com.capitalone.dashboard.plugin.metadata.HygieiaPluginMetadata;
import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

@Component
public class CodeQualityApiPlugin implements HygieiaPlugin {

  private static final Logger LOGGER = Logger.getLogger(CodeQualityApiPlugin.class);

  private HygieiaPluginMetadata hygieiaPluginMetadata = new CodeQualityApiPluginMetadata();

  @Override public void start() {
    LOGGER.info("Start CodeQualityApiPlugin");
  }

  @Override public void stop() {
    LOGGER.info("Stop CodeQualityApiPlugin");
  }

  @Override public HygieiaPluginMetadata getPluginMetadata() {
    return hygieiaPluginMetadata;
  }
}
