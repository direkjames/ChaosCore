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
package me.RockinChaos.core.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A utility class that simplifies reflection in Bukkit plugins.
 */
@SuppressWarnings({"unused"})
public class ReflectionUtils {
    private static final Map<String, Class<?>> CLASS_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Field> FIELD_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Constructor<?>> CONSTRUCTOR_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, FieldAccessor<?>> FIELD_ACCESSOR_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, MethodInvoker> METHOD_INVOKER_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Class<?>> METHOD_RETURN_TYPE_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Field[]> DECLARED_FIELDS_CACHE = new ConcurrentHashMap<>();

    private static final String OBC_PREFIX = Bukkit.getServer().getClass().getPackage().getName();
    private static final String NMS_PREFIX = OBC_PREFIX.replace("org.bukkit.craftbukkit", "net.minecraft.server");
    private static final String MC_PREFIX = "net.minecraft";
    private static final String VERSION = OBC_PREFIX.replace("org.bukkit.craftbukkit", "").replace(".", "");
    private static final boolean MC_REMAPPED = ServerUtils.hasUpdate("1_17");
    private static final Pattern MATCH_VARIABLE = Pattern.compile("\\{([^}]+)}");
    private static final boolean MODERN_NBT = ServerUtils.hasUpdate("26") || !isServerRemapped();
    private static final boolean MC_DEOBFUSCATION = MODERN_NBT || isPaperObfuscation();

    /**
     * Retrieve a field accessor for a specific field type and name.
     *
     * @param target - the target type.
     * @param name   - the name of the field, or NULL to ignore.
     * @return The field accessor.
     */
    public static @Nonnull <T> FieldAccessor<T> getField(final @Nonnull Class<?> target, final @Nonnull String name) {
        return getField(target, name, null, 0);
    }

    /**
     * Retrieve a field accessor for a specific field type and name.
     *
     * @param target    - the target type.
     * @param name      - the name of the field, or NULL to ignore.
     * @param fieldType - a compatible field type.
     * @return The field accessor.
     */
    public static @Nonnull <T> FieldAccessor<T> getField(final @Nonnull Class<?> target, final @Nullable String name, final @Nonnull Class<T> fieldType) {
        return getField(target, name, fieldType, 0);
    }

    /**
     * Retrieve a field accessor for a specific field type and name.
     *
     * @param className - lookup name of the class, see {@link #getClass(String)}.
     * @param name      - the name of the field, or NULL to ignore.
     * @param fieldType - a compatible field type.
     * @return The field accessor.
     */
    public static @Nonnull <T> FieldAccessor<T> getField(final @Nonnull String className, final @Nonnull String name, final @Nonnull Class<T> fieldType) {
        return getField(getClass(className), name, fieldType, 0);
    }

    /**
     * Retrieve a field accessor for a specific field type and name.
     *
     * @param target    - the target type.
     * @param fieldType - a compatible field type.
     * @param index     - the number of compatible fields to skip.
     * @return The field accessor.
     */
    public static @Nonnull <T> FieldAccessor<T> getField(final @Nonnull Class<?> target, @Nonnull final Class<T> fieldType, final int index) {
        return getField(target, null, fieldType, index);
    }

    /**
     * Retrieve a field accessor for a specific field type and name.
     *
     * @param className - lookup name of the class, see {@link #getClass(String)}.
     * @param fieldType - a compatible field type.
     * @param index     - the number of compatible fields to skip.
     * @return The field accessor.
     */
    public static @Nonnull <T> FieldAccessor<T> getField(final @Nonnull String className, final @Nonnull Class<T> fieldType, final int index) {
        return getField(getClass(className), fieldType, index);
    }

