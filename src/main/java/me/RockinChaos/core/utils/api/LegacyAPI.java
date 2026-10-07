/*
 * ChaosCore
 * Copyright (C) CraftationGaming <https://www.craftationgaming.com/>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package me.RockinChaos.core.utils.api;

import me.RockinChaos.core.Core;
import me.RockinChaos.core.handlers.ItemHandler;
import me.RockinChaos.core.utils.ReflectionUtils;
import me.RockinChaos.core.utils.ReflectionUtils.MinecraftMethod;
import me.RockinChaos.core.utils.SchedulerUtils;
import me.RockinChaos.core.utils.ServerUtils;
import me.RockinChaos.core.utils.StringUtils;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Skull;
import org.bukkit.block.banner.PatternType;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.AnvilInventory;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.MapMeta;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.map.MapView;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.math.BigInteger;
import java.util.*;

/**
 * Welcome to the magical land of make-believe.
 * These are Deprecated Legacy Methods and/or non-functioning methods
 * that exist to support legacy versions of Minecraft.
 */
@SuppressWarnings({"deprecation", "unused", "UnstableApiUsage"})
public class LegacyAPI {

    private static boolean legacyMaterial = false;

    /**
     * Updates the Players Inventory.
     *
     * @param player - The Player to have their Inventory updated.
     */
    public static void updateInventory(final @Nonnull Player player) {
        player.updateInventory();
    }

    /**
     * Gets the ItemStack in the Players Hand.
     *
     * @param player - The Player to have its ItemStack found.
     * @return The found ItemStack.
     */
    public static @Nonnull ItemStack getInHandItem(final @Nonnull Player player) {
        return player.getInventory().getItemInHand();
    }

    /**
     * Sets the ItemStack to the Players Hand.
     *
     * @param player - The Player to have the ItemStack given.
     * @param item   - The ItemStack to be set to the Players Hand.
     */
    public static void setInHandItem(final @Nonnull Player player, final @Nonnull ItemStack item) {
        player.setItemInHand(item);
    }

    /**
     * Creates a new ItemStack.
     *
     * @param material  - The Material to be set to the ItemStack.
     * @param count     - The ItemStack size.
     * @param dataValue - The Data Value to set to the ItemStack.
     * @return The new ItemStack.
     */
    public static @Nonnull ItemStack newItemStack(final @Nonnull Material material, final int count, final short dataValue) {
        return new ItemStack(material, count, dataValue);
    }

