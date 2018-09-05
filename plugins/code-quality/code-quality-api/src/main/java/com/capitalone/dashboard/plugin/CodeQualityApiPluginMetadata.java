package com.capitalone.dashboard.plugin;

import com.capitalone.dashboard.model.CollectorType;
import com.capitalone.dashboard.plugin.metadata.HygieiaPluginMetadata;

public class CodeQualityApiPluginMetadata implements HygieiaPluginMetadata {
  @Override public String getCollectorType() {
    return CollectorType.CodeQuality.toString();
  }

  @Override public String getCollectorName() {
    return "Sonar";
  }

  @Override public String getName() {
    return "Code Quality API Plugin";
  }
}
