#pragma once
#include "column_vector.h"

class LongColumnVector : public ColumnVector {
 public:
  LongColumnVector(int max_length) : ColumnVector(max_length) {
    value_type_ = ValueType::LONG;
    value_size_ = sizeof(long);
    buffer_ = new char[max_length * value_size_];
  }
};