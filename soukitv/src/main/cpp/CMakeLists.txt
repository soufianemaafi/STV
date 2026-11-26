cmake_minimum_required(VERSION 3.22.1)

project("stvsecurity")

add_library(stvsecurity SHARED stv_security.cpp)

target_compile_features(stvsecurity PRIVATE cxx_std_17)
target_link_options(stvsecurity PRIVATE "-Wl,-z,max-page-size=16384")
