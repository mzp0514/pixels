#pragma once
#include <atomic>

class RowGroupIdManager {
 public:
  static int next_rgid() { return next_rgid_++; }

 private:
  static std::atomic_int next_rgid_;
};

std::atomic_int RowGroupIdManager::next_rgid_(0);