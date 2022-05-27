#pragma once

enum class StatusCode {
  kOk = 0,
  kKeyNotExist = 1,
  kIndexError = 2,
  kRgFlushed = 3,
  kRgVisibilityNotExist = 4
};
