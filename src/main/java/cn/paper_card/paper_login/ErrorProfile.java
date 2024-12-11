package cn.paper_card.paper_login;

import org.jetbrains.annotations.NotNull;

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
}