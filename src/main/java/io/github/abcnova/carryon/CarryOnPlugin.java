package io.github.abcnova.carryon;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.logging.Level;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Keyed;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.Tag;
import org.bukkit.attribute.Attribute;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.TabExecutor;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.AbstractVillager;
import org.bukkit.entity.Ageable;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

public final class CarryOnPlugin
extends JavaPlugin
implements Listener,
TabExecutor {
    private NamespacedKey typeKey;
    private NamespacedKey materialKey;
    private NamespacedKey blockDataKey;
    private NamespacedKey inventoryKey;
    private NamespacedKey entityTypeKey;
    private NamespacedKey entityNameKey;
    private NamespacedKey entityHealthKey;
    private NamespacedKey entityBabyKey;
    private NamespacedKey entityRecipesKey;
    private NamespacedKey villagerDataKey;
    private MessageService messages;
    private SoundService sounds;
    private static final String MODRINTH_URL = "https://modrinth.com/plugin/carryonpaper";
    private static final String GITHUB_URL = "https://github.com/ABC_Nova/CarryOnPaper/releases/latest";
    private volatile String availableVersion;
    private String lastNotifiedVersion;

    public void onEnable() {
        this.saveDefaultConfig();
        this.migrateConfig();
        this.saveDefaultResource("sounds.yml");
        this.saveDefaultResource("lang/en_us.yml");
        this.saveDefaultResource("lang/de-eu.yml");
        this.saveDefaultResource("lang/fr_fr.yml");
        this.saveDefaultResource("lang/es_es.yml");
        this.typeKey = new NamespacedKey((Plugin)this, "carry_type");
        this.materialKey = new NamespacedKey((Plugin)this, "block_material");
        this.blockDataKey = new NamespacedKey((Plugin)this, "block_data");
        this.inventoryKey = new NamespacedKey((Plugin)this, "inventory");
        this.entityTypeKey = new NamespacedKey((Plugin)this, "entity_type");
        this.entityNameKey = new NamespacedKey((Plugin)this, "entity_name");
        this.entityHealthKey = new NamespacedKey((Plugin)this, "entity_health");
        this.entityBabyKey = new NamespacedKey((Plugin)this, "entity_baby");
        this.entityRecipesKey = new NamespacedKey((Plugin)this, "entity_recipes");
        this.villagerDataKey = new NamespacedKey((Plugin)this, "villager_data");
        this.messages = new MessageService(this);
        this.sounds = new SoundService(this);
        this.messages.load();
        this.sounds.load();
        this.startUpdateChecker();
        Bukkit.getPluginManager().registerEvents((Listener)this, (Plugin)this);
        PluginCommand command = this.getCommand("carryon");
        if (command != null) {
            command.setExecutor((CommandExecutor)this);
            command.setTabCompleter((TabCompleter)this);
        }
    }

    private void migrateConfig() {
        int configVersion = this.getConfig().getInt("config-version", 1);
        if (configVersion >= 7) {
            return;
        }
        this.getConfig().set("config-version", 7);
        this.getConfig().set("blocks.allow-all", false);
        this.getConfig().set("entities.allow-hostile-in-survival", null);
        this.getConfig().set("message-format", this.getConfig().getString("message-format", "minimessage"));
        this.getConfig().set("carrying.multiple.enabled", this.getConfig().getBoolean("carrying.multiple.enabled", false));
        this.getConfig().set("carrying.multiple.max-carried", this.getConfig().getInt("carrying.multiple.max-carried", 3));
        this.getConfig().set("carrying.allow-carriers-in-containers", this.getConfig().getBoolean("carrying.allow-carriers-in-containers", false));
        this.getConfig().set("entities.size-check.enabled", this.getConfig().getBoolean("entities.size-check.enabled", false));
        this.getConfig().set("update-checker.enabled", this.getConfig().getBoolean("update-checker.enabled", true));
        // Default to future-proof mob support so new Minecraft entity types are not silently excluded.
        this.getConfig().set("entities.use-whitelist", false);
        List<String> configuredBlocks = new ArrayList<>(this.getConfig().getStringList("blocks.whitelist"));
        for (String block : List.of("WHITE_SHULKER_BOX", "ORANGE_SHULKER_BOX", "MAGENTA_SHULKER_BOX", "LIGHT_BLUE_SHULKER_BOX", "YELLOW_SHULKER_BOX", "LIME_SHULKER_BOX", "PINK_SHULKER_BOX", "GRAY_SHULKER_BOX", "LIGHT_GRAY_SHULKER_BOX", "CYAN_SHULKER_BOX", "PURPLE_SHULKER_BOX", "BLUE_SHULKER_BOX", "BROWN_SHULKER_BOX", "GREEN_SHULKER_BOX", "RED_SHULKER_BOX", "BLACK_SHULKER_BOX", "ANVIL", "CHIPPED_ANVIL", "DAMAGED_ANVIL")) {
            if (!configuredBlocks.contains(block)) configuredBlocks.add(block);
        }
        this.getConfig().set("blocks.whitelist", configuredBlocks);
        this.saveConfig();
    }

    public void reloadConfig() {
        super.reloadConfig();
        if (this.messages != null) {
            this.messages.load();
        }
        if (this.sounds != null) {
            this.sounds.load();
        }
    }

    private void saveDefaultResource(String path) {
        File file = new File(this.getDataFolder(), path);
        if (!file.exists()) {
            this.saveResource(path, false);
        }
    }

    private void startUpdateChecker() {
        if (!this.getConfig().getBoolean("update-checker.enabled", true)) return;
        Bukkit.getScheduler().runTaskAsynchronously(this, this::checkForUpdate);
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, this::checkForUpdate, 12L * 60L * 60L * 20L, 12L * 60L * 60L * 20L);
    }

    private void checkForUpdate() {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection)new URL("https://api.modrinth.com/v2/project/z42br7Kq/version").openConnection();
            connection.setRequestProperty("User-Agent", "ABC_Nova/CarryOnPaper/1.0.2");
            connection.setConnectTimeout(4000);
            connection.setReadTimeout(4000);
            if (connection.getResponseCode() != 200) return;
            String json;
            try (InputStream input = connection.getInputStream()) {
                json = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            }
            Matcher matcher = Pattern.compile("\\\"version_number\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(json);
            if (!matcher.find()) return;
            String latest = matcher.group(1);
            if (!isNewer(latest, "1.0.2")) return;
            this.availableVersion = latest;
            Bukkit.getScheduler().runTask(this, () -> this.notifyUpdate(latest));
        } catch (Exception exception) {
            this.getLogger().fine("Update check unavailable: " + exception.getClass().getSimpleName());
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private static boolean isNewer(String candidate, String current) {
        try {
            String[] a = candidate.split("[.+-]", 4), b = current.split("[.+-]", 4);
            for (int i = 0; i < 3; i++) {
                int left = i < a.length ? Integer.parseInt(a[i]) : 0;
                int right = i < b.length ? Integer.parseInt(b[i]) : 0;
                if (left != right) return left > right;
            }
        } catch (NumberFormatException ignored) { return false; }
        return false;
    }

    private void notifyUpdate(String version) {
        if (version.equals(this.lastNotifiedVersion)) return;
        this.lastNotifiedVersion = version;
        this.getLogger().info("CarryOnPaper " + version + " is available. Modrinth: " + MODRINTH_URL + " | GitHub: " + GITHUB_URL);
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.isOp()) {
                player.sendMessage(ChatColor.GOLD + "CarryOnPaper " + version + " is available. Modrinth: " + MODRINTH_URL + " | GitHub: " + GITHUB_URL);
            }
        }
    }

    @EventHandler
    public void onUpdateNoticeJoin(org.bukkit.event.player.PlayerJoinEvent event) {
        String version = this.availableVersion;
        if (version != null && event.getPlayer().isOp()) {
            event.getPlayer().sendMessage(ChatColor.GOLD + "CarryOnPaper " + version + " is available. Modrinth: " + MODRINTH_URL + " | GitHub: " + GITHUB_URL);
        }
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onBlockInteract(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (this.isCarrier(hand) && event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            event.setCancelled(true);
            this.placeCarried(player, hand, event.getClickedBlock(), event.getBlockFace());
            return;
        }
        if (!this.getConfig().getBoolean("carrying.multiple.enabled", false) && this.getConfig().getBoolean("block-actions-while-carrying", true) && this.hasCarrier(player)) {
            event.setCancelled(true);
            this.tell((CommandSender)player, "already-carrying");
            return;
        }
        if (event.getAction() != Action.RIGHT_CLICK_BLOCK || event.getClickedBlock() == null) {
            return;
        }
        if (!this.canAttemptPickup(player)) {
            return;
        }
        if (!player.hasPermission("carryon.use")) {
            event.setCancelled(true);
            this.tell((CommandSender)player, "no-permission");
            return;
        }
        if (!player.hasPermission("carryon.carry.blocks")) {
            event.setCancelled(true);
            this.tell((CommandSender)player, "no-permission");
            return;
        }
        Block block = event.getClickedBlock();
        if (!this.canCarryBlock(block)) {
            return;
        }
        event.setCancelled(true);
        this.pickupBlock(player, block);
    }

    @EventHandler(priority=EventPriority.HIGHEST, ignoreCancelled=true)
    public void onEntityInteract(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }
        Player player = event.getPlayer();
        Entity entity = event.getRightClicked();
        if (!this.canCarryEntity(player, entity)) {
            return;
        }
        if (!this.canAttemptPickup(player)) {
            return;
        }
        if (!player.hasPermission("carryon.use")) {
            event.setCancelled(true);
            this.tell((CommandSender)player, "no-permission");
            return;
        }
        if (!player.hasPermission("carryon.carry.entities")) {
            event.setCancelled(true);
            this.tell((CommandSender)player, "no-permission");
            return;
        }
        event.setCancelled(true);
        this.pickupEntity(player, entity);
    }

    @EventHandler(ignoreCancelled=true)
    public void onInventoryClick(InventoryClickEvent event) {
        Player player;
        if (this.getConfig().getBoolean("carrying.allow-carriers-in-containers", false)) {
            return;
        }
        HumanEntity humanEntity = event.getWhoClicked();
        if (humanEntity instanceof Player && this.hasCarrier(player = (Player)humanEntity)) {
            ItemStack current = event.getCurrentItem();
            ItemStack cursor = event.getCursor();
            if (this.isCarrier(current) || this.isCarrier(cursor)) {
                event.setCancelled(true);
            }
        }
    }

    private boolean canAttemptPickup(Player player) {
        if (this.getConfig().getBoolean("require-sneak", true) && !player.isSneaking()) {
            return false;
        }
        if (!this.getConfig().getBoolean("carrying.multiple.enabled", false) && this.hasCarrier(player)) {
            this.tell((CommandSender)player, "already-carrying");
            return false;
        }
        int maxCarried = Math.max(1, this.getConfig().getInt("carrying.multiple.max-carried", 1));
        if (this.countCarriers(player) >= maxCarried && !player.hasPermission("carryon.bypass.limit")) {
            this.tell((CommandSender)player, "carry-limit-reached", "%limit%", String.valueOf(maxCarried));
            return false;
        }
        return !this.getConfig().getBoolean("require-empty-hands", true) || player.getInventory().getItemInMainHand().getType().isAir() && player.getInventory().getItemInOffHand().getType().isAir();
    }

    private void pickupBlock(Player player, Block block) {
        BlockState state = block.getState();
        ItemStack carrier = this.createBaseCarrier("block", this.visualMaterialForBlock(block.getType()));
        ItemMeta meta = carrier.getItemMeta();
        meta.setDisplayName(this.messages.color("&6Carried " + this.pretty(block.getType().name())));
        ArrayList<String> lore = new ArrayList<String>();
        lore.add(this.messages.color("&7Block: &f" + block.getType().name()));
        lore.add(this.messages.color("&8Right-click a block to place."));
        meta.setLore(lore);
        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(this.materialKey, PersistentDataType.STRING, block.getType().name());
        data.set(this.blockDataKey, PersistentDataType.STRING, block.getBlockData().getAsString());
        if (this.getConfig().getBoolean("blocks.preserve-inventories", true) && state instanceof InventoryHolder) {
            InventoryHolder holder = (InventoryHolder)state;
            String serialized = this.serializeInventory(holder.getInventory());
            if (serialized.isEmpty()) {
                this.tell((CommandSender)player, "cannot-carry");
                return;
            }
            data.set(this.inventoryKey, PersistentDataType.STRING, serialized);
            holder.getInventory().clear();
        }
        carrier.setItemMeta(meta);
        block.setType(Material.AIR, false);
        this.giveCarrier(player, carrier);
        this.applyCarryEffect(player);
        this.tell((CommandSender)player, "pickup-block", "%name%", this.pretty(state.getType().name()));
    }

    private void pickupEntity(Player player, Entity entity) {
        AbstractVillager villager;
        ItemStack carrier = this.createBaseCarrier("entity", this.visualMaterialForEntity(entity.getType()));
        ItemMeta meta = carrier.getItemMeta();
        String name = entity.getCustomName() == null ? this.pretty(entity.getType().name()) : entity.getCustomName();
        meta.setDisplayName(this.messages.color("&6Carried " + name));
        ArrayList<String> lore = new ArrayList<String>();
        lore.add(this.messages.color("&7Entity: &f" + entity.getType().name()));
        lore.add(this.messages.color("&8Right-click a block to place."));
        meta.setLore(lore);
        PersistentDataContainer data = meta.getPersistentDataContainer();
        data.set(this.entityTypeKey, PersistentDataType.STRING, entity.getType().name());
        if (entity.getCustomName() != null) {
            data.set(this.entityNameKey, PersistentDataType.STRING, entity.getCustomName());
        }
        if (entity instanceof LivingEntity) {
            LivingEntity living = (LivingEntity)entity;
            data.set(this.entityHealthKey, PersistentDataType.DOUBLE, living.getHealth());
        }
        if (entity instanceof Ageable) {
            Ageable ageable = (Ageable)entity;
            data.set(this.entityBabyKey, PersistentDataType.INTEGER, (ageable.isAdult() ? 0 : 1));
        }
        if (entity instanceof AbstractVillager) {
            villager = (AbstractVillager)entity;
            data.set(this.entityRecipesKey, PersistentDataType.STRING, this.serializeRecipes(villager.getRecipes()));
        }
        if (entity instanceof Villager) {
            villager = (Villager)entity;
            data.set(this.villagerDataKey, PersistentDataType.STRING, this.serializeVillagerData((Villager)villager));
        }
        carrier.setItemMeta(meta);
        entity.remove();
        this.giveCarrier(player, carrier);
        this.applyCarryEffect(player);
        this.tell((CommandSender)player, "pickup-entity", "%name%", name);
    }

    private ItemStack createBaseCarrier(String type) {
        return this.createBaseCarrier(type, this.getCarrierMaterial());
    }

    private ItemStack createBaseCarrier(String type, Material material) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(this.typeKey, PersistentDataType.STRING, type);
        item.setItemMeta(meta);
        return item;
    }

    private Material visualMaterialForBlock(Material material) {
        if (this.getConfig().getBoolean("use-realistic-carrier-items", true) && material.isItem()) {
            return material;
        }
        return this.getCarrierMaterial();
    }

    private Material visualMaterialForEntity(EntityType entityType) {
        Material egg;
        if (this.getConfig().getBoolean("use-realistic-carrier-items", true) && (egg = Material.matchMaterial((String)(entityType.name() + "_SPAWN_EGG"))) != null && egg.isItem()) {
            return egg;
        }
        return this.getCarrierMaterial();
    }

    private void giveCarrier(Player player, ItemStack carrier) {
        PlayerInventory inventory = player.getInventory();
        if (this.getConfig().getBoolean("carrying.multiple.enabled", false)) {
            inventory.addItem(new ItemStack[]{carrier}).values().forEach(item -> player.getWorld().dropItemNaturally(player.getLocation(), item));
        } else {
            inventory.setItemInMainHand(carrier);
        }
    }

    private void placeCarried(Player player, ItemStack carrier, Block clicked, BlockFace face) {
        String type = (String)carrier.getItemMeta().getPersistentDataContainer().get(this.typeKey, PersistentDataType.STRING);
        if ("block".equals(type)) {
            this.placeBlock(player, carrier, clicked, face);
        } else if ("entity".equals(type)) {
            this.placeEntity(player, carrier, clicked, face);
        }
    }

    private void placeBlock(Player player, ItemStack carrier, Block clicked, BlockFace face) {
        BlockState blockState;
        String inventoryText;
        Block target = clicked.getRelative(face);
        BlockState previousState = target.getState();
        if (!target.getType().isAir() && !target.isPassable()) {
            this.tell((CommandSender)player, "cannot-carry");
            return;
        }
        PersistentDataContainer data = carrier.getItemMeta().getPersistentDataContainer();
        String materialName = (String)data.get(this.materialKey, PersistentDataType.STRING);
        String blockDataText = (String)data.get(this.blockDataKey, PersistentDataType.STRING);
        Material material = Material.matchMaterial((String)(materialName == null ? "" : materialName));
        if (material == null || material.isAir()) {
            this.tell((CommandSender)player, "cannot-carry");
            return;
        }
        target.setType(material, false);
        if (blockDataText != null) {
            try {
                BlockData blockData = Bukkit.createBlockData((String)blockDataText);
                target.setBlockData(blockData, false);
            }
            catch (IllegalArgumentException ignored) {
                target.setType(material, false);
            }
        }
        if ((inventoryText = (String)data.get(this.inventoryKey, PersistentDataType.STRING)) != null) {
            blockState = target.getState();
            if (!(blockState instanceof InventoryHolder) || !this.restoreInventory(((InventoryHolder)blockState).getInventory(), inventoryText)) {
                previousState.update(true, false);
                this.tell((CommandSender)player, "cannot-carry");
                return;
            }
        }
        this.removeUsedCarrier(player);
        this.removeCarryEffectIfDone(player);
        this.tell((CommandSender)player, "place-block", "%name%", this.pretty(material.name()));
    }

    private void placeEntity(Player player, ItemStack carrier, Block clicked, BlockFace face) {
        Villager villager;
        PersistentDataContainer data = carrier.getItemMeta().getPersistentDataContainer();
        String entityTypeName = (String)data.get(this.entityTypeKey, PersistentDataType.STRING);
        EntityType type = this.parseEntityType(entityTypeName);
        if (type == null || !type.isSpawnable()) {
            this.tell((CommandSender)player, "cannot-carry");
            return;
        }
        Location location = clicked.getRelative(face).getLocation().add(0.5, 0.0, 0.5);
        Entity entity = player.getWorld().spawnEntity(location, type);
        String name = (String)data.get(this.entityNameKey, PersistentDataType.STRING);
        if (name != null) {
            entity.setCustomName(name);
            entity.setCustomNameVisible(true);
        }
        if (entity instanceof LivingEntity) {
            LivingEntity living = (LivingEntity)entity;
            Double health = (Double)data.get(this.entityHealthKey, PersistentDataType.DOUBLE);
            if (health != null) {
                Attribute maximum = resolveMaxHealthAttribute();
                org.bukkit.attribute.AttributeInstance maxHealth = living.getAttribute(maximum);
                if (maxHealth != null) living.setHealth(Math.max(1.0, Math.min(health, maxHealth.getValue())));
            }
        }
        if (entity instanceof Ageable) {
            Ageable ageable = (Ageable)entity;
            Integer baby = (Integer)data.get(this.entityBabyKey, PersistentDataType.INTEGER);
            if (baby != null && baby == 1) {
                ageable.setBaby();
            }
        }
        if (entity instanceof Villager) {
            villager = (Villager)entity;
            this.restoreVillagerData(villager, (String)data.get(this.villagerDataKey, PersistentDataType.STRING));
        }
        if (entity instanceof AbstractVillager) {
            AbstractVillager abstractVillager = (AbstractVillager)entity;
            List<MerchantRecipe> recipes = this.deserializeRecipes((String)data.get(this.entityRecipesKey, PersistentDataType.STRING));
            if (!recipes.isEmpty()) {
                abstractVillager.setRecipes(recipes);
            }
        }
        this.removeUsedCarrier(player);
        this.removeCarryEffectIfDone(player);
        this.tell((CommandSender)player, "place-entity", "%name%", this.pretty(type.name()));
    }

    private static Attribute resolveMaxHealthAttribute() {
        try { return Attribute.valueOf("MAX_HEALTH"); }
        catch (IllegalArgumentException ignored) { return Attribute.valueOf("GENERIC_MAX_HEALTH"); }
    }
    private boolean canCarryBlock(Block block) {
        Material type = block.getType();
        if (type.isAir()) {
            return false;
        }
        List blacklist = this.getConfig().getStringList("blocks.blacklist");
        if (this.containsMaterial(blacklist, type)) {
            return false;
        }
        if (this.getConfig().getBoolean("blocks.allow-all", false)) {
            return true;
        }
        return this.containsMaterial(this.getConfig().getStringList("blocks.whitelist"), type);
    }

    private boolean canCarryEntity(Player player, Entity entity) {
        if (!this.getConfig().getBoolean("entities.enabled", true)) {
            return false;
        }
        if (!(entity instanceof LivingEntity)) {
            return false;
        }
        if (entity instanceof Player) {
            return false;
        }
        if (this.containsText(this.getConfig().getStringList("entities.blacklist"), entity.getType().name())) {
            return false;
        }
        if (this.getConfig().getBoolean("entities.use-whitelist", true) && !this.containsText(this.getConfig().getStringList("entities.whitelist"), entity.getType().name())) {
            return false;
        }
        if (!this.getConfig().getBoolean("entities.size-check.enabled", false)) {
            return true;
        }
        return entity.getBoundingBox().getWidthX() <= this.getConfig().getDouble("entities.size-check.max-width", 2.0) && entity.getBoundingBox().getHeight() <= this.getConfig().getDouble("entities.size-check.max-height", 3.0);
    }

    private EntityType parseEntityType(String name) {
        if (name == null) {
            return null;
        }
        try {
            return EntityType.valueOf((String)name);
        }
        catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private boolean containsMaterial(List<String> values, Material material) {
        return values.stream().anyMatch(value -> {
            Material configured = Material.matchMaterial((String)value);
            return configured == material;
        });
    }

    private boolean containsText(List<String> values, String text) {
        return values.stream().anyMatch(value -> value.equalsIgnoreCase(text));
    }

    private boolean hasCarrier(Player player) {
        return this.countCarriers(player) > 0;
    }

    private int countCarriers(Player player) {
        int count = 0;
        for (ItemStack item : player.getInventory().getContents()) {
            if (!this.isCarrier(item)) continue;
            count += Math.max(1, item.getAmount());
        }
        return count;
    }

    private void removeUsedCarrier(Player player) {
        ItemStack hand = player.getInventory().getItemInMainHand();
        if (!this.isCarrier(hand)) {
            return;
        }
        if (hand.getAmount() <= 1) {
            player.getInventory().setItemInMainHand(null);
        } else {
            hand.setAmount(hand.getAmount() - 1);
        }
    }

    private boolean isCarrier(ItemStack item) {
        if (item == null || item.getType().isAir() || !item.hasItemMeta()) {
            return false;
        }
        return item.getItemMeta().getPersistentDataContainer().has(this.typeKey, PersistentDataType.STRING);
    }

    private Material getCarrierMaterial() {
        String configured = this.getConfig().getString("carrier-item-material", "PAPER");
        Material material = Material.matchMaterial((String)(configured == null ? "PAPER" : configured));
        if (material == null || material.isAir()) {
            this.getLogger().warning("Invalid carrier-item-material '" + configured + "', using PAPER.");
            return Material.PAPER;
        }
        return material;
    }

    private void applyCarryEffect(Player player) {
        double modifier = this.getConfig().getDouble("carry-speed-modifier", -0.08);
        if (modifier < 0.0) {
            int amplifier = Math.max(0, Math.min(4, (int)Math.ceil(Math.abs(modifier) * 12.0) - 1));
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, Integer.MAX_VALUE, amplifier, true, false, true));
        }
    }

    private void removeCarryEffect(Player player) {
        player.removePotionEffect(PotionEffectType.SLOWNESS);
    }

    private void removeCarryEffectIfDone(Player player) {
        if (!this.hasCarrier(player)) {
            this.removeCarryEffect(player);
        }
    }

    private String serializeVillagerData(Villager villager) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("profession", villager.getProfession().name());
        yaml.set("type", villager.getVillagerType().name());
        yaml.set("level", villager.getVillagerLevel());
        yaml.set("experience", villager.getVillagerExperience());
        yaml.set("restocks-today", optionalInt(villager, "getRestocksToday", 0));
        return yaml.saveToString();
    }

    private void restoreVillagerData(Villager villager, String serialized) {
        if (serialized == null || serialized.isBlank()) {
            return;
        }
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.loadFromString(serialized);
            String type = yaml.getString("type");
            String profession = yaml.getString("profession");
            if (type != null) {
                villager.setVillagerType(Villager.Type.valueOf((String)type));
            }
            if (profession != null) {
                villager.setProfession(Villager.Profession.valueOf((String)profession));
            }
            villager.setVillagerLevel(yaml.getInt("level", villager.getVillagerLevel()));
            villager.setVillagerExperience(yaml.getInt("experience", villager.getVillagerExperience()));
            invokeOptional(villager, "setRestocksToday", yaml.getInt("restocks-today", 0));
        }
        catch (Exception exception) {
            this.getLogger().log(Level.WARNING, "Could not restore villager data.", exception);
        }
    }

    private String serializeRecipes(List<MerchantRecipe> recipes) {
        YamlConfiguration yaml = new YamlConfiguration();
        yaml.set("count", recipes.size());
        for (int index = 0; index < recipes.size(); ++index) {
            MerchantRecipe recipe = recipes.get(index);
            String path = "recipes." + index + ".";
            yaml.set(path + "result", recipe.getResult());
            yaml.set(path + "ingredients", recipe.getIngredients());
            yaml.set(path + "uses", recipe.getUses());
            yaml.set(path + "max-uses", recipe.getMaxUses());
            yaml.set(path + "experience-reward", recipe.hasExperienceReward());
            yaml.set(path + "villager-experience", recipe.getVillagerExperience());
            yaml.set(path + "price-multiplier", Float.valueOf(recipe.getPriceMultiplier()));
            yaml.set(path + "demand", recipe.getDemand());
            yaml.set(path + "special-price", recipe.getSpecialPrice());
            yaml.set(path + "ignore-discounts", optionalBoolean(recipe, "shouldIgnoreDiscounts", false));
        }
        return yaml.saveToString();
    }

    private List<MerchantRecipe> deserializeRecipes(String serialized) {
        if (serialized == null || serialized.isBlank()) {
            return Collections.emptyList();
        }
        YamlConfiguration yaml = new YamlConfiguration();
        try {
            yaml.loadFromString(serialized);
            ArrayList<MerchantRecipe> recipes = new ArrayList<MerchantRecipe>();
            int count = yaml.getInt("count", 0);
            for (int index = 0; index < count; ++index) {
                String path = "recipes." + index + ".";
                ItemStack result = yaml.getItemStack(path + "result");
                if (result == null || result.getType().isAir()) continue;
                MerchantRecipe recipe = new MerchantRecipe(result, yaml.getInt(path + "uses", 0), yaml.getInt(path + "max-uses", 999999), yaml.getBoolean(path + "experience-reward", true), yaml.getInt(path + "villager-experience", 0), (float)yaml.getDouble(path + "price-multiplier", 0.05), yaml.getInt(path + "demand", 0), yaml.getInt(path + "special-price", 0));
                invokeOptional(recipe, "setIgnoreDiscounts", yaml.getBoolean(path + "ignore-discounts", false));
                List ingredients = yaml.getList(path + "ingredients", Collections.emptyList());
                for (Object ingredient : ingredients) {
                    if (!(ingredient instanceof ItemStack)) continue;
                    ItemStack item = (ItemStack)ingredient;
                    recipe.addIngredient(item);
                }
                recipes.add(recipe);
            }
            return recipes;
        }
        catch (Exception exception) {
            this.getLogger().log(Level.WARNING, "Could not restore villager trades.", exception);
            return Collections.emptyList();
        }
    }

    /*
     * Enabled aggressive exception aggregation
     */
    private static int optionalInt(Object target, String methodName, int fallback) {
        try { return ((Number)target.getClass().getMethod(methodName).invoke(target)).intValue(); }
        catch (ReflectiveOperationException | ClassCastException ignored) { return fallback; }
    }

    private static boolean optionalBoolean(Object target, String methodName, boolean fallback) {
        try { return (Boolean)target.getClass().getMethod(methodName).invoke(target); }
        catch (ReflectiveOperationException | ClassCastException ignored) { return fallback; }
    }

    private static void invokeOptional(Object target, String methodName, Object argument) {
        try { target.getClass().getMethod(methodName, argument.getClass()).invoke(target, argument); }
        catch (ReflectiveOperationException ignored) { }
    }

    private String serializeInventory(Inventory inventory) {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream();){
            String string;
            try (BukkitObjectOutputStream data = new BukkitObjectOutputStream((OutputStream)output);){
                data.writeInt(inventory.getSize());
                for (ItemStack item : inventory.getContents()) {
                    data.writeObject(item);
                }
                string = Base64.getEncoder().encodeToString(output.toByteArray());
            }
            return string;
        }
        catch (IOException exception) {
            this.getLogger().log(Level.WARNING, "Could not serialize carried block inventory.", exception);
            return "";
        }
    }

    private boolean restoreInventory(Inventory inventory, String serialized) {
        if (serialized.isBlank()) {
            return false;
        }
        try (ByteArrayInputStream input = new ByteArrayInputStream(Base64.getDecoder().decode(serialized));
             BukkitObjectInputStream data = new BukkitObjectInputStream((InputStream)input);){
            int size = data.readInt();
            if (size < 0 || size > inventory.getSize()) return false;
            ItemStack[] contents = new ItemStack[size];
            for (int slot = 0; slot < size; ++slot) {
                Object value = data.readObject();
                if (value != null && !(value instanceof ItemStack)) return false;
                contents[slot] = (ItemStack)value;
            }
            for (int slot = 0; slot < size; ++slot) inventory.setItem(slot, contents[slot]);
            return true;
        }
        catch (IOException | ClassNotFoundException exception) {
            this.getLogger().log(Level.WARNING, "Could not restore carried block inventory.", exception);
            return false;
        }
    }

    private String pretty(String key) {
        String lower = key.toLowerCase(Locale.ROOT).replace('_', ' ');
        StringBuilder builder = new StringBuilder(lower.length());
        boolean upper = true;
        for (char c : lower.toCharArray()) {
            builder.append(upper ? Character.toUpperCase(c) : c);
            upper = c == ' ';
        }
        return builder.toString();
    }

    private void tell(CommandSender sender, String key) {
        this.messages.send(sender, key);
        this.sounds.play(sender, key);
    }

    private void tell(CommandSender sender, String key, String placeholder, String value) {
        this.messages.send(sender, key, placeholder, value);
        this.sounds.play(sender, key);
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0 || args[0].equalsIgnoreCase("help")) {
            this.tell(sender, "help");
            return true;
        }
        switch (args[0].toLowerCase(Locale.ROOT)) {
            case "reload": {
                if (!sender.hasPermission("carryon.reload") && !sender.hasPermission("carryon.admin")) {
                    this.tell(sender, "no-permission");
                    return true;
                }
                this.reloadConfig();
                this.tell(sender, "reloaded");
                return true;
            }
            case "language": {
                if (!sender.hasPermission("carryon.language") && !sender.hasPermission("carryon.admin")) {
                    this.tell(sender, "no-permission");
                    return true;
                }
                if (args.length < 2 || !this.languageExists(args[1])) {
                    this.tell(sender, "language-usage");
                    return true;
                }
                this.getConfig().set("language", args[1].toLowerCase(Locale.ROOT));
                this.saveConfig();
                this.messages.load();
                this.tell(sender, "language-changed", "%language%", args[1]);
                return true;
            }
        }
        this.tell(sender, "unknown-subcommand");
        return true;
    }

    private boolean languageExists(String language) {
        return List.of("en_us", "de-eu", "fr_fr", "es_es").contains(language.toLowerCase(Locale.ROOT));
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return this.partial(args[0], List.of("help", "reload", "language"));
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("language")) {
            return this.partial(args[1], List.of("en_us", "de-eu", "fr_fr", "es_es"));
        }
        return Collections.emptyList();
    }

    private List<String> partial(String input, List<String> values) {
        String lower = input.toLowerCase(Locale.ROOT);
        return values.stream().filter(value -> value.toLowerCase(Locale.ROOT).startsWith(lower)).toList();
    }

    private static final class MessageService {
        private final CarryOnPlugin plugin;
        private FileConfiguration messages;
        private final MiniMessage miniMessage = MiniMessage.miniMessage();
        private final LegacyComponentSerializer legacy = LegacyComponentSerializer.legacyAmpersand();

        private MessageService(CarryOnPlugin plugin) {
            this.plugin = plugin;
        }

        private void load() {
            File langFile;
            String language = this.plugin.getConfig().getString("language", "en_us");
            if (language == null || !List.of("en_us", "de-eu", "fr_fr", "es_es").contains(language.toLowerCase(Locale.ROOT))) language = "en_us";
            File langDir = new File(this.plugin.getDataFolder(), "lang");
            if (!langDir.exists()) {
                langDir.mkdirs();
            }
            if (!(langFile = new File(langDir, language + ".yml")).exists()) {
                language = "en_us";
                langFile = new File(langDir, "en_us.yml");
            }
            this.messages = YamlConfiguration.loadConfiguration((File)langFile);
            InputStream defaultsStream = this.plugin.getResource("lang/" + language + ".yml");
            if (defaultsStream != null) {
                YamlConfiguration defaults = YamlConfiguration.loadConfiguration((Reader)new InputStreamReader(defaultsStream, StandardCharsets.UTF_8));
                this.messages.setDefaults((Configuration)defaults);
                this.messages.options().copyDefaults(true);
            }
        }

        private void send(CommandSender sender, String key) {
            sender.sendMessage(this.parse(this.messages.getString("prefix", "") + this.messages.getString(key, "<red>Missing message: " + key)));
        }

        private void send(CommandSender sender, String key, String placeholder, String value) {
            String message = this.messages.getString(key, "<red>Missing message: " + key).replace(placeholder, value);
            sender.sendMessage(this.parse(this.messages.getString("prefix", "") + message));
        }

        private String parse(String input) {
            Component component;
            if (this.plugin.getConfig().getString("message-format", "minimessage").equalsIgnoreCase("legacy")) {
                component = this.legacy.deserialize(input);
            } else {
                component = this.miniMessage.deserialize(input);
            }
            return LegacyComponentSerializer.legacySection().serialize(component);
        }

        private String color(String input) {
            return ChatColor.translateAlternateColorCodes((char)'&', (String)input);
        }
    }

    private static final class SoundService {
        private final CarryOnPlugin plugin;
        private FileConfiguration sounds;

        private SoundService(CarryOnPlugin plugin) {
            this.plugin = plugin;
        }

        private void load() {
            this.sounds = YamlConfiguration.loadConfiguration((File)new File(this.plugin.getDataFolder(), "sounds.yml"));
        }

        private void play(CommandSender sender, String key) {
            Sound sound;
            String section;
            if (!(sender instanceof Player)) {
                return;
            }
            Player player = (Player)sender;
            String string = section = this.sounds.isConfigurationSection(key) ? key : "default";
            if (!this.sounds.getBoolean(section + ".enabled", true)) {
                return;
            }
            String configured = this.sounds.getString(section + ".sound", this.sounds.getString("default.sound", "UI_BUTTON_CLICK"));
            try {
                sound = Sound.valueOf((String)configured);
            }
            catch (IllegalArgumentException exception) {
                sound = Sound.UI_BUTTON_CLICK;
            }
            float volume = (float)this.sounds.getDouble(section + ".volume", 0.55);
            float pitch = (float)this.sounds.getDouble(section + ".pitch", 1.15);
            player.playSound(player.getLocation(), sound, SoundCategory.MASTER, volume, pitch);
        }
    }
}

