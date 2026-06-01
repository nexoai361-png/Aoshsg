package com.example.calculator

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ApkVerificationTest {
    @Test
    fun verifyApkSizes() {
        // Cwd is generally the /app module subdirectory during gradle JVM test execution
        val rootDir = File("..") 
        val apk1 = File(rootDir, ".build-outputs/app-debug.apk")
        val apk2 = File(rootDir, "APK_DOWNLOAD/app-debug.apk")
        
        println("=========================================================================")
        println("VERIFYING APK SIZE & PATHS:")
        println("APK 1: Path: ${apk1.absolutePath}, Exists: ${apk1.exists()}, Size: ${apk1.length()} bytes (${apk1.length().toDouble() / (1024*1024)} MB)")
        println("APK 2: Path: ${apk2.absolutePath}, Exists: ${apk2.exists()}, Size: ${apk2.length()} bytes (${apk2.length().toDouble() / (1024*1024)} MB)")
        println("=========================================================================")
        
        assertTrue("APK '.build-outputs/app-debug.apk' must exist", apk1.exists())
        assertTrue("APK 'APK_DOWNLOAD/app-debug.apk' must exist", apk2.exists())
        assertTrue("APK '.build-outputs/app-debug.apk' size must be > 1 MB", apk1.length() > 1024 * 1024)
        assertTrue("APK 'APK_DOWNLOAD/app-debug.apk' size must be > 1 MB", apk2.length() > 1024 * 1024)
    }
}
