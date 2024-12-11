package cn.paper_card.paper_login;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

class SessionServiceInvocationHandler implements InvocationHandler {
    private final @NotNull Object target;
    private final @NotNull PluginMain plugin;

    public SessionServiceInvocationHandler(@NotNull Object target, @NotNull PluginMain plugin) {
        this.target = target;
        this.plugin = plugin;
    }

    private @NotNull GameProfile parseProfile(@NotNull JsonObject json) throws Exception {

        final JsonElement idEle = json.get("id");
        final JsonElement nameEle = json.get("name");
        final JsonElement propertiesEle = json.get("properties");

        if (idEle == null) throw new Exception("返回JSON中缺失id！");
        if (nameEle == null) throw new Exception("返回JSON中缺失name！");

        final String id = idEle.getAsString();
        final String name = nameEle.getAsString();

        if (id.length() != 32) throw new Exception("返回JSON的中的id不正确：" + id);

        final String formatId = id.substring(0, 8) + "-" +
                id.substring(8, 12) + "-" +
                id.substring(12, 16) + "-" +
                id.substring(16, 20) + "-" +
                id.substring(20, 32);

        final UUID uuid;
        try {
            uuid = UUID.fromString(formatId);
        } catch (IllegalArgumentException e) {
            throw new Exception("返回JSON的中的id不正确：" + id, e);
        }

        final GameProfile profile = new GameProfile(uuid, name);

        if (propertiesEle != null) {
            final JsonArray array;
            try {
                array = propertiesEle.getAsJsonArray();
            } catch (IllegalStateException e) {
                throw new Exception("返回JSON的中的properties不正确，应该是一个数组！", e);
            }

            for (JsonElement jsonElement : array) {
                final JsonObject object;

                try {
                    object = jsonElement.getAsJsonObject();
                } catch (IllegalStateException e) {
                    throw new Exception("返回JSON中的properties数组中元素不是JsonObject类型！");
                }

                final JsonElement nameEle1 = object.get("name");
                final JsonElement valueEle1 = object.get("value");
                final JsonElement signatureEle = object.get("signature");

                final String pName = nameEle1.getAsString();
                final String pValue = valueEle1.getAsString();

                final String sign;
                if (signatureEle == null) {
                    sign = null;
                } else {
                    if (signatureEle.isJsonNull()) {
                        sign = null;
                    } else {
                        sign = signatureEle.getAsString();
                    }
                }

                profile.getProperties().put(pName, new Property(
                        pName, pValue, sign
                ));
            }
        }

        return profile;
    }

    @Nullable JsonObject reqPaperHasJoined(@NotNull String userName, @NotNull String serverId, @Nullable InetAddress ip) throws IOException {

        final HttpURLConnection connection = getHttpURLConnection(userName, serverId, ip);

        final int responseCode;
        final String message;

        try {
            responseCode = connection.getResponseCode();
            message = connection.getResponseMessage();
        } catch (IOException e) {
            connection.disconnect();
            throw e;
        }

        // 无效会话
        if (responseCode == 204) {
            connection.disconnect();
            return null;
        }

        if (responseCode != 200) {
            connection.disconnect();
            throw new IOException("http status: %s (%d)".formatted(message, responseCode));
        }

        final InputStream inputStream;

        try {
            inputStream = connection.getInputStream();
        } catch (IOException e) {
            connection.disconnect();
            throw e;
        }

        final InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);

        final JsonObject jsonObject;

        try {
            jsonObject = new Gson().fromJson(reader, JsonObject.class);
        } catch (Exception e) {
            try {
                reader.close();
            } catch (IOException ignored) {
            }

            try {
                inputStream.close();
            } catch (IOException ignored) {
            }

            connection.disconnect();

            throw new IOException(e);
        }

        IOException exception = null;

        try {
            reader.close();
        } catch (IOException e) {
            exception = e;
        }

        try {
            inputStream.close();
        } catch (IOException e) {
            exception = e;
        }

        connection.disconnect();

        if (exception != null) {
            throw exception;
        }

        return jsonObject;
    }

    @NotNull
    private static HttpURLConnection getHttpURLConnection(@NotNull String userName, @NotNull String serverId, @Nullable InetAddress ip) throws IOException {
        final StringBuilder sb = new StringBuilder();
        sb.append("https://api.paper-card.cn/ygg");
        sb.append("/sessionserver/session/minecraft/hasJoined");
        sb.append("?serverId=");
        sb.append(serverId);
        sb.append("&username=");
        sb.append(userName);

        if (ip != null) {
            sb.append("&ip=");
            sb.append(ip);
        }

        // 特有的
        sb.append("&errorProfile=true");

        final URL url = new URL(sb.toString());

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setConnectTimeout(4000);
        connection.setReadTimeout(8000);
        return connection;
    }

    private @NotNull Object myHasJoinedServer(@NotNull String username, @NotNull String serverId, @Nullable InetAddress ip) throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        final JsonObject json;

        try {
            json = this.reqPaperHasJoined(username, serverId, ip);
        } catch (IOException e) {
            return ErrorProfile.create("[ 无法连接到纸片身份验证务器 ]\n可能是网络问题或纸片服务器宕机", e).toProfileResult();
        }

        this.plugin.getSLF4JLogger().info("认证结果: " + json);

        if (json == null) {
            return ErrorProfile.create("[ 身份验证失败 ]\n请尝试重连、重启游戏、更换登录方式").toProfileResult();
        }

        final GameProfile gameProfile;

        try {
            gameProfile = this.parseProfile(json);
        } catch (Exception e) {
            return ErrorProfile.create("[ 身份验证: 解析Profile失败 ]", e).toProfileResult();
        }

        return gameProfile.toProfileResult();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        // 输出方法名称和参数
        final StringBuilder sb = new StringBuilder();

        sb.append(method.getName());
        sb.append("{");

        boolean first = true;

        for (Object arg : args) {
            if (first) {
                first = false;
            } else {
                sb.append(", ");
            }
            sb.append(arg);
        }
        sb.append("}");

        this.plugin.getSLF4JLogger().info(sb.toString());

        if (method.getName().equals("hasJoinedServer")) {

            final String username = (String) args[0];
            final String serverId = (String) args[1];
            final InetAddress ip = (InetAddress) args[2];

            try {
                return myHasJoinedServer(username, serverId, ip);
            } catch (Exception e) {
                this.plugin.getSLF4JLogger().error("myHasJoinedServer: ", e);
            }

            // 调用原来的
            return method.invoke(this.target, args);

        } else if (method.getName().equals("fetchProfile")) {

            final UUID uuid = (UUID) args[0];

            if (uuid.equals(ErrorProfile.ERROR_UUID)) return null;

            final Object result = method.invoke(this.target, args);

            this.plugin.getSLF4JLogger().info("调用结果: " + result);

            return result;
        } else {
            final Object result = method.invoke(this.target, args);

            this.plugin.getSLF4JLogger().info("调用结果: " + result);

            return result;
        }
    }
}