package com.dev.ministudio.editor;

import android.text.Editable;
import android.text.TextWatcher;
import android.widget.EditText;
import android.widget.TextView;
import java.util.Stack;

public class EditorController {
    private EditText editor;
    private TextView lineNumbers;
    private Stack<String> undoStack = new Stack<>();
    private Stack<String> redoStack = new Stack<>();
    private boolean isInternalChange = false;

    public EditorController(EditText editor, TextView lineNumbers) {
        this.editor = editor;
        this.lineNumbers = lineNumbers;
        setupListeners();
    }

    private void setupListeners() {
        editor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (!isInternalChange) {
                    undoStack.push(s.toString());
                    redoStack.clear(); // เมื่อมีการพิมพ์ใหม่ ต้องล้าง Redo
                    if (undoStack.size() > 30) undoStack.remove(0); 
                }
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                updateLineNumbers();
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    public void updateLineNumbers() {
        int count = Math.max(1, editor.getLineCount());
        StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= count; i++) sb.append(i).append("\n");
        lineNumbers.setText(sb.toString());
    }

    public void undo() {
        if (!undoStack.isEmpty()) {
            isInternalChange = true;
            redoStack.push(editor.getText().toString());
            editor.setText(undoStack.pop());
            editor.setSelection(editor.getText().length());
            isInternalChange = false;
        }
    }

    public void redo() {
        if (!redoStack.isEmpty()) {
            isInternalChange = true;
            undoStack.push(editor.getText().toString());
            editor.setText(redoStack.pop());
            editor.setSelection(editor.getText().length());
            isInternalChange = false;
        }
    }

    public void insertText(String text) {
        int start = editor.getSelectionStart();
        editor.getText().insert(start, text);
    }
}
