package org.opencastproject.project.structure.common.impl.api;

import java.util.List;

public interface CommonService {
  List<String> getQuarkusProfiles();
  List<String> getQuarkusActiveProfiles();
}
