#pragma once

void SetInvisible(uint64_t visibility[], int rid) {
  visibility[rid >> 6] = ~(~visibility[rid >> 6] | (1ull << (rid & 63)));
}