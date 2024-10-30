# google/leveldb builds for different OSes and architectures

This repository uses GitHub Actions to build [google/leveldb](https://github.com/google/leveldb) for different OSes and architectures.
Check the releases for the pre-built binaries.

## Supported OSes and architectures

- Windows: x64, arm64
- Linux: x64, arm64, armv7-a
- Android: arm64-v8a, armeabi-v7a, x86, x86_64
- macOS: x64, arm64
- iOS: arm64, arm64-simulator, x64-simulator
- watchOS: arm64, arm64-simulator, x64-simulator
- tvOS: arm64, arm64-simulator, x64-simulator

Libraries are built as dynamic libraries (`.dll`, `.so`, `.dylib`).

NOTHING HAS YET BEEN TESTED!

# How it works

A [trigger.yml](.github/workflows/trigger.yml) checks hourly for new releases of [google/leveldb](https://github.com/google/leveldb)
and triggers the [build.yml](.github/workflows/build.yml) workflow for each new release from Google and creates a
new release with the pre-built binaries.