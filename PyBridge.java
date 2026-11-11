package dev.pybridge;

import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * PyBridge: принимает команды из Python по TCP и выполняет их на сервере.
 *
 * Протокол (текстовый, UTF-8, одна строка = одно сообщение):
 *   клиент -> "AUTH <token>"       сервер -> "OK" | "ERR"
 *   клиент -> "<команда>"          сервер -> "OK <base64 вывода>" | "ERR <base64 вывода>"
 */
public final class PyBridge extends JavaPlugin {

    private volatile boolean running;
    private ServerSocket serverSocket;
    private Thread acceptThread;
    private String token;
    private int timeoutSeconds;

    @Override
    public void onEnable() {
        saveDefaultConfig();

        token = getConfig().getString("token", "");
        if (token == null || token.isBlank()) {
            token = UUID.randomUUID().toString().replace("-", "");
            getConfig().set("token", token);
            saveConfig();
            getLogger().info("Сгенерирован новый токен, он лежит в plugins/PyBridge/config.yml");
        }

        int port = getConfig().getInt("port", 25580);
        String bind = getConfig().getString("bind", "127.0.0.1");
        timeoutSeconds = Math.max(1, getConfig().getInt("timeout-seconds", 10));

        try {
            serverSocket = new ServerSocket(port, 20, InetAddress.getByName(bind));
        } catch (IOException e) {
            getLogger().severe("Не удалось открыть порт " + bind + ":" + port + " — " + e.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        running = true;
        acceptThread = new Thread(this::acceptLoop, "PyBridge-Accept");
        acceptThread.setDaemon(true);
        acceptThread.start();
        getLogger().info("PyBridge слушает " + bind + ":" + port);
    }

    @Override
    public void onDisable() {
        running = false;
        try {
            if (serverSocket != null) serverSocket.close();
        } catch (IOException ignored) {
        }
    }

    private void acceptLoop() {
        while (running) {
            try {
                Socket client = serverSocket.accept();
                Thread t = new Thread(() -> handleClient(client), "PyBridge-Client");
                t.setDaemon(true);
                t.start();
            } catch (IOException e) {
                if (running) getLogger().warning("Ошибка accept: " + e.getMessage());
            }
        }
    }

    private void handleClient(Socket client) {
        String remote = String.valueOf(client.getRemoteSocketAddress());
        try (client;
             BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8), true)) {

            client.setSoTimeout(30_000);

            String first = in.readLine();
            if (first == null || !first.startsWith("AUTH ")
                    || !constantTimeEquals(first.substring(5).trim(), token)) {
                out.println("ERR");
                getLogger().warning("Неудачная авторизация от " + remote);
                return;
            }
            out.println("OK");
            client.setSoTimeout(0);
            getLogger().info("Python-клиент подключён: " + remote);

            String line;
            while (running && (line = in.readLine()) != null) {
                line = line.strip();
                if (line.isEmpty()) {
                    out.println("OK ");
                    continue;
                }
                if (line.startsWith("/")) line = line.substring(1);
                out.println(runOnMainThread(line));
            }
        } catch (IOException ignored) {
            // клиент отключился
        } finally {
            getLogger().info("Python-клиент отключён: " + remote);
        }
    }

    private String runOnMainThread(String command) {
        CompletableFuture<String> future = new CompletableFuture<>();

        Bukkit.getScheduler().runTask(this, () -> {
            StringBuilder output = new StringBuilder();
            try {
                CommandSender sender = Bukkit.createCommandSender(component ->
                        output.append(PlainTextComponentSerializer.plainText().serialize(component)).append('\n'));
                boolean ok = Bukkit.dispatchCommand(sender, command);
                future.complete((ok ? "OK " : "ERR ") + b64(output.toString().stripTrailing()));
            } catch (Throwable t) {
                future.complete("ERR " + b64("Исключение: " + t));
            }
        });

        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            return "ERR " + b64("Таймаут или ошибка выполнения: " + e);
        }
    }

    private static String b64(String s) {
        return Base64.getEncoder().encodeToString(s.getBytes(StandardCharsets.UTF_8));
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
