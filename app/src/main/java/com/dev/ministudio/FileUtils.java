package com.dev.ministudio;

import android.content.Context;
import android.net.Uri;
import java.io.*;

public class FileUtils {
    public static void copyFile(Context context, Uri sourceUri, String destFolderPath, String fileName) throws IOException {
        File dir = new File(destFolderPath);
        if (!dir.exists()) dir.mkdirs();

        InputStream in = context.getContentResolver().openInputStream(sourceUri);
        File destFile = new File(dir, fileName);
        FileOutputStream out = new FileOutputStream(destFile);

        byte[] buffer = new byte[1024];
        int read;
        while ((read = in.read(buffer)) != -1) {
            out.write(buffer, 0, read);
        }
        out.flush();
        out.close();
        in.close();
    }
}
