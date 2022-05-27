#pragma once
#include <unordered_map>

#include "log4cxx/logger.h"
#include "version.h"

class VersionManager {
 public:
  static void AddVersion(std::string key, long* delete_timestamps, long lwm,
                         int* indices, int size = 1024) {
    vms_[key] = new Version(delete_timestamps, lwm, indices, size);
    LOG4CXX_DEBUG_FMT(logger, "add new version {}", key);
  }

  static void SetDeleteIntent(std::string key, int rid, long ts) {
    vms_[key]->SetDeleteIntent(rid, ts);
  }

  static StatusCode GetVisibility(std::string key, long ts,
                            std::shared_ptr<stm::SharedMemory> mem, long pos) {
    if (vms_.find(key) == vms_.end()) {
      return StatusCode::kRgVisibilityNotExist;
    }
    vms_[key]->GetVisibility(ts, mem, pos);
    return StatusCode::kOk;
  }

  static std::unordered_map<std::string, Version*>
      vms_;  // s_name:t_name:rg_id -> vcs

 private:
  static log4cxx::LoggerPtr logger;
};

std::unordered_map<std::string, Version*> VersionManager::vms_;
log4cxx::LoggerPtr VersionManager::logger(
    log4cxx::Logger::getLogger("VersionManager"));
