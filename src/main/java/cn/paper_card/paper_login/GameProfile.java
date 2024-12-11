package cn.paper_card.paper_login;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.UUID;

record Property(
        @NotNull String name,
        @NotNull String value,
        @Nullable String sign
) {

    @NotNull Object toAuthlib() throws ClassNotFoundException, InvocationTargetException, InstantiationException, IllegalAccessException, NoSuchMethodException {
        final Class<?> kl = Class.forName("com.mojang.authlib.properties.Property");
        final Constructor<?> constructor = kl.getConstructor(String.class, String.class, String.class);
        return constructor.newInstance(this.name, this.value, this.sign);
    }
}

class GameProfile {
    private final @NotNull UUID uuid;
    private final @NotNull String name;

    private final @NotNull HashMap<String, Property> properties;

    GameProfile(@NotNull UUID uuid, @NotNull String name) {
        this.uuid = uuid;
        this.name = name;
        this.properties = new HashMap<>();
    }

    @NotNull HashMap<String, Property> getProperties() {
        return this.properties;
    }

    @NotNull Object toAuthlib() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        final Class<?> klGameProfile = Class.forName("com.mojang.authlib.GameProfile");
        final Constructor<?> constructor = klGameProfile.getConstructor(UUID.class, String.class);
        final Object object = constructor.newInstance(this.uuid, this.name);

        final Method getProperties = klGameProfile.getMethod("getProperties");

        final Object propertyMap = getProperties.invoke(object);
        final Class<?> klPropertyMap = propertyMap.getClass();

        final Method put = klPropertyMap.getMethod("put", Object.class, Object.class);

        for (Property value : this.properties.values()) {
            put.invoke(propertyMap, value.name(), value.toAuthlib());
        }

        return object;
    }

    @NotNull Object toProfileResult() throws ClassNotFoundException, NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        final Class<?> klProfileResult = Class.forName("com.mojang.authlib.yggdrasil.ProfileResult");
        final Constructor<?> constructor = klProfileResult.getConstructor(Class.forName("com.mojang.authlib.GameProfile"));
        return constructor.newInstance(this.toAuthlib());
    }
}