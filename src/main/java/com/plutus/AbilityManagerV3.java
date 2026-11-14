package com.plutus;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class AbilityManagerV3 {

    private final PlutusMacePluginV3 plugin;
    private final Map<UUID, Long> dashCooldowns = new HashMap<>();
    private final Map<UUID, Long> pactCooldowns = new HashMap<>();
    private final Map<UUID, Set<UUID>> activePacts = new HashMap<>();

    private byte[] packBytes;
    private String packHash;
    private String hostedPackUrl;

    public AbilityManagerV3(PlutusMacePluginV3 plugin) {
        this.plugin = plugin;
        generateAndHostPack();
    }

    private void generateAndHostPack() {
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    try (ZipOutputStream zos = new ZipOutputStream(baos)) {
                        addFileToZip(zos, "pack.mcmeta", "{\"pack\":{\"description\":\"Plutus Mace Assets\",\"pack_format\":37}}");
                        addResourceToZip(zos, "assets/minecraft/textures/item/plutus_mace.png");
                        addResourceToZip(zos, "assets/minecraft/models/item/plutus_mace.json");
                        addResourceToZip(zos, "assets/minecraft/models/item/mace.json");
                    }
                    packBytes = baos.toByteArray();
                    MessageDigest digest = MessageDigest.getInstance("SHA-1");
                    byte[] hash = digest.digest(packBytes);
                    StringBuilder sb = new StringBuilder();
                    for (byte b : hash) sb.append(String.format("%02x", b));
                    packHash = sb.toString();
                    hostedPackUrl = uploadToCatbox(packBytes);
                } catch (Exception e) { plugin.getLogger().severe("Error: " + e.getMessage()); }
            }
        }.runTaskAsynchronously(plugin);
    }

    private void addFileToZip(ZipOutputStream zos, String path, String content) throws IOException {
        zos.putNextEntry(new ZipEntry(path));
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private void addResourceToZip(ZipOutputStream zos, String path) throws IOException {
        InputStream is = plugin.getResource(path);
        if (is == null) return;
        zos.putNextEntry(new ZipEntry(path));
        byte[] buffer = new byte[1024];
        int len;
        while ((len = is.read(buffer)) > 0) zos.write(buffer, 0, len);
        zos.closeEntry();
        is.close();
    }

    private String uploadToCatbox(byte[] data) {
        try {
            String boundary = "*****" + System.currentTimeMillis() + "*****";
            HttpURLConnection conn = (HttpURLConnection) new URL("https://catbox.moe/user/api.php").openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
            try (DataOutputStream dos = new DataOutputStream(conn.getOutputStream())) {
                dos.writeBytes("--" + boundary + "\r\nContent-Disposition: form-data; name=\"reqtype\"\r\n\r\nfileupload\r\n");
                dos.writeBytes("--" + boundary + "\r\nContent-Disposition: form-data; name=\"fileToUpload\"; filename=\"p.zip\"\r\nContent-Type: application/zip\r\n\r\n");
                dos.write(data);
                dos.writeBytes("\r\n--" + boundary + "--\r\n");
            }
            if (conn.getResponseCode() == 200) {
                try (Scanner s = new Scanner(conn.getInputStream())) { return s.hasNext() ? s.next() : null; }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public void sendPack(Player player) {
        if (hostedPackUrl != null && packHash != null) {
            player.setResourcePack(hostedPackUrl, packHash, true, MiniMessage.miniMessage().deserialize("<red>Plutus Mace texture pack loading..."));
        }
    }

    public void handleAbility(Player player, boolean isSneaking) {
        if (isSneaking) triggerPact(player); else triggerDash(player);
    }

    private void triggerDash(Player p) {
        p.setVelocity(p.getLocation().getDirection().multiply(1.8).setY(0.25));
        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_WITHER_SHOOT, 1f, 0.8f);
        p.getNearbyEntities(4.0, 4.0, 4.0).stream().filter(e -> e instanceof Player && !e.equals(p)).forEach(e -> ((Player) e).damage(3.0, p));
    }

    private void triggerPact(Player p) {
        Set<UUID> targets = new HashSet<>();
        p.getNearbyEntities(15.0, 15.0, 15.0).stream().filter(e -> e instanceof Player && !e.equals(p)).limit(3).forEach(e -> targets.add(e.getUniqueId()));
        if (!targets.isEmpty()) activePacts.put(p.getUniqueId(), targets);
    }

    public void handleDamageSharing(Player v, double dmg) {
        activePacts.values().stream().filter(s -> s.contains(v.getUniqueId())).findFirst().ifPresent(s ->
            s.stream().filter(id -> !id.equals(v.getUniqueId())).forEach(id -> {
                Player o = plugin.getServer().getPlayer(id);
                if (o != null && o.isOnline()) o.damage(dmg);
            }));
    }

    public void handleBloodToll(Player a) { a.damage(1.0); }
}
