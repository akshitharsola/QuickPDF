# QuickPDF Release Guide

This guide explains how to create releases for QuickPDF and set up automatic APK distribution.

## 🚀 Quick Release Steps

### 1. Prepare for Release

1. **Update Version Numbers**:
   - Open `app/build.gradle.kts`
   - Update `versionName` (e.g., "1.0.1")
   - Update `versionCode` (increment by 1)

2. **Update Version in Settings**:
   - Open `app/src/main/res/xml/preferences.xml`
   - Update the version summary in the "version" preference

3. **Test the Build**:
   ```bash
   ./gradlew clean assembleDebug
   ```

### 2. Create GitHub Release

#### Option A: Automatic Release (Recommended)

1. **Tag and Push**:
   ```bash
   git tag v1.0.1
   git push origin v1.0.1
   ```

2. **GitHub Actions will automatically**:
   - Build debug and release APKs
   - Create a GitHub release
   - Upload APK files
   - Generate release notes

#### Option B: Manual Release

1. **Build APKs locally**:
   ```bash
   ./gradlew clean assembleDebug assembleRelease
   ```

2. **Create Release on GitHub**:
   - Go to your repository on GitHub
   - Click "Releases" → "Create a new release"
   - Tag version: `v1.0.1`
   - Release title: `QuickPDF v1.0.1`
   - Upload APK files from `app/build/outputs/apk/`

### 3. Update Configuration

#### Set Repository URL in UpdateChecker

Open `app/src/main/java/com/quickpdf/reader/utils/UpdateChecker.kt` and update:

```kotlin
private const val GITHUB_API_URL = "https://api.github.com/repos/YOUR_USERNAME/QuickPDF/releases/latest"
```

Replace `YOUR_USERNAME` with your actual GitHub username.

## 🔧 Advanced Setup

### Setting Up App Signing (Optional but Recommended)

1. **Generate Keystore**:
   ```bash
   keytool -genkey -v -keystore quickpdf-release.keystore -alias quickpdf -keyalg RSA -keysize 2048 -validity 10000
   ```

2. **Add Secrets to GitHub**:
   - Go to Repository Settings → Secrets and Variables → Actions
   - Add these secrets:
     - `KEYSTORE_FILE`: Base64 encoded keystore file
     - `KEYSTORE_PASSWORD`: Your keystore password
     - `KEY_ALIAS`: Your key alias (e.g., "quickpdf")
     - `KEY_PASSWORD`: Your key password

3. **Encode Keystore**:
   ```bash
   base64 -i quickpdf-release.keystore | pbcopy  # macOS
   base64 -i quickpdf-release.keystore            # Linux
   ```

### GitHub Actions Workflow Files

The following workflow files are included:

1. **`.github/workflows/build-and-release.yml`**:
   - Triggers on version tags (v*.*.*)
   - Builds and releases APKs
   - Creates GitHub releases automatically

2. **`.github/workflows/build-debug.yml`**:
   - Triggers on push to main/develop
   - Builds debug APKs for testing
   - Runs tests and uploads artifacts

## 📱 User Installation Guide

### For End Users

1. **Download APK**:
   - Go to the [Releases page](https://github.com/YOUR_USERNAME/QuickPDF/releases)
   - Download the latest APK file

2. **Enable Unknown Sources**:
   - Settings → Security → Unknown Sources
   - Or Settings → Apps → Special Access → Install Unknown Apps

3. **Install APK**:
   - Open the downloaded APK file
   - Follow installation prompts
   - Grant necessary permissions

### For Developers

1. **Clone Repository**:
   ```bash
   git clone https://github.com/YOUR_USERNAME/QuickPDF.git
   cd QuickPDF
   ```

2. **Build and Install**:
   ```bash
   ./gradlew installDebug
   ```

## 🔄 Update System

### How It Works

1. **Automatic Checks**: When enabled in settings, app checks for updates on startup
2. **Manual Checks**: Users can manually check via Settings → Check for Updates
3. **Update Notifications**: Shows dialog when new version is available
4. **Direct Download**: Links directly to GitHub release page

### Configuration

The update system uses GitHub's API to check for releases:
- API Endpoint: `https://api.github.com/repos/YOUR_USERNAME/QuickPDF/releases/latest`
- Checks version tags (must start with 'v')
- Downloads APK files from release assets

## 📋 Release Checklist

### Before Release
- [ ] Update version numbers in `build.gradle.kts`
- [ ] Update version in `preferences.xml`
- [ ] Test app functionality
- [ ] Update repository URL in `UpdateChecker.kt`
- [ ] Commit all changes

### During Release
- [ ] Create and push version tag
- [ ] Verify GitHub Actions completed successfully
- [ ] Check that APK files are attached to release
- [ ] Test download and installation

### After Release
- [ ] Update documentation if needed
- [ ] Announce release (if applicable)
- [ ] Monitor for issues

## 🛠️ Troubleshooting

### Common Issues

1. **Build Fails**:
   - Check Java version (should be 11)
   - Clean and rebuild: `./gradlew clean build`

2. **GitHub Actions Fail**:
   - Check workflow logs in Actions tab
   - Verify secrets are configured correctly

3. **Update Check Fails**:
   - Verify repository URL in `UpdateChecker.kt`
   - Check internet permission in `AndroidManifest.xml`

4. **APK Won't Install**:
   - Enable "Install from Unknown Sources"
   - Check if package name conflicts with existing app

### Logs and Debugging

- **Build logs**: Check GitHub Actions logs
- **App logs**: Use `adb logcat` or Android Studio
- **Update logs**: Look for "UpdateChecker" tag in logs

## 📞 Support

For issues related to:
- **Building**: Check GitHub Actions logs and build output
- **Installation**: Verify Android settings and permissions
- **Updates**: Check network connectivity and repository URL

---

*Last Updated: June 29, 2025*
*QuickPDF v1.0+ Release Process*