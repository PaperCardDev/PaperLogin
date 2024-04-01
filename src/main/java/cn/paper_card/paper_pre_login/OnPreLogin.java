package cn.paper_card.paper_pre_login;

import cn.paper_card.disallow_all.DisallowAllApi;
import cn.paper_card.little_skin_login.api.LittleSkinLoginApi;
import cn.paper_card.paper_card_auth.api.PaperCardAuthApi;
import cn.paper_card.paper_card_ban.api.PaperCardBanApi;
import cn.paper_card.paper_login.api.PaperSkinLoginApi;
import cn.paper_card.paper_whitelist.api.PaperWhitelistApi;
import cn.paper_card.qq_group_access.api.GroupAccess;
import cn.paper_card.qq_group_access.api.QqGroupAccessApi;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    private @Nullable GroupAccess getMainGroupAccessOrNull(@NotNull QqGroupAccessApi groupAccessApi) {
        try {
            return groupAccessApi.createMainGroupAccess();
        } catch (Exception e) {
            this.plugin.getSLF4JLogger().warn(e.toString());
            return null;
        }
    }

    private boolean checkBanAndReport(@NotNull AsyncPlayerPreLoginEvent event, @NotNull UUID uuid, @NotNull String name) throws Exception {
        final PaperCardBanApi api = this.plugin.getPaperCardBanApi();

        if (api == null) return false;

        // 检查封禁
        final Object kickMessage = api.checkPlayerBanned(uuid, name);

        if (kickMessage != null) {
            event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_BANNED);
            event.kickMessage((Component) kickMessage);
            return true;
        }

        // 检查举报
        final Object kickMessage2 = api.checkPlayerReported(uuid, name);

        if (kickMessage2 != null) {
            event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_BANNED);
            event.kickMessage((Component) kickMessage2);
            return true;
        }

        return false;
    }

    void on(@NotNull AsyncPlayerPreLoginEvent event) {
        try {
            this.on0(event);
        } catch (Exception e) {
            this.plugin.getSLF4JLogger().error("error when login", e);
            kickWhenException(event, e);
        }
    }

    private void on0(@NotNull AsyncPlayerPreLoginEvent event) throws Exception {

        this.plugin.getSLF4JLogger().info("onPreLogin {name: %s, uuid: %s}".formatted(
                event.getName(), event.getUniqueId()
        ));

        // 检查LittleSkin未绑定登录
        {
            LittleSkinLoginApi api = null;
            try {
                api = this.plugin.getLittleSkinLoginApi();
            } catch (NoClassDefFoundError ignored) {
            }
            if (api != null) {
                api.onPreLoginCheckNotBind(event);
                if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;
            }
        }


        // 检查PaperSkin未绑定登录
        {
            PaperSkinLoginApi api = null;

            try {
                api = this.plugin.getPaperSkinLoginApi();
            } catch (NoClassDefFoundError ignored) {
            }

            if (api != null) {
                api.onPreLoginCheckNotBind(event);
                if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;
            }
        }

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


        final GroupAccess mainGroup;
        {
            final QqGroupAccessApi api = this.plugin.getQqGroupAccessApi();
            if (api != null) {
                mainGroup = getMainGroupAccessOrNull(api);
            } else {
                mainGroup = null;
            }
        }

        // 检查离线正版验证
        if (paperCardAuthApi != null) {
            paperCardAuthApi.onPreLoginCheckMojangOffline(event, mainGroup == null ? null : (qq, message) -> {
                try {
                    mainGroup.sendAtMessage(qq, message);
                } catch (Exception e) {
                    this.plugin.getSLF4JLogger().error("Fail to send qq message", e);
                }
            });

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


        // 检查封禁和举报
        if (this.checkBanAndReport(event, event.getUniqueId(), event.getName())) return;


        // 白名单检查
        final PaperWhitelistApi api = this.plugin.getPaperWhitelistApi();
        if (api != null) {
            final TextComponent.Builder text = Component.text();
            text.append(Component.text("您可以登录官网："));
//            text.appendNewline();
            text.append(Component.text("paper-card.cn").decorate(TextDecoration.UNDERLINED));
//            text.appendNewline();
            text.append(Component.text(" 自助申请白名单噢~"));

            final TextComponent msg = text.build().color(NamedTextColor.GREEN);

            api.onPreLoginCheck(event, msg);
            if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) return;
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
}