    /**
     * Gets the GameRule value for the World.
     *
     * @param world    - The world being referenced.
     * @param gamerule - The gamerule to locate.
     * @return The boolean value of the gamerule.
     */
    public static boolean hasGameRule(final @Nonnull World world, final @Nonnull String gamerule) {
        try {
            final String value = (String) ReflectionUtils.getMethod(world.getClass(), "getGameRuleValue", String.class).invoke(world, gamerule);
            return value.isEmpty() || Boolean.parseBoolean(value);
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Creates a new ShapedRecipe.
     *
     * @param item - The ItemStack to be crafted.
     * @return The new ShapedRecipe.
     */
    public static @Nonnull ShapedRecipe newShapedRecipe(final @Nonnull ItemStack item) {
        return new ShapedRecipe(item);
    }

    /**
     * Adds an ingredient to the ShapedRecipe with a Data Value.
     *
     * @param shapedRecipe - The shaped recipe reference.
     * @param character    - The identifier of the ingredient.
     * @param material     - The material of the ingredient.
     * @param itemData     - The Data Value of the ingredient.
     */
    public static void setIngredient(final @Nonnull ShapedRecipe shapedRecipe, final char character, final @Nonnull Material material, final byte itemData) {
        shapedRecipe.setIngredient(character, material, itemData);
    }

    /**
     * Matches the Material from its Bukkit Material and Data Value.
     *
     * @param typeID    - The ID of the Material to be fetched.
     * @param dataValue - The Data value to be matched.
     * @return The found Bukkit Material.
     */
    public static @Nonnull org.bukkit.Material getMaterial(final int typeID, final byte dataValue) {
        initializeLegacy();
        return Core.getCore().getPlugin().getServer().getUnsafe().fromLegacy(new org.bukkit.material.MaterialData(findMaterial(typeID), dataValue));
    }

    /**
     * Matches the Material from its Bukkit Material and Data Value.
     *
     * @param material  - The Material to be matched.
     * @param dataValue - The Data value to be matched.
     * @return The found Bukkit Material.
     */
    public static @Nonnull org.bukkit.Material getMaterial(final @Nullable Material material, final byte dataValue) {
        if (material == null) {
            return Material.AIR;
        }
        initializeLegacy();
        return Core.getCore().getPlugin().getServer().getUnsafe().fromLegacy(new org.bukkit.material.MaterialData(material, dataValue));
    }

    /**
     * Gets the Material from its corresponding ID.
     *
     * @param typeID - The ID of the Material to be fetched.
     * @return The found Bukkit Material.
     */
    public static @Nonnull org.bukkit.Material findMaterial(final int typeID) {
        final Material[] foundMaterial = new Material[1];
        EnumSet.allOf(Material.class).forEach(material -> {
            try {
                if (StringUtils.containsIgnoreCase(material.toString(), "LEGACY_") && material.getId() == typeID || !ServerUtils.hasUpdate("1_13") && material.getId() == typeID) {
                    try {
                        initializeLegacy();
                    } catch (Exception e) {
                        ServerUtils.sendSevereTrace(e);
                    }
                    foundMaterial[0] = material;
                }
            } catch (Exception ignored) {
            }
        });
        return foundMaterial[0];
    }

    /**
     * Sends an info/debug message if the server is running Minecraft 1.13+ and is attempting to call a Legacy material.
     */
    private static void initializeLegacy() {
        if (ServerUtils.hasUpdate("1_13") && !legacyMaterial) {
            legacyMaterial = true;
            ServerUtils.logInfo("Initializing Legacy Material Support ...");
            ServerUtils.logDebug("Your items.yml has one or more item(s) containing a numerical id and/or data values.");
            ServerUtils.logDebug("Minecraft 1.13 removed the use of these values, please change your items ids to reflect this change.");
            ServerUtils.logDebug("Your custom items will continue to function but the id set may not appear as expected.");
            ServerUtils.logDebug("If you believe this is a bug, please report it to the developer!");
            try {
                throw new Exception("Invalid usage of item id, this is not a bug!");
            } catch (Exception e) {
                ServerUtils.sendDebugTrace(e);
            }
        }
    }

    /**
     * Gets the ID of the specified Material.
     *
     * @param material - The Material to have its ID fetched.
     * @return The ID of the Material.
     */
    public static int getMaterialID(final @Nonnull Material material) {
        return material.getId();
    }

    /**
     * Gets the Durability from the ItemStack.
     *
     * @param item - The ItemStack to have its durability fetched.
     * @return The Durability of the ItemStack.
     */
    public static short getDurability(final @Nonnull ItemStack item) {
        return item.getDurability();
    }

    /**
     * Sets the Durability to the ItemStack.
     *
     * @param item       - The ItemStack to have its Durability set.
     * @param durability - The Durability to be set to the ItemStack.
     * @return the newly set Durability on the ItemStack.
     */
    public static @Nonnull ItemStack setDurability(final @Nonnull ItemStack item, final short durability) {
        item.setDurability(durability);
        return item;
    }

    /**
     * Gets the Enchantments String name.
     *
     * @param enchant - The Enchantment to have its String name fetched.
     * @return The Enchantments String name.
     */
    public static @Nonnull String getEnchantName(final @Nonnull Object enchant) {
        try {
            return (String) ReflectionUtils.getMethod(enchant.getClass(), "getName").invoke(enchant);
        } catch (Exception e) {
            ServerUtils.sendDebugTrace(e);
            throw new IllegalArgumentException("{LegacyAPI} An error has occurred while getting the Enchantment Name", e);
        }
    }

    /**
     * Gets the list of Enchantments.
     *
     * @return The full list of registered Enchants.
     */
    public static @Nonnull List<Enchantment> getEnchants() {
        try {
            return Arrays.asList((Enchantment[]) ReflectionUtils.getMethod(ReflectionUtils.getBukkitClass("enchantments.Enchantment"), "values").invoke(null));
        } catch (Exception e) {
            ServerUtils.sendDebugTrace(e);
            throw new IllegalArgumentException("{LegacyAPI} An error has occurred while getting Enchantment#values", e);
        }
    }

    /**
     * Gets the PotionEffectType instance from its String name.
     *
     * @param effect - The Effect to have its String name fetched.
     * @return The PotionEffectType instance.
     */
    public static @Nullable PotionEffectType getEffectByName(final @Nonnull String effect) {
        if (ServerUtils.hasUpdate("1_20_3")) {
            PotionEffectType type = null;
            try {
                type = Registry.EFFECT.get(Objects.requireNonNull(NamespacedKey.fromString(effect.toLowerCase())));
            } catch (NullPointerException ignored) {
            }
            if (type != null) {
                return type;
            } else {
                return PotionEffectType.getByName(effect.toUpperCase());
            }
        } else {
            return PotionEffectType.getByName(effect.toUpperCase());
        }
    }

    /**
     * Gets the list of PotionEffectType.
     *
     * @return The full list of registered Effects.
     */
    public static @Nonnull List<PotionEffectType> getEffects() {
        try {
            return Arrays.asList((PotionEffectType[]) ReflectionUtils.getMethod(ReflectionUtils.getBukkitClass("potion.PotionEffectType"), "values").invoke(null));
        } catch (Exception e) {
            ServerUtils.sendDebugTrace(e);
            throw new IllegalArgumentException("{LegacyAPI} An error has occurred while getting PotionEffectType#values", e);
        }
    }

    /**
     * Gets the list of Attribute.
     *
     * @return The full list of registered Attributes.
     */
    public static @Nonnull List<Attribute> getAttributes() {
        try {
            return Arrays.asList((Attribute[]) ReflectionUtils.getMethod(ReflectionUtils.getBukkitClass("attribute.Attribute"), "values").invoke(null));
        } catch (Exception e) {
            ServerUtils.sendDebugTrace(e);
            throw new IllegalArgumentException("{LegacyAPI} An error has occurred while getting Attribute#values", e);
        }
    }


    /**
     * Gets the list of Sounds.
     *
     * @return The full list of registered Sounds.
     */
    public static @Nonnull List<Sound> getSounds() {
        try {
            return Arrays.asList((Sound[]) ReflectionUtils.getMethod(ReflectionUtils.getBukkitClass("Sound"), "values").invoke(null));
        } catch (Exception e) {
            ServerUtils.sendDebugTrace(e);
            throw new IllegalArgumentException("{LegacyAPI} An error has occurred while getting Sound#values", e);
        }
    }

    /**
     * Gets the Enchantment from its String name.
     *
     * @param name - The String name of the Enchantment.
     * @return The found Enchantment.
     */
    public static @Nullable org.bukkit.enchantments.Enchantment getEnchant(final @Nonnull String name) {
        return org.bukkit.enchantments.Enchantment.getByName(name.toUpperCase());
    }

    /**
     * Gets the current SkullType of the Skull.
     *
     * @param skull - The Skull to fetch the SkullType.
     * @return The found SkullType.
     */
    public static @Nonnull SkullType getSkullType(final @Nonnull Skull skull) {
        return skull.getSkullType();
    }

    /**
     * Gets the current Skull Owner of the Skull.
     *
     * @param skull - The Skull to have its owner fetched.
     * @return The found Skull Owner.
     */
    public static @Nullable String getSkullOwner(final @Nonnull Skull skull) {
        return skull.getOwner();
    }

    /**
     * Gets the current Skull Owner of the SkullMeta.
     *
     * @param skullMeta - The SkullMeta to have its owner fetched.
     * @return The found Skull Owner.
     */
    public static @Nullable String getSkullOwner(final @Nonnull SkullMeta skullMeta) {
        return skullMeta.getOwner();
    }

    /**
     * Sets the owner to the SkullMeta.
     *
     * @param skullMeta - The SkullMeta to have its owner set.
     * @param owner     - The owner to be set to the SkullMeta.
     * @return The newly set SkullMeta.
     */
    public static @Nonnull ItemMeta setSkullOwner(final @Nonnull SkullMeta skullMeta, final @Nonnull String owner) {
        skullMeta.setOwner(owner);
        SchedulerUtils.run(() -> {
            if (!ServerUtils.hasUpdate("1_13")) {
                final Location loc = new Location(Bukkit.getWorlds().get(0), 200, 1, 200);
                final BlockState blockState = loc.getBlock().getState();
                try {
                    loc.getBlock().setType(Material.valueOf("SKULL"));
                    final Skull skull = (Skull) loc.getBlock().getState();
                    skull.setSkullType(SkullType.PLAYER);
                    skull.setOwner(owner);
                    skull.update();
                    final String texture = ItemHandler.getSkullTexture(skull);
                    if (texture != null && !texture.isEmpty()) {
                        ItemHandler.setSkullTexture(skullMeta, texture);
                    }
                } catch (Exception ignored) {
                }
                blockState.update(true);
            }
        });
        return skullMeta;
    }

    /**
     * Checks if setTarget exists for the Entity.
     *
     * @param current - The Entity to be checked.
     * @return If setTarget exists for the Entity.
     */
    public static boolean setTargetExists(final @Nonnull Entity current) {
        try {
            ReflectionUtils.getMethod(current.getClass(), "setTarget", LivingEntity.class);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Sets the Max Health of the Player.
     *
     * @param player    - The Player to have their max health set.
     * @param maxHealth - The Max Health to be set.
     */
    public static void setMaxHealth(final @Nonnull Player player, final double maxHealth) {
        player.setMaxHealth(maxHealth);
    }

    /**
     * Sets the Health of the Player.
     *
     * @param player - The Player to have their health set.
     * @param health - The Health to be set.
     */
    public static void setHealth(final @Nonnull Player player, final int health) {
        player.setHealth(health);
    }

    /**
     * Gets the Block Data from the specified Block.
     *
     * @param block - The Block being referenced.
     * @return The determined Block Data.
     */
    public static byte getBlockData(final @Nonnull Block block) {
        return block.getData();
    }

    /**
     * Gets the Bukkit Player from their String name.
     *
     * @param playerName - The String name of the Bukkit Player.
     * @return The found Player.
     */
    public static @Nullable Player getPlayer(final @Nonnull String playerName) {
        return Bukkit.getPlayer(playerName);
    }

    /**
     * Gets the Bukkit OfflinePlayer from their String name.
     *
     * @param playerName - The String name of the Bukkit OfflinePlayer.
     * @return The found OfflinePlayer.
     */
    public static @Nonnull OfflinePlayer getOfflinePlayer(final @Nonnull String playerName) {
        return Bukkit.getOfflinePlayer(playerName);
    }

    /**
     * Sets the Map ID to the MapMeta.
     *
     * @param meta  - The MapMeta to have its Map ID set.
     * @param mapId - The Map ID to be set to the item.
     */
    public static void setMapID(final @Nonnull MapMeta meta, final int mapId) {
        meta.setMapId(mapId);
    }

    /**
     * Gets the ID from the MapView.
     *
     * @param view - The MapView to have its ID fetched.
     * @return The ID of the MapView.
     */
    public static short getMapID(final @Nonnull org.bukkit.map.MapView view) {
        try {
            return (short) view.getId();
        } catch (Exception | NoSuchMethodError e) {
            try {
                return (short) ReflectionUtils.getMethod(ReflectionUtils.getBukkitClass("map.MapView"), "getId").invoke(view);
            } catch (Exception | NoSuchMethodError e2) {
                return 1;
            }
        }
    }

    /**
     * Gets the MapView from the specified ID.
     *
     * @param id - The ID of the MapView to be fetched.
     * @return The Fetched MapView.
     */
    public static @Nullable MapView getMapView(final int id) {
        try {
            return Core.getCore().getPlugin().getServer().getMap((short) id);
        } catch (Exception | NoSuchMethodError e) {
            try {
                return (org.bukkit.map.MapView) ReflectionUtils.getMethod(ReflectionUtils.getBukkitClass("Bukkit"), "getMap", short.class).invoke(null, (short) id);
            } catch (Exception | NoSuchMethodError e2) {
                return null;
            }
        }
    }

    /**
     * Creates a MapView for the main Server World.
     *
     * @return The new MapView.
     */
    public static @Nonnull MapView createMapView() {
        return Core.getCore().getPlugin().getServer().createMap(Core.getCore().getPlugin().getServer().getWorlds().get(0));
    }

    /**
     * Sets the armor value to the items attributes.
     *
     * @param tempItem         - The ItemStack to be updated.
     * @param attribIdentifier - The identifier of the attributes.
     * @param attributes       - A list of attributes to be set.
     * @return The new ItemStack with set Attributes.
     */
    public static @Nonnull ItemStack setAttributes(final @Nonnull ItemStack tempItem, final @Nonnull String attribIdentifier, final @Nonnull Map<String, Double> attributes) {
        if (!ServerUtils.hasUpdate("1_13") && !attributes.isEmpty()) {
            try {
                String slot;
                if (ItemHandler.getDesignatedSlot(tempItem.getType()).equalsIgnoreCase("noslot")) {
                    slot = "HAND";
                } else {
                    slot = ItemHandler.getDesignatedSlot(tempItem.getType()).toUpperCase();
                }
                final Class<?> craftItemStack = ReflectionUtils.getCraftBukkitClass("inventory.CraftItemStack");
                final Class<?> itemClass = ReflectionUtils.getMinecraftClass("ItemStack");
                final Class<?> baseClass = ReflectionUtils.getMinecraftClass("NBTBase");
                final Object nms = ReflectionUtils.getMethod(craftItemStack, "asNMSCopy", ItemStack.class).invoke(null, tempItem);
                final Object modifiers = ReflectionUtils.getConstructor(ReflectionUtils.getMinecraftClass("NBTTagList")).invoke();
                Object tag = ReflectionUtils.getMethod(itemClass, MinecraftMethod.getTag.getMethod()).invoke(nms);
                if (tag == null) {
                    tag = ReflectionUtils.getConstructor(ReflectionUtils.getMinecraftClass("NBTTagCompound")).invoke();
                }
                for (final String attribute : attributes.keySet()) {
                    final int uuid = new BigInteger((attribIdentifier + attribute).getBytes()).intValue();
                    final Object attrib = ReflectionUtils.getConstructor(ReflectionUtils.getMinecraftClass("NBTTagCompound")).invoke();
                    final double value = attributes.get(attribute);
                    StringBuilder name = new StringBuilder(attribute.toLowerCase().replaceFirst("_", "."));
                    if (name.toString().contains("_")) {
                        String[] nameSplit = name.toString().split("_");
                        name = new StringBuilder(nameSplit[0]);
                        nameSplit[0] = "";
                        for (String rename : nameSplit) {
                            name.append(org.apache.commons.lang.StringUtils.capitalize(rename));
                        }
                    }
                    ReflectionUtils.getMethod(attrib.getClass(), MinecraftMethod.setString.getMethod(), String.class, String.class).invoke(attrib, "AttributeName", name.toString());
                    ReflectionUtils.getMethod(attrib.getClass(), MinecraftMethod.setString.getMethod(), String.class, String.class).invoke(attrib, "Name", name.toString());
                    ReflectionUtils.getMethod(attrib.getClass(), MinecraftMethod.setString.getMethod(), String.class, String.class).invoke(attrib, "Slot", slot);
                    ReflectionUtils.getMethod(attrib.getClass(), MinecraftMethod.setDouble.getMethod(), String.class, double.class).invoke(attrib, "Amount", value);
                    ReflectionUtils.getMethod(attrib.getClass(), MinecraftMethod.setInt.getMethod(), String.class, int.class).invoke(attrib, "Operation", 0);
                    ReflectionUtils.getMethod(attrib.getClass(), MinecraftMethod.setInt.getMethod(), String.class, int.class).invoke(attrib, "UUIDLeast", uuid);
                    ReflectionUtils.getMethod(attrib.getClass(), MinecraftMethod.setInt.getMethod(), String.class, int.class).invoke(attrib, "UUIDMost", (uuid / 2));
                    ReflectionUtils.getMethod(modifiers.getClass(), MinecraftMethod.add.getMethod(), baseClass).invoke(modifiers, attrib);
                }
                ReflectionUtils.getMethod(tag.getClass(), MinecraftMethod.set.getMethod(), String.class, baseClass).invoke(tag, "AttributeModifiers", modifiers);
                return (ItemStack) ReflectionUtils.getCraftMirror(nms.getClass()).invoke(null, nms);
            } catch (Exception e) {
                ServerUtils.sendDebugTrace(e);
            }
        }
        return tempItem;
    }

    /**
     * Attempts to get a new AttributeModifier instance.
     *
     * @param uuid - The unique UUID to identify the AttributeModifier as.
     * @param attrib - The actual name of the Attribute.
     * @param value - The amount or strength of the Attribute.
     * @param slot - The Equipment slot of the Attribute.
     * @return The new AttributeModifier instance.
     */
    public static @Nonnull AttributeModifier getAttribute(final @Nonnull String uuid, final @Nonnull String attrib, final double value, final @Nonnull EquipmentSlot slot) {
        try {
            final ReflectionUtils.ConstructorInvoker constructor = ReflectionUtils.getConstructor(AttributeModifier.class, UUID.class, String.class, double.class, AttributeModifier.Operation.class, EquipmentSlot.class);
            return (AttributeModifier) constructor.invoke(UUID.nameUUIDFromBytes(uuid.getBytes()), attrib.toLowerCase().replace("_", "."), value, AttributeModifier.Operation.ADD_NUMBER, slot);
        } catch (Exception e) {
            ServerUtils.sendSevereTrace(e);
            throw new RuntimeException("{LegacyAPI} An error has occurred getting the attribute: " + attrib, e);
        }
    }

    /**
     * Attempts to set the Repair Cost for the AnvilInventory.
     *
     * @param inventory - The AnvilInventory instance.
     * @param cost      - The cost of the repair.
     */
    public static void setRepairCost(final AnvilInventory inventory, final int cost) {
        try {
            ReflectionUtils.getMethod(inventory.getClass(), "setRepairCost", int.class).invoke(inventory, cost);
        } catch (Exception e) {
            ServerUtils.logSevere("{LegacyAPI} An error has occurred while setting the repair cost!");
            ServerUtils.sendSevereTrace(e);
        }
    }

    /**
     * Color Encodes a String so that it is completely hidden in color codes,
     * this will be invisible to a normal eye and will not display any text.
     * Only to be used on server versions below 1.13, will not function on 1.13+.
     *
     * @param str - The String to be Color Encoded.
     * @return The Color Encoded String.
     */
    public static @Nonnull String colorEncode(final @Nonnull String str) {
        final StringBuilder hiddenData = new StringBuilder();
        for (char c : str.toCharArray()) {
            hiddenData.append(ChatColor.COLOR_CHAR).append(c);
        }
        return hiddenData.toString();
    }

    /**
     * Decodes a Color Encoded String.
     * Only to be used on server versions below 1.13, will not function on 1.13+.
     *
     * @param str - The String to be Color Decoded.
     * @return The Color Decoded String.
     */
    public static @Nonnull String colorDecode(final @Nonnull String str) {
        final String[] hiddenData = str.split("(?:\\w{2,}|\\d[0-9A-Fa-f])+");
        final StringBuilder returnData = new StringBuilder();
        final String[] d = hiddenData[hiddenData.length - 1].split(String.valueOf(ChatColor.COLOR_CHAR));
        for (int i = 1; i < d.length; i++) {
            returnData.append(d[i]);
        }
        return returnData.toString();
    }

    /**
     * Checks if the Sk89q Plugins are the Legacy version.
     *
     * @return If the plugins are Legacy.
     */
    public static boolean legacySk89q() {
        try {
            ReflectionUtils.getClass("com.sk89q.worldedit.Vector");
            return true;
        } catch (Exception ignored) {}
        return false;
    }

    /**
     * Sets the potion type as a potion data to the PotionMeta.
     *
     * @param tempMeta   - The PotionMeta having the potion data set to.
     * @param potionType - The potion type to be set.
     */
    public static void setPotionData(final @Nonnull PotionMeta tempMeta, final @Nonnull PotionType potionType) {
        ReflectionUtils.setBasePotionData(tempMeta, potionType);
    }

    /**
     * Sets the potion type as a potion data to the PotionMeta.
     *
     * @param tempMeta   - The PotionMeta having the potion data set to.
     * @param potionType - The potion type to be set.
     * @param upgraded   - If this is an upgraded potion type.
     * @param extended   - If this is an extended potion type.
     */
    public static void setPotionData(final @Nonnull PotionMeta tempMeta, final @Nonnull PotionType potionType, final boolean extended, final boolean upgraded) {
        ReflectionUtils.setBasePotionData(tempMeta, potionType, extended, upgraded);
    }

    /**
     * Gets the PatternType instance given the Pattern Name.
     *
     * @param patternName - The PatternType to be fetched.
     * @return the PatternType instance from the Pattern Name.
     */
    public static @Nonnull PatternType getPattern(final @Nonnull String patternName) {
        try {
            return (PatternType) ReflectionUtils.getMethod(PatternType.class, "valueOf", String.class).invoke(null, patternName.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("{LegacyAPI} An error has occurred while getting the PatternType: " + patternName, e);
        }
    }

    /**
     * Gets the Pattern Name of the given PatternType.
     *
     * @param pattern - The PatternType to have its name fetched.
     * @return the Pattern Name of the given PatternType.
     */
    public static @Nonnull String getPatternName(final @Nonnull Object pattern) {
        try {
            return (String) ReflectionUtils.getMethod(pattern.getClass(), "name").invoke(pattern);
        } catch (Exception e) {
            throw new IllegalArgumentException("{LegacyAPI} An error has occurred while getting the Pattern Name", e);
        }
    }

    /**
     * Gets the list of Patterns.
     *
     * @return The full list of registered Patterns.
     */
    public static @Nonnull List<PatternType> getPatterns() {
        try {
            return Arrays.asList((PatternType[]) ReflectionUtils.getMethod(ReflectionUtils.getBukkitClass("block.banner.PatternType"), "values").invoke(null));
        } catch (Exception e) {
            throw new IllegalArgumentException("{LegacyAPI} An error has occurred while getting Pattern#values", e);
        }
    }

    /**
     * Gets the Potion Name from its Type.
     *
     * @param type - The potion name to be fetched.
     * @return the potion effect type name.
     */
    public static @Nonnull String getEffectName(final @Nonnull Object type) {
        try {
            return (String) ReflectionUtils.getMethod(type.getClass(), "getName").invoke(type);
        } catch (Exception e) {
            throw new IllegalArgumentException("{LegacyAPI} An error has occurred while getting the PotionEffectType Name", e);
        }
    }

    /**
     * Gets the Attribute Name.
     *
     * @param attribute - The attribute name to be fetched.
     * @return the attribute name.
     */
    public static @Nonnull String getAttributeName(final @Nonnull Object attribute) {
        try {
            return (String) ReflectionUtils.getMethod(attribute.getClass(), "name").invoke(attribute);
        } catch (Exception e) {
            throw new IllegalArgumentException("{LegacyAPI} An error has occurred while getting the Attribute Name", e);
        }
    }

    /**
     * Gets the Attribute instance given the Attribute Name.
     *
     * @param attributeName - The Attribute to be fetched.
     * @return the Attribute instance from the Attribute Name.
     */
    public static @Nonnull Attribute getAttribute(final @Nonnull String attributeName) {
        try {
            return (Attribute) ReflectionUtils.getMethod(Attribute.class, "valueOf", String.class).invoke(null, attributeName.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("{LegacyAPI} An error has occurred while getting the Attribute: " + attributeName, e);
        }
    }

    /**
     * Gets the Sound Name.
     *
     * @param sound - The Sound name to be fetched.
     * @return the sound name.
     */
    public static @Nonnull String getSoundName(final @Nonnull Object sound) {
        try {
            return (String) ReflectionUtils.getMethod(sound.getClass(), "name").invoke(sound);
        } catch (Exception e) {
            throw new IllegalArgumentException("{LegacyAPI} An error has occurred while getting the Sound Name", e);
        }
    }

    /**
     * Gets the Sound instance given the Sound Name.
     *
     * @param soundName - The Sound to be fetched.
     * @return the Sound instance from the Sound Name.
     */
    public static @Nonnull Sound getSound(final @Nonnull String soundName) {
        try {
            return (Sound) ReflectionUtils.getMethod(Sound.class, "valueOf", String.class).invoke(null, soundName.toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("{LegacyAPI} An error has occurred while getting the Sound: " + soundName, e);
        }
    }

    /**
     * Gets the Enchantment instance from its String name.
     *
     * @param name - The enchantment name to be fetched.
     * @return the Enchantment instance.
     */
    public static @Nullable Enchantment getEnchantByKey(final @Nonnull String name) {
        if (ServerUtils.hasUpdate("1_20_3")) {
            final Enchantment enchant = Registry.ENCHANTMENT.get(org.bukkit.NamespacedKey.minecraft(name.toLowerCase()));
            if (enchant != null) {
                return enchant;
            } else {
                return Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(name.toLowerCase()));
            }
        } else {
            return Enchantment.getByKey(org.bukkit.NamespacedKey.minecraft(name.toLowerCase()));
        }
    }

    /**
     * Gets the rename text of the anvil item.
     *
     * @param event - The event being referenced.
     * @return The rename text of the anvil item.
     */
    public static @Nonnull String getRenameText(final @Nonnull Object event) {
        try {
            final Object inventory = ReflectionUtils.getMethod(event.getClass(), "getInventory").invoke(event);
            return (String) ReflectionUtils.getMethod(inventory.getClass(), "getRenameText").invoke(inventory);
        } catch (Exception e) {
            ServerUtils.logSevere("{LegacyAPI} An error has occurred with InventoryEvent#getRenameText!");
            return "";
        }
    }

    /**
     * Gets the Data Value from the ItemStack.
     *
     * @param item - The ItemStack to have its Data Value fetched.
     * @return The data value as an Integer.
     */
    public static int getDataValue(final @Nonnull ItemStack item) {
        return Objects.requireNonNull(item.getData()).getData();
    }

    /**
     * Gets the Data Value for the corresponding Material.
     *
     * @param material - The Material to have its data value fetched.
     * @return The Data Value.
     */
    public static int getDataValue(final @Nonnull Material material) {
        if (material == Material.STONE) {
            return 6;
        } else if (StringUtils.containsIgnoreCase(material.toString(), "DIRT")) {
            return 2;
        } else if (material.toString().equalsIgnoreCase("WOOD")) {
            return 5;
        } else if (material.toString().equalsIgnoreCase("LOG")) {
            return 3;
        } else if (StringUtils.containsIgnoreCase(material.toString(), "SAPLING")) {
            return 5;
        } else if (material.toString().equalsIgnoreCase("SAND")) {
            return 1;
        } else if (material.toString().equalsIgnoreCase("LEAVES")) {
            return 3;
        } else if (StringUtils.containsIgnoreCase(material.toString(), "SPONGE")) {
            return 1;
        } else if (StringUtils.containsIgnoreCase(material.toString(), "SANDSTONE") && !StringUtils.containsIgnoreCase(material.toString(), "STAIRS")) {
            return 2;
        } else if (StringUtils.containsIgnoreCase(material.toString(), "LONG_GRASS")) {
            return 2;
        } else if (StringUtils.containsIgnoreCase(material.toString(), "RED_ROSE")) {
            return 8;
        } else if (StringUtils.containsIgnoreCase(material.toString(), "WOOD_STEP")) {
            return 5;
        } else if (StringUtils.containsIgnoreCase(material.toString(), "STEP")) {
            return 7;
        } else if (StringUtils.containsIgnoreCase(material.toString(), "STAINED_GLASS")) {
            return 15;
        } else if (StringUtils.containsIgnoreCase(material.toString(), "MONSTER_EGGS")) {
            return 5;
        } else if (StringUtils.containsIgnoreCase(material.toString(), "SMOOTH_BRICK")) {
            return 3;
        } else if (StringUtils.containsIgnoreCase(material.toString(), "COBBLE_WALL")) {
            return 1;
        } else if (StringUtils.containsIgnoreCase(material.toString(), "QUARTZ_BLOCK")) {
            return 2;
        } else if (StringUtils.containsIgnoreCase(material.toString(), "STAINED_CLAY")) {
            return 15;
        } else if (StringUtils.containsIgnoreCase(material.toString(), "LOG_2")) {
            return 1;
        } else if (StringUtils.containsIgnoreCase(material.toString(), "LEAVES_2")) {
            return 1;
        } else if (material.toString().equalsIgnoreCase("PRISMARINE")) {
            return 2;
        } else if (StringUtils.containsIgnoreCase(material.toString(), "CARPET")) {
            return 15;
        } else if (StringUtils.containsIgnoreCase(material.toString(), "DOUBLE_PLANT")) {
            return 5;
        } else if (StringUtils.containsIgnoreCase(material.toString(), "RED_SANDSTONE")) {
            return 2;
        } else if (StringUtils.containsIgnoreCase(material.toString(), "GOLDEN_APPLE")) {
            return 1;
        } else if (StringUtils.containsIgnoreCase(material.toString(), "RAW_FISH")) {
            return 3;
        } else if (StringUtils.containsIgnoreCase(material.toString(), "COOKED_FISHED")) {
            return 1;
        } else if (StringUtils.containsIgnoreCase(material.toString(), "INK_SAC")) {
            return 15;
        } else if (StringUtils.containsIgnoreCase(material.toString(), "SKULL_ITEM") && ServerUtils.hasUpdate("1_9")) {
            return 5;
        } else if (StringUtils.containsIgnoreCase(material.toString(), "SKULL_ITEM")) {
            return 4;
        } else if (StringUtils.containsIgnoreCase(material.toString(), "CONCRETE")) {
            return 15;
        } else if (StringUtils.containsIgnoreCase(material.toString(), "WOOL")) {
            return 15;
        }
        return 0;
    }
}