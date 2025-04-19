cmake_minimum_required(VERSION 3.4.1)

add_library(
        matrixops
        SHARED
        matrixops.cpp)

include_directories(src/main/cpp/eigen)

find_library(
        log-lib
        log)

target_link_libraries(
        matrixops
        ${log-lib})
