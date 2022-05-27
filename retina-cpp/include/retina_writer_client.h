#pragma once
#include <grpcpp/grpcpp.h>

#include <iostream>
#include <memory>
#include <string>

#include "retina_writer.grpc.pb.h"

using grpc::Channel;
using grpc::ClientContext;
using grpc::Status;
using retina_writer::proto::FlushRequest;
using retina_writer::proto::FlushResponse;
using retina_writer::proto::RetinaWriterService;

class RetinaWriterClient {
 public:
  RetinaWriterClient(std::shared_ptr<Channel> channel)
      : stub_(RetinaWriterService::NewStub(channel)) {}

  // Assembles the client's payload, sends it and presents the response back
  // from the server.
  long Flush(std::string& schema_name, std::string& table_name, int& rgid,
             long& pos, std::string& file_path) {
    // Data we are sending to the server.
    FlushRequest request;
    request.set_schemaname(schema_name);
    request.set_tablename(table_name);
    request.set_rgid(rgid);
    request.set_filepath(file_path);
    request.set_pos(pos);

    // Container for the data we expect from the server.
    FlushResponse reply;

    // Context for the client. It could be used to convey extra information to
    // the server and/or tweak certain RPC behaviors.
    ClientContext context;

    // The actual RPC.
    Status status = stub_->Flush(&context, request, &reply);

    // Act upon its status.
    if (status.ok()) {
      return pos;
    } else {
      std::cout << status.error_code() << ": " << status.error_message()
                << std::endl;
      return -1;
    }
  }

 private:
  std::unique_ptr<RetinaWriterService::Stub> stub_;
};