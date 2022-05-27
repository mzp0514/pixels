#pragma once
#include <atomic>

class WaterMarkManager {
 public:
  static void set_hwm(long hwm) {
    if (hwm > hwm_) hwm_ = hwm;
  }

  static void set_lwm(long lwm) {
    if (lwm > lwm_) lwm_ = lwm;
  }

  static long lwm() { return lwm_; }

  static long hwm() { return hwm_; }

 private:
  static std::atomic_long lwm_;
  static std::atomic_long hwm_;
};

std::atomic_long WaterMarkManager::lwm_(0);
std::atomic_long WaterMarkManager::hwm_(0);