# Local Testing Guide for QuickPDF

## 🎯 Password-Protected PDF Testing

### Prerequisites
1. Android Studio installed
2. Android device connected via USB with Developer Options enabled
3. Password-protected PDF files for testing

### Setting Up Android Studio

1. **Open Project in Android Studio**
   ```bash
   # Navigate to project directory
   cd /Users/akshitharsola/Documents/Secure_Attend/QuickPDF
   
   # Open with Android Studio
   open -a "Android Studio" .
   ```

2. **Enable Developer Options on Device**
   - Settings → About Phone → Build Number (tap 7 times)
   - Settings → Developer Options → USB Debugging (enable)

3. **Connect Device**
   - Connect via USB cable
   - Select "File Transfer" mode
   - Accept USB debugging prompt

### Testing Steps

#### 1. Install Debug APK
```bash
# From Android Studio terminal or command line
./gradlew installDebug
```

#### 2. Test Regular PDFs
- Open any non-password-protected PDF
- Verify normal viewing functionality works

#### 3. Test Password-Protected PDFs
- Create or download password-protected PDF files
- Test different scenarios:
  - ✅ **Correct Password**: Should unlock and display PDF
  - ❌ **Wrong Password**: Should show error and retry
  - 🚫 **Cancel Dialog**: Should return to main screen

#### 4. Test Edge Cases
- Empty password field
- Very long passwords
- Special characters in password
- Multiple retry attempts
- Memory management with large PDFs

### Creating Test PDFs

#### Method 1: Using LibreOffice
1. Open LibreOffice Writer
2. Create a document with some content
3. File → Export as PDF
4. Check "Encrypt this document with a password"
5. Set password (e.g., "test123")
6. Save as `test_password_protected.pdf`

#### Method 2: Using Chrome
1. Open any webpage
2. Print → Save as PDF
3. Use a PDF tool to add password protection

### Expected Behavior

#### 🔓 **Regular PDF**
1. File opens immediately
2. No password dialog shown
3. Normal viewing experience

#### 🔒 **Password-Protected PDF**
1. App detects password protection
2. Password dialog appears
3. User enters password
4. **If correct**: PDF unlocks and displays normally
5. **If incorrect**: Error message, dialog stays open for retry

### Debug Information

#### Logcat Monitoring
```bash
# Monitor logs during testing
adb logcat | grep -E "(PdfPasswordHandler|PdfViewerActivity|PasswordDialog)"
```

#### Key Log Messages
- `"PDF is password-protected, showing password dialog"`
- `"Password-protected PDF opened successfully"`
- `"Invalid password provided"`
- `"Successfully unlocked PDF to temporary file"`

### Common Issues and Solutions

#### Issue: Password dialog not appearing
- **Check**: Logcat for `"PDF is password-protected"` message
- **Solution**: Ensure PDF is actually password-protected

#### Issue: "Invalid password" with correct password
- **Check**: Password encoding/special characters
- **Solution**: Try simple alphanumeric passwords first

#### Issue: App crashes on password entry
- **Check**: Memory usage with large PDFs
- **Solution**: Test with smaller password-protected PDFs

### Performance Testing

#### Memory Usage
- Monitor memory usage during PDF unlocking
- Test with various PDF sizes
- Check for memory leaks

#### File Cleanup
- Verify temporary files are cleaned up
- Check cache directory: `/data/data/com.quickpdf.reader.debug/cache/`

### Sample Test Cases

#### Test Case 1: Basic Password Protection
```
File: simple_password.pdf
Password: "test123"
Expected: Dialog appears, password accepted, PDF displays
```

#### Test Case 2: Wrong Password
```
File: simple_password.pdf
Password: "wrong123"
Expected: Error message, dialog remains open
```

#### Test Case 3: Empty Password
```
File: simple_password.pdf
Password: ""
Expected: Unlock button disabled
```

#### Test Case 4: Password with Special Characters
```
File: special_chars.pdf
Password: "Test@123!"
Expected: Password accepted, PDF displays
```

### Troubleshooting

#### APK Installation Issues
```bash
# Uninstall existing version
adb uninstall com.quickpdf.reader.debug

# Install fresh APK
./gradlew installDebug
```

#### File Access Issues
- Ensure app has storage permissions
- Test with files in different locations
- Check Android version compatibility

### Success Criteria

✅ **All tests pass when:**
- Password dialog appears for protected PDFs
- Correct passwords unlock PDFs successfully
- Wrong passwords show appropriate error messages
- Unlocked PDFs display normally
- No memory leaks or crashes
- Temporary files are cleaned up properly

### Next Steps

After successful testing:
1. Create signed release APK
2. Test installation over existing version
3. Verify signature consistency
4. Deploy to distribution channel

---

**Note**: This implementation uses iText7 library to handle password-protected PDFs. The app creates temporary unlocked versions of protected PDFs for viewing with Android's native PdfRenderer.