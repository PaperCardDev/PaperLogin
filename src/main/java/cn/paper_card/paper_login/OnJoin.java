package cn.paper_card.paper_login;

import cn.paper_card.client.api.PaperClientApi;
import com.destroystokyo.paper.profile.PlayerProfile;
import com.google.gson.JsonElement;
import de.themoep.minedown.adventure.MineDown;
import net.kyori.adventure.inventory.Book;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

class OnJoin implements Listener {

    private final @NotNull PluginMain plugin;

    OnJoin(@NotNull PluginMain plugin) {
        this.plugin = plugin;
    }

    private void showBookPopup(@NotNull Player player) {
        this.plugin.getTaskScheduler().runTaskLaterAsynchronously(() -> {

            final PaperClientApi api = this.plugin.getPaperClientApi();

            if (api == null) return;

            if (!player.isOnline()) return;

            final JsonElement dataEle = PaperBookPopup.req(api, player, this.plugin.getSLF4JLogger());

            final String bookMinedown = PaperBookPopup.bookMinedown(dataEle, this.plugin.getSLF4JLogger());

            // 没有弹窗
            if (bookMinedown == null) return;

            this.plugin.getTaskScheduler().runTask(player, () -> {
                final Component component = new MineDown(bookMinedown).toComponent();
                final Book.Builder builder = Book.builder();
                builder.addPage(component);
                player.openBook(builder);
            });
        }, 40);
    }

    private void broadcastLoginType(@NotNull Player player) {

        this.plugin.getTaskScheduler().runTaskLater(() -> {
            if (!player.isOnline()) return;


            final PlayerProfile profile = player.getPlayerProfile();

            final Server server = this.plugin.getServer();

            if (profile.hasProperty("paper-login")) {

                server.broadcast(Component.text()
                        .append(Component.text().append(player.displayName()).build().color(NamedTextColor.WHITE))
                        .append(Component.text(" 使用 "))
                        .append(Component.text("[纸片正版登录]").color(NamedTextColor.GOLD)
                                .decorate(TextDecoration.UNDERLINED).clickEvent(ClickEvent.openUrl("https://paper-card.cn/yggdrasil")))
                        .append(Component.text(" ~"))
                        .build()
                        .color(NamedTextColor.GRAY));

            } else if (profile.hasProperty("little-skin-uuid")) {

                server.broadcast(Component.text()
                        .append(Component.text().append(player.displayName()).build().color(NamedTextColor.WHITE))
                        .append(Component.text(" 使用 "))
                        .append(Component.text("[LittleSkin登录]").color(NamedTextColor.GOLD)
                                .decorate(TextDecoration.UNDERLINED).clickEvent(ClickEvent.openUrl("https://littleskin.cn")))
                        .append(Component.text(" ~"))
                        .build()
                        .color(NamedTextColor.GRAY));
            }

        }, 40);
    }

    @EventHandler
    public void on(@NotNull PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        final PlayerProfile profile = player.getPlayerProfile();

        // 检查是否包含错误
        final TextComponent kickMessage = ErrorProfile.checkProfileError(profile);

        if (kickMessage == null) {
            this.showBookPopup(player);
            this.broadcastLoginType(player);
            return;
        }

        // 踢出
        this.plugin.getTaskScheduler().runTaskLater(() -> player.kick(kickMessage), 1);
    }
}