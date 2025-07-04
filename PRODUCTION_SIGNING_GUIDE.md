# 🔐 QuickPDF Production Signing Guide

This guide sets up production-grade APK signing for consistent, secure updates, similar to the SecureVault-Android project distribution strategy.

## 🎯 **Overview**

QuickPDF uses a two-tier signing strategy:
- **Debug builds**: Use debug signature for development
- **Release builds**: Use production keystore for distribution

## 📋 **Step 1: Create Production Keystore**

### **Generate Production Keystore** (Run locally)

```bash
keytool -genkey -v \
  -keystore quickpdf-production.keystore \
  -alias quickpdf-prod \
  -keyalg RSA \
  -keysize 4096 \
  -validity 25000 \
  -storepass "QuickPDF2024@Secure!" \
  -keypass "QuickPDF2024@Secure!" \
  -dname "CN=QuickPDF Reader, OU=QuickPDF Development, O=QuickPDF App, L=San Francisco, S=California, C=US"
```

**This creates:**
- **File**: `quickpdf-production.keystore`
- **Store Password**: `QuickPDF2024@Secure!`
- **Key Alias**: `quickpdf-prod`
- **Key Password**: `QuickPDF2024@Secure!`
- **Validity**: 68+ years (25,000 days)
- **Key Size**: 4096 bits (high security)

### **Security Features:**
- ✅ **RSA 4096-bit encryption**
- ✅ **Strong password protection**
- ✅ **Long validity period**
- ✅ **Professional certificate details**

## 📋 **Step 2: Encode Keystore for GitHub**

### **Encode to Base64:**

```bash
# macOS/Linux
base64 -i quickpdf-production.keystore > production-keystore-base64.txt

# Windows
certutil -encode quickpdf-production.keystore production-keystore-base64.txt
```

Copy the entire base64 string (remove header/footer lines if using Windows).

## 📋 **Step 3: Configure GitHub Secrets**

### **Add Repository Secrets:**

1. **Go to**: `https://github.com/YOUR_USERNAME/QuickPDF`
2. **Navigate**: Settings → Secrets and Variables → Actions
3. **Add these secrets**:

| Secret Name | Value | Description |
|-------------|-------|-------------|
| `KEYSTORE_FILE` | Base64 encoded keystore | Production keystore file |
| `KEYSTORE_PASSWORD` | `QuickPDF2024@Secure!` | Keystore password |
| `KEY_ALIAS` | `quickpdf-prod` | Key alias name |
| `KEY_PASSWORD` | `QuickPDF2024@Secure!` | Key password |

## 📋 **Step 4: Build Configuration**

### **Gradle Signing Config** (`app/build.gradle.kts`):

```kotlin
signingConfigs {
    create("release") {
        // Production keystore configuration
        storeFile = file(findProperty("QUICKPDF_KEYSTORE_FILE") ?: "quickpdf-production.keystore")
        storePassword = findProperty("QUICKPDF_KEYSTORE_PASSWORD") as String? ?: "QuickPDF2024@Secure!"
        keyAlias = findProperty("QUICKPDF_KEY_ALIAS") as String? ?: "quickpdf-prod"
        keyPassword = findProperty("QUICKPDF_KEY_PASSWORD") as String? ?: "QuickPDF2024@Secure!"
        
        // Enable all signing versions for compatibility
        enableV1Signing = true
        enableV2Signing = true
        enableV3Signing = true
        enableV4Signing = true
    }
}

buildTypes {
    debug {
        applicationIdSuffix = ".debug"
        versionNameSuffix = "-debug"
        signingConfig = signingConfigs.getByName("debug")
    }
    
    release {
        isMinifyEnabled = true
        isShrinkResources = true
        signingConfig = signingConfigs.getByName("release")
    }
}
```

## 📋 **Step 5: GitHub Actions Workflow**

### **Key Features:**
- **Automatic keystore setup** from GitHub secrets
- **Keystore validation** before building
- **Signed APK verification** after building
- **Clear build status** in release notes

### **Workflow Steps:**
1. **Setup keystore** from base64 secret
2. **Validate keystore** access and credentials
3. **Build signed APK** with production keystore
4. **Verify APK signing** status
5. **Create release** with proper naming

