package cn.paper_card.paper_login;

import cn.paper_card.client.api.PaperClientApi;
import cn.paper_card.client.api.PaperResponseError;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.themoep.minedown.adventure.MineDown;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

class PaperLoginPopup {

    static @NotNull JsonElement reqLoginPopup(@NotNull AsyncPlayerPreLoginEvent event, @NotNull PaperClientApi clientApi) {

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

    static @Nullable TextComponent getKickMessage(@NotNull JsonElement respJson) {

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
        if (kickMessage == null) return null;
        if (kickMessage.isJsonNull()) return null;


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

        return builder.build();
    }
}
