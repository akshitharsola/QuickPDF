# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

QuickPDF is an Android application built with Kotlin using the modern Android development stack. The project uses Gradle with Kotlin DSL for build configuration and follows standard Android project structure.

## Development Commands

### Building the Project
```bash
./gradlew build                    # Build the entire project
./gradlew app:assembleDebug       # Build debug APK
./gradlew app:assembleRelease     # Build release APK
```

### Testing
```bash
./gradlew test                     # Run unit tests
./gradlew connectedAndroidTest    # Run instrumented tests on device/emulator
./gradlew app:testDebugUnitTest   # Run unit tests for debug variant only
```

### Cleaning and Setup
```bash
./gradlew clean                   # Clean build artifacts
./gradlew app:installDebug        # Install debug APK on connected device
```

## Architecture and Structure

### Module Structure
- **app/**: Main application module containing all source code
- **gradle/**: Gradle configuration and version catalogs
- **build.gradle.kts**: Root project build configuration
- **settings.gradle.kts**: Project settings and module declarations

### Key Configuration Files
- **gradle/libs.versions.toml**: Version catalog managing all dependency versions
- **app/build.gradle.kts**: Application-specific build configuration
- **app/src/main/AndroidManifest.xml**: Application manifest (currently minimal, no activities defined)

### Source Code Organization
- **Package**: `com.quickpdf.reader`
- **Main Source**: `app/src/main/java/com/example/quickpdf/` (directory structure unchanged)
- **Test Source**: `app/src/test/java/com/example/quickpdf/` (directory structure unchanged)
- **Instrumented Tests**: `app/src/androidTest/java/com/example/quickpdf/` (directory structure unchanged)

### Target Configuration
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 35 (Android 15)
- **Compile SDK**: 35
- **Java Version**: 11
- **Kotlin JVM Target**: 11

### Dependencies
Uses AndroidX libraries with Material Design components:
- androidx.core:core-ktx
- androidx.appcompat:appcompat  
- com.google.android.material:material
- JUnit for unit testing
- Espresso for UI testing

## Development Notes

- **MVVM Architecture**: Uses ViewModels, LiveData, and Repository pattern
- **Database**: Room database for storing recent files and bookmarks
- **PDF Rendering**: Native Android PdfRenderer API for efficient PDF display
- **UI Framework**: Material Design 3 with ViewBinding enabled
- **Navigation**: ViewPager2 for PDF page navigation, standard Activities for app navigation
- **Custom Components**: ZoomableImageView for pinch-to-zoom functionality
- **Performance**: Lazy loading, bitmap caching, and background threading for optimal performance

## Key Classes and Structure

### Data Layer
- `QuickPDFDatabase`: Room database with RecentFile and Bookmark entities
- `PdfRepository`: Repository managing database operations
- `RecentFileDao` & `BookmarkDao`: Database access objects

### UI Layer  
- `MainActivity`: File browser with recent files list
- `PdfViewerActivity`: Main PDF viewing interface with zoom and navigation
- `SettingsActivity`: App preferences and configuration
- `ZoomableImageView`: Custom ImageView with pinch-to-zoom support

### Utilities
- `PdfRendererUtil`: Handles PDF rendering and page management
- `FileUtil`: File operations and URI handling
- `PermissionUtil`: Storage permission management

### Adapters
- `RecentFilesAdapter`: RecyclerView adapter for recent files list
- `PdfPageAdapter`: ViewPager2 adapter for PDF page display

## Implementation Features Complete
- ✅ PDF rendering with native PdfRenderer API
- ✅ Zoom, pan, and navigation controls
- ✅ Recent files storage with Room database
- ✅ Bookmarks functionality
- ✅ Material Design 3 theming
- ✅ Night mode support
- ✅ Multiple view modes (fit-width, fit-page, original)
- ✅ File picker integration
- ✅ Permission handling for storage access
- ✅ Settings with preferences

## Missing Features (marked as "coming soon")
- Search functionality within PDF text
- Page thumbnails grid view
- Advanced annotation support