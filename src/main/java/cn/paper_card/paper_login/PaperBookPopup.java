package cn.paper_card.paper_login;

import cn.paper_card.client.api.PaperClientApi;
import cn.paper_card.client.api.PaperResponseError;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.io.IOException;

class PaperBookPopup {

    static @Nullable JsonElement req(@NotNull PaperClientApi api, @NotNull Player player, @NotNull Logger logger) {

        final JsonElement dataEle;
        try {
            dataEle = api.getWithAuth("/popup/mc/join/" + player.getUniqueId());
        } catch (PaperResponseError | IOException e) {
            logger.error("fail to request /popup/mc/join/{uuid}: ", e);
            return null;
        }

        // 没有弹窗

        return dataEle;
    }

    static @Nullable String bookMinedown(@Nullable JsonElement dataEle, @NotNull Logger logger) {

        // 没有弹窗
        if (dataEle == null || dataEle.isJsonNull()) return null;

        String minedown;

        try {
            final JsonObject dataObj = dataEle.getAsJsonObject();
            minedown = dataObj.get("book_minedown").getAsString();
        } catch (Exception e) {
            logger.error("invalid data format: ", e);
            return null;
        }

        if (minedown == null || minedown.isEmpty()) return null;

        return minedown;
    }
}