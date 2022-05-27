#pragma once
#include <condition_variable>
#include <queue>
#include <thread>

#include "log4cxx/logger.h"
#include "vectorized_row_batch.h"

struct WriteTask {
  std::packaged_task<void()> task;
  std::function<void()> callback;
};

class Writer {
 public:
  Writer() {
    std::thread worker([&]() { Run(); });
    worker.detach();
  }

  void Submit(WriteTask& task) {
    std::unique_lock<std::mutex> lck(mu_);
    LOG4CXX_DEBUG(logger, "one task submitted...");
    std::cout << "one task submitted..." << std::endl;
    queue_.push(std::move(task));
    if (queue_.size() == 1) {
      cond_.notify_one();
    }
  }

  void Stop() { flag_ = true; }

 private:
  void Run() {
    std::cout << "flushing thread start..." << std::endl;
    LOG4CXX_DEBUG(logger, "flushing thread start...");
    while (true) {
      std::unique_lock<std::mutex> lck(mu_);
      while (queue_.empty()) {
        LOG4CXX_DEBUG(logger, "flushing thread waiting...");
        if (flag_) {
          return;
        }
        cond_.wait(lck);
      }
      WriteTask write_task = std::move(queue_.front());
      queue_.pop();
      std::cout << "flushing thread: a task fetched..." << std::endl;
      LOG4CXX_DEBUG(logger, "flushing thread: a task fetched...");

      write_task.task();

      std::cout << "flushing thread: task finished..." << std::endl;
      if (write_task.callback) {
        write_task.callback();
      }
      LOG4CXX_DEBUG(logger, "flushing thread: task finished...");
     
    }
  }

  std::mutex mu_;
  std::condition_variable cond_;
  std::queue<WriteTask> queue_;
  volatile bool flag_ = false;

  static log4cxx::LoggerPtr logger;
};

log4cxx::LoggerPtr Writer::logger(log4cxx::Logger::getLogger("Writer"));
