package cn.paper_card.paper_login;

import com.destroystokyo.paper.profile.PlayerProfile;
import com.destroystokyo.paper.profile.ProfileProperty;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

class ErrorProfile {
    static final @NotNull UUID ERROR_UUID = new UUID(0, 0);

    static @NotNull GameProfile create(@NotNull String errorMessage) {
        final GameProfile error = new GameProfile(ERROR_UUID, "ERROR");
        error.getProperties().put("error", new Property("error", errorMessage, null));
        return error;
    }

    static @NotNull GameProfile create(@NotNull String title, @NotNull Throwable e) {
        final StringBuilder sb = new StringBuilder();
        sb.append(title);

        for (Throwable t = e; t != null; t = t.getCause()) {
            sb.append('\n');
            sb.append(t);
        }

        return create(sb.toString());
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

}