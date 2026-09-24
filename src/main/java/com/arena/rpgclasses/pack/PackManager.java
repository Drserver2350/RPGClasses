package com.arena.rpgclasses.pack;

import com.arena.rpgclasses.RPGClassesPlugin;
import com.sun.net.httpserver.HttpServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.Executors;

/**
 * Extracts the bundled resource pack, optionally serves it via a built-in HTTP server,
 * and pushes it to players on join.
 */
public final class PackManager {

    private final RPGClassesPlugin plugin;
    private File packFile;
    private byte[] sha1;
    private String sha1Hex;
    private HttpServer server;
    private String url;

    public PackManager(RPGClassesPlugin plugin) { this.plugin = plugin; }

    public boolean enabled() { return plugin.getConfig().getBoolean("resource-pack.enabled", true) && url != null; }
    public String url() { return url; }
    public String sha1Hex() { return sha1Hex; }

    public void start() {
        if (!plugin.getConfig().getBoolean("resource-pack.enabled", true)) return;
        try {
            extract();
            hash();
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to prepare resource pack: " + e.getMessage());
            return;
        }
        String mode = plugin.getConfig().getString("resource-pack.mode", "builtin");
        if (mode.equalsIgnoreCase("url")) {
            url = plugin.getConfig().getString("resource-pack.url");
            plugin.getLogger().info("Resource pack mode: external URL -> " + url);
        } else {
            startServer();
        }
    }

    public void stop() {
        if (server != null) { server.stop(0); server = null; }
    }

    private void extract() throws IOException {
        packFile = new File(plugin.getDataFolder(), "pack.zip");
        File stamp = new File(plugin.getDataFolder(), ".pack-version");
        String ver = plugin.getPluginMeta().getVersion();
        boolean fresh = !packFile.exists() || !stamp.exists() || !Files.readString(stamp.toPath()).trim().equals(ver);
        if (fresh) {
            try (InputStream in = plugin.getResource("pack.zip")) {
                if (in == null) throw new IOException("pack.zip missing from jar");
                Files.copy(in, packFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            Files.writeString(stamp.toPath(), ver);
            plugin.getLogger().info("Extracted resource pack to " + packFile.getPath());
        }
    }

    private void hash() throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        try (InputStream in = new FileInputStream(packFile)) {
            byte[] buf = new byte[8192]; int n;
            while ((n = in.read(buf)) > 0) md.update(buf, 0, n);
        }
        sha1 = md.digest();
        sha1Hex = HexFormat.of().formatHex(sha1);
        plugin.getLogger().info("Resource pack SHA-1: " + sha1Hex);
    }

    private void startServer() {
        int port = plugin.getConfig().getInt("resource-pack.builtin.port", 8123);
        try {
            server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
            server.createContext("/pack.zip", ex -> {
                byte[] data = Files.readAllBytes(packFile.toPath());
                ex.getResponseHeaders().add("Content-Type", "application/zip");
                ex.sendResponseHeaders(200, data.length);
                try (OutputStream os = ex.getResponseBody()) { os.write(data); }
            });
            server.createContext("/", ex -> {
                byte[] body = ("RPGClasses pack server. SHA-1: " + sha1Hex).getBytes(StandardCharsets.UTF_8);
                ex.sendResponseHeaders(200, body.length);
                try (OutputStream os = ex.getResponseBody()) { os.write(body); }
            });
            server.setExecutor(Executors.newFixedThreadPool(2, r -> { Thread t = new Thread(r, "RPGClasses-PackServer"); t.setDaemon(true); return t; }));
            server.start();
        } catch (IOException e) {
            plugin.getLogger().severe("Could not start built-in pack server on port " + port + ": " + e.getMessage());
            return;
        }
        String addr = plugin.getConfig().getString("resource-pack.builtin.public-address", "auto");
        if (addr == null || addr.isBlank() || addr.equalsIgnoreCase("auto")) {
            String serverIp = Bukkit.getIp();
            addr = (serverIp != null && !serverIp.isBlank() && !serverIp.equals("0.0.0.0")) ? serverIp : null;
            if (addr == null) {
                final int fport = port;
                // resolve public IP asynchronously so we don't block startup
                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                    String ip = detectPublicIp();
                    url = "http://" + ip + ":" + fport + "/pack.zip";
                    plugin.getLogger().info("Built-in pack server ready: " + url + "  (set resource-pack.builtin.public-address in config.yml if this is wrong)");
                });
                return;
            }
        }
        url = "http://" + addr + ":" + port + "/pack.zip";
        plugin.getLogger().info("Built-in pack server ready: " + url);
    }

    private String detectPublicIp() {
        for (String svc : new String[]{"https://api.ipify.org", "https://checkip.amazonaws.com", "https://ifconfig.me/ip"}) {
            try {
                HttpURLConnection c = (HttpURLConnection) URI.create(svc).toURL().openConnection();
                c.setConnectTimeout(3000); c.setReadTimeout(3000);
                c.setRequestProperty("User-Agent", "RPGClasses");
                try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()))) {
                    String ip = r.readLine();
                    if (ip != null && !ip.isBlank()) return ip.trim();
                }
            } catch (Exception ignored) {}
        }
        try { return InetAddress.getLocalHost().getHostAddress(); } catch (Exception e) { return "127.0.0.1"; }
    }

    public void send(Player p) {
        if (!enabled()) return;
        boolean required = plugin.getConfig().getBoolean("resource-pack.required", false);
        String raw = plugin.getConfig().getString("resource-pack.prompt", "&6RPGClasses resource pack");
        Component prompt = LegacyComponentSerializer.legacyAmpersand().deserialize(raw);
        try {
            p.setResourcePack(url, sha1, prompt, required);
        } catch (Exception e) {
            plugin.getLogger().warning("Could not send resource pack to " + p.getName() + ": " + e.getMessage());
            p.sendMessage(Component.text("Resource pack could not be sent: " + e.getMessage(), NamedTextColor.RED));
        }
    }
}
