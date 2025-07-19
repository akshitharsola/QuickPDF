# Password-Protected PDF Testing Guide

## 🔧 **Issue Fixed**
The app was showing "This PDF is password-protected and cannot be opened" instead of the password dialog. This has been fixed by:
- ✅ Checking for password protection BEFORE attempting to open with native PdfRenderer
- ✅ Properly routing password-protected PDFs to the password dialog
- ✅ Improved error handling and logging

## 🧪 **Testing Steps**

### 1. **Install the Fixed App**
```bash
# From project directory
./gradlew installDebug

# Or install APK manually
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### 2. **Create Test PDFs**

#### **Method 1: Using Chrome (Recommended)**
1. Open any webpage in Chrome
2. Press `Ctrl+P` (or `Cmd+P` on Mac)
3. Select "Save as PDF"
4. Save as `test_normal.pdf` (this will be unprotected)

#### **Method 2: Using LibreOffice**
1. Open LibreOffice Writer
2. Type some text: "This is a password-protected PDF test"
3. Go to File → Export as PDF
4. In the PDF export dialog:
   - Click "Security" tab
   - Check "Encrypt this document with a password"
   - Set password: `test123`
   - Click "Export"
5. Save as `test_password.pdf`

#### **Method 3: Using Online Tool**
1. Go to https://smallpdf.com/protect-pdf
2. Upload any PDF file
3. Set password: `test123`
4. Download the protected PDF

### 3. **Test Normal PDF (Should work normally)**
1. Open QuickPDF app
2. Select `test_normal.pdf`
3. **Expected**: PDF opens immediately
4. **If not working**: Check file permissions

### 4. **Test Password-Protected PDF (Main Test)**
1. Open QuickPDF app
2. Select `test_password.pdf`
3. **Expected**: Password dialog appears
4. **If you see error message**: Check logcat for debugging

### 5. **Test Password Scenarios**

#### **Scenario A: Correct Password**
1. Enter password: `test123`
2. Tap "Unlock"
3. **Expected**: PDF unlocks and displays normally

#### **Scenario B: Wrong Password**
1. Enter password: `wrong123`
2. Tap "Unlock"
3. **Expected**: Error message appears, dialog stays open
4. Try again with correct password

#### **Scenario C: Empty Password**
1. Leave password field empty
2. **Expected**: Unlock button should be disabled

#### **Scenario D: Cancel Dialog**
1. Tap "Cancel" button
2. **Expected**: Returns to main screen

## 🔍 **Debugging with Logcat**

### **Monitor Logs**
```bash
# Monitor all app logs
adb logcat | grep -E "(PdfPasswordHandler|PdfViewerActivity|PasswordDialog)"

# OR monitor specific tags
adb logcat -s PdfPasswordHandler,PdfViewerActivity
```

### **Expected Log Messages**

#### **For Password-Protected PDF:**
```
PdfPasswordHandler: Checking if PDF is password-protected for URI: ...
PdfPasswordHandler: PDF open error: 'bad user password', password-related: true
PdfRendererUtil: PDF is password-protected, cannot open without password
PdfViewerActivity: PDF is password-protected, showing password dialog
```

#### **For Correct Password:**
```
PdfPasswordHandler: Successfully unlocked PDF to temporary file: ...
PdfViewerActivity: Password-protected PDF opened successfully with X pages
```

#### **For Wrong Password:**
```
PdfPasswordHandler: Password validation failed
PdfViewerActivity: Invalid password provided
```

## 🚨 **If Password Dialog Still Doesn't Appear**

### **Check 1: Logcat Output**
Look for these specific messages:
- `"Checking if PDF is password-protected"`
- `"PDF open error"`
- `"password-related: true"`

### **Check 2: PDF File Validation**
Test with different password-protected PDFs:
- Try PDFs created with different tools
- Verify the PDF actually requires a password in other viewers

### **Check 3: iText7 Library**
Ensure iText7 is properly included:
```bash
# Check if iText7 is in the APK
unzip -l app/build/outputs/apk/debug/app-debug.apk | grep -i itext
```

### **Check 4: Clean and Rebuild**
```bash
./gradlew clean
./gradlew assembleDebug
./gradlew installDebug
```

## 📱 **Device Testing Checklist**

### **Before Testing:**
- [ ] Android device connected via USB
- [ ] USB debugging enabled
- [ ] File access permissions granted to app
- [ ] Test PDF files copied to device

### **During Testing:**
- [ ] Monitor logcat for error messages
- [ ] Test with different PDF files
- [ ] Test different password scenarios
- [ ] Check memory usage for large PDFs

### **After Testing:**
- [ ] Verify temporary files are cleaned up
- [ ] Check app doesn't crash on orientation change
- [ ] Test with multiple password-protected PDFs

## 🔄 **Common Issues and Solutions**

### **Issue: "File not found" error**
**Solution**: Ensure PDF file is accessible and app has storage permissions

### **Issue: Password dialog appears but password doesn't work**
**Solution**: 
- Check if PDF uses owner password vs user password
- Try different password types (alphanumeric, special chars)
- Verify password is correct in other PDF viewers

### **Issue: App crashes when entering password**
**Solution**: 
- Check available memory
- Test with smaller PDF files
- Monitor logcat for OutOfMemoryError

### **Issue: PDF unlocks but doesn't display**
**Solution**: 
- Check if PDF has content
- Verify PDF isn't corrupted
- Test with different PDF files

## 📊 **Success Criteria**

✅ **Password dialog appears** for password-protected PDFs
✅ **Correct password unlocks** and displays PDF
✅ **Wrong password shows error** and allows retry
✅ **Cancel works** and returns to main screen
✅ **No crashes** during password entry
✅ **Memory usage** is reasonable
✅ **Temporary files** are cleaned up

## 🆘 **If Still Not Working**

1. **Share logcat output** during PDF opening
2. **Test with different PDF files** to isolate the issue
3. **Check app version** and build timestamp
4. **Verify iText7 library** is properly included
5. **Test on different Android versions** if possible

---

**Note**: The app now uses iText7 library for robust password handling. This should resolve the "cannot be opened" error and show the password dialog properly.