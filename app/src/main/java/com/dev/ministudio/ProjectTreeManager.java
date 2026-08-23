package com.dev.ministudio;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.dev.ministudio.fs.FileSystemManager;
import com.dev.ministudio.model.FileNode;
import com.dev.ministudio.model.ProjectModel;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import io.github.rosemoe.sora.langs.java.JavaLanguage;

public class ProjectTreeManager {

    private final MainActivity activity;
    private final ListView treeView;
    private List<FileNode> masterFileList = new ArrayList<>();
    private FileTreeAdapter fileTreeAdapter;
    private int lastClickedPosition = -1;
    private File folderForImport = null;

    public ProjectTreeManager(MainActivity activity, ListView treeView) {
        this.activity = activity;
        this.treeView = treeView;
    }

    public void initializeFileTree() {
        ProjectModel currentProject = activity.getCurrentProject();
        if (currentProject == null) return;

        File projectRoot = new File(currentProject.getRootPath());
        
        // 🌟 ปรับปรุง: กรองไฟล์/โฟลเดอร์ที่ขึ้นต้นด้วยจุด (.) ออกตั้งแต่หน้าแรกสุด (ซ่อน .git)
        List<FileNode> rawRootList = FileSystemManager.loadRootDirectory(projectRoot);
        masterFileList = new ArrayList<>();
        if (rawRootList != null) {
            for (FileNode node : rawRootList) {
                if (node.file != null && !node.file.getName().startsWith(".")) {
                    masterFileList.add(node);
                }
            }
        }

        fileTreeAdapter = new FileTreeAdapter(activity, masterFileList);
        treeView.setAdapter(fileTreeAdapter);

        treeView.setOnItemClickListener((parent, view, position, id) -> {
            FileNode selectedNode = masterFileList.get(position);

            if (selectedNode.isDirectory) {
                if (!selectedNode.isExpanded) {
                    selectedNode.isExpanded = true;
                    
                    // 🌟 ปรับปรุง: กรองไฟล์ระบบซ่อนออกจากโฟลเดอร์ลูกหลานตอนที่น้ากดกางใช้งาน
                    List<FileNode> rawChildren = FileSystemManager.loadChildren(selectedNode.file, selectedNode.depth);
                    List<FileNode> children = new ArrayList<>();
                    if (rawChildren != null) {
                        for (FileNode child : rawChildren) {
                            if (child.file != null && !child.file.getName().startsWith(".")) {
                                children.add(child);
                            }
                        }
                    }
                    masterFileList.addAll(position + 1, children);
                } else {
                    selectedNode.isExpanded = false;
                    int nextPosition = position + 1;
                    while (nextPosition < masterFileList.size() && masterFileList.get(nextPosition).depth > selectedNode.depth) {
                        masterFileList.remove(nextPosition);
                    }
                }
                fileTreeAdapter.notifyDataSetChanged();
                
            } else {
                String fileName = selectedNode.file.getName().toLowerCase();
                if (fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".jpeg") || fileName.endsWith(".webp")) {
                    activity.getDialogManager().showImageViewerDialog(selectedNode.file);
                } else {
                    fileTreeAdapter.setSelectedPosition(position);
                    currentProject.setCurrentOpenFile(selectedNode.file);
                    openFile(selectedNode.file);
                    activity.getDrawerLayout().closeDrawers();
                }
            }
        });

