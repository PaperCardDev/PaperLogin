package cn.paper_card.paper_pre_login;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
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
            return;
        }

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