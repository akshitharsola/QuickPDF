# QuickPDF - Lightweight Android PDF Viewer

A fast, lightweight PDF viewer for Android built with modern Android development practices. QuickPDF provides essential PDF viewing features while maintaining a small app size under 10MB.

## Features

### Core Features
- **Native PDF Rendering**: Uses Android's built-in PdfRenderer API for fast, efficient rendering
- **Smooth Navigation**: Swipe gestures and page controls for easy navigation
- **Zoom & Pan**: Pinch-to-zoom and double-tap zoom with smooth panning
- **File Browser Integration**: Open PDFs directly from file managers or other apps
- **Recent Files**: Quick access to recently opened documents
- **Night Mode**: Dark theme with inverted colors for better reading in low light
- **Multiple View Modes**: Fit-to-width, fit-to-page, and original size viewing options

### Advanced Features
- **Bookmarks**: Save and navigate to important pages
- **Page Thumbnails**: Grid view for quick page navigation (coming soon)
- **Search**: Find text within documents (coming soon)
- **Material Design 3**: Modern, adaptive UI following Google's latest design guidelines
- **Responsive Layout**: Optimized for phones and tablets

## Technical Implementation

### Architecture
- **MVVM Pattern**: Modern Android architecture with ViewModels and LiveData
- **Room Database**: Local storage for recent files and bookmarks
- **Kotlin**: 100% Kotlin codebase for better performance and safety
- **ViewPager2**: Smooth page navigation with efficient memory usage
- **Material Components**: Latest Material Design 3 components

### Performance Optimizations
- **Lazy Loading**: Pages are rendered on-demand to minimize memory usage
- **Memory Management**: Efficient bitmap handling with automatic recycling
- **Background Threading**: File operations and rendering happen off the main thread
- **Page Caching**: Intelligent caching system for smooth scrolling

### Permissions
- **Storage Access**: Read external storage permission for accessing PDF files
- **File System Access**: Uses Android's Storage Access Framework for secure file access

## Development

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 24+ (Android 7.0)
- Kotlin 1.9.24+

### Building the Project
```bash
git clone <repository-url>
cd QuickPDF
./gradlew build
```

### Running Tests
```bash
./gradlew test                     # Unit tests
./gradlew connectedAndroidTest    # Instrumented tests
```

### Project Structure
```
app/src/main/java/com/example/quickpdf/
├── data/                          # Data layer
│   ├── database/                  # Room database entities and DAOs
│   └── repository/                # Repository pattern implementation
├── ui/                           # UI layer
│   ├── adapters/                 # RecyclerView adapters
│   ├── custom/                   # Custom UI components
│   ├── MainActivity.kt           # Main file browser screen
│   ├── PdfViewerActivity.kt      # PDF viewing screen
│   └── SettingsActivity.kt       # Settings screen
├── utils/                        # Utility classes
└── QuickPDFApplication.kt        # Application class
```

## Dependencies

### Core Dependencies
- AndroidX Core KTX
- AndroidX AppCompat
- Material Components
- AndroidX Activity & Fragment KTX
- AndroidX Lifecycle (ViewModel & LiveData)
- AndroidX ViewPager2
- AndroidX RecyclerView
- AndroidX ConstraintLayout
- AndroidX Navigation
- AndroidX DocumentFile
- AndroidX Preference KTX
- Kotlinx Coroutines

### Database
- AndroidX Room (Runtime, KTX, Compiler)

### Testing
- JUnit
- AndroidX Test (JUnit & Espresso)

## Target Requirements
- **App Size**: Under 10MB
- **Minimum SDK**: Android 7.0 (API 24)
- **Target SDK**: Android 15 (API 35)
- **Java Version**: 11

## Compatibility
- **Android Versions**: 7.0 to 15+ (API 24+)
- **Screen Sizes**: Optimized for phones and tablets
- **Orientations**: Portrait and landscape support
- **Themes**: Light and dark theme support

## Performance Benchmarks
- **Cold Start**: < 2 seconds
- **PDF Loading**: < 3 seconds for typical documents
- **Memory Usage**: < 100MB for standard documents
- **Page Rendering**: < 500ms per page

## Privacy & Security
- **No Network Access**: App works completely offline
- **No Data Collection**: No analytics or tracking
- **Local Storage Only**: All data stored locally on device
- **Secure File Access**: Uses Android's Storage Access Framework

## Contributors

This project was developed collaboratively:

- **[Akshit Harsola](https://github.com/akshitharsola)** - Project Lead & Developer
- **[Claude AI](https://claude.ai/code)** - AI Development Assistant & Code Generation

## Contributing
1. Fork the repository
2. Create a feature branch (`git checkout -b feature/new-feature`)
3. Commit your changes (`git commit -am 'Add new feature'`)
4. Push to the branch (`git push origin feature/new-feature`)
5. Create a Pull Request

## License
This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Roadmap

### Version 1.1 (Coming Soon)
- [ ] Text search functionality
- [ ] Page thumbnails grid view
- [ ] Export bookmarks
- [ ] Improved zoom controls

### Version 1.2 (Future)
- [ ] Annotation support
- [ ] Split-screen viewing
- [ ] Cloud storage integration
- [ ] Print functionality

## Support
For bug reports and feature requests, please create an issue on the GitHub repository.

## Acknowledgments
- Android's PdfRenderer API for PDF processing
- Material Design team for design guidelines
- AndroidX team for modern Android components