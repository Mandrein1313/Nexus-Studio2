package com.dev.ministudio.model;

import java.io.File;


public class FileNode {
    public File file;
    public int depth;          // ระดับความลึก (0 = นอกสุด, 1 = ลูกชั้นแรก, 2 = ลูกชั้นสอง...)
    public boolean isExpanded; // สถานะกางออก (true = กางอยู่, false = หุบอยู่)
    public boolean isDirectory;

    public FileNode(File file, int depth, boolean isDirectory) {
        this.file = file;
        this.depth = depth;
        this.isDirectory = isDirectory;
        this.isExpanded = false;
    }
}

