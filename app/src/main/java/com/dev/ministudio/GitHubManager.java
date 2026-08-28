package com.dev.ministudio;

import android.os.Handler;
import android.os.Looper;
import net.lingala.zip4j.ZipFile;
import okhttp3.*;
import java.io.*;

public class GitHubManager {
    private final OkHttpClient client = new OkHttpClient();

    public void downloadRepo(String repoZipUrl, File targetDir, Runnable onComplete) {
        Request request = new Request.Builder().url(repoZipUrl).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                // แจ้งเตือน Error
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    File zipFile = new File(targetDir.getParent(), "temp.zip");
                    try (InputStream is = response.body().byteStream();
                         FileOutputStream fos = new FileOutputStream(zipFile)) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = is.read(buffer)) != -1) fos.write(buffer, 0, len);
                    }

                    // แตกไฟล์ด้วย Zip4j
                    new ZipFile(zipFile).extractAll(targetDir.getAbsolutePath());
                    zipFile.delete(); // ลบไฟล์ zip ทิ้ง

                    new Handler(Looper.getMainLooper()).post(onComplete);
                }
            }
        });
    }
}
