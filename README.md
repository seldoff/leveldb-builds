# google/leveldb builds for different OSes and architectures

This repository uses GitHub Actions to build [google/leveldb](https://github.com/google/leveldb) for different OSes and architectures.
The main goal is to provide pre-built binaries to be used from Kotlin/Multiplatform in project [lamba92/kotlin-leveldb](https://github.com/lamba92/kotlin-leveldb).
Check the releases for the pre-built binaries.

## Supported OSes and architectures

- Windows: x64, arm64
- Linux: x64, arm64, arm7-a
- Android: arm64-v8a, arm7-a, x86, x64
- macOS: x64, arm64
- iOS: arm64, arm64-simulator, x64-simulator
- watchOS: arm64, arm64-simulator, x64-simulator
- tvOS: arm64, arm64-simulator, x64-simulator

Libraries are built as dynamic libraries (`.dll`/`.lib`, `.so`, `.dylib`) and static libraries (`.lib`, `.a`) in both Debug and Release configurations.

Binaries have been tested on some platforms, check the test results from [lamba92/kotlin-leveldb's CI](https://github.com/lamba92/kotlin-leveldb/actions/workflows/test.yml) for more information.

Edit for CI
