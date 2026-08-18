package com.dev.ministudio;

import android.content.Context;
import java.io.File;
import java.io.FileOutputStream;

public class BuildEnvironmentManager {

    private final Context context;

    public BuildEnvironmentManager(Context context) {
        this.context = context;
    }

    // 🌟 ระบบตรวจสอบ: ถ้าโปรเจกต์ใช้ .kts อยู่แล้ว ให้ข้ามเฉพาะการสร้างไฟล์ Gradle ทับ
    private boolean isModernGradleProject(String rootPath) {
        File rootDir = new File(rootPath);
        if (!rootDir.exists()) return false;
        
        // ถ้าเจอไฟล์ .kts ให้ถือว่าเป็นโปรเจกต์สมัยใหม่ ไม่ต้องยุ่งกับโครงสร้าง Gradle เดิม
        return new File(rootPath, "build.gradle.kts").exists() || 
               new File(rootPath, "settings.gradle.kts").exists();
    }

    // 🌟 เตรียม Workflow สำหรับ GitHub Actions (เปิดสิทธิ์ให้สร้างไฟล์บิวด์ได้กับทุกโปรเจกต์)
    public void prepareGitHubWorkflow(String localProjectPath, String projectName, String packageName, String language, int minSdk) {
        try {
            File workflowDir = new File(localProjectPath, ".github/workflows");
            if (!workflowDir.exists()) {
                workflowDir.mkdirs();
            }

            File buildYamlFile = new File(workflowDir, "build.yml");
            if (!buildYamlFile.exists()) {
                // ปรับสคริปต์ค้นหาไฟล์ APK ให้เป็นแบบยืดหยุ่น (**/build/outputs/apk/**/*.apk)
                // เพื่อรองรับทั้งโมดูลชื่อ 'app', 'main' หรือชื่ออื่นๆ บน GitHub Actions
                String workflowContent = "name: Android Cloud Build Pipeline\n\n" +
                        "on:\n" +
                        "  push:\n" +
                        "    branches: [ \"main\", \"master\" ]\n" +
                        "  workflow_dispatch:\n\n" +
                        "permissions:\n" +
                        "  contents: write\n\n" +
                        "jobs:\n" +
                        "  build:\n" +
                        "    runs-on: ubuntu-latest\n\n" +
                        "    steps:\n" +
                        "    - name: Checkout repository\n" +
                        "      uses: actions/checkout@v4\n\n" +
                        "    - name: Set up JDK 17\n" +
                        "      uses: actions/setup-java@v4\n" +
                        "      with:\n" +
                        "        distribution: 'temurin'\n" +
                        "        java-version: '17'\n\n" +
                        "    - name: Setup Gradle 8.13\n" +
                        "      uses: gradle/actions/setup-gradle@v4\n" +
                        "      with:\n" +
                        "        gradle-version: '8.13'\n\n" +
                        "    - name: Clean & Build Debug APK\n" +
                        "      run: gradle clean assembleDebug --no-daemon --build-cache\n\n" +
                        "    - name: Generate Timestamp\n" +
                        "      id: timestamp\n" +
                        "      run: echo \"timestamp=$(date +'%Y%m%d-%H%M%S')\" >> $GITHUB_OUTPUT\n\n" +
                        "    - name: Upload APK to Release\n" +
                        "      uses: softprops/action-gh-release@v2\n" +
                        "      with:\n" +
                        "        tag_name: \"build-${{ steps.timestamp.outputs.timestamp }}\"\n" +
                        "        name: \"Build ${{ steps.timestamp.outputs.timestamp }}\"\n" +
                        "        draft: false\n" +
                        "        prerelease: false\n" +
                        "        make_latest: true\n" +
                        "        overwrite_files: true\n" +
                        "        files: \"**/build/outputs/apk/**/*.apk\"\n" +
                        "      env:\n" +
                        "        GITHUB_TOKEN: ${{ secrets.GITHUB_TOKEN }}\n";

                writeFile(buildYamlFile, workflowContent);
            }
          
            // ตรวจสอบก่อนสร้างโครงสร้างไฟล์ Gradle เพื่อไม่ให้ไปเขียนทับโปรเจกต์ .kts เดิมที่มีอยู่แล้ว
            if (!isModernGradleProject(localProjectPath)) {
                setupGradleProjectFiles(localProjectPath, projectName, packageName, language, minSdk);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupGradleProjectFiles(String rootPath, String projectName, String packageName, String language, int minSdk) {
        // 1. สร้าง settings.gradle
        File settingsFile = new File(rootPath, "settings.gradle");
        if (!settingsFile.exists()) {
            String settingsContent = "pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }\n" +
                    "dependencyResolutionManagement { repositoriesMode.set(RepositoriesMode.PREFER_PROJECT); repositories { google(); mavenCentral() } }\n" +
                    "rootProject.name = '" + projectName + "'\ninclude ':app'\n";
            writeFile(settingsFile, settingsContent);
        }

        // 2. สร้าง root build.gradle
        File rootBuildGradleFile = new File(rootPath, "build.gradle");
        if (!rootBuildGradleFile.exists()) {
            String rootBuildGradle = "Kotlin".equals(language) ? 
                    "plugins { id 'com.android.application' version '8.4.0' apply false; id 'com.android.library' version '8.4.0' apply false; id 'org.jetbrains.kotlin.android' version '1.9.0' apply false }" :
                    "plugins { id 'com.android.application' version '8.4.0' apply false; id 'com.android.library' version '8.4.0' apply false }";
            writeFile(rootBuildGradleFile, rootBuildGradle);
        }

        // 3. สร้าง app/build.gradle
        File appDir = new File(rootPath, "app");
        if (!appDir.exists()) appDir.mkdirs();
        File appBuildGradleFile = new File(appDir, "build.gradle");
        
        if (!appBuildGradleFile.exists()) {
            String appBuildGradle = "plugins {\n    id 'com.android.application'\n" + 
                    ("Kotlin".equals(language) ? "    id 'org.jetbrains.kotlin.android'\n" : "") + 
                    "}\n\nandroid {\n    namespace '" + packageName + "'\n    compileSdk 34\n" +
                    "    defaultConfig {\n        applicationId '" + packageName + "'\n        minSdk " + minSdk + "\n        targetSdk 34\n    }\n" +
                    "    compileOptions {\n        sourceCompatibility JavaVersion.VERSION_17\n        targetCompatibility JavaVersion.VERSION_17\n    }\n}\n" +
                    "dependencies {\n    implementation 'androidx.appcompat:appcompat:1.6.1'\n    implementation 'com.google.android.material:material:1.9.0'\n}";
            writeFile(appBuildGradleFile, appBuildGradle);
        }

        // 4. สร้าง gradle.properties
        File gradleProps = new File(rootPath, "gradle.properties");
        if (!gradleProps.exists()) {
            String content = "android.useAndroidX=true\nandroid.enableJetifier=true\norg.gradle.caching=true\norg.gradle.parallel=true";
            writeFile(gradleProps, content);
        }

        // 5. สร้าง .gitignore
        File gitIgnore = new File(rootPath, ".gitignore");
        if (!gitIgnore.exists()) {
            writeFile(gitIgnore, ".gradle/\nbuild/\napp/build/\nlocal.properties\n.idea/");
        }
    }

    private void writeFile(File targetFile, String content) {
        try (FileOutputStream fos = new FileOutputStream(targetFile)) {
            fos.write(content.getBytes("UTF-8"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
