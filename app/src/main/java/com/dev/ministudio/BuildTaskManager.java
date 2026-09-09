package com.dev.ministudio;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.Looper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.PushCommand;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.json.JSONArray;
import org.json.JSONObject;

public class BuildTaskManager {

    public interface BuildListener {
        void onLogAppend(String text, int color);
        void onBuildStarted();
        void onBuildFinished(boolean success, String apkPath);
    }

    private final Context context;
    private final String projectPath;
    private final BuildEnvironmentManager envManager;
    private final BuildListener listener;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private final int COLOR_INFO = Color.parseColor("#4FC3F7");
    private final int COLOR_SUCCESS = Color.parseColor("#81C784");
    private final int COLOR_ERROR = Color.parseColor("#FF8A80");
    private final int COLOR_WARNING = Color.parseColor("#FFB74D");
    private final int COLOR_TASK = Color.parseColor("#A9B1D6");

    private BuildSummaryAnalyzer externalAnalyzer;

    public BuildTaskManager(Context context, String projectPath, BuildListener listener) {
        this.context = context;
        this.projectPath = projectPath;
        this.envManager = new BuildEnvironmentManager(context);
        this.listener = listener;
    }

    public void setAnalyzer(BuildSummaryAnalyzer analyzer) {
        this.externalAnalyzer = analyzer;
    }

