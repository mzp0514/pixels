#pragma once

enum class ValueType {
  NONE,  // Useful when the type of column vector has not be determined yet.
  LONG,
  DOUBLE,
  BYTES,
  DECIMAL,
  TIMESTAMP,
  INTERVAL_DAY_TIME,
  STRUCT,
  LIST,
  MAP,
  UNION
};