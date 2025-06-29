# 🔐 QuickPDF Keystore Setup Guide

This guide will help you set up APK signing for consistent updates.

## 📋 **Step 1: Generate Keystore (Run this locally)**

Open Terminal/Command Prompt on your computer and run:

```bash
keytool -genkey -v \
  -keystore quickpdf-release.keystore \
  -alias quickpdf \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -storepass quickpdf123 \
  -keypass quickpdf123 \
  -dname "CN=QuickPDF, OU=Development, O=QuickPDF App, L=Your City, S=Your State, C=US"
```

**This creates:**
- **File**: `quickpdf-release.keystore`
- **Store Password**: `quickpdf123`
- **Key Alias**: `quickpdf`
- **Key Password**: `quickpdf123`
- **Validity**: 27+ years

**⚠️ Important**: Keep this keystore file safe! If you lose it, you can't update your app.

## 📋 **Step 2: Encode Keystore for GitHub**

After generating the keystore, encode it to base64:

### **On macOS/Linux:**
```bash
base64 -i quickpdf-release.keystore | pbcopy
```

### **On Windows:**
```bash
certutil -encode quickpdf-release.keystore keystore-base64.txt
# Then copy content from keystore-base64.txt (remove header/footer lines)
```

### **Alternative (any platform):**
```bash
base64 quickpdf-release.keystore > keystore-base64.txt
cat keystore-base64.txt
```

Copy the base64 output (long string of characters).

## 📋 **Step 3: Add Secrets to GitHub**

1. **Go to your repository**: https://github.com/akshitharsola/QuickPDF
2. **Click**: Settings → Secrets and variables → Actions
3. **Click**: "New repository secret"

**Add these 4 secrets:**

| Secret Name | Value |
|-------------|-------|
| `KEYSTORE_FILE` | The base64 string from Step 2 |
| `KEYSTORE_PASSWORD` | `quickpdf123` |
| `KEY_ALIAS` | `quickpdf` |
| `KEY_PASSWORD` | `quickpdf123` |

## 📋 **Step 4: Test Signed Build**

After adding secrets, create a new release:

```bash
git tag v1.0.3
git push origin v1.0.3
```

The workflow will now:
1. ✅ **Decode keystore** from base64
2. ✅ **Sign APK** with your keystore
3. ✅ **Upload signed APK** to release

## 🔧 **Verification**

### **Check Build Logs:**
- Go to Actions tab
- Look for "Sign Release APK" step
- Should show: "Signing release APK..." (instead of "No keystore found")

### **Test Update:**
1. **Uninstall current app** from your device
2. **Install signed APK** from new release
3. **Future updates** will install seamlessly (same signature)

## 📱 **For Production Use**

### **Recommended Changes:**
```bash
# Generate with stronger settings:
keytool -genkey -v \
  -keystore quickpdf-release.keystore \
  -alias quickpdf \
  -keyalg RSA \
  -keysize 4096 \
  -validity 25000 \
  -dname "CN=Your Name, OU=QuickPDF, O=Your Organization, L=Your City, S=Your State, C=Your Country"
```

### **Security Best Practices:**
- ✅ **Use strong passwords** (replace `quickpdf123`)
- ✅ **Backup keystore file** safely
- ✅ **Never commit keystore** to git
- ✅ **Keep passwords secure**

## 🚨 **Important Notes**

### **Keystore Security:**
- **This keystore signs all your app updates**
- **If lost**: You can't update the app (users must uninstall/reinstall)
- **If compromised**: Someone could sign malicious updates
- **Backup multiple places**: Cloud storage, external drive, etc.

### **Password Security:**
- **Current passwords are simple** for demonstration
- **For production**: Use strong, unique passwords
- **Store securely**: Password manager, secure notes

## 🔄 **Update Process After Setup**

1. **Make changes** to your code
2. **Commit and tag** new version: `git tag v1.0.4 && git push origin v1.0.4`
3. **GitHub Actions automatically**:
   - Builds APK
   - Signs with your keystore
   - Creates release
   - Users get seamless updates!

---

**Ready? Run Step 1 on your computer, then let me know when you have the keystore file!**