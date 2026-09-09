package com.dev.ministudio;

public class ProjectTemplate {
    public final String id;
    public final String name;
    public final String description;
    public final int previewColor; // สีหัวการ์ด (จำลอง icon)

    public ProjectTemplate(String id, String name, String description, int previewColor) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.previewColor = previewColor;
    }
}