        treeView.setOnItemLongClickListener((parent, view, position, id) -> {
            FileNode selectedNode = masterFileList.get(position);
            File currentFile = selectedNode.file;
            lastClickedPosition = position;

            BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(activity);
            View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_bottom_file_menu, null);
            bottomSheetDialog.setContentView(dialogView);

            TextView tvHeader = dialogView.findViewById(R.id.tvDialogHeader);
            LinearLayout menuContainer = dialogView.findViewById(R.id.menuContainer);

            tvHeader.setText(selectedNode.isDirectory ? "จัดการโฟลเดอร์: " + currentFile.getName() : "จัดการไฟล์: " + currentFile.getName());

            List<MainActivity.MenuOption> options = new ArrayList<>();
            options.add(new MainActivity.MenuOption("สร้างไฟล์ใหม่", android.R.drawable.ic_menu_add));
            options.add(new MainActivity.MenuOption("สร้างโฟลเดอร์ใหม่", android.R.drawable.ic_menu_preferences)); 
            options.add(new MainActivity.MenuOption("เปลี่ยนชื่อ", android.R.drawable.ic_menu_edit));
            options.add(new MainActivity.MenuOption("ลบ", android.R.drawable.ic_menu_delete));
            
            if (selectedNode.isDirectory) {
                options.add(new MainActivity.MenuOption("นำเข้าไฟล์ (Import)", android.R.drawable.ic_menu_share));
            }

            for (MainActivity.MenuOption option : options) {
                View itemView = activity.getLayoutInflater().inflate(R.layout.dialog_menu_item, null);
                ImageView imgIcon = itemView.findViewById(R.id.menuIcon);
                TextView tvTitle = itemView.findViewById(R.id.menuTitle);

                tvTitle.setText(option.title);
                imgIcon.setImageResource(option.iconRes);

                itemView.setOnClickListener(v -> {
                    bottomSheetDialog.dismiss(); 
                    if (option.title.equals("สร้างไฟล์ใหม่")) {
                        activity.getDialogManager().showCreateFileDialog(selectedNode.isDirectory ? currentFile : currentFile.getParentFile(), selectedNode.isDirectory ? selectedNode : findParentNode(selectedNode));
                    } else if (option.title.equals("สร้างโฟลเดอร์ใหม่")) {
                        activity.getDialogManager().showCreateFolderDialog(selectedNode.isDirectory ? currentFile : currentFile.getParentFile(), selectedNode.isDirectory ? selectedNode : findParentNode(selectedNode));
                    } else if (option.title.equals("เปลี่ยนชื่อ")) {
                        activity.getDialogManager().showRenameDialog(currentFile, selectedNode);
                    } else if (option.title.equals("ลบ")) {
                        activity.getDialogManager().showDeleteConfirmationDialog(currentFile.getName(), () -> {
                            boolean success = FileSystemManager.deleteFileOrFolder(currentFile);
                            if (success) {
                                activity.showToast("ลบสำเร็จแล้ว");
                                masterFileList.remove(position);
                                if (fileTreeAdapter != null) {
                                    fileTreeAdapter.setSelectedPosition(-1);
                                    fileTreeAdapter.notifyDataSetChanged();
                                }
                            } else {
                                activity.showToast("ลบไม่สำเร็จ");
                            }
                        });
                    } else if (option.title.equals("นำเข้าไฟล์ (Import)")) {
                        folderForImport = currentFile; 
                        activity.openFilePicker(); 
                    }
                });
                menuContainer.addView(itemView);
            }
            bottomSheetDialog.show();
            return true;
        });
    }

    private FileNode findParentNode(FileNode childNode) {
        if (childNode == null || lastClickedPosition == -1) return null;
        for (int i = lastClickedPosition; i >= 0; i--) {
            FileNode potentialParent = masterFileList.get(i);
            if (potentialParent.isDirectory && potentialParent.depth < childNode.depth) {
                return potentialParent; 
            }
        }
        return null;
    }

    // 🌟 เมทอดปรับปรุงใหม่: เพิ่มระเบียบการกรองไฟล์ซ่อนตอนกดเคลียร์รีเฟรชข้อมูล
    public void refreshFileTree() {
        ProjectModel currentProject = activity.getCurrentProject();
        if (currentProject == null) return;

        File projectRoot = new File(currentProject.getRootPath());

        // 1. เก็บรักษาที่อยู่โฟลเดอร์ทั่วไปที่เคยถูกกดเปิดค้างไว้
        List<String> expandedPaths = new ArrayList<>();
        if (masterFileList != null) {
            for (FileNode node : masterFileList) {
                if (node.isDirectory && node.isExpanded && node.file != null) {
                    expandedPaths.add(node.file.getAbsolutePath());
                }
            }
        }

        // 2. ดึงเฉพาะโครงสร้าง Root โฟลเดอร์หลักขึ้นมาใหม่ และคัดกรองโฟลเดอร์ซ่อนออกทันที
        List<FileNode> rawRootList = FileSystemManager.loadRootDirectory(projectRoot);
        List<FileNode> newRootList = new ArrayList<>();
        if (rawRootList != null) {
            for (FileNode node : rawRootList) {
                if (node.file != null && !node.file.getName().startsWith(".")) {
                    newRootList.add(node);
                }
            }
        }
        
        List<FileNode> rebuiltList = new ArrayList<>();

        // 3. ทยอยเอาโครงสร้างย่อยเสียบประกอบคืนตำแหน่งความลึกเดิมอัติโนมัติ
        rebuildTreeRecursive(newRootList, expandedPaths, rebuiltList);

        // 4. อัปเดตผลิใบข้อมูลบนหน้าจอ UI
        masterFileList.clear();
        masterFileList.addAll(rebuiltList);

        if (fileTreeAdapter != null) {
            fileTreeAdapter.notifyDataSetChanged();
        }
    }

    // ฟังก์ชันช่วยจัดแจงและแตกหน่อโครงสร้างย่อยวนซ้ำ พร้อมระบบกรองไฟล์ระบบออก
    private void rebuildTreeRecursive(List<FileNode> currentNodes, List<String> expandedPaths, List<FileNode> outputList) {
        if (currentNodes == null) return;

        for (FileNode node : currentNodes) {
            outputList.add(node);
            
            if (node.isDirectory && node.file != null && expandedPaths.contains(node.file.getAbsolutePath())) {
                node.isExpanded = true;
                
                // โหลดลูกหลานของโฟลเดอร์นี้ตามลำดับชั้นความลึก และกรองไฟล์ซ่อนออก
                List<FileNode> rawChildren = FileSystemManager.loadChildren(node.file, node.depth);
                List<FileNode> children = new ArrayList<>();
                if (rawChildren != null) {
                    for (FileNode child : rawChildren) {
                        if (child.file != null && !child.file.getName().startsWith(".")) {
                            children.add(child);
                        }
                    }
                }
                
                if (!children.isEmpty()) {
                    rebuildTreeRecursive(children, expandedPaths, outputList);
                }
            }
        }
    }

    /**
     * 🔍 [เพิ่มใหม่]: ตัวช่วยสแกนค้นหาไฟล์ตามชื่อ วิ่งเจาะลึกเข้าไปในทุก ๆ โฟลเดอร์ย่อย (BFS Pattern)
     * เพื่อรับแรงกระแทกจากคำสั่งจิ้มวาร์ปหน้าแก้ไขโค้ดเมื่อพบ Error Link
     */
    public java.io.File findFileInProject(String rootPath, String targetFileName) {
        java.io.File root = new java.io.File(rootPath);
        if (!root.exists()) return null;
        
        java.util.Queue<java.io.File> queue = new java.util.LinkedList<>();
        queue.add(root);
        
        while (!queue.isEmpty()) {
            java.io.File current = queue.poll();
            java.io.File[] files = current.listFiles();
            if (files != null) {
                for (java.io.File f : files) {
                    if (f.isDirectory()) {
                        queue.add(f);
                    } else if (f.getName().equals(targetFileName)) {
                        return f; // ค้นเจอเป้าหมาย ส่งข้อมูลพิกัดไฟล์กลับทันที!
                    }
                }
            }
        }
        return null; // วิ่งหาจนทั่วแล้วไม่พบ
    }

