package com.dev.ministudio.fs;

import com.dev.ministudio.model.FileNode;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FileSystemManager {

    public static List<FileNode> loadRootDirectory(File rootDir) {
        List<FileNode> result = new ArrayList<>();
        if (rootDir != null && rootDir.exists() && rootDir.isDirectory()) {
            File[] listFiles = rootDir.listFiles();
            if (listFiles != null) {
                List<File> folders = new ArrayList<>();
                List<File> files = new ArrayList<>();

                for (File file : listFiles) {
                    if (file.getName().equals(".thumbnails") || file.getName().equals("TemporaryItems")) continue;
                    if (file.isDirectory()) folders.add(file);
                    else if (file.isFile()) files.add(file);
                }

                Collections.sort(folders, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
                Collections.sort(files, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));

                for (File f : folders) result.add(new FileNode(f, 0, true));
                for (File f : files) result.add(new FileNode(f, 0, false));
            }
        }
        return result;
    }

    public static List<FileNode> loadChildren(File parentDir, int currentDepth) {
        List<FileNode> childNodes = new ArrayList<>();
        File[] listFiles = parentDir.listFiles();
        
        if (listFiles != null) {
            List<File> folders = new ArrayList<>();
            List<File> files = new ArrayList<>();

            for (File file : listFiles) {
                if (file.isDirectory()) folders.add(file);
                else if (file.isFile()) files.add(file);
            }

            Collections.sort(folders, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));
            Collections.sort(files, (f1, f2) -> f1.getName().compareToIgnoreCase(f2.getName()));

            int nextDepth = currentDepth + 1;
            for (File f : folders) childNodes.add(new FileNode(f, nextDepth, true));
            for (File f : files) childNodes.add(new FileNode(f, nextDepth, false));
        }
        return childNodes;
    }

    // ✨ เติมเต็มระบบ: สร้างโฟลเดอร์ใหม่
    public static boolean createNewFolder(File parentDir, String folderName) {
        File newFolder = new File(parentDir, folderName);
        if (!newFolder.exists()) {
            return newFolder.mkdirs();
        }
        return false;
    }

    // 🆕 ฟีเจอร์ใหม่: สร้างไฟล์เปล่าใหม่ (เช่น main.py, index.html)
    public static boolean createNewFile(File parentDir, String fileName) {
        File newFile = new File(parentDir, fileName);
        if (!newFile.exists()) {
            try {
                return newFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }

    // 🆕 ฟีเจอร์ใหม่: เปลี่ยนชื่อไฟล์หรือโฟลเดอร์ (Rename)
    public static boolean renameFileOrFolder(File targetFile, String newName) {
        if (targetFile == null || !targetFile.exists()) return false;
        File parent = targetFile.getParentFile();
        File renamedFile = new File(parent, newName);
        if (!renamedFile.exists()) {
            return targetFile.renameTo(renamedFile);
        }
        return false;
    }

    public static boolean deleteFileOrFolder(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            File[] children = fileOrDirectory.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteFileOrFolder(child); 
                }
            }
        }
        return fileOrDirectory.delete(); 
    }

    public static void importFileToFolder(File sourceFile, File destDir) throws IOException {
        File destFile = new File(destDir, sourceFile.getName());
        if (!destFile.exists()) {
            destFile.createNewFile();
        }

        try (FileChannel srcChannel = new FileInputStream(sourceFile).getChannel();
             FileChannel destChannel = new FileOutputStream(destFile).getChannel()) {
            destChannel.transferFrom(srcChannel, 0, srcChannel.size());
        }
    }
    /**
 * ค้นหาไฟล์ตามชื่อ (บางส่วนของชื่อก็ได้) ทั่วทั้งโปรเจกต์
 * ข้ามโฟลเดอร์ที่ไม่จำเป็น เช่น .git, build, .gradle
 */
public static List<File> searchFilesByName(File rootDir, String query) {
    List<File> results = new ArrayList<>();
    if (rootDir == null || !rootDir.exists() || query == null || query.trim().isEmpty()) {
        return results;
    }
    String q = query.trim().toLowerCase();
    searchRecursive(rootDir, q, results, 0);
    return results;
}

private static void searchRecursive(File dir, String queryLower, List<File> out, int depth) {
    if (depth > 20 || out.size() >= 100) return; // กันลึกเกิน / ผลลัพธ์เยอะเกิน
    File[] files = dir.listFiles();
    if (files == null) return;

    for (File f : files) {
        String name = f.getName();
        if (name.startsWith(".") && (name.equals(".git") || name.equals(".gradle") || name.equals(".idea"))) {
            continue;
        }
        if (f.isDirectory()) {
            if (name.equals("build") || name.equals("node_modules") || name.equals(".thumbnails")) {
                continue;
            }
            searchRecursive(f, queryLower, out, depth + 1);
        } else {
            if (name.toLowerCase().contains(queryLower)) {
                out.add(f);
            }
        }
    }
}
}
