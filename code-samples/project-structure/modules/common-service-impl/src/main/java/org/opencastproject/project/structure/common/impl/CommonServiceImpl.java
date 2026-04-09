package org.opencastproject.project.structure.common.impl;

import org.opencastproject.project.structure.common.impl.api.CommonService;

import java.util.List;

import io.quarkus.runtime.configuration.ConfigUtils;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class CommonServiceImpl implements CommonService {

  @Override
  public List<String> getQuarkusProfiles() {
    return ConfigUtils.getProfiles();
    //return ConfigProvider.getConfig().unwrap(SmallRyeConfig.class).getProfiles();
  }

  @Override
  public List<String> getQuarkusActiveProfiles() {
    return getQuarkusProfiles().stream()
        .filter(ConfigUtils::isProfileActive)
        .toList();
  }
}
