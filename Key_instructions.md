# 🔐 QuickPDF Key Instructions

Quick reference for QuickPDF signing and release process.

## 📋 **GitHub Secrets (Already Configured)**

| Secret Name | Value | Status |
|-------------|-------|---------|
| `KEYSTORE_FILE` | Base64 encoded keystore | ✅ Set |
| `KEYSTORE_PASSWORD` | `QuickPDF2024@Secure!` | ✅ Set |
| `KEY_ALIAS` | `quickpdf-prod` | ✅ Set |
| `KEY_PASSWORD` | `QuickPDF2024@Secure!` | ✅ Set |

## 🚀 **Release Process**

### **1. Update Version**
```bash
# Edit app/build.gradle.kts
versionCode = X
versionName = "1.0.X"
```

### **2. Create Release**
```bash
git add .
git commit -m "Release v1.0.X"
git tag v1.0.X
git push origin v1.0.X
```

### **3. GitHub Actions Automatically:**
- ✅ Builds debug APK
- ✅ Builds signed release APK
- ✅ Creates GitHub release
- ✅ Uploads APK files

## 🔧 **Local Testing (Optional)**
```bash
./gradlew clean
./gradlew assembleRelease \
  -PQUICKPDF_KEYSTORE_FILE="quickpdf-production.keystore" \
  -PQUICKPDF_KEYSTORE_PASSWORD="QuickPDF2024@Secure!" \
  -PQUICKPDF_KEY_ALIAS="quickpdf-prod" \
  -PQUICKPDF_KEY_PASSWORD="QuickPDF2024@Secure!"
```

## 📱 **Distribution**

### **Release Files:**
- `QuickPDF-v1.0.X-debug.apk` - Debug build
- `QuickPDF-v1.0.X-release.apk` - **Signed production build**

### **User Installation:**
1. Download release APK from GitHub
2. Enable "Install from Unknown Sources"
3. Install APK (seamless updates with same signature)

## 🔒 **Security Notes**

### **Keystore Location:**
- **Production**: `quickpdf-production.keystore` (4096-bit, 68+ years validity)
- **Backup**: Keep multiple secure copies
- **Never commit** keystore files to repository

### **Build Configuration:**
- **Debug builds**: Use debug signature + `.debug` suffix
- **Release builds**: Use production signature
- **Signing versions**: v1, v2, v3, v4 enabled

## 🚨 **Emergency Commands**

### **Regenerate Keystore (If Lost):**
```bash
keytool -genkey -v -keystore quickpdf-production.keystore -alias quickpdf-prod -keyalg RSA -keysize 4096 -validity 25000 -storepass QuickPDF2024@Secure! -keypass QuickPDF2024@Secure!
```

### **Re-encode for GitHub:**
```bash
base64 -i quickpdf-production.keystore | pbcopy
```

### **Update GitHub Secret:**
- Go to: Repository Settings → Secrets → Actions
- Update `KEYSTORE_FILE` with new base64 value

## ✅ **Release Checklist**

### **Before Release:**
- [ ] Version updated in `build.gradle.kts`
- [ ] Local build test successful
- [ ] All changes committed

### **After Release:**
- [ ] GitHub Actions completed successfully
- [ ] Release shows "✅ Signed APK" status
- [ ] APK files attached to release
- [ ] Download and installation test

---

**⚠️ CRITICAL: If keystore is lost, all users must uninstall and reinstall the app!**

*Last Updated: July 4, 2025 - QuickPDF v1.2+*