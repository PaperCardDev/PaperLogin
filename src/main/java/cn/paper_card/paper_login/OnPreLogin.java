package cn.paper_card.paper_login;

import cn.paper_card.client.api.PaperClientApi;
import cn.paper_card.client.api.PaperResponseError;
import cn.paper_card.disallow_all.DisallowAllApi;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.themoep.minedown.adventure.MineDown;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.UUID;

class OnPreLogin {
    private final @NotNull PluginMain plugin;


    OnPreLogin(@NotNull PluginMain plugin) {
        this.plugin = plugin;
    }

    static void appendPlayerAndTime(@NotNull TextComponent.Builder text, @NotNull String name, @NotNull UUID uuid) {
        text.appendNewline();
        text.append(Component.text("游戏角色：%s (%s)".formatted(name, uuid)).color(NamedTextColor.GRAY));

        text.appendNewline();
        text.append(Component.text("时间：%s".formatted(Util.dateFormat().format(System.currentTimeMillis())))
                .color(NamedTextColor.GRAY));
    }

    static void kickWhenException(@NotNull AsyncPlayerPreLoginEvent event, @NotNull Throwable e) {

        final TextComponent.Builder text = Component.text();

        text.append(Component.text("[ 系统错误 ]")
                .color(NamedTextColor.DARK_RED).decorate(TextDecoration.BOLD));

        for (Throwable t = e; t != null; t = t.getCause()) {
            text.appendNewline();
            text.append(Component.text(t.toString()).color(NamedTextColor.RED));
        }

        appendPlayerAndTime(text, event.getName(), event.getUniqueId());

        event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
        event.kickMessage(text.build());
    }

    void on(@NotNull AsyncPlayerPreLoginEvent event) {
        try {
            this.on0(event);
        } catch (Exception e) {
            this.plugin.getSLF4JLogger().error("error when login: ", e);
            kickWhenException(event, e);
        }
    }

    static @Nullable TextComponent checkProfileError(@NotNull PlayerProfile profile) {

        String title = null;
        String detail = null;

        for (ProfileProperty property : profile.getProperties()) {
            final String name = property.getName();
            if ("error".equals(name)) {
                final String error = property.getValue();
                final int i = error.indexOf('\n');
                if (i >= 0) {
                    title = error.substring(0, i);
                    if (i + 1 < error.length()) {
                        detail = error.substring(i + 1);
                    } else {
                        detail = "";
                    }
                } else {
                    title = "";
                    detail = error;
                }
                break;
            }
        }

        // 没有错误
        if (title == null) {
            return null;
        }

        return Component.text()
                .append(Component.text(title).color(NamedTextColor.DARK_RED))
                .appendNewline()
                .append(Component.text(detail).color(NamedTextColor.RED))
                .build();
    }

    private void checkError(@NotNull AsyncPlayerPreLoginEvent event) {

        final PlayerProfile profile = event.getPlayerProfile();

        final TextComponent kickMessage = checkProfileError(profile);

        // 没有错误
        if (kickMessage == null) {
            event.setLoginResult(AsyncPlayerPreLoginEvent.Result.ALLOWED);
            return;
        }

        event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);

        event.kickMessage(kickMessage);
    }

    private void on0(@NotNull AsyncPlayerPreLoginEvent event) {

        this.plugin.getSLF4JLogger().info("onPreLogin {name: %s, uuid: %s}".formatted(
                event.getName(), event.getUniqueId()
        ));

        // 检查错误
        this.checkError(event);

        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;

        // 检查无效UUID
        // TODO


        // 记录登录
        // TODO

        // 生成登录弹窗（检查白名单和封禁）
        final JsonElement respJson = this.reqLoginPopup(event);

        this.plugin.getSLF4JLogger().info(respJson.toString());

        final JsonObject jsonObject = respJson.getAsJsonObject();
        final JsonElement kickMessage = jsonObject.get("kick_message");

        /*
        {
            "ec": "ok",
            "em": "ok",
            "data": {
                "whitelist": null,
                "whitelist_check": true,
                "ban": null,
                "ban_check": true,
                "kick_message": [
                    "[ 纸片白名单 ]",
                    "您未申请白名单，请先申请白名单",
                    "您可以登录官网: paper-card.cn 自助申请白名单噢~",
                    "游戏角色: Paper99 (20554467-84cb-4773-a084-e3cfa867d481)"
                ]
            }
        }
         */

        if (kickMessage != null && !kickMessage.isJsonNull()) {
            final TextComponent.Builder builder = Component.text();
            int index = 0;
            for (final JsonElement line : kickMessage.getAsJsonArray()) {
                if (index > 0) {
                    builder.appendNewline();
                }

                final String lineStr = line.getAsString();
                builder.append(new MineDown(lineStr).toComponent());

                ++index;
            }

            event.kickMessage(builder.build());

            // TODO: 应该让服务端返回类型
            event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST);
            return;
        }


        // disallow all
        final DisallowAllApi disallowAllApi = this.plugin.getDisallowAllApi();
        if (disallowAllApi != null) {
            disallowAllApi.onPreLoginCheck(event);
            if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;
        }

        // 全部检查通过
        event.setLoginResult(AsyncPlayerPreLoginEvent.Result.ALLOWED);
    }

    @NotNull
    private JsonElement reqLoginPopup(@NotNull AsyncPlayerPreLoginEvent event) {

        final PaperClientApi clientApi = this.plugin.getPaperClientApi();
        if (clientApi == null) {
            throw new RuntimeException("PaperClientApi is null");
        }

        final JsonElement respJson;
        try {
            respJson = clientApi.getWithAuth("/popup/mc/login/" + event.getUniqueId() + "?name=" + event.getName());
        } catch (PaperResponseError | IOException e) {
            throw new RuntimeException(e);
        }

        if (respJson == null || respJson.isJsonNull()) {
            throw new RuntimeException("resp json is null");
        }
        return respJson;
    }
}
