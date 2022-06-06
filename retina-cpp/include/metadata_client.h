/*
 * Copyright 2017-2019 PixelsDB.
 *
 * This file is part of Pixels.
 *
 * Pixels is free software: you can redistribute it and/or modify
 * it under the terms of the Affero GNU General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 *
 * Pixels is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * Affero GNU General Public License for more details.
 *
 * You should have received a copy of the Affero GNU General Public
 * License along with Pixels.  If not, see
 * <https://www.gnu.org/licenses/>.
 */
#pragma once
#include <grpcpp/grpcpp.h>

#include <iostream>
#include <memory>
#include <string>

#include "metadata.grpc.pb.h"

/**
 * @author mzp0514
 * @date 27/05/2022
 */

using grpc::Channel;
using grpc::ClientContext;
using grpc::Status;
using metadata::proto::RowGroup;
using metadata::proto::AddRowGroupRequest;
using metadata::proto::AddRowGroupResponse;
using metadata::proto::MetadataService;
using metadata::proto::UpdateRowGroupRequest;
using metadata::proto::UpdateRowGroupResponse;

class MetadataClient {
 public:
  MetadataClient(std::shared_ptr<Channel> channel)
      : stub_(MetadataService::NewStub(channel)) {}

  // uint64 id = 1;
  // uint64 layoutId = 2;
  // string filePath = 3;
  // uint32 fileRgIdx = 4;
  // bool isWriteBuffer = 5;
  void AddRowGroup(int& rgid, std::string& file_path) {
    RowGroup rg;
    rg.set_id(rgid);
    rg.set_iswritebuffer(true);
    rg.set_filepath(file_path);
    AddRowGroupRequest request;
    request.set_allocated_rowgroup(&rg);

    // Container for the data we expect from the server.
    AddRowGroupResponse reply;

    // Context for the client. It could be used to convey extra information to
    // the server and/or tweak certain RPC behaviors.
    ClientContext context;

    // The actual RPC.
    Status status = stub_->AddRowGroup(&context, request, &reply);

    // Act upon its status.
    if (status.ok()) {
      return;
    } else {
      std::cout << status.error_code() << ": " << status.error_message()
                << std::endl;
      return;
    }
  }

  void UpdateRowGroup(int& rgid) {
    RowGroup rg;
    rg.set_id(rgid);
    rg.set_iswritebuffer(false);
    UpdateRowGroupRequest request;
    request.set_allocated_rowgroup(&rg);

    // Container for the data we expect from the server.
    UpdateRowGroupResponse reply;

    // Context for the client. It could be used to convey extra information to
    // the server and/or tweak certain RPC behaviors.
    ClientContext context;

    // The actual RPC.
    Status status = stub_->UpdateRowGroup(&context, request, &reply);

    // Act upon its status.
    if (status.ok()) {
      return;
    } else {
      std::cout << status.error_code() << ": " << status.error_message()
                << std::endl;
      return;
    }
  }

 private:
  std::unique_ptr<MetadataService::Stub> stub_;
};