package com.plutus;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PlutusMacePluginV3 extends JavaPlugin {

    private static PlutusMacePluginV3 instance;
    private NamespacedKey maceKey;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private AbilityManagerV3 abilityManager;

    @Override
    public void onEnable() {
        instance = this;
        maceKey = new NamespacedKey(this, "plutus_mace");

        if (!new File(getDataFolder(), "config.yml").exists()) {
            saveResource("config.yml", false);
        }

        abilityManager = new AbilityManagerV3(this);

        Objects.requireNonNull(getCommand("giveplutus")).setExecutor((sender, command, label, args) -> {
            if (!(sender instanceof org.bukkit.entity.Player player)) {
                sender.sendMessage("Players only.");
                return true;
            }
            if (!player.hasPermission("plutus.admin")) {
                player.sendMessage(mm.deserialize("<red>No permission."));
                return true;
            }

            player.getInventory().addItem(createMace());
            player.sendMessage(mm.deserialize(getConfig().getString("weapon-name", "<gradient:#8B0000:#FF0000><bold>Plutus Mace</bold></gradient>") + " <gray>given."));
            return true;
        });

        getServer().getPluginManager().registerEvents(new MaceListenerV3(this, abilityManager), this);
        getLogger().info("PlutusMace v3.0.0 enabled successfully!");
    }

    public ItemStack createMace() {
        FileConfiguration config = getConfig();
        ItemStack mace = new ItemStack(Material.MACE);
        ItemMeta meta = mace.getItemMeta();

        if (meta != null) {
            meta.displayName(mm.deserialize(config.getString("weapon-name", "<gradient:#8B0000:#FF0000><bold>Plutus Mace</bold></gradient>")));

            List<String> loreConfig = config.getStringList("lore");
            if (loreConfig.isEmpty()) {
                loreConfig = List.of(
                    "<gray>A weapon forged from blood and crimson flame.",
                    "<dark_red>⚡ Passive: Blood Toll",
                    "<red>⚔ Ability 1: Blood Dash <gray>(Press F)",
                    "<dark_purple>⛓ Ability 2: Blood Pact <gray>(Shift + F)"
                );
            }
            List<Component> loreLines = new ArrayList<>();
            for (String s : loreConfig) {
                loreLines.add(mm.deserialize(s));
            }
            meta.lore(loreLines);

            meta.setCustomModelData(config.getInt("custom-model-data", 70001));
            meta.getPersistentDataContainer().set(maceKey, PersistentDataType.BYTE, (byte) 1);
            mace.setItemMeta(meta);
        }
        return mace;
    }

    public boolean isPlutusMace(ItemStack item) {
        if (item == null || item.getType() != Material.MACE || !item.hasItemMeta()) return false;
        return item.getItemMeta().getPersistentDataContainer().has(maceKey, PersistentDataType.BYTE);
    }

    public AbilityManagerV3 getAbilityManager() {
        return abilityManager;
    }

    public static PlutusMacePluginV3 getInstance() {
        return instance;
    }
}
