#include "version.h"

#include <gtest/gtest.h>

#include <algorithm>
#include <cstdio>
#include <iostream>

#include "mq/shared_memory.h"
#include "test_utils.h"

std::shared_ptr<stm::SharedMemory> mem =
    std::make_shared<stm::SharedMemory>("/dev/shm/version", 4096);

class VersionTest : public ::testing::Test {
 protected:
  void SetUp() override {
    vm = new Version(1024);
    for (int i = 0; i < 6; i++) {
      vm->SetDeleteIntent(delete_rids_[i], delete_ts_[i]);
    }
  }

  // void TearDown() override {}

  int delete_rids_[6] = {1023, 0, 10, 2, 9, 7};
  long delete_ts_[6] = {0, 1, 2, 2, 3, 3};

  Version* vm;
};

TEST_F(VersionTest, TESTDELETEINTENT) {
  long pos = 0;
  long query_ts = 2;
  vm->GetVisibility(query_ts, mem, pos);
  pos = 0;
  int len = mem->getInt(pos);
  ASSERT_EQ(len, 1024 >> 3);
  pos += sizeof(int);
  uint64_t buf[len / sizeof(uint64_t)];
  mem->getBytes(pos, (stm::byte*)buf, 0, len);

  uint64_t visibility_res[1024 >> 6];
  memset(visibility_res, 255, sizeof(visibility_res));
  SetInvisible(visibility_res, 1023);
  SetInvisible(visibility_res, 0);
  SetInvisible(visibility_res, 10);
  SetInvisible(visibility_res, 2);
  for (int i = 0; i < len / sizeof(uint64_t); i++) {
    ASSERT_EQ(visibility_res[i], buf[i]);
  }
}

TEST_F(VersionTest, TESTFLUSH) {
  vm->ClearDeleted(2);
  ASSERT_EQ(vm->GetHashmapSize(), 2);

  long pos = 0;
  long query_ts = 2;
  vm->GetVisibility(query_ts, mem, pos);
  pos = 0;
  int len = mem->getInt(pos);
  ASSERT_EQ(len, 1024 >> 3);
  pos += sizeof(int);
  uint64_t buf[len / sizeof(uint64_t)];
  mem->getBytes(pos, (stm::byte*)buf, 0, len);

  uint64_t visibility_res[1024 >> 6];
  memset(visibility_res, 255, sizeof(visibility_res));
  SetInvisible(visibility_res, 1023);
  SetInvisible(visibility_res, 0);
  SetInvisible(visibility_res, 10);
  SetInvisible(visibility_res, 2);
  for (int i = 0; i < len / sizeof(uint64_t); i++) {
    ASSERT_EQ(visibility_res[i], ((uint64_t*)buf)[i]);
  }
}