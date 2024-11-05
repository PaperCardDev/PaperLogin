package cn.paper_card.paper_pre_login;

import cn.paper_card.client.api.PaperClientApi;
import cn.paper_card.client.api.PaperResponseError;
import cn.paper_card.disallow_all.DisallowAllApi;
import cn.paper_card.paper_card_auth.api.PaperCardAuthApi;
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

import java.io.IOException;
import java.net.InetAddress;
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
        text.append(Component.text("[ PaperCard | 系统错误 ]")
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
            this.plugin.getSLF4JLogger().error("error when login", e);
            kickWhenException(event, e);
        }
    }

    private void checkError(@NotNull AsyncPlayerPreLoginEvent event) {

        final PlayerProfile profile = event.getPlayerProfile();

        String title = null;
        String detail = null;

        for (ProfileProperty property : profile.getProperties()) {
            final String name = property.getName();
            if ("error-title".equals(name)) {
                title = property.getValue();
            } else if ("error-detail".equals(name)) {
                detail = property.getValue();
            }

            if (title != null && detail != null) break;
        }

        if (title == null && detail == null) {
            event.setLoginResult(AsyncPlayerPreLoginEvent.Result.ALLOWED);
            return;
        }

        event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
        event.kickMessage(Component.text()
                .append(Component.text(title != null ? title : "NO_TITLE").color(NamedTextColor.DARK_RED))
                .appendNewline()
                .append(Component.text(detail != null ? detail : "NO_DETAIL").color(NamedTextColor.RED))
                .build());
    }

    private void on0(@NotNull AsyncPlayerPreLoginEvent event) {

        this.plugin.getSLF4JLogger().info("onPreLogin {name: %s, uuid: %s}".formatted(
                event.getName(), event.getUniqueId()
        ));

        // 检查LittleSkin未绑定登录
        //        {
        //            LittleSkinLoginApi api = null;
        //            try {
//                    api = this.plugin.getLittleSkinLoginApi();
//            } catch (NoClassDefFoundError ignored) {
//            }
//            if (api != null) {
//                api.onPreLoginCheckNotBind(event);
//                if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;
//            }
//        }


        // 检查PaperSkin未绑定登录
//        {
//            PaperSkinLoginApi api = null;
//
//            try {
//                api = this.plugin.getPaperSkinLoginApi();
//            } catch (NoClassDefFoundError ignored) {
//            }
//
//            if (api != null) {
//                api.onPreLoginCheckNotBind(event);
//                if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;
//            }
//        }

        // 检查错误
        this.checkError(event);
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;

        // 检查无效UUID
        PaperCardAuthApi paperCardAuthApi = null;

        try {
            paperCardAuthApi = this.plugin.getPaperCardAuthApi();
        } catch (NoClassDefFoundError ignored) {
        }

        if (paperCardAuthApi != null) {
            paperCardAuthApi.onPreLoginCheckInvalid(event);
            if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;
        }


//        final GroupAccess mainGroup;
//        {
//            final QqGroupAccessApi api = this.plugin.getQqGroupAccessApi();
//            if (api != null) {
//                mainGroup = getMainGroupAccessOrNull(api);
//            } else {
//                mainGroup = null;
//            }
//        }

        // 检查离线正版验证
        if (paperCardAuthApi != null) {
            paperCardAuthApi.onPreLoginCheckMojangOffline(event, null);
            if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;
        }

        // 记录登录
        if (paperCardAuthApi != null) {
            final InetAddress address = event.getAddress();
            final String ip = address.getHostAddress();

            final PaperCardAuthApi api = paperCardAuthApi;

            this.plugin.getTaskScheduler().runTaskAsynchronously(() -> {
                if (ip == null) return;
                final String name = event.getName();

                final boolean added;
                try {
                    added = api.getPreLoginIpService().addOrUpdateByUuidAndIp(
                            event.getUniqueId(),
                            ip,
                            name,
                            System.currentTimeMillis()
                    );
                } catch (Exception e) {
                    this.plugin.getSLF4JLogger().warn("Fail to add login record", e);
                    return;
                }

                this.plugin.getLogger().info("%s了登录记录 {name: %s, ip: %s}".formatted(
                        added ? "添加" : "更新", name, ip
                ));
            });
        }

        // 生成登录弹窗（检查白名单和封禁）
        final JsonElement respJson = this.getLoginPopup(event);

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
    private JsonElement getLoginPopup(@NotNull AsyncPlayerPreLoginEvent event) {

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
