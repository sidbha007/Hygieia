package com.capitalone.dashboard.plugin.core;

import com.capitalone.dashboard.plugin.metadata.HygieiaPluginMetadata;

/**
 * Interface for Hygieia Plugin
 */
public interface HygieiaPlugin {

  /**
   * On Start of Plugin
   */
  void start();

  /**
   * On Stop of Plugin
   */
  void stop();

  /**
   * Metadata for Plugin
   * @return metadata for plugin {com.capitalone.dashboard.plugin.metadata.HygieiaPluginMetadata}
   */
  HygieiaPluginMetadata getPluginMetadata();

}