## 📋 **Step 6: Release Process**

### **Create Release:**

```bash
# Update version in build.gradle.kts first
git add .
git commit -m "Release v1.0.3"
git tag v1.0.3
git push origin v1.0.3
```

### **Automatic Process:**
1. **GitHub Actions** triggers on tag push
2. **Builds** debug and release APKs
3. **Signs** release APK with production keystore
4. **Creates** GitHub release with APK files
5. **Generates** release notes with signing status

## 📱 **Distribution Strategy**

### **Release Naming:**
- `QuickPDF-v1.0.3-debug.apk` - Debug build for testing
- `QuickPDF-v1.0.3-release.apk` - Signed production build

### **User Installation:**
1. **Download** release APK from GitHub releases
2. **Enable** "Install from Unknown Sources"
3. **Install** APK (seamless updates with same signature)

## 🔒 **Security Best Practices**

### **Keystore Security:**
- ✅ **Strong passwords** (not simple test passwords)
- ✅ **4096-bit keys** for maximum security
- ✅ **Long validity** (25,000 days)
- ✅ **Backup keystore** safely (multiple locations)

### **Secret Management:**
- ✅ **GitHub Secrets** for CI/CD security
- ✅ **Base64 encoding** for safe storage
- ✅ **Environment variables** for local builds
- ✅ **Never commit** keystore to repository

### **Build Security:**
- ✅ **Separate debug/release** signing
- ✅ **Code minification** enabled for release
- ✅ **Resource shrinking** enabled for release
- ✅ **All signing versions** enabled for compatibility

## 🔄 **Update Process**

### **For Users:**
1. **Download** new release APK
2. **Install** over existing version (same signature)
3. **No uninstall** required (seamless updates)

### **For Developers:**
1. **Update** version in `build.gradle.kts`
2. **Commit** changes
3. **Tag** new version: `git tag v1.0.4`
4. **Push** tag: `git push origin v1.0.4`
5. **GitHub Actions** handles the rest automatically

## 🛠️ **Local Testing**

### **Test Signed Build Locally:**

```bash
# Build signed release APK
./gradlew assembleRelease \
  -PQUICKPDF_KEYSTORE_FILE="quickpdf-production.keystore" \
  -PQUICKPDF_KEYSTORE_PASSWORD="QuickPDF2024@Secure!" \
  -PQUICKPDF_KEY_ALIAS="quickpdf-prod" \
  -PQUICKPDF_KEY_PASSWORD="QuickPDF2024@Secure!"

# Verify APK signature
apksigner verify --verbose app/build/outputs/apk/release/app-release.apk
```

## 🚨 **Emergency Procedures**

### **Lost Keystore:**
- **Impact**: Cannot update existing installations
- **Solution**: Users must uninstall and reinstall
- **Prevention**: Keep multiple backups

### **Compromised Keystore:**
- **Impact**: Security vulnerability
- **Solution**: Generate new keystore, notify users
- **Prevention**: Secure storage, access control

### **Build Failures:**
- **Check**: GitHub Actions logs
- **Verify**: Secret values in GitHub
- **Test**: Local build with same parameters

## 📊 **Verification Checklist**

### **Before Release:**
- [ ] Keystore file exists and is valid
- [ ] GitHub secrets configured correctly
- [ ] Build configuration updated
- [ ] Version numbers incremented
- [ ] Local build test successful

### **After Release:**
- [ ] GitHub Actions completed successfully
- [ ] APK files attached to release
- [ ] Release notes show "Signed APK" status
- [ ] APK signature verification passes
- [ ] Installation test successful

## 🔗 **Related Documentation**

- `KEYSTORE_SETUP.md` - Basic keystore setup
- `RELEASE_GUIDE.md` - Release process details
- `INSTALLATION_GUIDE.md` - User installation guide
- `PROJECT_STATUS.md` - Project status and features

---

**⚠️ IMPORTANT: This keystore signs all QuickPDF updates. Keep it secure and backed up!**

*Production Signing Guide - Updated for QuickPDF v1.2+*