    public void startCloudBuild(final String githubToken, final String repoUrl,
                                final String projectName, final String packageName) {
        postUiEvent(BuildListener::onBuildStarted);

        new Thread(() -> {
            try {
                sendProgress("$ ./gradlew :app:assembleDebug\n", COLOR_INFO);
                sendProgress("Starting Gradle Daemon...\n", COLOR_INFO);

                File projectDir = new File(projectPath);

                createGitIgnore(projectDir);
                envManager.prepareGitHubWorkflow(projectPath, projectName, packageName, "Java", 21);

                sendProgress("> Configure project :app\n", COLOR_TASK);
                sendProgress("Uploading sources to GitHub remote...\n", COLOR_INFO);

                Git git;
                File gitDir = new File(projectDir, ".git");
                if (!gitDir.exists()) {
                    git = Git.init().setDirectory(projectDir).call();
                } else {
                    git = Git.open(projectDir);
                }

                git.add().addFilepattern(".").call();
                git.commit().setMessage("Cloud Build Request - "
                        + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date())).call();

                StoredConfig config = git.getRepository().getConfig();
                config.setString("remote", "origin", "url", repoUrl);
                config.save();

                UsernamePasswordCredentialsProvider credentials =
                        new UsernamePasswordCredentialsProvider(githubToken, "");
                PushCommand push = git.push();
                push.setCredentialsProvider(credentials);
                push.setForce(true);
                push.setRemote("origin");
                push.add("master").add("main");
                push.call();

                sendProgress("> Task :app:preBuild\n", COLOR_TASK);
                sendProgress("> Task :app:preDebugBuild\n", COLOR_TASK);
                sendProgress("Sources uploaded. Waiting for cloud build...\n", COLOR_SUCCESS);

                String repoPath = repoUrl.replace("https://github.com/", "").replace(".git", "");
                monitorWorkflowRuns(githubToken, repoPath, projectName);

            } catch (Exception e) {
                sendProgress("\nBUILD FAILED\n", COLOR_ERROR);
                sendProgress("Execution failed: " + e.getMessage() + "\n", COLOR_ERROR);
                postUiEvent(l -> l.onBuildFinished(false, null));
            }
        }).start();
    }

    private void monitorWorkflowRuns(String token, String repoPath, String projectName) {
        try {
            String urlStr = "https://api.github.com/repos/" + repoPath + "/actions/runs?per_page=1";
            sendProgress("> Task :app:mergeDebugResources\n", COLOR_TASK);
            sendProgress("Waiting for GitHub Actions runner...\n", COLOR_INFO);

            long startTime = System.currentTimeMillis();
            long runId = -1;

            while (System.currentTimeMillis() - startTime < 180000) {
                Thread.sleep(5000);
                HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
                conn.setRequestProperty("Authorization", "Bearer " + token);
                conn.setRequestProperty("Accept", "application/vnd.github.v3+json");

                if (conn.getResponseCode() == 200) {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) sb.append(line);
                    reader.close();

                    JSONObject json = new JSONObject(sb.toString());
                    JSONArray runs = json.getJSONArray("workflow_runs");
                    if (runs.length() > 0) {
                        JSONObject latestRun = runs.getJSONObject(0);
                        runId = latestRun.getLong("id");
                        String status = latestRun.getString("status");

                        sendProgress("Pipeline status: [" + status.toUpperCase() + "]\n", COLOR_WARNING);

                        if ("completed".equals(status)) {
                            String conclusion = latestRun.getString("conclusion");
                            if ("success".equals(conclusion)) {
                                sendProgress("> Task :app:processDebugManifest\n", COLOR_TASK);
                                sendProgress("> Task :app:compileDebugJavaWithJavac\n", COLOR_TASK);
                                sendProgress("> Task :app:dexBuilderDebug\n", COLOR_TASK);
                                sendProgress("> Task :app:packageDebug\n", COLOR_TASK);
                                sendProgress("> Task :app:assembleDebug\n", COLOR_TASK);
                                sendProgress("\nBUILD SUCCESSFUL\n", COLOR_SUCCESS);
                                sendProgress("Fetching APK artifact...\n", COLOR_SUCCESS);

                                DownloadTaskManager downloadTask = new DownloadTaskManager(
                                        context, projectName,
                                        new DownloadTaskManager.DownloadListener() {
                                            @Override
                                            public void onDownloadLog(String text, int color) {
                                                sendProgress(text + "\n", color);
                                            }

                                            @Override
                                            public void onDownloadFinished(boolean success, File apkFile) {
                                                if (success && apkFile != null) {
                                                    sendProgress(
                                                            "APK → " + apkFile.getAbsolutePath() + "\n",
                                                            COLOR_INFO);
                                                }
                                                postUiEvent(l -> l.onBuildFinished(
                                                        success,
                                                        apkFile != null ? apkFile.getAbsolutePath() : null));
                                            }
                                        });
                                downloadTask.startFetchAndInstall();

                            } else {
                                sendProgress("\nBUILD FAILED\n", COLOR_ERROR);
                                sendProgress("Fetching error logs from server...\n", COLOR_ERROR);
                                fetchAndParseBuildLogs(token, repoPath, runId);
                                postUiEvent(l -> l.onBuildFinished(false, null));
                            }
                            return;
                        }
                    }
                }
            }
            sendProgress("\nBUILD FAILED\n", COLOR_ERROR);
            sendProgress("Timeout waiting for GitHub Actions (3 min)\n", COLOR_ERROR);
            postUiEvent(l -> l.onBuildFinished(false, null));
        } catch (Exception e) {
            sendProgress("\nBUILD FAILED\n", COLOR_ERROR);
            sendProgress("Status check error: " + e.getMessage() + "\n", COLOR_ERROR);
            postUiEvent(l -> l.onBuildFinished(false, null));
        }
    }

    private void fetchAndParseBuildLogs(String token, String repoPath, long runId) {
        try {
            String jobsUrl = "https://api.github.com/repos/" + repoPath + "/actions/runs/" + runId + "/jobs";
            HttpURLConnection conn = (HttpURLConnection) new URL(jobsUrl).openConnection();
            conn.setRequestProperty("Authorization", "Bearer " + token);

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();

                JSONObject json = new JSONObject(sb.toString());
                JSONArray jobs = json.getJSONArray("jobs");
                if (jobs.length() > 0) {
                    JSONObject job = jobs.getJSONObject(0);
                    long jobId = job.getLong("id");

                    String logUrl = "https://api.github.com/repos/" + repoPath + "/actions/jobs/" + jobId + "/logs";
                    HttpURLConnection logConn = (HttpURLConnection) new URL(logUrl).openConnection();
                    logConn.setRequestProperty("Authorization", "Bearer " + token);

                    if (logConn.getResponseCode() == 200) {
                        BufferedReader logReader =
                                new BufferedReader(new InputStreamReader(logConn.getInputStream()));
                        final BuildSummaryAnalyzer analyzer =
                                (externalAnalyzer != null) ? externalAnalyzer : new BuildSummaryAnalyzer();

                        while ((line = logReader.readLine()) != null) {
                            boolean shouldStop = analyzer.analyzeLine(line, COLOR_WARNING, (txt, col) -> {
                                // keep internal only
                            });
                            if (shouldStop) break;
                        }
                        logReader.close();

                        analyzer.printSummary((txt, col) -> sendProgress(txt, col));
                        return;
                    }
                }
            }
            sendProgress("Could not fetch build logs from GitHub\n", COLOR_ERROR);
        } catch (Exception e) {
            sendProgress("Log parse error: " + e.getMessage() + "\n", COLOR_ERROR);
        }
    }

    private void createGitIgnore(File projectDir) {
        try {
            File gitIgnoreFile = new File(projectDir, ".gitignore");
            String content = ".gradle/\nbuild/\napp/build/\n*.iml\nlocal.properties\n";
            try (FileOutputStream fos = new FileOutputStream(gitIgnoreFile)) {
                fos.write(content.getBytes("UTF-8"));
            }
        } catch (Exception ignored) {
        }
    }

    private void sendProgress(final String text, final int color) {
        uiHandler.post(() -> listener.onLogAppend(text, color));
    }

    private interface UiEventAction {
        void run(BuildListener listener);
    }

    private void postUiEvent(final UiEventAction action) {
        uiHandler.post(() -> action.run(listener));
    }
}