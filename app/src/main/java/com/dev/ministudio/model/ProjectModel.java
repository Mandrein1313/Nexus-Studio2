package com.dev.ministudio.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ProjectModel {
    private String projectName;
    private String rootPath;
    private File currentOpenFile;
    
    // รายการไฟล์ที่เปิดค้างไว้ใน Tabs
    private List<File> openedFiles = new ArrayList<>();

    public ProjectModel(String projectName, String rootPath) {
        this.projectName = projectName;
        this.rootPath = rootPath;
    }

    // --- Getters & Setters ---
    public String getProjectName() { 
        return projectName; 
    }
    
    public String getRootPath() { 
        return rootPath; 
    }
    
    public void setCurrentOpenFile(File file) { 
        this.currentOpenFile = file; 
        // เมื่อเซ็ตเป็นไฟล์ปัจจุบัน ให้เพิ่มเข้าไปในรายการ Tabs อัตโนมัติ (ถ้ายังไม่มี)
        addFileToTabs(file);
    }
    
    public File getCurrentOpenFile() { 
        return currentOpenFile; 
    }

    // --- ระบบจัดการ Tabs ---
    public void addFileToTabs(File file) {
        if (file != null && file.isFile() && !openedFiles.contains(file)) {
            openedFiles.add(file);
        }
    }

    public void removeFileFromTabs(File file) {
        openedFiles.remove(file);
    }

    public List<File> getOpenedFiles() {
        return openedFiles;
    }

    // ตรวจสอบตำแหน่งของไฟล์ปัจจุบันในรายการ Tabs (ใช้สำหรับเลื่อน RecyclerView)
    public int getCurrentFileIndex() {
        return openedFiles.indexOf(currentOpenFile);
    }

    /**
     * คืนค่าตำแหน่งโฟลเดอร์รากสูงสุดของโปรเจกต์ (เช่น /sdcard/MiniStudio/morning)
     * เพื่อให้ FileSystemManager นำไปใช้สแกนหาโครงสร้างจริงและตัดคำแสดงผล Relative Path
     */
    public String getSrcPath() {
        return rootPath; 
    }
}