public void openFile(File file) {
    if (file == null || !file.exists()) return;
    ProjectModel currentProject = activity.getCurrentProject();

    try {
        activity.getAutoSaveHandler().removeCallbacks(activity.getSaveRunnable());

        if (currentProject != null) {
            if (!currentProject.getOpenedFiles().contains(file)) {
                currentProject.getOpenedFiles().add(file);
            }
            currentProject.setCurrentOpenFile(file);
        }

        // อ่านไฟล์
        FileInputStream fis = new FileInputStream(file);
        BufferedReader reader = new BufferedReader(new InputStreamReader(fis, "UTF-8"));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            sb.append(line).append("\n");
        }
        reader.close();
        
        final String fileContent = sb.toString();

        // 🌟 แก้ไขตรงนี้: บังคับอัปเดตสถานะ UI ก่อน แล้วค่อยใส่ข้อความ
        activity.runOnUiThread(() -> {
            // 1. สั่งเปิดหน้าจอ Editor ทันที
            activity.setEditorActiveState(true); 
            
            // 2. ใส่โค้ดลงไป
            if (activity.getCodeEditor() != null) {
                activity.getCodeEditor().setText(fileContent);
                activity.getCodeEditor().setEditorLanguage(new JavaLanguage());
                activity.getCodeEditor().invalidate(); // บังคับวาดใหม่
            }
            
            // 3. อัปเดต Path และ Tab
            activity.updateFilePathStatus(file);
            if (activity.getTabAdapter() != null) activity.getTabAdapter().notifyDataSetChanged();
        });

    } catch (Exception e) {
        e.printStackTrace();
    }
}


    public void saveFile() {
        ProjectModel currentProject = activity.getCurrentProject();
        if (currentProject == null || currentProject.getCurrentOpenFile() == null) return;
        File fileToSave = currentProject.getCurrentOpenFile();
        try {
            FileOutputStream fos = new FileOutputStream(fileToSave);
            fos.write(activity.getCodeEditor().getText().toString().getBytes("UTF-8"));
            fos.close();
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
    }

    public File getFolderForImport() {
        return folderForImport;
    }
    
    public void onActivityResult(int requestCode, int resultCode, android.content.Intent data) {
        if (resultCode != android.app.Activity.RESULT_OK || data == null) return;
        
        android.net.Uri selectedFileUri = data.getData();
        if (selectedFileUri == null) return;

        ProjectModel currentProject = activity.getCurrentProject();
        java.io.File destinationFolder = folderForImport;
        if (destinationFolder == null && currentProject != null) {
            destinationFolder = new java.io.File(currentProject.getRootPath());
        }

        if (destinationFolder == null) {
            activity.showToast("❌ ไม่พบตำแหน่งที่ตั้งสำหรับนำเข้าไฟล์");
            return;
        }

        final java.io.File finalDestFolder = destinationFolder;

        new Thread(() -> {
            try {
                String fileName = "imported_file";
                android.database.Cursor cursor = activity.getContentResolver().query(selectedFileUri, null, null, null, null);
                if (cursor != null) {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (nameIndex != -1 && cursor.moveToFirst()) {
                        fileName = cursor.getString(nameIndex);
                    }
                    cursor.close();
                }

                java.io.File targetFile = new java.io.File(finalDestFolder, fileName);
                
                int copyCount = 1;
                String baseName = fileName;
                String extension = "";
                int dotIndex = fileName.lastIndexOf('.');
                if (dotIndex != -1) {
                    baseName = fileName.substring(0, dotIndex);
                    extension = fileName.substring(dotIndex);
                }
                while (targetFile.exists()) {
                    targetFile = new java.io.File(finalDestFolder, baseName + "_" + copyCount + extension);
                    copyCount++;
                }

                java.io.InputStream inputStream = activity.getContentResolver().openInputStream(selectedFileUri);
                java.io.FileOutputStream outputStream = new java.io.FileOutputStream(targetFile);
                
                byte[] buffer = new byte[4096];
                int bytesRead;
                if (inputStream != null) {
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    inputStream.close();
                }
                outputStream.close();

                final String finalFileName = targetFile.getName();
                activity.runOnUiThread(() -> {
                    activity.showToast("✨ นำเข้าไฟล์สำเร็จ: " + finalFileName);
                    refreshFileTree();
                });

            } catch (Exception e) {
                e.printStackTrace();
                activity.runOnUiThread(() -> activity.showToast("❌ การนำเข้าไฟล์ล้มเหลว: " + e.getMessage()));
            }
        }).start();
    }
}
