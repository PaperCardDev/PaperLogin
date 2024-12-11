package cn.paper_card.paper_login;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.lang.reflect.*;
import java.util.LinkedList;
import java.util.List;

class SessionServiceReplacer {
    private final @NotNull PluginMain plugin;

    SessionServiceReplacer(@NotNull PluginMain plugin) {
        this.plugin = plugin;
    }

    private @NotNull Logger getLogger() {
        return this.plugin.getSLF4JLogger();
    }

    private static @Nullable Field findFirstNonStaticDeclaredFieldOfType
            (@NotNull Class<?> klass, @NotNull Class<?> type) {

        for (final Field declaredField : klass.getDeclaredFields()) {
            // 忽略静态属性
            if (Modifier.isStatic(declaredField.getModifiers()))
                continue;

            if (declaredField.getType() == type)
                return declaredField;
        }

        return null;
    }

    void doReplace() throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, NoSuchFieldException, InstantiationException {

        // minecraft session service
        final Class<?> classSessionService;
        classSessionService = Class.forName("com.mojang.authlib.minecraft.MinecraftSessionService");

        // 原生服务器类
        final Class<?> classMinecraftServer;
        classMinecraftServer = Class.forName("net.minecraft.server.MinecraftServer");

        // 原生服务汇总类
        final Class<?> classServices;
        classServices = Class.forName("net.minecraft.server.Services");

        // MinecraftServer 的 getServer方法
        final Method methodGetServer;
        methodGetServer = classMinecraftServer.getMethod("getServer");

        // 服务器对象
        final Object objectServer;
        objectServer = methodGetServer.invoke(null);

        // 服务汇总field
        final Field fieldServices; // net.minecraft.server.Services

        fieldServices = findFirstNonStaticDeclaredFieldOfType(classMinecraftServer, classServices);
        if (fieldServices == null) {
            throw new NoSuchFieldException("Type: " + classServices.getName());
        }
        fieldServices.setAccessible(true);

        // 服务汇总对象
        final Object objectServicesOld;
        objectServicesOld = fieldServices.get(objectServer);
        if (objectServicesOld == null) {
            throw new IllegalAccessException(fieldServices.getType().getName() + " is null!");
        }

        // SessionService
        final Field fieldServicesSessionService;
        fieldServicesSessionService = findFirstNonStaticDeclaredFieldOfType(classServices, classSessionService);
        if (fieldServicesSessionService == null) {
            throw new NoSuchFieldException(classServices.getName() + "has no field: " + classSessionService.getName());
        }
        fieldServicesSessionService.setAccessible(true);


        // Services上的所有属性
        final List<Field> fieldsOfServices = new LinkedList<>();
        for (final Field field : classServices.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers())) {
                field.setAccessible(true);
                fieldsOfServices.add(field);
                this.getLogger().info("Type: " + field.getType().getName());
            }
        }
        this.getLogger().info("Field count: " + fieldsOfServices.size());

        // 旧Services的成员变量
        int replaceCount = 0;
        final Object[] membersOfOldServices = new Object[fieldsOfServices.size()];
        for (int i = 0; i < membersOfOldServices.length; ++i) {
            final Field field = fieldsOfServices.get(i);
            membersOfOldServices[i] = field.get(objectServicesOld);

            if (field.getType() == classSessionService) {
                final Object oldSessionService = membersOfOldServices[i];

                // 动态代理
                final SessionServiceInvocationHandler invocationHandler = new SessionServiceInvocationHandler(oldSessionService,
                        this.plugin);

                final Object newSessionService = Proxy.newProxyInstance(classSessionService.getClassLoader(),
                        new Class[]{classSessionService},
                        invocationHandler
                );

                membersOfOldServices[i] = newSessionService;

                this.getLogger().info("Replace MinecraftSessionService.");

                ++replaceCount;
            }
        }

        if (replaceCount == 0) {
            throw new NoSuchFieldError("No such field: " + classSessionService.getName());
        }

        // 查找Services构造函数
        this.getLogger().info("Finding constructor of class Services...");
        Constructor<?> constructorServices = null;
        for (Constructor<?> constructor : classServices.getConstructors()) {
            final Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length == membersOfOldServices.length) {
                boolean sameParameters = true;
                for (int i = 0; i < parameterTypes.length; ++i) {
                    if (!parameterTypes[i].isAssignableFrom(membersOfOldServices[i].getClass())) {
                        sameParameters = false;
                        break;
                    }
                }

                if (sameParameters) {
                    constructorServices = constructor;
                    constructorServices.setAccessible(true);
                    break;
                }
            }
        }
        if (constructorServices == null) {
            throw new NoSuchMethodException("No constructor of class Services");
        }

        // 新的Services对象
        final Object objectNewServices;
        this.getLogger().info("Create new Services object...");
        objectNewServices = constructorServices.newInstance(membersOfOldServices);

        this.getLogger().info("Replace old Services object...");
        fieldServices.set(objectServer, objectNewServices);
    }
}
