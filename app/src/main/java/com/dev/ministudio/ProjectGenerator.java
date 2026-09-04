package com.dev.ministudio;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;

public class ProjectGenerator {

    public static boolean create(String projectName, String packageName,
                                 String templateId, String language, int minSdk) {
        try {
            String root = "/sdcard/MiniStudio/" + projectName;
            String pkgPath = packageName.replace('.', '/');
            boolean isKotlin = "Kotlin".equalsIgnoreCase(language);
            String srcFolder = isKotlin ? "kotlin" : "java";
            String ext = isKotlin ? ".kt" : ".java";

            // โฟลเดอร์พื้นฐาน
            mkdirs(root + "/app/src/main/" + srcFolder + "/" + pkgPath);
            mkdirs(root + "/app/src/main/res/layout");
            mkdirs(root + "/app/src/main/res/values");
            mkdirs(root + "/app/src/main/res/drawable");
            mkdirs(root + "/app/src/main/res/menu");
            mkdirs(root + "/gradle/wrapper");

            // ไฟล์ร่วมทุก template
            write(root + "/settings.gradle", settingsGradle(projectName));
            write(root + "/build.gradle", rootBuildGradle());
            write(root + "/gradle.properties", gradleProperties());
            write(root + "/app/build.gradle", appBuildGradle(packageName, templateId, minSdk, isKotlin));
            write(root + "/app/src/main/res/values/strings.xml", stringsXml(projectName));
            write(root + "/app/src/main/res/values/colors.xml", colorsXml());
            write(root + "/app/src/main/res/values/themes.xml", themesXml());

            String mainPath = root + "/app/src/main/" + srcFolder + "/" + pkgPath + "/MainActivity" + ext;

            switch (templateId) {
                case "no_activity":
                    write(root + "/app/src/main/AndroidManifest.xml",
                            manifestNoActivity(packageName));
                    break;

                case "empty":
                    write(root + "/app/src/main/AndroidManifest.xml",
                            manifestWithActivity(packageName));
                    write(mainPath, isKotlin
                            ? emptyMainActivityKt(packageName)
                            : emptyMainActivityJava(packageName));
                    write(root + "/app/src/main/res/layout/activity_main.xml", emptyLayout());
                    break;

                case "basic":
                    write(root + "/app/src/main/AndroidManifest.xml",
                            manifestWithActivity(packageName));
                    write(mainPath, isKotlin
                            ? basicMainActivityKt(packageName)
                            : basicMainActivityJava(packageName));
                    write(root + "/app/src/main/res/layout/activity_main.xml", basicLayout());
                    break;

                case "nav_drawer":
                    write(root + "/app/src/main/AndroidManifest.xml",
                            manifestWithActivity(packageName));
                    write(mainPath, isKotlin
                            ? drawerMainActivityKt(packageName)
                            : drawerMainActivityJava(packageName));
                    write(root + "/app/src/main/res/layout/activity_main.xml", drawerLayout());
                    write(root + "/app/src/main/res/layout/nav_header.xml", navHeader());
                    write(root + "/app/src/main/res/menu/nav_menu.xml", navMenu());
                    break;

                case "bottom_nav":
                    write(root + "/app/src/main/AndroidManifest.xml",
                            manifestWithActivity(packageName));
                    write(mainPath, isKotlin
                            ? bottomNavMainActivityKt(packageName)
                            : bottomNavMainActivityJava(packageName));
                    write(root + "/app/src/main/res/layout/activity_main.xml", bottomNavLayout());
                    write(root + "/app/src/main/res/menu/bottom_nav_menu.xml", bottomNavMenu());

                    // Fragments
                    String fragPath = root + "/app/src/main/" + srcFolder + "/" + pkgPath + "/";
                    if (isKotlin) {
                        write(fragPath + "HomeFragment.kt", simpleFragmentKt(packageName, "HomeFragment", "Home"));
                        write(fragPath + "DashboardFragment.kt", simpleFragmentKt(packageName, "DashboardFragment", "Dashboard"));
                        write(fragPath + "NotificationsFragment.kt", simpleFragmentKt(packageName, "NotificationsFragment", "Notifications"));
                    } else {
                        write(fragPath + "HomeFragment.java", simpleFragmentJava(packageName, "HomeFragment", "Home"));
                        write(fragPath + "DashboardFragment.java", simpleFragmentJava(packageName, "DashboardFragment", "Dashboard"));
                        write(fragPath + "NotificationsFragment.java", simpleFragmentJava(packageName, "NotificationsFragment", "Notifications"));
                    }
                    break;

                case "tabs":
                    write(root + "/app/src/main/AndroidManifest.xml",
                            manifestWithActivity(packageName));
                    write(mainPath, isKotlin
                            ? tabsMainActivityKt(packageName)
                            : tabsMainActivityJava(packageName));
                    write(root + "/app/src/main/res/layout/activity_main.xml", tabsLayout());
                    write(root + "/app/src/main/" + srcFolder + "/" + pkgPath + "/PageFragment" + ext,
                            isKotlin ? pageFragmentKt(packageName) : pageFragmentJava(packageName));
                    break;

                default:
                    return false;
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // ========== Helpers ==========
    private static void mkdirs(String path) {
        new File(path).mkdirs();
    }

    private static void write(String path, String content) throws Exception {
        File f = new File(path);
        File parent = f.getParentFile();
        if (parent != null && !parent.exists()) parent.mkdirs();
        try (OutputStreamWriter w = new OutputStreamWriter(
                new FileOutputStream(f), StandardCharsets.UTF_8)) {
            w.write(content);
        }
    }

    // ========== Gradle & Manifest ==========
    private static String settingsGradle(String name) {
        return "pluginManagement {\n" +
                "    repositories {\n" +
                "        google()\n" +
                "        mavenCentral()\n" +
                "        gradlePluginPortal()\n" +
                "    }\n" +
                "}\n" +
                "dependencyResolutionManagement {\n" +
                "    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)\n" +
                "    repositories {\n" +
                "        google()\n" +
                "        mavenCentral()\n" +
                "    }\n" +
                "}\n" +
                "rootProject.name = \"" + name + "\"\n" +
                "include ':app'\n";
    }

    private static String rootBuildGradle() {
        return "plugins {\n" +
                "    id 'com.android.application' version '8.2.0' apply false\n" +
                "    id 'org.jetbrains.kotlin.android' version '1.9.22' apply false\n" +
                "}\n";
    }

    private static String gradleProperties() {
        return "org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8\n" +
                "android.useAndroidX=true\n" +
                "android.nonTransitiveRClass=true\n";
    }

    private static String appBuildGradle(String pkg, String templateId, int minSdk, boolean isKotlin) {
        StringBuilder sb = new StringBuilder();
        sb.append("plugins {\n");
        sb.append("    id 'com.android.application'\n");
        if (isKotlin) {
            sb.append("    id 'org.jetbrains.kotlin.android'\n");
        }
        sb.append("}\n\n");
        sb.append("android {\n");
        sb.append("    namespace '").append(pkg).append("'\n");
        sb.append("    compileSdk 34\n\n");
        sb.append("    defaultConfig {\n");
        sb.append("        applicationId \"").append(pkg).append("\"\n");
        sb.append("        minSdk ").append(minSdk).append("\n");
        sb.append("        targetSdk 34\n");
        sb.append("        versionCode 1\n");
        sb.append("        versionName \"1.0\"\n");
        sb.append("    }\n\n");
        sb.append("    buildTypes {\n");
        sb.append("        release {\n");
        sb.append("            minifyEnabled false\n");
        sb.append("            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'\n");
        sb.append("        }\n");
        sb.append("    }\n");
        sb.append("    compileOptions {\n");
        sb.append("        sourceCompatibility JavaVersion.VERSION_1_8\n");
        sb.append("        targetCompatibility JavaVersion.VERSION_1_8\n");
        sb.append("    }\n");
        if (isKotlin) {
            sb.append("    kotlinOptions {\n");
            sb.append("        jvmTarget = '1.8'\n");
            sb.append("    }\n");
        }
        sb.append("}\n\n");
        sb.append("dependencies {\n");
        sb.append("    implementation 'androidx.appcompat:appcompat:1.6.1'\n");
        sb.append("    implementation 'com.google.android.material:material:1.11.0'\n");
        sb.append("    implementation 'androidx.constraintlayout:constraintlayout:2.1.4'\n");
        if (isKotlin) {
            sb.append("    implementation 'androidx.core:core-ktx:1.12.0'\n");
        }
        if ("tabs".equals(templateId) || "bottom_nav".equals(templateId)) {
            sb.append("    implementation 'androidx.viewpager2:viewpager2:1.0.0'\n");
        }
        sb.append("    implementation 'androidx.navigation:navigation-fragment:2.7.6'\n");
        sb.append("    implementation 'androidx.navigation:navigation-ui:2.7.6'\n");
        sb.append("}\n");
        return sb.toString();
    }

    private static String manifestNoActivity(String pkg) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" +
                "    <application\n" +
                "        android:allowBackup=\"true\"\n" +
                "        android:label=\"@string/app_name\"\n" +
                "        android:supportsRtl=\"true\"\n" +
                "        android:theme=\"@style/Theme.MyApp\" />\n" +
                "</manifest>\n";
    }

    private static String manifestWithActivity(String pkg) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" +
                "    <application\n" +
                "        android:allowBackup=\"true\"\n" +
                "        android:icon=\"@android:drawable/sym_def_app_icon\"\n" +
                "        android:label=\"@string/app_name\"\n" +
                "        android:supportsRtl=\"true\"\n" +
                "        android:theme=\"@style/Theme.MyApp\">\n" +
                "        <activity\n" +
                "            android:name=\".MainActivity\"\n" +
                "            android:exported=\"true\">\n" +
                "            <intent-filter>\n" +
                "                <action android:name=\"android.intent.action.MAIN\" />\n" +
                "                <category android:name=\"android.intent.category.LAUNCHER\" />\n" +
                "            </intent-filter>\n" +
                "        </activity>\n" +
                "    </application>\n" +
                "</manifest>\n";
    }

    private static String stringsXml(String name) {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<resources>\n" +
                "    <string name=\"app_name\">" + name + "</string>\n" +
                "    <string name=\"navigation_drawer_open\">Open navigation drawer</string>\n" +
                "    <string name=\"navigation_drawer_close\">Close navigation drawer</string>\n" +
                "</resources>\n";
    }

    private static String colorsXml() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<resources>\n" +
                "    <color name=\"purple_200\">#FFBB86FC</color>\n" +
                "    <color name=\"purple_500\">#FF6200EE</color>\n" +
                "    <color name=\"purple_700\">#FF3700B3</color>\n" +
                "    <color name=\"teal_200\">#FF03DAC5</color>\n" +
                "    <color name=\"teal_700\">#FF018786</color>\n" +
                "    <color name=\"black\">#FF000000</color>\n" +
                "    <color name=\"white\">#FFFFFFFF</color>\n" +
                "</resources>\n";
    }