    /**
     * Retrieve a field accessor for a specific field type and name.
     *
     * @param target    - the targeted class.
     * @param fieldType - a compatible field type.
     * @param index     - the number of compatible fields to skip.
     * @return The field accessor.
     */
    @SuppressWarnings("unchecked")
    private static @Nonnull <T> FieldAccessor<T> getField(final @Nonnull Class<?> target, final @Nullable String name, @Nullable Class<T> fieldType, int index) {
        final String cacheKey = target.getName() + "|" + name + "|" + (fieldType != null ? fieldType.getName() : "null") + "|" + index;
        FieldAccessor<?> cached = FIELD_ACCESSOR_CACHE.get(cacheKey);
        if (cached != null) {
            return (FieldAccessor<T>) cached;
        }
        for (final Field field : target.getDeclaredFields()) {
            if ((name == null || field.getName().equals(name)) && (fieldType == null || fieldType.isAssignableFrom(field.getType())) && index-- <= 0) {
                field.setAccessible(true);
                FIELD_CACHE.put(cacheKey, field);
                FieldAccessor<T> accessor = new FieldAccessor<T>() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public T get(Object target) {
                        try {
                            return (T) field.get(target);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException("Cannot access reflection.", e);
                        }
                    }
                    @Override
                    public void set(Object target, Object value) {
                        try {
                            field.set(target, value);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException("Cannot access reflection.", e);
                        }
                    }
                    @Override
                    public boolean hasField(Object target) {
                        return field.getDeclaringClass().isAssignableFrom(target.getClass());
                    }
                };
                FIELD_ACCESSOR_CACHE.put(cacheKey, accessor);
                return accessor;
            }
        }
        if (target.getSuperclass() != null) {
            return getField(target.getSuperclass(), name, fieldType, index);
        } else {
            throw new IllegalArgumentException("Cannot find field with type " + fieldType);
        }
    }

    /**
     * Retrieve a declared field for a class and name.
     *
     * @param clazz       - the target class.
     * @param fieldName   - the name of the field, or NULL to ignore.
     * @return The declared field.
     */
    public static @Nonnull Field getDeclaredField(final @Nonnull Class<?> clazz, final @Nonnull String fieldName) {
        final String cacheKey = clazz.getName() + "|declared|" + fieldName;
        Field cached = FIELD_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            FIELD_CACHE.put(cacheKey, field);
            return field;
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("Cannot find declared field: " + fieldName, e);
        }
    }

    /**
     * Retrieve declared fields for a class (cached).
     * All fields are pre-set to accessible.
     *
     * @param clazz - the target class.
     * @return The array of declared fields.
     */
    public static @Nonnull Field[] getDeclaredFields(final @Nonnull Class<?> clazz) {
        final String cacheKey = clazz.getName();
        final Field[] cached = DECLARED_FIELDS_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        final Field[] fields = clazz.getDeclaredFields();
        for (final Field field : fields) {
            field.setAccessible(true);
        }
        DECLARED_FIELDS_CACHE.put(cacheKey, fields);
        return fields;
    }

    /**
     * Gets the field value of the target object.
     *
     * @param object  - The object to have its field value retrieved.
     * @param name    - The String name of the Field to access.
     * @return The found Field Value as an Object instance.
     */
    public static @Nonnull Object getFieldValue(final @Nonnull Object object, final @Nonnull String name) {
        try {
            return getField(object.getClass(), name).get(object);
        } catch (Exception e) {
            throw new RuntimeException("Cannot get field value " + name, e);
        }
    }

    /**
     * Search for the first publicly and privately defined method of the given name and parameter count.
     *
     * @param className  - lookup name of the class, see {@link #getClass(String)}.
     * @param methodName - the method name, or NULL to skip.
     * @param params     - the expected parameters.
     * @return An object that invokes this specific method.
     * @throws IllegalStateException If we cannot find this method.
     */
    public static @Nonnull MethodInvoker getMethod(final @Nonnull String className, final @Nonnull String methodName, final @Nonnull Class<?>... params) {
        return getTypedMethod(getClass(className), methodName, null, params);
    }

    /**
     * Search for the first publicly and privately defined method of the given name and parameter count.
     *
     * @param clazz      - a class to start with.
     * @param methodName - the method name, or NULL to skip.
     * @param params     - the expected parameters.
     * @return An object that invokes this specific method.
     * @throws IllegalStateException If we cannot find this method.
     */
    public static @Nonnull MethodInvoker getMethod(final @Nonnull Class<?> clazz, final @Nonnull String methodName, final @Nonnull Class<?>... params) {
        return getTypedMethod(clazz, methodName, null, params);
    }

    /**
     * Get the return type of method (cached).
     *
     * @param clazz      - the class containing the method.
     * @param methodName - the method name.
     * @param params     - the expected parameters.
     * @return The return type of the method.
     * @throws IllegalStateException If we cannot find this method.
     */
    public static @Nonnull Class<?> getReturnType(final @Nonnull Class<?> clazz, final @Nonnull String methodName, final @Nonnull Class<?>... params) {
        final String cacheKey = clazz.getName() + "|returnType|" + methodName + "|" + Arrays.toString(params);
        Class<?> cached = METHOD_RETURN_TYPE_CACHE.get(cacheKey);
        if (cached != null) {
            return cached;
        }
        try {
            Method method = clazz.getMethod(methodName, params);
            Class<?> returnType = method.getReturnType();
            METHOD_RETURN_TYPE_CACHE.put(cacheKey, returnType);
            return returnType;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(String.format("Unable to find method %s (%s).", methodName, Arrays.asList(params)), e);
        }
    }

    /**
     * Search for the first publicly and privately defined method of the given name and parameter count.
     *
     * @param clazz      - a class to start with.
     * @param methodName - the method name, or NULL to skip.
     * @param returnType - the expected return type, or NULL to ignore.
     * @param params     - the expected parameters.
     * @return An object that invokes this specific method.
     * @throws IllegalStateException If we cannot find this method.
     */
    public static @Nonnull MethodInvoker getTypedMethod(final @Nonnull Class<?> clazz, final @Nonnull String methodName, final @Nullable Class<?> returnType, final @Nonnull Class<?>... params) {
        final String cacheKey = clazz.getName() + "|" + methodName + "|" + (returnType != null ? returnType.getName() : "null") + "|" + Arrays.toString(params);
        final MethodInvoker cached = METHOD_INVOKER_CACHE.get(cacheKey);
        if (cached != null) return cached;
        if (returnType == null) {
            try {
                final Method method = clazz.getMethod(methodName, params);
                method.setAccessible(true);
                final MethodInvoker invoker = (target, arguments) -> {
                    try {
                        return method.invoke(target, arguments);
                    } catch (Exception e) {
                        throw new RuntimeException("Cannot invoke method " + method, e);
                    }
                };
                METHOD_INVOKER_CACHE.put(cacheKey, invoker);
                return invoker;
            } catch (NoSuchMethodException ignored) {}
        }
        for (final Method method : clazz.getDeclaredMethods()) {
            if (method.getName().equals(methodName) && (returnType == null || method.getReturnType().equals(returnType)) && Arrays.equals(method.getParameterTypes(), params)) {
                method.setAccessible(true);
                final MethodInvoker invoker = (target, arguments) -> {
                    try {
                        return method.invoke(target, arguments);
                    } catch (Exception e) {
                        throw new RuntimeException("Cannot invoke method " + method, e);
                    }
                };
                METHOD_INVOKER_CACHE.put(cacheKey, invoker);
                return invoker;
            }
        }
        if (clazz.getSuperclass() != null) {
            return getTypedMethod(clazz.getSuperclass(), methodName, returnType, params);
        } else {
            throw new IllegalStateException(String.format("Unable to find method %s (%s).", methodName, Arrays.asList(params)));
        }
    }

    /**
     * Attempts to get the PlayerField.
     *
     * @param player - the player being referenced.
     * @param field  - the field being fetched.
     * @return The PlayerField.
     */
    public static @Nonnull Object getPlayerField(final @Nonnull Player player, final @Nonnull String field) {
        try {
            final Object craftPlayer = getEntity(player);
            return craftPlayer != null ? getField(craftPlayer.getClass(), field).get(craftPlayer) : new Object();
        } catch (Exception e) {
            throw new RuntimeException("Cannot invoke player field " + field, e);
        }
    }

    /**
     * Attempts to invoke a specific method given parameters.
     *
     * @param methodName - the method name, or NULL to skip.
     * @param params     - the expected parameters.
     * @return The newly created Object.
     */
    public static @Nonnull Object invokeMethod(final @Nonnull String methodName, final @Nonnull Object... params) {
        try {
            return getMethod((Class<?>)(params[0] instanceof Class<?> ? params[0] : params[0].getClass()), methodName).invoke(params[params.length - 1]);
        } catch (Exception e) {
            throw new RuntimeException("Cannot invoke method " + methodName, e);
        }
    }

    /**
     * Search for the first publicly and privately defined constructor of the given name and parameter count.
     *
     * @param className - lookup name of the class, see {@link #getClass(String)}.
     * @param params    - the expected parameters.
     * @return An object that invokes this constructor.
     * @throws IllegalStateException If we cannot find this method.
     */
    public static @Nonnull ConstructorInvoker getConstructor(final @Nonnull String className, final @Nonnull Class<?>... params) {
        return getConstructor(getClass(className), params);
    }

    /**
     * Search for the first publicly and privately defined constructor of the given name and parameter count.
     *
     * @param clazz  - a class to start with.
     * @param params - the expected parameters.
     * @return An object that invokes this constructor.
     * @throws IllegalStateException If we cannot find this method.
     */
    public static @Nonnull ConstructorInvoker getConstructor(final @Nonnull Class<?> clazz, final @Nonnull Class<?>... params) {
        final String cacheKey = clazz.getName() + "|" + Arrays.toString(params);
        Constructor<?> cached = CONSTRUCTOR_CACHE.get(cacheKey);
        if (cached != null) {
            return arguments -> {
                try {
                    return cached.newInstance(arguments);
                } catch (Exception e) {
                    throw new RuntimeException("Cannot invoke constructor " + cached, e);
                }
            };
        }
        try {
            final Constructor<?> constructor = clazz.getConstructor(params);
            constructor.setAccessible(true);
            CONSTRUCTOR_CACHE.put(cacheKey, constructor);
            return arguments -> {
                try {
                    return constructor.newInstance(arguments);
                } catch (Exception e) {
                    throw new RuntimeException("Cannot invoke constructor " + constructor, e);
                }
            };
        } catch (NoSuchMethodException ignored) {}
        for (final Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            if (Arrays.equals(constructor.getParameterTypes(), params)) {
                constructor.setAccessible(true);
                CONSTRUCTOR_CACHE.put(cacheKey, constructor);
                return arguments -> {
                    try {
                        return constructor.newInstance(arguments);
                    } catch (Exception e) {
                        throw new RuntimeException("Cannot invoke constructor " + constructor, e);
                    }
                };
            }
        }
        throw new IllegalStateException(String.format("Unable to find constructor for %s (%s).", clazz, Arrays.asList(params)));
    }

    /**
     * Retrieve a class from its full name, without knowing its type on compile time.
     * <p>
     * This is useful when looking up fields by a NMS or OBC type.
     * <p>
     *
     * @param lookupName - the class name with variables.
     * @return The class.
     */
    @SuppressWarnings("unchecked")
    public static @Nonnull Class<Object> getUntypedClass(final @Nonnull String lookupName) {
        return (Class<Object>) getClass(lookupName);
    }

    /**
     * Retrieve a class from its full name.
     * <p>
     * Strings enclosed with curly brackets - such as {TEXT} - will be replaced according to the following table:
     * <p>
     * <table border="1">
     * <tr>
     * <th>Variable</th>
     * <th>Content</th>
     * </tr>
     * <tr>
     * <td>{nms}</td>
     * <td>Actual package name of net.minecraft.server.VERSION</td>
     * </tr>
     * <tr>
     * <td>{obc}</td>
     * <td>Actual package name of org.bukkit.craftbukkit.VERSION</td>
     * </tr>
     * <tr>
     * <td>{version}</td>
     * <td>The current Minecraft package VERSION, if any.</td>
     * </tr>
     * </table>
     *
     * @param lookupName - the class name with variables.
     * @return The looked up class.
     * @throws IllegalArgumentException If a variable or class could not be found.
     */
    public static @Nonnull Class<?> getClass(final @Nonnull String lookupName) {
        return getCanonicalClass(expandVariables(lookupName));
    }

    /**
     * Retrieve a class in the net.minecraft.server.VERSION.* package.
     *
     * @param name - the name of the class, excluding the package.
     * @throws IllegalArgumentException If the class doesn't exist.
     */
    public static @Nonnull Class<?> getMinecraftClass(final @Nonnull String name) {
        if (MC_REMAPPED) {
            try {
                return getMinecraftTag(name);
            } catch (Exception e) {
                return getCanonicalClass(NMS_PREFIX + "." + name);
            }
        } else {
            return getCanonicalClass(NMS_PREFIX + "." + name);
        }
    }

    /**
     * Retrieve a class in the org.bukkit.craftbukkit.VERSION.* package.
     *
     * @param name - the name of the class, excluding the package.
     * @throws IllegalArgumentException If the class doesn't exist.
     */
    public static @Nonnull Class<?> getCraftBukkitClass(final @Nonnull String name) {
        return getCanonicalClass(OBC_PREFIX + "." + name);
    }

    /**
     * Gets the CraftItemStack method that wraps an NMS ItemStack as a Bukkit ItemStack.
     * Spigot names this method asCraftMirror, while Paper 26.3+ (and its forks such as Purpur)
     * renamed it to asBukkitMirror, so both names are tried.
     *
     * @param nmsItemClass - the NMS ItemStack class being mirrored.
     * @return An object that invokes the mirror method.
     * @throws IllegalStateException If neither method can be found.
     */
    public static @Nonnull MethodInvoker getCraftMirror(final @Nonnull Class<?> nmsItemClass) {
        final Class<?> craftItemStack = getCraftBukkitClass("inventory.CraftItemStack");
        try {
            return getMethod(craftItemStack, "asCraftMirror", nmsItemClass);
        } catch (IllegalStateException e) {
            final String aliasKey = craftItemStack.getName() + "|asCraftMirror|null|" + Arrays.toString(new Class<?>[]{nmsItemClass});
            final MethodInvoker invoker = getMethod(craftItemStack, "asBukkitMirror", nmsItemClass);
            METHOD_INVOKER_CACHE.put(aliasKey, invoker);
            return invoker;
        }
    }

    /**
     * Retrieve a class in the org.bukkit.* package.
     *
     * @param name - the name of the class, excluding the package.
     * @throws IllegalArgumentException If the class doesn't exist.
     */
    public static @Nonnull Class<?> getBukkitClass(final @Nonnull String name) {
        return getCanonicalClass("org.bukkit." + name);
    }

    /**
     * Retrieve a class by its canonical name, using a cache to avoid repeated lookups.
     *
     * @param canonicalName - the canonical name.
     * @return The class.
     * @throws IllegalArgumentException If the class doesn't exist.
     */
    public static @Nonnull Class<?> getCanonicalClass(final @Nonnull String canonicalName) {
        final Class<?> clazz = CLASS_CACHE.computeIfAbsent(canonicalName, key -> {
            try {
                return Class.forName(key);
            } catch (ClassNotFoundException e) {
                return ReflectionUtils.class;
            }
        });
        if (clazz != ReflectionUtils.class) {
            return clazz;
        } else {
            throw new IllegalArgumentException("Cannot find " + canonicalName);
        }
    }

    /**
     * Sets the potion type as a potion data to the PotionMeta.
     *
     * @param tempMeta   - The PotionMeta having the potion data set to.
     * @param potionType - The potion type to be set.
     */
    public static void setBasePotionData(final @Nonnull PotionMeta tempMeta, final @Nonnull PotionType potionType) {
        try {
            final Class<?> potionDataClass = getCanonicalClass("org.bukkit.potion.PotionData");
            final Object potionData = getConstructor(potionDataClass, getCanonicalClass("org.bukkit.potion.PotionType")).invoke(potionType);
            final Class<?> itemMetaClass = getCanonicalClass("org.bukkit.inventory.meta.PotionMeta");
            getMethod(itemMetaClass, "setBasePotionData", potionDataClass).invoke(tempMeta, potionData);
        } catch (Exception e) {
            ServerUtils.sendSevereTrace(e);
        }
    }

    /**
     * Sets the potion type as a potion data to the PotionMeta.
     *
     * @param tempMeta   - The PotionMeta having the potion data set to.
     * @param potionType - The potion type to be set.
     * @param upgraded   - If this is an upgraded potion type.
     * @param extended   - If this is an extended potion type.
     */
    public static void setBasePotionData(final @Nonnull PotionMeta tempMeta, final @Nonnull PotionType potionType, final boolean extended, final boolean upgraded) {
        try {
            final Class<?> potionDataClass = getCanonicalClass("org.bukkit.potion.PotionData");
            final Object potionData = getConstructor(potionDataClass, getCanonicalClass("org.bukkit.potion.PotionType"), boolean.class, boolean.class).invoke(potionType, extended, upgraded);
            final Class<?> itemMetaClass = getCanonicalClass("org.bukkit.inventory.meta.PotionMeta");
            getMethod(itemMetaClass, "setBasePotionData", potionDataClass).invoke(tempMeta, potionData);
        } catch (Exception e) {
            ServerUtils.sendSevereTrace(e);
        }
    }

    /**
     * Sends a PacketPlayOutSetSlot Packet to the specified player.
     *
     * @param player - The player receiving the packet.
     * @param item   - The ItemStack to be sent to the slot.
     * @param index  - The slot to have the item sent.
     */
    public static void sendPacketPlayOutSetSlot(final @Nonnull Player player, final @Nullable ItemStack item, int index, int windowId) {
        final Class<?> itemStack = getMinecraftClass("ItemStack");
        final Object nms = getMethod(getCraftBukkitClass("inventory.CraftItemStack"), "asNMSCopy", ItemStack.class).invoke(null, item);
        final Class<?> playOutSlot = getMinecraftClass("PacketPlayOutSetSlot");
        Object packet;
        if (MC_REMAPPED) {
            try {
                packet = getConstructor(playOutSlot, int.class, int.class, int.class, itemStack).invoke(windowId, 0, index, itemStack.cast(nms));
            } catch (Exception e) {
                packet = getConstructor(playOutSlot, int.class, int.class, itemStack).invoke(windowId, index, itemStack.cast(nms));
            }
        } else {
            packet = getConstructor(playOutSlot, int.class, int.class, itemStack).invoke(windowId, index, itemStack.cast(nms));
        }
        sendPacket(player, packet);
    }

    /**
     * Sends a Packet Object to the specified player.
     *
     * @param player - The player receiving the packet.
     * @param packet - The Packet Object being sent.
     */
    public static void sendPacket(final @Nonnull Player player, final @Nonnull Object packet) {
        final Object nmsPlayer = getEntity(player);
        if (nmsPlayer == null) return;
        if (ServerUtils.hasUpdate("1_21_7") && ServerUtils.isPaper) {
            final Object connection = getField(nmsPlayer.getClass(), "connection").get(nmsPlayer);
            final Class<?> packetClass = getMinecraftClass("Packet");
            getMethod(connection.getClass(), "send", packetClass).invoke(connection, packet);
        } else {
            final Object playerHandle = getField(nmsPlayer.getClass(), MinecraftField.PlayerConnection.getField()).get(nmsPlayer);
            final Class<?> packetClass = getMinecraftClass("Packet");
            getMethod(playerHandle.getClass(), MinecraftMethod.sendPacket.getMethod(), packetClass).invoke(playerHandle, packet);
        }
    }

    /**
     * Gets the ChatComponent Object from a String.
     *
     * @param content - The String to be converted to a ChatComponent.
     * @return The completed ChatComponent.
     */
    public static @Nullable Object literalChatComponent(final @Nonnull String content) {
        try {
            if (ServerUtils.hasUpdate("1_19")) {
                return getMethod(getMinecraftClass("IChatBaseComponent"), MinecraftMethod.literal.getMethod(), String.class).invoke(null, content);
            } else {
                return getConstructor(getMinecraftClass("ChatComponentText"), String.class).invoke(content);
            }
        } catch (Exception e) {
            ServerUtils.sendSevereTrace(e);
        }
        return null;
    }

    /**
     * Gets the ChatComponent Object from a JSON String.
     *
     * @param json - The JSON to be converted to a ChatComponent.
     * @return The completed ChatComponent.
     */
    public static @Nullable Object jsonChatComponent(final @Nonnull String json) {
        try {
            return getMethod(getMinecraftClass("IChatBaseComponent"), MinecraftMethod.fromJson.getMethod(), String.class).invoke(null, json);
        } catch (Exception e) {
            ServerUtils.sendSevereTrace(e);
        }
        return null;
    }

    /**
     * Turns a {@link Player} into an NMS one
     *
     * @param player The player to be converted
     * @return the NMS EntityPlayer
     */
    public static @Nullable Object getEntity(final @Nonnull Player player) {
        try {
            return invokeMethod("getHandle", player);
        } catch (Exception e) {
            ServerUtils.sendSevereTrace(e);
        }
        return null;
    }

    /**
     * Expand variables such as "{nms}" and "{obc}" to their corresponding packages.
     *
     * @param name - the full name of the class.
     * @return The expanded string.
     */
    private static @Nonnull String expandVariables(final @Nonnull String name) {
        final StringBuffer output = new StringBuffer();
        final Matcher matcher = MATCH_VARIABLE.matcher(name);
        while (matcher.find()) {
            final String variable = matcher.group(1);
            String replacement;
            if ("nms".equalsIgnoreCase(variable)) {
                if (MC_REMAPPED) {
                    try {
                        final String forClass = getMinecraftClass("PlayerConnection").getCanonicalName();
                        if (forClass != null) {
                            replacement = MC_PREFIX;
                        } else {
                            replacement = NMS_PREFIX;
                        }
                    } catch (Exception e) {
                        replacement = NMS_PREFIX;
                    }
                } else {
                    replacement = NMS_PREFIX;
                }
            } else if ("obc".equalsIgnoreCase(variable)) {
                replacement = OBC_PREFIX;
            } else if ("version".equalsIgnoreCase(variable)) {
                replacement = VERSION;
            } else {
                throw new IllegalArgumentException("Unknown variable: " + variable);
            }
            if (!replacement.isEmpty() && matcher.end() < name.length() && name.charAt(matcher.end()) != '.')
                replacement += ".";
            matcher.appendReplacement(output, Matcher.quoteReplacement(replacement));

        }
        matcher.appendTail(output);
        return output.toString();
    }

    /**
     * Gets the correct location of the searchable tag.
     *
     * @param name - The Tag being located.
     * @return The located searchable tag.
     */
    public static @Nonnull Class<?> getMinecraftTag(final @Nonnull String name) {
        for (MinecraftTags tag : MinecraftTags.values()) {
            if (tag.name().equalsIgnoreCase(name)) {
                return getCanonicalClass(MC_PREFIX + tag.tag + "." + tag.getClassName());
            }
        }
        return getCanonicalClass(NMS_PREFIX + "." + name);
    }

    /**
     * Checks if the server classes are obfuscated.
     *
     * @return If the server classes are obfuscated.
     */
    public static boolean isPaperObfuscation() {
        if (ServerUtils.hasUpdate("1_20_5")) {
            try {
                getField(getMinecraftClass("EntityHuman"), MinecraftField.DefaultContainer.getField());
                return false;
            } catch (Exception e) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the server classes are obfuscated.
     * Skipped if server is outside the 1.20.5 - 1.21.11 version range.
     *
     * @return If the server classes are obfuscated.
     */
    private static boolean isServerRemapped() {
        if (ServerUtils.hasUpdate("1_20_5") && !ServerUtils.hasUpdate("26")) {
            try {
                final Class<?> compoundTag = Class.forName("net.minecraft.nbt.CompoundTag");
                compoundTag.getMethod("putString", String.class, String.class);
                return false;
            } catch (ClassNotFoundException | NoSuchMethodException e) {
                return true;
            }
        }
        return true;
    }

    /**
     * Checks if the Server is running a remapped version of NBT and if its fields/methods are obfuscated.
     *
     * @return If the Server is remapped and obfuscated.
     */
    public static boolean obfuscated() {
        return MC_REMAPPED && !MC_DEOBFUSCATION;
    }

    /**
     * Searchable methods that no longer require NBT Reflections.
     */
    public enum MinecraftMethod {
        valueOf("valueOf", "valueOf", (ServerUtils.hasUpdate("1_18") ? "a" : "valueOf")),
        add("add", "add", (ServerUtils.hasUpdate("1_18") ? "c" : "add")),
        set("set", "set", (ServerUtils.hasUpdate("1_18") ? "a" : "set")),
        get("get", "get", "a"),
        getBase("get", "get", "c"),
        of("of", "of", "a"),
        setInt("setInt", "setInt", (ServerUtils.hasUpdate("1_18") ? "a" : "setInt")),
        getPage("a", "a", "a"),
        getTag("getTag", "getTag", (ServerUtils.hasUpdate("1_19") ? "v" : ServerUtils.hasUpdate("1_18_2") ? "t" : ServerUtils.hasUpdate("1_18") ? "s" : "getTag")),
        setTag("setTag", "setTag", (ServerUtils.hasUpdate("1_18") ? "c" : "setTag")),
        addTag("addTag", "addTag", (ServerUtils.hasUpdate("1_18") ? "d" : "addTag")),
        setCompound("setCompound", "put", "a"),
        getKeys((ServerUtils.hasUpdate("1_13") ? "getKeys": "c"), (ServerUtils.hasUpdate("1_21_7") ? "keySet" : "getAllKeys"), (ServerUtils.hasUpdate("1_20") ? "e" : "d")),
        getTypeId("getTypeId", "getId", (ServerUtils.hasUpdate("1_20") ? "b" : "a")),
        setString("setString", "putString", (ServerUtils.hasUpdate("1_18") ? "a" : "setString")),
        getString("getString", ServerUtils.hasUpdate("1_21_5") ? "getStringOr" : "getString", ServerUtils.hasUpdate("1_21_5") ? "b" : ServerUtils.hasUpdate("1_18") ? "l" : "getString"),
        setDouble("setDouble", "setDouble", (ServerUtils.hasUpdate("1_18") ? "a" : "setDouble")),
        fromJson("a", MODERN_NBT ? "fromJson" : "a", "a"),
        literal("b", MODERN_NBT ? "literal" : "a", "b"),
        setComponent("b", MODERN_NBT ? "set" : "b", "b"),
        readUtf((ServerUtils.hasUpdate("1_9") ? "e" : "c"), MODERN_NBT ? "readUtf" : "e", (ServerUtils.hasUpdate("1_9") ? "e" : "c")),
        put("set", "put", (ServerUtils.hasUpdate("1_18") ? "a" : "put")),
        build("build", "build", "a"),
        builder("builder", "builder", "a"),
        copyTag("copyTag", "copyTag", ServerUtils.hasUpdate("1_21_9") ? "b" : ServerUtils.hasUpdate("1_21_4") ? "d" : "c"),
        getServer("getServer", "getServer", "b"),
        registryAccess("registryAccess", "registryAccess", "bc"),
        At("at", "create", (ServerUtils.hasUpdate("1_18") ? "a" : "at")),
        AddSlotListener("addSlotListener", "initMenu", (ServerUtils.hasUpdate("1_18") ? "a" : "initMenu")),
        PlayerInventory("inventory", "getInventory", (ServerUtils.hasUpdate("1_21_11") ? "gK" : ServerUtils.hasUpdate("1_21_9") ? "gB"
                : ServerUtils.hasUpdate("1_21_6") ? "gs" : ServerUtils.hasUpdate("1_21_5") ? "gj" : ServerUtils.hasUpdate("1_21_2") ? "gi"
                : ServerUtils.hasUpdate("1_21") ? "fY" : ServerUtils.hasUpdate("1_20_5") ? "gc" : ServerUtils.hasUpdate("1_20_3") ? "fS"
                : ServerUtils.hasUpdate("1_20_2") ? "fR" : ServerUtils.hasUpdate("1_20") ? "fN" : ServerUtils.hasUpdate("1_19_3") ? "fJ"
                : ServerUtils.hasUpdate("1_19_3") ? "fE" : ServerUtils.hasUpdate("1_19") ? "fB" : ServerUtils.hasUpdate("1_18_2") ? "fr"
                : ServerUtils.hasUpdate("1_18") ? "fq" : "getInventory")),
        withReplacedPages("withReplacedPages", "withReplacedPages", "b"),
        getComponents("getComponents", "getComponents", "a"),
        applyComponentsAndValidate("applyComponentsAndValidate", "applyComponentsAndValidate", "a"),
        sendPacket("sendPacket", "sendPacket", (ServerUtils.hasUpdate("1_20_2") ? "b" : ServerUtils.hasUpdate("1_18") ? "a" : "sendPacket"));
        public final String legacy;
        public final String original;
        public final String remapped;

        MinecraftMethod(final String legacy, final String original, final String remapped) {
            this.legacy = legacy;
            this.original = original;
            this.remapped = remapped;
        }

        public @Nonnull String getMethod() {
            try {
                return (obfuscated() ? this.remapped : MC_REMAPPED ? this.original : this.legacy);
            } catch (Exception e) {
                return this.original;
            }
        }
    }

    /**
     * Searchable tags that no longer require NBT Reflections.
     */
    public enum MinecraftField {
        PlayerConnection("playerConnection", "connection", (ServerUtils.hasUpdate("1_21_6") ? "g" : ServerUtils.hasUpdate("1_21_2") ? "f" : ServerUtils.hasUpdate("1_20") ? "c" : "b")),
        ActiveContainer("activeContainer", "containerMenu", (ServerUtils.hasUpdate("1_21_11") ? "cn" : ServerUtils.hasUpdate("1_21_9") ? "cl" : ServerUtils.hasUpdate("1_21_6") ? "cn"
                : ServerUtils.hasUpdate("1_21_5") ? "bR" : ServerUtils.hasUpdate("1_21") ? "cd" : ServerUtils.hasUpdate("1_20_5") ? "cb"
                : ServerUtils.hasUpdate("1_20_2") ? "bS" : ServerUtils.hasUpdate("1_20") ? "bR" : ServerUtils.hasUpdate("1_19_3") ? "bP"
                : ServerUtils.hasUpdate("1_19") ? "bU" : ServerUtils.hasUpdate("1_18_2") ? "bV" : ServerUtils.hasUpdate("1_18") ? "bW" : "bV")),
        DefaultContainer("defaultContainer", "inventoryMenu", (ServerUtils.hasUpdate("1_21_11") ? "cm" : ServerUtils.hasUpdate("1_21_9") ? "ck"
                : ServerUtils.hasUpdate("1_21_6") ? "cm" : ServerUtils.hasUpdate("1_21_5") ? "bQ" : ServerUtils.hasUpdate("1_21") ? "cc"
                : ServerUtils.hasUpdate("1_20_5") ? "ca" : ServerUtils.hasUpdate("1_20_2") ? "bR" : ServerUtils.hasUpdate("1_20") ? "bQ"
                : ServerUtils.hasUpdate("1_19_3") ? "bO" : ServerUtils.hasUpdate("1_19") ? "bT" : ServerUtils.hasUpdate("1_18_2") ? "bU"
                : ServerUtils.hasUpdate("1_18") ? "bV" : "bU")),
        Anvil("ANVIL", "ANVIL", ServerUtils.hasUpdate("1_20_3") ? "i" : "h"),
        RenameText("renameText", "renameText", "v"),
        CustomName("CUSTOM_NAME", "CUSTOM_NAME", ServerUtils.hasUpdate("1_21_11") ? "h" : "g"),
        GetSlot("getSlot", "getSlot", (ServerUtils.hasUpdate("1_18_2") ? "b" : ServerUtils.hasUpdate("1_18") ? "a" : "getSlot")),
        HasItem("hasItem", "hasItem", (ServerUtils.hasUpdate("1_20_3") ? "h" : ServerUtils.hasUpdate("1_18") ? "f" : "hasItem")),
        GetItem("getItem", "getItem", (ServerUtils.hasUpdate("1_20_3") ? "g" : ServerUtils.hasUpdate("1_18") ? "e" : "getItem")),
        CustomData("CUSTOM_DATA", "CUSTOM_DATA", "b"),
        CanPlaceOn("CAN_PLACE_ON", "CAN_PLACE_ON", "m"),
        CanBreak("CAN_BREAK", "CAN_BREAK", "n"),
        TooltipDisplay("TOOLTIP_DISPLAY", "TOOLTIP_DISPLAY", "q"),
        TooltipStyle("TOOLTIP_STYLE", "TOOLTIP_STYLE", "G"),
        RepairCost("REPAIR_COST", "REPAIR_COST", "r"),
        UseCooldown("USE_COOLDOWN", "USE_COOLDOWN", "y"),
        Equippable("EQUIPPABLE", "EQUIPPABLE", "D"),
        Repairable("REPAIRABLE", "REPAIRABLE", "E"),
        windowId("windowId", ServerUtils.hasUpdate("1_21") ? "containerId" : "windowId", ServerUtils.hasUpdate("1_21_3") ? "l" : "j"),
        WrittenBookContent("WRITTEN_BOOK_CONTENT", "WRITTEN_BOOK_CONTENT", "J"),
        NetworkManager("networkManager", "networkManager", (ServerUtils.hasUpdate("1_19") ? "b" : "a")),
        PlayerAbilities("abilities", "abilities", (ServerUtils.hasUpdate("1_21_9") ? "cG" : ServerUtils.hasUpdate("1_21_8") ? "cT" : ServerUtils.hasUpdate("1_21_3") ? "i" : ServerUtils.hasUpdate("1_20_6") ? "cA" : ServerUtils.hasUpdate("1_20_4") ? "co" : ServerUtils.hasUpdate("1_20") ? "cn" : ServerUtils.hasUpdate("1_19_4") ? "cm" : ServerUtils.hasUpdate("1_19_1") ? "cr" : "cq")),
        Invulnerable("isInvulnerable", "invulnerable", "a");

        public final String legacy;
        public final String original;
        public final String remapped;

        MinecraftField(final String legacy, final String original, final String remapped) {
            this.legacy = legacy;
            this.original = original;
            this.remapped = remapped;
        }

        public @Nonnull String getField() {
            try {
                return (obfuscated() ? this.remapped : MC_REMAPPED ? this.original : this.legacy);
            } catch (Exception e) {
                return this.original;
            }
        }
    }

    /**
     * Searchable tags that no longer require NBT Reflections.
     */
    public enum MinecraftTags {
        NBTTagCompound(".nbt", "CompoundTag"),
        NBTTagList(".nbt", "ListTag"),
        NBTTagString(".nbt", "StringTag"),
        NBTBase(".nbt", "Tag"),
        ItemStack(".world.item"),
        Packet(".network.protocol"),
        PacketLoginInStart(".network.protocol.login", "ServerboundHelloPacket"),
        PacketPlayOutSetSlot(".network.protocol.game", "ClientboundContainerSetSlotPacket"),
        PacketPlayOutOpenWindow(".network.protocol.game", "ClientboundOpenScreenPacket"),
        PacketPlayOutCloseWindow(".network.protocol.game", "ClientboundContainerClosePacket"),
        PlayerConnection(".server.network", "ServerGamePacketListenerImpl"),
        EntityPlayer(".server.level", "ServerPlayer"),
        NetworkManager(".network", "Connection"),
        MinecraftServer(".server"),
        ServerConnection(".server.network", "ServerConnectionListener"),
        IChatBaseComponent(".network.chat", "Component"),
        ChatComponentText(".network.chat", "Component"),
        IChatBaseComponent$ChatSerializer(".network.chat"), // < 1.21.6, no longer exists.
        HolderLookup$a(".core"),
        PacketPlayOutChat(".network.protocol.game", "ClientboundPlayerChatPacket"),
        ClientboundSystemChatPacket(".network.protocol.game"),
        ChatMessageType(".network.chat", "ChatType"),
        ChatMessage(".server", "IChatBaseComponent"),
        HolderSet(".core"),
        BuiltInRegistries(".core.registries"),
        CriterionConditionBlock(".advancements.critereon", "BlockPredicate"),
        BlockPosition(".core", "BlockPos"),
        Block(".world.level.block"),
        Blocks(".world.level.block"),
        ContainerAnvil(".world.inventory", "AnvilMenu"),
        EntityHuman(".world.entity.player", "Player"),
        ICrafting(".world.inventory", "ContainerListener"),
        ContainerAccess(".world.inventory", "ContainerLevelAccess"),
        Containers(".world.inventory", "MenuType"),
        Container(".world.inventory", "AbstractContainerMenu"),
        ContainerProperty(".world.inventory"),
        World(".world.level", "Level"),
        PlayerInventory(".world.entity.player", "Inventory"),
        DataComponents(".core.component"),
        DataComponentPatch(".core.component"),
        DataComponentType(".core.component"),
        DataComponentGetter(".core.component"),
        DataComponentMap(".core.component"),
        MinecraftKey(".resources", "ResourceLocation"),
        CustomData(".world.item.component"),
        AdventureModePredicate(".world.item"),
        TooltipDisplay(".world.item.component"),
        UseCooldown(".world.item.component"),
        Equippable(".world.item.equipment"),
        Repairable(".world.item.enchantment"),
        BookContent(".world.item.component"),
        WrittenBookContent(".world.item.component");

        public final String tag;
        public final String modernName;

        MinecraftTags(final String tag) {
            this(tag, null);
        }

        MinecraftTags(final String tag, final String modernName) {
            this.tag = tag;
            this.modernName = modernName;
        }

        public String getClassName() {
            return MODERN_NBT && this.modernName != null ? this.modernName : this.name();
        }
    }

    /**
     * An interface for invoking a specific constructor.
     */
    public interface ConstructorInvoker {
        /**
         * Invoke a constructor for a specific class.
         *
         * @param arguments - the arguments to pass to the constructor.
         * @return The constructed object.
         */
        Object invoke(final @Nonnull Object... arguments);
    }

    /**
     * An interface for invoking a specific method.
     */
    public interface MethodInvoker {
        /**
         * Invoke a method on a specific target object.
         *
         * @param target    - the target object, or NULL for a method.
         * @param arguments - the arguments to pass to the method.
         * @return The return value, or NULL if is void.
         */
        Object invoke(final @Nullable Object target, final @Nonnull Object... arguments);
    }

    /**
     * An interface for retrieving the field content.
     *
     * @param <T> - field type.
     */
    public interface FieldAccessor<T> {
        /**
         * Retrieve the content of a field.
         *
         * @param target - the target object, or NULL for a field.
         * @return The value of the field.
         */
        T get(final Object target);

        /**
         * Set the content of a field.
         *
         * @param target - the target object, or NULL for a field.
         * @param value  - the new value of the field.
         */
        void set(final Object target, final Object value);

        /**
         * Determine if the given object has this field.
         *
         * @param target - the object to test.
         * @return TRUE if it does, FALSE otherwise.
         */
        boolean hasField(final Object target);
    }

    /**
     * Refreshes the caches.
     */
    public static void refresh() {
        CLASS_CACHE.clear();
        FIELD_CACHE.clear();
        CONSTRUCTOR_CACHE.clear();
        FIELD_ACCESSOR_CACHE.clear();
        METHOD_INVOKER_CACHE.clear();
        METHOD_RETURN_TYPE_CACHE.clear();
        DECLARED_FIELDS_CACHE.clear();
    }
}