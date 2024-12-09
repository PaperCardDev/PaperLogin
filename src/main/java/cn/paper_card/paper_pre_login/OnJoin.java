package cn.paper_card.paper_pre_login;

import cn.paper_card.client.api.PaperClientApi;
import cn.paper_card.client.api.PaperResponseError;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import de.themoep.minedown.adventure.MineDown;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

class OnJoin implements Listener {

    private final @NotNull PluginMain plugin;

    OnJoin(@NotNull PluginMain plugin) {
        this.plugin = plugin;
    }

    private void showBookPopup(@NotNull Player player) {
        this.plugin.getTaskScheduler().runTaskAsynchronously(() -> {

            final PaperClientApi api = this.plugin.getPaperClientApi();

            if (api == null) return;

            JsonElement dataEle;
            try {
                dataEle = api.getWithAuth("/popup/mc/join/" + player.getUniqueId());
            } catch (PaperResponseError | IOException e) {
                this.plugin.getSLF4JLogger().error("fail to request /popup/mc/join/{uuid}: ", e);
                return;
            }

            // 没有弹窗
            if (dataEle == null || dataEle.isJsonNull()) return;

            String minedown;

            try {
                final JsonObject dataObj = dataEle.getAsJsonObject();
                minedown = dataObj.get("book_minedown").getAsString();
            } catch (Exception e) {
                this.plugin.getSLF4JLogger().error("invalid data format: ", e);
                return;
            }

            if (minedown == null || minedown.isEmpty()) return;


            final String finalMinedown = minedown;

            this.plugin.getTaskScheduler().runTask(player, () -> {
                final Component component = new MineDown(finalMinedown).toComponent();
                final Book.Builder builder = Book.builder();
                builder.addPage(component);
                player.openBook(builder);
            });
        });
    }

    @EventHandler
    public void on(@NotNull PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final PlayerProfile profile = player.getPlayerProfile();

        final String keyTitle = "error-title";
        final String keyDetail = "error-detail";

        String title = null;
        String detail = null;

        for (ProfileProperty property : profile.getProperties()) {

            final String name = property.getName();
            if (keyTitle.equals(name)) {
                title = property.getValue();
            } else if (keyDetail.equals(name)) {
                detail = property.getValue();
            }

            if (title != null && detail != null) break;
        }

        if (title == null && detail == null) {
            this.showBookPopup(player);
        } else {
            // 在下一刻
            final String finalTitle = title;
            final String finalDetail = detail;
            this.plugin.getTaskScheduler().runTaskLater(() -> player.kick(Component.text()
                    .append(Component.text(finalTitle != null ? finalTitle : "NO_TITLE").color(NamedTextColor.DARK_RED))
                    .appendNewline()
                    .append(Component.text(finalDetail != null ? finalDetail : "NO_DETAIL").color(NamedTextColor.RED))
                    .build()), 1);
        }
    }
}