package fr.oreo.oJobs.utils;

import me.clip.placeholderapi.PlaceholderAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.bukkit.profile.PlayerTextures;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class ItemBuilder {

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private static final String RESET = "<!italic>";
    private static final boolean PAPI = Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null;

    private final ItemStack item;
    private final ItemMeta  meta;

    private ItemBuilder(Material material) {
        this.item = new ItemStack(material);
        this.meta = item.getItemMeta();
    }

    public static ItemBuilder of(Material material) {
        return new ItemBuilder(material != null ? material : Material.STONE);
    }

    public static ItemBuilder of(String materialName) {
        Material mat = Material.matchMaterial(materialName);
        return new ItemBuilder(mat != null ? mat : Material.STONE);
    }


    private static Component parse(String raw, Player player) {
        if (raw == null) return Component.empty();
        String resolved = (PAPI && player != null)
                ? PlaceholderAPI.setPlaceholders(player, raw)
                : raw;
        return MM.deserialize(RESET + resolved);
    }

    private static Component parse(String raw) {
        return parse(raw, null);
    }


    public ItemBuilder name(String miniMessage) {
        if (meta != null) {
            meta.displayName(parse(miniMessage));
        }
        return this;
    }

    public ItemBuilder name(String miniMessage, Player player) {
        if (meta != null) {
            meta.displayName(parse(miniMessage, player));
        }
        return this;
    }

    public ItemBuilder name(Component component) {
        if (meta != null) meta.displayName(component);
        return this;
    }


    public ItemBuilder lore(List<String> lines) {
        return lore(lines, null);
    }

    public ItemBuilder lore(List<String> lines, Player player) {
        if (meta != null) {
            List<Component> parsed = new ArrayList<>(lines.size());
            for (String line : lines) {
                if (line == null || line.isEmpty()) {
                    parsed.add(Component.empty());
                } else {
                    parsed.add(parse(line, player));
                }
            }
            meta.lore(parsed);
        }
        return this;
    }

    public ItemBuilder loreLine(String line) {
        return loreLine(line, null);
    }

    public ItemBuilder loreLine(String line, Player player) {
        if (meta != null) {
            List<Component> current = meta.lore() != null
                    ? new ArrayList<>(meta.lore())
                    : new ArrayList<>();
            if (line == null || line.isEmpty()) {
                current.add(Component.empty());
            } else {
                current.add(parse(line, player));
            }
            meta.lore(current);
        }
        return this;
    }


    public ItemBuilder amount(int amount) {
        item.setAmount(Math.max(1, Math.min(64, amount)));
        return this;
    }

    public ItemBuilder customModelData(int data) {
        if (meta != null && data >= 0) meta.setCustomModelData(data);
        return this;
    }

    public ItemBuilder glow() {
        if (meta != null) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        return this;
    }

    public ItemBuilder addEnchant(Enchantment enchantment, int level) {
        if (meta != null) meta.addEnchant(enchantment, level, true);
        return this;
    }

    public ItemBuilder hideAttributes() {
        if (meta != null) meta.addItemFlags(
                ItemFlag.HIDE_ATTRIBUTES,
                ItemFlag.HIDE_ENCHANTS,
                ItemFlag.HIDE_UNBREAKABLE,
                ItemFlag.HIDE_ITEM_SPECIFICS);
        return this;
    }

    public ItemBuilder skullTexture(String textureUrl) {
        if (meta instanceof SkullMeta skullMeta) {
            try {
                PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID());
                PlayerTextures textures = profile.getTextures();
                textures.setSkin(URI.create(textureUrl).toURL());
                profile.setTextures(textures);
                skullMeta.setOwnerProfile(profile);
            } catch (Exception ignored) {
            }
        }
        return this;
    }

    public ItemStack build() {
        if (meta != null) item.setItemMeta(meta);
        return item;
    }

    public static ItemStack filler(Material material) {
        return ItemBuilder.of(material)
                .name("<!italic> ")
                .hideAttributes()
                .build();
    }
}