    private static String themesXml() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<resources>\n" +
                "    <style name=\"Theme.MyApp\" parent=\"Theme.MaterialComponents.DayNight.DarkActionBar\">\n" +
                "        <item name=\"colorPrimary\">@color/purple_500</item>\n" +
                "        <item name=\"colorPrimaryVariant\">@color/purple_700</item>\n" +
                "        <item name=\"colorOnPrimary\">@color/white</item>\n" +
                "        <item name=\"colorSecondary\">@color/teal_200</item>\n" +
                "        <item name=\"colorSecondaryVariant\">@color/teal_700</item>\n" +
                "        <item name=\"colorOnSecondary\">@color/black</item>\n" +
                "    </style>\n" +
                "    <style name=\"Theme.MyApp.NoActionBar\">\n" +
                "        <item name=\"windowActionBar\">false</item>\n" +
                "        <item name=\"windowNoTitle\">true</item>\n" +
                "    </style>\n" +
                "</resources>\n";
    }

    // ========== XML Layouts & Menus ==========
    private static String emptyLayout() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<androidx.constraintlayout.widget.ConstraintLayout\n" +
                "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                "    xmlns:app=\"http://schemas.android.com/apk/res-auto\"\n" +
                "    android:layout_width=\"match_parent\"\n" +
                "    android:layout_height=\"match_parent\">\n\n" +
                "    <TextView\n" +
                "        android:layout_width=\"wrap_content\"\n" +
                "        android:layout_height=\"wrap_content\"\n" +
                "        android:text=\"Hello World!\"\n" +
                "        app:layout_constraintBottom_toBottomOf=\"parent\"\n" +
                "        app:layout_constraintEnd_toEndOf=\"parent\"\n" +
                "        app:layout_constraintStart_toStartOf=\"parent\"\n" +
                "        app:layout_constraintTop_toTopOf=\"parent\" />\n\n" +
                "</androidx.constraintlayout.widget.ConstraintLayout>\n";
    }

    private static String basicLayout() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<androidx.coordinatorlayout.widget.CoordinatorLayout\n" +
                "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                "    xmlns:app=\"http://schemas.android.com/apk/res-auto\"\n" +
                "    android:layout_width=\"match_parent\"\n" +
                "    android:layout_height=\"match_parent\">\n\n" +
                "    <com.google.android.material.appbar.AppBarLayout\n" +
                "        android:layout_width=\"match_parent\"\n" +
                "        android:layout_height=\"wrap_content\">\n" +
                "        <androidx.appcompat.widget.Toolbar\n" +
                "            android:id=\"@+id/toolbar\"\n" +
                "            android:layout_width=\"match_parent\"\n" +
                "            android:layout_height=\"?attr/actionBarSize\"\n" +
                "            android:background=\"?attr/colorPrimary\"\n" +
                "            app:title=\"@string/app_name\"\n" +
                "            app:titleTextColor=\"@android:color/white\" />\n" +
                "    </com.google.android.material.appbar.AppBarLayout>\n\n" +
                "    <TextView\n" +
                "        android:layout_width=\"wrap_content\"\n" +
                "        android:layout_height=\"wrap_content\"\n" +
                "        android:text=\"Basic Project\"\n" +
                "        android:layout_gravity=\"center\" />\n\n" +
                "    <com.google.android.material.floatingactionbutton.FloatingActionButton\n" +
                "        android:id=\"@+id/fab\"\n" +
                "        android:layout_width=\"wrap_content\"\n" +
                "        android:layout_height=\"wrap_content\"\n" +
                "        android:layout_gravity=\"bottom|end\"\n" +
                "        android:layout_margin=\"16dp\"\n" +
                "        app:srcCompat=\"@android:drawable/ic_input_add\" />\n\n" +
                "</androidx.coordinatorlayout.widget.CoordinatorLayout>\n";
    }

    private static String drawerLayout() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<androidx.drawerlayout.widget.DrawerLayout\n" +
                "    xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                "    xmlns:app=\"http://schemas.android.com/apk/res-auto\"\n" +
                "    android:id=\"@+id/drawer_layout\"\n" +
                "    android:layout_width=\"match_parent\"\n" +
                "    android:layout_height=\"match_parent\">\n\n" +
                "    <LinearLayout\n" +
                "        android:layout_width=\"match_parent\"\n" +
                "        android:layout_height=\"match_parent\"\n" +
                "        android:orientation=\"vertical\">\n" +
                "        <androidx.appcompat.widget.Toolbar\n" +
                "            android:id=\"@+id/toolbar\"\n" +
                "            android:layout_width=\"match_parent\"\n" +
                "            android:layout_height=\"?attr/actionBarSize\"\n" +
                "            android:background=\"?attr/colorPrimary\"\n" +
                "            app:titleTextColor=\"@android:color/white\" />\n" +
                "        <TextView\n" +
                "            android:layout_width=\"match_parent\"\n" +
                "            android:layout_height=\"match_parent\"\n" +
                "            android:gravity=\"center\"\n" +
                "            android:text=\"Navigation Drawer Project\" />\n" +
                "    </LinearLayout>\n\n" +
                "    <com.google.android.material.navigation.NavigationView\n" +
                "        android:id=\"@+id/nav_view\"\n" +
                "        android:layout_width=\"wrap_content\"\n" +
                "        android:layout_height=\"match_parent\"\n" +
                "        android:layout_gravity=\"start\"\n" +
                "        app:headerLayout=\"@layout/nav_header\"\n" +
                "        app:menu=\"@menu/nav_menu\" />\n\n" +
                "</androidx.drawerlayout.widget.DrawerLayout>\n";
    }

    private static String navHeader() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                "    android:layout_width=\"match_parent\"\n" +
                "    android:layout_height=\"176dp\"\n" +
                "    android:background=\"?attr/colorPrimary\"\n" +
                "    android:gravity=\"bottom\"\n" +
                "    android:orientation=\"vertical\"\n" +
                "    android:padding=\"16dp\">\n" +
                "    <TextView\n" +
                "        android:layout_width=\"match_parent\"\n" +
                "        android:layout_height=\"wrap_content\"\n" +
                "        android:text=\"Nexus Studio\"\n" +
                "        android:textColor=\"@android:color/white\"\n" +
                "        android:textSize=\"20sp\"\n" +
                "        android:textStyle=\"bold\" />\n" +
                "    <TextView\n" +
                "        android:layout_width=\"match_parent\"\n" +
                "        android:layout_height=\"wrap_content\"\n" +
                "        android:text=\"android@nexus.dev\"\n" +
                "        android:textColor=\"@android:color/white\" />\n" +
                "</LinearLayout>\n";
    }

    private static String navMenu() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<menu xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" +
                "    <group android:checkableBehavior=\"single\">\n" +
                "        <item android:id=\"@+id/nav_home\" android:title=\"Home\" />\n" +
                "        <item android:id=\"@+id/nav_gallery\" android:title=\"Gallery\" />\n" +
                "        <item android:id=\"@+id/nav_slideshow\" android:title=\"Slideshow\" />\n" +
                "    </group>\n" +
                "</menu>\n";
    }

    private static String bottomNavLayout() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                "    xmlns:app=\"http://schemas.android.com/apk/res-auto\"\n" +
                "    android:layout_width=\"match_parent\"\n" +
                "    android:layout_height=\"match_parent\"\n" +
                "    android:orientation=\"vertical\">\n\n" +
                "    <FrameLayout\n" +
                "        android:id=\"@+id/fragment_container\"\n" +
                "        android:layout_width=\"match_parent\"\n" +
                "        android:layout_height=\"0dp\"\n" +
                "        android:layout_weight=\"1\" />\n\n" +
                "    <com.google.android.material.bottomnavigation.BottomNavigationView\n" +
                "        android:id=\"@+id/bottom_nav\"\n" +
                "        android:layout_width=\"match_parent\"\n" +
                "        android:layout_height=\"wrap_content\"\n" +
                "        app:menu=\"@menu/bottom_nav_menu\" />\n\n" +
                "</LinearLayout>\n";
    }

    private static String bottomNavMenu() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<menu xmlns:android=\"http://schemas.android.com/apk/res/android\">\n" +
                "    <item android:id=\"@+id/nav_home\" android:title=\"Home\"\n" +
                "        android:icon=\"@android:drawable/ic_menu_compass\" />\n" +
                "    <item android:id=\"@+id/nav_dashboard\" android:title=\"Dashboard\"\n" +
                "        android:icon=\"@android:drawable/ic_menu_manage\" />\n" +
                "    <item android:id=\"@+id/nav_notifications\" android:title=\"Notifications\"\n" +
                "        android:icon=\"@android:drawable/ic_popup_reminder\" />\n" +
                "</menu>\n";
    }

    private static String tabsLayout() {
        return "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "<LinearLayout xmlns:android=\"http://schemas.android.com/apk/res/android\"\n" +
                "    xmlns:app=\"http://schemas.android.com/apk/res-auto\"\n" +
                "    android:layout_width=\"match_parent\"\n" +
                "    android:layout_height=\"match_parent\"\n" +
                "    android:orientation=\"vertical\">\n\n" +
                "    <com.google.android.material.tabs.TabLayout\n" +
                "        android:id=\"@+id/tab_layout\"\n" +
                "        android:layout_width=\"match_parent\"\n" +
                "        android:layout_height=\"wrap_content\"\n" +
                "        app:tabMode=\"fixed\" />\n\n" +
                "    <androidx.viewpager2.widget.ViewPager2\n" +
                "        android:id=\"@+id/view_pager\"\n" +
                "        android:layout_width=\"match_parent\"\n" +
                "        android:layout_height=\"0dp\"\n" +
                "        android:layout_weight=\"1\" />\n\n" +
                "</LinearLayout>\n";
    }

    // ========== Java Templates ==========
    private static String emptyMainActivityJava(String pkg) {
        return "package " + pkg + ";\n\n" +
                "import androidx.appcompat.app.AppCompatActivity;\n" +
                "import android.os.Bundle;\n\n" +
                "public class MainActivity extends AppCompatActivity {\n" +
                "    @Override\n" +
                "    protected void onCreate(Bundle savedInstanceState) {\n" +
                "        super.onCreate(savedInstanceState);\n" +
                "        setContentView(R.layout.activity_main);\n" +
                "    }\n" +
                "}\n";
    }

    private static String basicMainActivityJava(String pkg) {
        return "package " + pkg + ";\n\n" +
                "import android.os.Bundle;\n" +
                "import android.widget.Toast;\n" +
                "import androidx.appcompat.app.AppCompatActivity;\n" +
                "import com.google.android.material.floatingactionbutton.FloatingActionButton;\n\n" +
                "public class MainActivity extends AppCompatActivity {\n" +
                "    @Override\n" +
                "    protected void onCreate(Bundle savedInstanceState) {\n" +
                "        super.onCreate(savedInstanceState);\n" +
                "        setContentView(R.layout.activity_main);\n\n" +
                "        FloatingActionButton fab = findViewById(R.id.fab);\n" +
                "        fab.setOnClickListener(v ->\n" +
                "            Toast.makeText(this, \"FAB clicked\", Toast.LENGTH_SHORT).show());\n" +
                "    }\n" +
                "}\n";
    }

    private static String drawerMainActivityJava(String pkg) {
        return "package " + pkg + ";\n\n" +
                "import android.os.Bundle;\n" +
                "import android.view.MenuItem;\n" +
                "import android.widget.Toast;\n" +
                "import androidx.annotation.NonNull;\n" +
                "import androidx.appcompat.app.ActionBarDrawerToggle;\n" +
                "import androidx.appcompat.app.AppCompatActivity;\n" +
                "import androidx.appcompat.widget.Toolbar;\n" +
                "import androidx.core.view.GravityCompat;\n" +
                "import androidx.drawerlayout.widget.DrawerLayout;\n" +
                "import com.google.android.material.navigation.NavigationView;\n\n" +
                "public class MainActivity extends AppCompatActivity\n" +
                "        implements NavigationView.OnNavigationItemSelectedListener {\n\n" +
                "    private DrawerLayout drawerLayout;\n\n" +
                "    @Override\n" +
                "    protected void onCreate(Bundle savedInstanceState) {\n" +
                "        super.onCreate(savedInstanceState);\n" +
                "        setContentView(R.layout.activity_main);\n\n" +
                "        Toolbar toolbar = findViewById(R.id.toolbar);\n" +
                "        setSupportActionBar(toolbar);\n\n" +
                "        drawerLayout = findViewById(R.id.drawer_layout);\n" +
                "        NavigationView navigationView = findViewById(R.id.nav_view);\n" +
                "        navigationView.setNavigationItemSelectedListener(this);\n\n" +
                "        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(\n" +
                "                this, drawerLayout, toolbar,\n" +
                "                R.string.navigation_drawer_open,\n" +
                "                R.string.navigation_drawer_close);\n" +
                "        drawerLayout.addDrawerListener(toggle);\n" +
                "        toggle.syncState();\n" +
                "    }\n\n" +
                "    @Override\n" +
                "    public boolean onNavigationItemSelected(@NonNull MenuItem item) {\n" +
                "        Toast.makeText(this, item.getTitle(), Toast.LENGTH_SHORT).show();\n" +
                "        drawerLayout.closeDrawer(GravityCompat.START);\n" +
                "        return true;\n" +
                "    }\n\n" +
                "    @Override\n" +
                "    public void onBackPressed() {\n" +
                "        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {\n" +
                "            drawerLayout.closeDrawer(GravityCompat.START);\n" +
                "        } else {\n" +
                "            super.onBackPressed();\n" +
                "        }\n" +
                "    }\n" +
                "}\n";
    }

    private static String bottomNavMainActivityJava(String pkg) {
        return "package " + pkg + ";\n\n" +
                "import android.os.Bundle;\n" +
                "import androidx.appcompat.app.AppCompatActivity;\n" +
                "import androidx.fragment.app.Fragment;\n" +
                "import com.google.android.material.bottomnavigation.BottomNavigationView;\n\n" +
                "public class MainActivity extends AppCompatActivity {\n" +
                "    @Override\n" +
                "    protected void onCreate(Bundle savedInstanceState) {\n" +
                "        super.onCreate(savedInstanceState);\n" +
                "        setContentView(R.layout.activity_main);\n\n" +
                "        BottomNavigationView nav = findViewById(R.id.bottom_nav);\n" +
                "        nav.setOnItemSelectedListener(item -> {\n" +
                "            Fragment f;\n" +
                "            int id = item.getItemId();\n" +
                "            if (id == R.id.nav_dashboard) f = new DashboardFragment();\n" +
                "            else if (id == R.id.nav_notifications) f = new NotificationsFragment();\n" +
                "            else f = new HomeFragment();\n" +
                "            getSupportFragmentManager().beginTransaction()\n" +
                "                    .replace(R.id.fragment_container, f).commit();\n" +
                "            return true;\n" +
                "        });\n" +
                "        nav.setSelectedItemId(R.id.nav_home);\n" +
                "    }\n" +
                "}\n";
    }

    private static String simpleFragmentJava(String pkg, String className, String label) {
        return "package " + pkg + ";\n\n" +
                "import android.os.Bundle;\n" +
                "import android.view.LayoutInflater;\n" +
                "import android.view.View;\n" +
                "import android.view.ViewGroup;\n" +
                "import android.widget.TextView;\n" +
                "import androidx.annotation.NonNull;\n" +
                "import androidx.annotation.Nullable;\n" +
                "import androidx.fragment.app.Fragment;\n\n" +
                "public class " + className + " extends Fragment {\n" +
                "    @Nullable\n" +
                "    @Override\n" +
                "    public View onCreateView(@NonNull LayoutInflater inflater,\n" +
                "                             @Nullable ViewGroup container,\n" +
                "                             @Nullable Bundle savedInstanceState) {\n" +
                "        TextView tv = new TextView(requireContext());\n" +
                "        tv.setText(\"" + label + "\");\n" +
                "        tv.setTextSize(24f);\n" +
                "        tv.setGravity(android.view.Gravity.CENTER);\n" +
                "        return tv;\n" +
                "    }\n" +
                "}\n";
    }

    private static String tabsMainActivityJava(String pkg) {
        return "package " + pkg + ";\n\n" +
                "import android.os.Bundle;\n" +
                "import androidx.annotation.NonNull;\n" +
                "import androidx.appcompat.app.AppCompatActivity;\n" +
                "import androidx.fragment.app.Fragment;\n" +
                "import androidx.fragment.app.FragmentActivity;\n" +
                "import androidx.viewpager2.adapter.FragmentStateAdapter;\n" +
                "import androidx.viewpager2.widget.ViewPager2;\n" +
                "import com.google.android.material.tabs.TabLayout;\n" +
                "import com.google.android.material.tabs.TabLayoutMediator;\n\n" +
                "public class MainActivity extends AppCompatActivity {\n" +
                "    @Override\n" +
                "    protected void onCreate(Bundle savedInstanceState) {\n" +
                "        super.onCreate(savedInstanceState);\n" +
                "        setContentView(R.layout.activity_main);\n\n" +
                "        ViewPager2 pager = findViewById(R.id.view_pager);\n" +
                "        TabLayout tabs = findViewById(R.id.tab_layout);\n" +
                "        pager.setAdapter(new PagerAdapter(this));\n" +
                "        new TabLayoutMediator(tabs, pager, (tab, pos) ->\n" +
                "                tab.setText(\"Tab \" + (pos + 1))).attach();\n" +
                "    }\n\n" +
                "    static class PagerAdapter extends FragmentStateAdapter {\n" +
                "        public PagerAdapter(@NonNull FragmentActivity fa) { super(fa); }\n" +
                "        @NonNull @Override\n" +
                "        public Fragment createFragment(int position) {\n" +
                "            return PageFragment.newInstance(position + 1);\n" +
                "        }\n" +
                "        @Override public int getItemCount() { return 3; }\n" +
                "    }\n" +
                "}\n";
    }

    private static String pageFragmentJava(String pkg) {
        return "package " + pkg + ";\n\n" +
                "import android.os.Bundle;\n" +
                "import android.view.LayoutInflater;\n" +
                "import android.view.View;\n" +
                "import android.view.ViewGroup;\n" +
                "import android.widget.TextView;\n" +
                "import androidx.annotation.NonNull;\n" +
                "import androidx.annotation.Nullable;\n" +
                "import androidx.fragment.app.Fragment;\n\n" +
                "public class PageFragment extends Fragment {\n" +
                "    private static final String ARG_NUM = \"num\";\n\n" +
                "    public static PageFragment newInstance(int num) {\n" +
                "        PageFragment f = new PageFragment();\n" +
                "        Bundle b = new Bundle();\n" +
                "        b.putInt(ARG_NUM, num);\n" +
                "        f.setArguments(b);\n" +
                "        return f;\n" +
                "    }\n\n" +
                "    @Nullable\n" +
                "    @Override\n" +
                "    public View onCreateView(@NonNull LayoutInflater inflater,\n" +
                "                             @Nullable ViewGroup container,\n" +
                "                             @Nullable Bundle savedInstanceState) {\n" +
                "        TextView tv = new TextView(requireContext());\n" +
                "        int num = getArguments() != null ? getArguments().getInt(ARG_NUM) : 0;\n" +
                "        tv.setText(\"Page \" + num);\n" +
                "        tv.setTextSize(28f);\n" +
                "        tv.setGravity(android.view.Gravity.CENTER);\n" +
                "        return tv;\n" +
                "    }\n" +
                "}\n";
    }

    // ========== Kotlin Templates ==========
    private static String emptyMainActivityKt(String pkg) {
        return "package " + pkg + "\n\n" +
                "import androidx.appcompat.app.AppCompatActivity\n" +
                "import android.os.Bundle\n\n" +
                "class MainActivity : AppCompatActivity() {\n" +
                "    override fun onCreate(savedInstanceState: Bundle?) {\n" +
                "        super.onCreate(savedInstanceState)\n" +
                "        setContentView(R.layout.activity_main)\n" +
                "    }\n" +
                "}\n";
    }

    private static String basicMainActivityKt(String pkg) {
        return "package " + pkg + "\n\n" +
                "import android.os.Bundle\n" +
                "import android.widget.Toast\n" +
                "import androidx.appcompat.app.AppCompatActivity\n" +
                "import com.google.android.material.floatingactionbutton.FloatingActionButton\n\n" +
                "class MainActivity : AppCompatActivity() {\n" +
                "    override fun onCreate(savedInstanceState: Bundle?) {\n" +
                "        super.onCreate(savedInstanceState)\n" +
                "        setContentView(R.layout.activity_main)\n\n" +
                "        val fab: FloatingActionButton = findViewById(R.id.fab)\n" +
                "        fab.setOnClickListener {\n" +
                "            Toast.makeText(this, \"FAB clicked\", Toast.LENGTH_SHORT).show()\n" +
                "        }\n" +
                "    }\n" +
                "}\n";
    }

    private static String drawerMainActivityKt(String pkg) {
        return "package " + pkg + "\n\n" +
                "import android.os.Bundle\n" +
                "import android.view.MenuItem\n" +
                "import android.widget.Toast\n" +
                "import androidx.appcompat.app.ActionBarDrawerToggle\n" +
                "import androidx.appcompat.app.AppCompatActivity\n" +
                "import androidx.appcompat.widget.Toolbar\n" +
                "import androidx.core.view.GravityCompat\n" +
                "import androidx.drawerlayout.widget.DrawerLayout\n" +
                "import com.google.android.material.navigation.NavigationView\n\n" +
                "class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {\n\n" +
                "    private lateinit var drawerLayout: DrawerLayout\n\n" +
                "    override fun onCreate(savedInstanceState: Bundle?) {\n" +
                "        super.onCreate(savedInstanceState)\n" +
                "        setContentView(R.layout.activity_main)\n\n" +
                "        val toolbar: Toolbar = findViewById(R.id.toolbar)\n" +
                "        setSupportActionBar(toolbar)\n\n" +
                "        drawerLayout = findViewById(R.id.drawer_layout)\n" +
                "        val navigationView: NavigationView = findViewById(R.id.nav_view)\n" +
                "        navigationView.setNavigationItemSelectedListener(this)\n\n" +
                "        val toggle = ActionBarDrawerToggle(\n" +
                "            this, drawerLayout, toolbar,\n" +
                "            R.string.navigation_drawer_open,\n" +
                "            R.string.navigation_drawer_close\n" +
                "        )\n" +
                "        drawerLayout.addDrawerListener(toggle)\n" +
                "        toggle.syncState()\n" +
                "    }\n\n" +
                "    override fun onNavigationItemSelected(item: MenuItem): Boolean {\n" +
                "        Toast.makeText(this, item.title, Toast.LENGTH_SHORT).show()\n" +
                "        drawerLayout.closeDrawer(GravityCompat.START)\n" +
                "        return true\n" +
                "    }\n\n" +
                "    override fun onBackPressed() {\n" +
                "        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {\n" +
                "            drawerLayout.closeDrawer(GravityCompat.START)\n" +
                "        } else {\n" +
                "            super.onBackPressed()\n" +
                "        }\n" +
                "    }\n" +
                "}\n";
    }

    private static String bottomNavMainActivityKt(String pkg) {
        return "package " + pkg + "\n\n" +
                "import android.os.Bundle\n" +
                "import androidx.appcompat.app.AppCompatActivity\n" +
                "import androidx.fragment.app.Fragment\n" +
                "import com.google.android.material.bottomnavigation.BottomNavigationView\n\n" +
                "class MainActivity : AppCompatActivity() {\n" +
                "    override fun onCreate(savedInstanceState: Bundle?) {\n" +
                "        super.onCreate(savedInstanceState)\n" +
                "        setContentView(R.layout.activity_main)\n\n" +
                "        val nav: BottomNavigationView = findViewById(R.id.bottom_nav)\n" +
                "        nav.setOnItemSelectedListener { item ->\n" +
                "            val f: Fragment = when (item.itemId) {\n" +
                "                R.id.nav_dashboard -> DashboardFragment()\n" +
                "                R.id.nav_notifications -> NotificationsFragment()\n" +
                "                else -> HomeFragment()\n" +
                "            }\n" +
                "            supportFragmentManager.beginTransaction()\n" +
                "                .replace(R.id.fragment_container, f).commit()\n" +
                "            true\n" +
                "        }\n" +
                "        nav.selectedItemId = R.id.nav_home\n" +
                "    }\n" +
                "}\n";
    }

    private static String tabsMainActivityKt(String pkg) {
        return "package " + pkg + "\n\n" +
                "import android.os.Bundle\n" +
                "import androidx.appcompat.app.AppCompatActivity\n" +
                "import androidx.fragment.app.Fragment\n" +
                "import androidx.fragment.app.FragmentActivity\n" +
                "import androidx.viewpager2.adapter.FragmentStateAdapter\n" +
                "import androidx.viewpager2.widget.ViewPager2\n" +
                "import com.google.android.material.tabs.TabLayout\n" +
                "import com.google.android.material.tabs.TabLayoutMediator\n\n" +
                "class MainActivity : AppCompatActivity() {\n" +
                "    override fun onCreate(savedInstanceState: Bundle?) {\n" +
                "        super.onCreate(savedInstanceState)\n" +
                "        setContentView(R.layout.activity_main)\n\n" +
                "        val pager: ViewPager2 = findViewById(R.id.view_pager)\n" +
                "        val tabs: TabLayout = findViewById(R.id.tab_layout)\n" +
                "        pager.adapter = PagerAdapter(this)\n" +
                "        TabLayoutMediator(tabs, pager) { tab, pos ->\n" +
                "            tab.text = \"Tab ${pos + 1}\"\n" +
                "        }.attach()\n" +
                "    }\n\n" +
                "    class PagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {\n" +
                "        override fun createFragment(position: Int): Fragment =\n" +
                "            PageFragment.newInstance(position + 1)\n" +
                "        override fun getItemCount(): Int = 3\n" +
                "    }\n" +
                "}\n";
    }

    private static String simpleFragmentKt(String pkg, String className, String label) {
        return "package " + pkg + "\n\n" +
                "import android.os.Bundle\n" +
                "import android.view.LayoutInflater\n" +
                "import android.view.View\n" +
                "import android.view.ViewGroup\n" +
                "import android.widget.TextView\n" +
                "import androidx.fragment.app.Fragment\n\n" +
                "class " + className + " : Fragment() {\n" +
                "    override fun onCreateView(\n" +
                "        inflater: LayoutInflater,\n" +
                "        container: ViewGroup?,\n" +
                "        savedInstanceState: Bundle?\n" +
                "    ): View {\n" +
                "        val tv = TextView(requireContext())\n" +
                "        tv.text = \"" + label + "\"\n" +
                "        tv.textSize = 24f\n" +
                "        tv.gravity = android.view.Gravity.CENTER\n" +
                "        return tv\n" +
                "    }\n" +
                "}\n";
    }

    private static String pageFragmentKt(String pkg) {
        return "package " + pkg + "\n\n" +
                "import android.os.Bundle\n" +
                "import android.view.LayoutInflater\n" +
                "import android.view.View\n" +
                "import android.view.ViewGroup\n" +
                "import android.widget.TextView\n" +
                "import androidx.fragment.app.Fragment\n\n" +
                "class PageFragment : Fragment() {\n" +
                "    companion object {\n" +
                "        private const val ARG_NUM = \"num\"\n" +
                "        fun newInstance(num: Int): PageFragment {\n" +
                "            val f = PageFragment()\n" +
                "            val b = Bundle()\n" +
                "            b.putInt(ARG_NUM, num)\n" +
                "            f.arguments = b\n" +
                "            return f\n" +
                "        }\n" +
                "    }\n\n" +
                "    override fun onCreateView(\n" +
                "        inflater: LayoutInflater,\n" +
                "        container: ViewGroup?,\n" +
                "        savedInstanceState: Bundle?\n" +
                "    ): View {\n" +
                "        val tv = TextView(requireContext())\n" +
                "        val num = arguments?.getInt(ARG_NUM) ?: 0\n" +
                "        tv.text = \"Page $num\"\n" +
                "        tv.textSize = 28f\n" +
                "        tv.gravity = android.view.Gravity.CENTER\n" +
                "        return tv\n" +
                "    }\n" +
                "}\n";
    }
}
