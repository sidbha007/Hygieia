package com.capitalone.dashboard.plugin;

import com.capitalone.dashboard.model.Collector;
import com.capitalone.dashboard.model.CollectorType;
import com.capitalone.dashboard.plugin.core.HygieiaPlugin;
import com.capitalone.dashboard.plugin.metadata.HygieiaPluginMetadata;
import com.capitalone.dashboard.request.CollectorRequest;
import com.capitalone.dashboard.service.CollectorService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.List;

@Component
public class PluginWrapper {

  private static final Logger LOGGER = LoggerFactory.getLogger(PluginWrapper.class);

  @Autowired(required = false)
  private List<HygieiaPlugin> plugins;

  @Autowired
  private CollectorService collectorService;

  public List<HygieiaPlugin> getPlugins() {
    return (null != plugins ? Collections.unmodifiableList(plugins) : null);
  }

  /**
   * Start Plugins
   */
  public final void startPlugins() {
    LOGGER.info("startPlugins {}", plugins);
    if (null == plugins) {
      return;
    }

    for (HygieiaPlugin plugin : plugins) {
      HygieiaPluginMetadata pluginMetadata = plugin.getPluginMetadata();
      if (null == pluginMetadata) {
        continue;
      }

      createCollector(pluginMetadata);
      LOGGER.info("Start Plugin Name: {}, CollectorName: {}, CollectorType: {}",
        pluginMetadata.getName(),
        pluginMetadata.getCollectorName(),
        pluginMetadata.getCollectorType()
      );

      plugin.start();
    }
  }

  /**
   * Stop Plugins
   */
  public final void stopPlugins() {
    if (null == plugins) {
      return;
    }

    for (HygieiaPlugin plugin : plugins) {
      LOGGER.info("Stop Plugin Name: {}, CollectorName: {}, CollectorType: {}",
        plugin.getPluginMetadata().getName(),
        plugin.getPluginMetadata().getCollectorName(),
        plugin.getPluginMetadata().getCollectorType()
      );
      plugin.stop();
    }
  }

  private Collector createCollector(HygieiaPluginMetadata pluginMetadata) {
    CollectorRequest collectorReq = new CollectorRequest();
    collectorReq.setName(pluginMetadata.getCollectorName());  //for now hardcode it.
    collectorReq.setCollectorType(CollectorType.fromString(pluginMetadata.getCollectorType()));
    Collector col = collectorReq.toCollector();
    col.setEnabled(true);
    col.setOnline(true);
    col.setLastExecuted(System.currentTimeMillis());

    return collectorService.createCollector(col);
  }

  @PostConstruct
  public void init(){
    startPlugins();
  }
}
