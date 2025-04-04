package cn.paper_card.paper_login;

import cn.paper_card.client.api.PaperClientApi;
import cn.paper_card.disallow_all.DisallowAllApi;
import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;

public final class PluginMain extends JavaPlugin {

    private DisallowAllApi disallowAllApi = null;

    private PaperClientApi paperClientApi = null;

    private final @NotNull TaskScheduler taskScheduler;

    public PluginMain() {
        this.taskScheduler = UniversalScheduler.getScheduler(this);
    }

    private @Nullable DisallowAllApi getDisallowAllApi0() {
        final Plugin plugin = this.getServer().getPluginManager().getPlugin("DisallowAll");
        if (plugin instanceof final DisallowAllApi api) return api;
        return null;
    }

    @Override
    public void onEnable() {
        try {
            new SessionServiceReplacer(this).doReplace();
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException |
                 NoSuchFieldException | InstantiationException e) {

            throw new RuntimeException("fail to replace session service: ", e);
        }

        try {
            this.paperClientApi = this.getServer().getServicesManager().load(PaperClientApi.class);
        } catch (NoClassDefFoundError e) {
            this.getSLF4JLogger().warn("请安装PaperClientApi插件以使用完整功能");
        }

        try {
            this.disallowAllApi = this.getDisallowAllApi0();
        } catch (NoClassDefFoundError e) {
            this.getSLF4JLogger().info("no disallow all api: " + e);
        }


        this.getServer().getPluginManager().registerEvents(new Listener() {

            @EventHandler
            public void on(@NotNull AsyncPlayerPreLoginEvent event) {
                try {
                    new OnPreLogin(PluginMain.this).on(event);
                } catch (Throwable e) {
                    getSLF4JLogger().error("", e);
                    Util.kickWhenException(event, e);
                }
            }
        }, this);

        this.getServer().getPluginManager().registerEvents(new Listener() {
            @EventHandler
            public void on(@NotNull PlayerJoinEvent event) {
                try {
                    new OnJoin(PluginMain.this).on(event);
                } catch (Throwable e) {
                    getSLF4JLogger().error("", e);
                }
            }
        }, this);
    }

    @Override
    public void onDisable() {
        this.disallowAllApi = null;

        this.paperClientApi = null;

        this.taskScheduler.cancelTasks(this);
    }

    @NotNull TaskScheduler getTaskScheduler() {
        return this.taskScheduler;
    }

    @Nullable DisallowAllApi getDisallowAllApi() {
        return this.disallowAllApi;
    }

    @Nullable PaperClientApi getPaperClientApi() {
        return this.paperClientApi;
    }
}
