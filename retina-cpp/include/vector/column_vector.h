#pragma once
#include <algorithm>
#include <cstring>
#include <iostream>
#include <sstream>
#include <string>

#include "mq/shared_memory.h"
#include "rocksdb/slice.h"
#include "types.h"

using Slice = rocksdb::Slice;

class ColumnVector {
 public:
  ColumnVector(int max_length) {
    max_length_ = max_length;
    cur_length_ = 0;
  };

  ~ColumnVector() {
    if (buffer_) delete[] buffer_;
  }

  virtual bool Add(int index, Slice& value) {
    assert(value.size_ == value_size_);
    memcpy(buffer_ + index * value_size_, value.data_, value_size_);
    cur_length_ = std::max(index + 1, cur_length_);
    return true;
  };

  virtual bool FilteredFlush(std::shared_ptr<stm::SharedMemory> mem, long& pos,
                             int indices[], int len) {
    mem->setShort(pos, static_cast<short>(value_type_));  // type
    pos += sizeof(short);

    mem->setInt(pos, len * value_size_);  // length
    pos += sizeof(int);

    for (int i = 0; i < len; i++) {  // data
      mem->setBytes(pos, (stm::byte*)buffer_, indices[i] * value_size_,
                    value_size_);
      pos += value_size_;
    }
    return true;
  };

  virtual bool SortedFlush(std::shared_ptr<stm::SharedMemory> mem, long& pos,
                           int indices[]) {
    mem->setShort(pos, static_cast<short>(value_type_));  // type
    pos += sizeof(short);

    mem->setInt(pos, cur_length_ * value_size_);  // length
    pos += sizeof(int);

    for (int i = 0; i < cur_length_; i++) {
      mem->setBytes(pos, (stm::byte*)buffer_, indices[i] * value_size_,
                    value_size_);  // data
      pos += value_size_;
    }
    return true;
  };

  virtual Slice operator[](int index) {
    return Slice(buffer_ + index * value_size_, value_size_);
  }

  ValueType value_type() const { return value_type_; }

  char* buffer() { return buffer_; }

 protected:
  ValueType value_type_;
  int value_size_;
  int cur_length_;
  int max_length_;
  char* buffer_;
};