#include <gtest/gtest.h>

#include <algorithm>
#include <chrono>
#include <iostream>

#include "retina_service_impl.h"

const int row_num = 791;
const int batch_num = 100;
std::string makeFixedLength(const int i, const int length) {
  std::ostringstream ostr;

  if (i < 0) ostr << '-';

  ostr << std::setfill('0') << std::setw(length) << (i < 0 ? -i : i);

  return ostr.str();
}

class RetinaTest : public ::testing::Test {
 protected:
  std::vector<Slice> make_tuple(long long_vals[], int long_len,
                                std::string str_vals[], int str_len) {
    std::vector<Slice> tuple;
    for (int i = 0; i < long_len; i++) {
      tuple.push_back(Slice((char*)(long_vals + i), sizeof(long)));
    }

    for (int i = 0; i < str_len; i++) {
      tuple.push_back(Slice(str_vals[i]));
    }
    return tuple;
  }

  void SetUp() override {
    service = new RetinaServiceImpl();
    for (int i = 0; i < 5; i++) {
      cols.push_back(ValueType::LONG);
    }
    for (int i = 0; i < 2; i++) {
      cols.push_back(ValueType::BYTES);
    }
    int u = 0;
    for (int k = 0; k < batch_num; k++) {
      for (int i = 0; i < row_num; i++) {
        indices[k][i] = i;
        for (int j = 0; j < 2; j++) {
          str_vals_[k][i][j] = makeFixedLength(u, 4);
        }
        for (int j = 0; j < 5; j++) {
          long_vals_[k][i][j] = u;
        }
        ver_vals_[k][i] = u;
        u++;
      }

      // std::sort(indices[k], indices[k] + row_num, [&](int i, int j) {
      //   return str_vals_[k][i][0] < str_vals_[k][j][0];
      // });
    }
  }

  void TearDown() override {
    std::cout << "Test tear down" << std::endl;
    if (service) {
      delete service;
      service = nullptr;
    }
  }

  std::vector<ValueType> cols;
  
  long long_vals_[batch_num][row_num][5];
  std::string str_vals_[batch_num][row_num][2];
  long ver_vals_[batch_num][row_num];
  int indices[batch_num][row_num];
  int pk_ = 6;
  RetinaServiceImpl* service;

  std::string schema_name_ = "s0";
  std::string table_name_ = "t0";

};

TEST_F(RetinaTest, TEST) {
  auto start = std::chrono::steady_clock::now();
  for (int k = 0; k < batch_num; k++) {
    auto long_vals_k = long_vals_[k];
    auto str_vals_k = str_vals_[k];
    auto ver_vals_k = ver_vals_[k];

    for (int i = 0; i < row_num; i++) {
      std::vector<Slice> values =
          make_tuple(long_vals_k[i], 5, str_vals_k[i], 2);
      service->Insert(schema_name_, table_name_, pk_, values, cols,
                      ver_vals_k[i]);
    }
  }
  auto end = std::chrono::steady_clock::now();

  std::cout << "Elapsed time in milliseconds: "
            << std::chrono::duration_cast<std::chrono::milliseconds>(end -
                                                                     start)
                   .count()
            << " ms" << std::endl;
}