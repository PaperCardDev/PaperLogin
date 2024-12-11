package cn.paper_card.paper_login;

import cn.paper_card.client.api.PaperClientApi;
import cn.paper_card.disallow_all.DisallowAllApi;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.google.gson.JsonElement;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.jetbrains.annotations.NotNull;

class OnPreLogin {
    private final @NotNull PluginMain plugin;

    OnPreLogin(@NotNull PluginMain plugin) {
        this.plugin = plugin;
    }


    void on(@NotNull AsyncPlayerPreLoginEvent event) {
        try {
            this.on0(event);
        } catch (Throwable e) {
            this.plugin.getSLF4JLogger().error("error when login: ", e);
            Util.kickWhenException(event, e);
        }
    }


    private void checkError(@NotNull AsyncPlayerPreLoginEvent event) {

        final PlayerProfile profile = event.getPlayerProfile();

        final TextComponent kickMessage = ErrorProfile.checkProfileError(profile);

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
        final PaperClientApi clientApi = this.plugin.getPaperClientApi();

        if (clientApi != null) {

            final JsonElement respJson = PaperLoginPopup.reqLoginPopup(event, clientApi);

            this.plugin.getSLF4JLogger().info(respJson.toString());

            final TextComponent kickMessage = PaperLoginPopup.getKickMessage(respJson);

            if (kickMessage != null) {
                event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST);
                event.kickMessage(kickMessage);
                return;
            }
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