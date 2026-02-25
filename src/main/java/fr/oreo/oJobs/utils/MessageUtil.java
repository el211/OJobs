package fr.oreo.oJobs.utils;

import fr.oreo.oJobs.OJobs;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.Map;


public final class MessageUtil {

    private static final MiniMessage MM = MiniMessage.miniMessage();
    private static OJobs plugin;

    private MessageUtil() {}

    public static void init(OJobs instance) {
        plugin = instance;
    }


    public static Component parse(String raw, Map<String, String> placeholders) {
        if (raw == null) return Component.empty();
        TagResolver.Builder builder = TagResolver.builder();
        if (placeholders != null) {
            placeholders.forEach((k, v) ->
                    builder.resolver(Placeholder.parsed(k, v != null ? v : "")));
        }
        return MM.deserialize(raw, builder.build());
    }

    public static Component parse(String raw) {
        return raw == null ? Component.empty() : MM.deserialize(raw);
    }


    public static void send(Player player, String key, Map<String, String> replacements) {
        String prefix = plugin.getConfigManager().getLangConfig().getString("prefix", "<dark_gray>[<gold>Jobs</gold>]</dark_gray> ");
        String raw    = plugin.getConfigManager().getLangConfig().getString("messages." + key, key);
        if (raw.isBlank()) return;

        if (plugin.isPApiEnabled()) {
            raw = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, raw);
        }

        Component msg = parse(prefix + raw, replacements);
        player.sendMessage(msg);
    }

    public static void send(Player player, String key) {
        send(player, key, Map.of());
    }


    public static void sendRaw(Player player, String miniMessage, Map<String, String> replacements) {
        player.sendMessage(parse(miniMessage, replacements));
    }


    public static void sendActionBar(Player player, String miniMessage, Map<String, String> replacements) {
        if (!plugin.getConfigManager().getConfig().getBoolean("settings.actionbar-enabled", true)) return;
        if (plugin.isPApiEnabled()) {
            miniMessage = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, miniMessage);
        }
        player.sendActionBar(parse(miniMessage, replacements));
    }

    public static void sendActionBar(Player player, String miniMessage) {
        sendActionBar(player, miniMessage, Map.of());
    }


    public static void sendTitle(Player player,
                                 String titleRaw, String subtitleRaw,
                                 Map<String, String> replacements) {
        if (!plugin.getConfigManager().getConfig().getBoolean("settings.title-enabled", true)) return;

        int fadeIn  = plugin.getConfigManager().getConfig().getInt("settings.title-fade-in", 10);
        int stay    = plugin.getConfigManager().getConfig().getInt("settings.title-stay", 60);
        int fadeOut = plugin.getConfigManager().getConfig().getInt("settings.title-fade-out", 20);

        if (plugin.isPApiEnabled()) {
            titleRaw    = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, titleRaw);
            subtitleRaw = me.clip.placeholderapi.PlaceholderAPI.setPlaceholders(player, subtitleRaw);
        }

        Title title = Title.title(
                parse(titleRaw, replacements),
                parse(subtitleRaw, replacements),
                Title.Times.times(
                        Duration.ofMillis(fadeIn * 50L),
                        Duration.ofMillis(stay * 50L),
                        Duration.ofMillis(fadeOut * 50L)
                )
        );
        player.showTitle(title);
    }


    public static void playSound(Player player, String soundName, float volume, float pitch) {
        if (soundName == null || soundName.isBlank()) return;
        try {
            org.bukkit.Sound sound = org.bukkit.Sound.valueOf(soundName.toUpperCase());
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException ignored) {
        }
    }

    public static void playSound(Player player, String soundName) {
        playSound(player, soundName, 1.0f, 1.0f);
    }


    public static void broadcast(String miniMessage, Map<String, String> replacements) {
        Component msg = parse(miniMessage, replacements);
        org.bukkit.Bukkit.broadcast(msg);
    }
}
