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

    /** ผลลัพธ์ค้นหาในเนื้อหาไฟล์ */
    public static class ContentMatch {
        public final File file;
        public final int lineNumber; // 1-based
        public final String lineText;

        public ContentMatch(File file, int lineNumber, String lineText) {
            this.file = file;
            this.lineNumber = lineNumber;
            this.lineText = lineText;
        }
    }

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

    /**
     * ค้นหาข้อความในเนื้อหาไฟล์ทั้งโปรเจกต์
     */
    public static List<ContentMatch> searchContentInProject(File rootDir, String query, int maxResults) {
        List<ContentMatch> results = new ArrayList<>();
        if (rootDir == null || !rootDir.exists() || query == null) return results;
        String q = query.trim();
        if (q.length() < 2) return results;
        if (maxResults <= 0) maxResults = 200;

        searchContentRecursive(rootDir, q.toLowerCase(), results, 0, maxResults);
        return results;
    }

    private static void searchContentRecursive(File dir, String queryLower,
                                              List<ContentMatch> out, int depth, int maxResults) {
        if (depth > 18 || out.size() >= maxResults) return;
        File[] files = dir.listFiles();
        if (files == null) return;

        for (File f : files) {
            if (out.size() >= maxResults) return;
            String name = f.getName();
            if (name.equals(".git") || name.equals(".gradle") || name.equals(".idea")
                    || name.equals("build") || name.equals("node_modules")
                    || name.equals(".thumbnails")) {
                continue;
            }
            if (f.isDirectory()) {
                searchContentRecursive(f, queryLower, out, depth + 1, maxResults);
            } else if (isTextSearchable(name)) {
                scanFileContent(f, queryLower, out, maxResults);
            }
        }
    }

    private static boolean isTextSearchable(String name) {
        String n = name.toLowerCase();
        return n.endsWith(".java") || n.endsWith(".kt") || n.endsWith(".xml")
                || n.endsWith(".gradle") || n.endsWith(".kts") || n.endsWith(".properties")
                || n.endsWith(".json") || n.endsWith(".md") || n.endsWith(".txt")
                || n.endsWith(".yml") || n.endsWith(".yaml") || n.endsWith(".pro")
                || n.endsWith(".css") || n.endsWith(".js") || n.endsWith(".html");
    }

    private static void scanFileContent(File file, String queryLower,
                                       List<ContentMatch> out, int maxResults) {
        // ข้ามไฟล์ใหญ่เกิน ~1.5MB
        if (file.length() > 1_500_000) return;
        try {
            java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(new java.io.FileInputStream(file), "UTF-8"));
            String line;
            int lineNo = 0;
            while ((line = br.readLine()) != null) {
                lineNo++;
                if (out.size() >= maxResults) break;
                if (line.toLowerCase().contains(queryLower)) {
                    String trimmed = line.trim();
                    if (trimmed.length() > 120) {
                        trimmed = trimmed.substring(0, 117) + "...";
                    }
                    out.add(new ContentMatch(file, lineNo, trimmed));
                }
            }
            br.close();
        } catch (Exception ignored) {
        }
    }
}
