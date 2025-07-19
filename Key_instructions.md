# 🔐 QuickPDF Key Instructions

Quick reference for QuickPDF signing, testing, and production release process.

## 🎯 **Distribution Strategy**

### **Development & Testing**
- **Android Studio**: Debug builds with debug signature
- **Local Testing**: Use `./gradlew installDebug` or IDE run
- **APK Sharing**: Only debug APKs for internal testing
- **Package ID**: `com.quickpdf.reader.debug` (separate from production)

### **Production Distribution**
- **GitHub Releases**: Signed production APKs only
- **User Downloads**: From GitHub releases page
- **Updates**: Seamless with same production signature
- **Package ID**: `com.quickpdf.reader` (production)

## 📋 **GitHub Secrets (Production Signing)**

| Secret Name | Value | Status |
|-------------|-------|---------|
| `KEYSTORE_FILE` | Base64 encoded production keystore | ✅ Set |
| `KEYSTORE_PASSWORD` | `QuickPDF2024@Secure!` | ✅ Set |
| `KEY_ALIAS` | `quickpdf-prod` | ✅ Set |
| `KEY_PASSWORD` | `QuickPDF2024@Secure!` | ✅ Set |

## 🚀 **Production Release Process**

### **1. Update Version Numbers**
```bash
# Edit app/build.gradle.kts
versionCode = X          # Increment by 1
versionName = "1.X.X"    # Semantic versioning
```

### **2. Test Locally (Optional)**
```bash
# Debug build for testing
./gradlew clean
./gradlew installDebug

# Or use Android Studio Run button
```

### **3. Create Production Release**
```bash
git add .
git commit -m "Release v1.X.X - [brief description]"
git tag v1.X.X
git push origin v1.X.X
```

### **4. GitHub Actions Automatically:**
- ✅ Validates keystore access
- ✅ Builds debug APK (for reference)
- ✅ Builds **signed** production APK
- ✅ **Fails if signing fails** (no unsigned releases)
- ✅ Creates GitHub release with APK files
- ✅ Generates release notes

## 📱 **User Distribution Files**

### **Release Contains:**
- `QuickPDF-v1.X.X-debug.apk` - **For developers only**
- `QuickPDF-v1.X.X-release.apk` - **For end users** (signed)

### **User Installation Instructions:**
1. Download `QuickPDF-v1.X.X-release.apk` from GitHub
2. Enable "Install from Unknown Sources" if needed
3. Install APK (updates seamlessly over previous versions)
4. Grant necessary permissions

## 🔧 **Testing vs Production**

### **For Development/Testing:**
```bash
# Local development
./gradlew installDebug

# Test APK creation
./gradlew assembleDebug

# Local release build test (optional)
./gradlew assembleRelease \
  -PQUICKPDF_KEYSTORE_FILE="quickpdf-production.keystore" \
  -PQUICKPDF_KEYSTORE_PASSWORD="QuickPDF2024@Secure!" \
  -PQUICKPDF_KEY_ALIAS="quickpdf-prod" \
  -PQUICKPDF_KEY_PASSWORD="QuickPDF2024@Secure!"
```

### **For Production:**
- **Never** distribute debug APKs to users
- **Always** use GitHub releases for distribution
- **Only** download release APKs for installation
- **Verify** APK shows `com.quickpdf.reader` (not `.debug`)

## 🔒 **Security & Keystore Management**

### **Keystore Details:**
- **File**: `quickpdf-production.keystore`
- **Algorithm**: RSA 4096-bit
- **Validity**: 68+ years (25,000 days)
- **Alias**: `quickpdf-prod`
- **Password**: `QuickPDF2024@Secure!`

### **Critical Security Rules:**
- ✅ **Never commit** keystore files to repository
- ✅ **Keep multiple backups** of production keystore
- ✅ **GitHub secrets only** for CI/CD signing
- ✅ **Production keystore required** for all releases

### **Emergency Keystore Recovery:**
```bash
# If keystore is lost - LAST RESORT
keytool -genkey -v \
  -keystore quickpdf-production.keystore \
  -alias quickpdf-prod \
  -keyalg RSA \
  -keysize 4096 \
  -validity 25000 \
  -storepass QuickPDF2024@Secure! \
  -keypass QuickPDF2024@Secure!

# Re-encode for GitHub
base64 -i quickpdf-production.keystore | pbcopy

# Update GitHub secret KEYSTORE_FILE with new base64 value
```

**⚠️ WARNING**: New keystore = all users must uninstall and reinstall!

## ✅ **Release Checklist**

### **Before Release:**
- [ ] Version numbers updated in `app/build.gradle.kts`
- [ ] Local debug build tested successfully
- [ ] All code changes committed and pushed
- [ ] Release notes prepared (if needed)

### **During Release:**
- [ ] Tag created and pushed: `git tag v1.X.X && git push origin v1.X.X`
- [ ] GitHub Actions workflow starts automatically
- [ ] Workflow completes successfully (no failures)

### **After Release:**
- [ ] Check release page has both debug and release APKs
- [ ] Release notes show "✅ Signed APK" status
- [ ] Download and test release APK installation
- [ ] Verify app shows correct version (no "-debug")
- [ ] Verify package ID is `com.quickpdf.reader` (not `.debug`)

### **Release Verification:**
```bash
# Check installed app details
adb shell dumpsys package com.quickpdf.reader | grep version
```

## 🚨 **Troubleshooting**

### **Workflow Fails with "Keystore setup failed":**
- Check all 4 GitHub secrets are set correctly
- Verify base64 encoding of keystore file
- Ensure keystore passwords match secrets

### **APK Won't Install Over Previous Version:**
- Different signatures detected
- User must uninstall old version first
- Check if debug vs release APK mixing

### **No Release APK in GitHub Release:**
- Workflow failed during signing step
- Check GitHub Actions logs for errors
- Verify keystore accessibility

## 📞 **Quick Commands Reference**

```bash
# Development
./gradlew installDebug                    # Install debug version
./gradlew clean                          # Clean build

# Release
git tag v1.X.X && git push origin v1.X.X  # Create release
git log --oneline -10                     # Check recent commits

# Emergency
base64 -i quickpdf-production.keystore | pbcopy  # Re-encode keystore
```

---

**🎯 Key Principle**: 
- **Debug builds** = Development & testing only
- **Release builds** = Production distribution only
- **Never mix** debug and release APKs for users

**📱 User Downloads**: Always from GitHub releases, always the `release.apk` file

*Last Updated: July 4, 2025 - QuickPDF v1.2.2+*