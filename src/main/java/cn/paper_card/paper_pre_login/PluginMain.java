package cn.paper_card.paper_pre_login;

import cn.paper_card.ban.api.PaperBanApi;
import cn.paper_card.disallow_all.DisallowAllApi;
import cn.paper_card.paper_card_auth.api.PaperCardAuthApi;
import cn.paper_card.paper_whitelist.api.PaperWhitelistApi;
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

public final class PluginMain extends JavaPlugin {

    private PaperWhitelistApi paperWhitelistApi = null;

    private DisallowAllApi disallowAllApi = null;

    private PaperCardAuthApi paperCardAuthApi = null;

//    private QqGroupAccessApi qqGroupAccessApi = null;

    private PaperBanApi paperBanApi = null;


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
            this.paperWhitelistApi = this.getServer().getServicesManager().load(PaperWhitelistApi.class);
        } catch (NoClassDefFoundError e) {
            this.getSLF4JLogger().warn(e.toString());
        }

        try {
            this.disallowAllApi = this.getDisallowAllApi0();
        } catch (NoClassDefFoundError e) {
            this.getSLF4JLogger().warn(e.toString());
        }

        try {
            this.paperCardAuthApi = this.getServer().getServicesManager().load(PaperCardAuthApi.class);
        } catch (NoClassDefFoundError e) {
            this.getSLF4JLogger().warn(e.toString());
        }


        try {
            this.paperBanApi = this.getServer().getServicesManager().load(PaperBanApi.class);
        } catch (NoClassDefFoundError e) {
            this.getSLF4JLogger().warn(e.toString());
        }

        this.getServer().getPluginManager().registerEvents(new Listener() {

            @EventHandler
            public void on(@NotNull AsyncPlayerPreLoginEvent event) {
                try {
                    new OnPreLogin(PluginMain.this).on(event);
                } catch (Throwable e) {
                    getSLF4JLogger().error("", e);
                    OnPreLogin.kickWhenException(event, e);
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
        this.paperWhitelistApi = null;
        this.disallowAllApi = null;
        this.paperCardAuthApi = null;
//        this.qqGroupAccessApi = null;
        this.paperBanApi = null;

        this.taskScheduler.cancelTasks(this);
    }

    @NotNull TaskScheduler getTaskScheduler() {
        return this.taskScheduler;
    }

    @Nullable PaperWhitelistApi getPaperWhitelistApi() {
        return this.paperWhitelistApi;
    }

    @Nullable DisallowAllApi getDisallowAllApi() {
        return this.disallowAllApi;
    }

    @Nullable PaperCardAuthApi getPaperCardAuthApi() {
        return this.paperCardAuthApi;
    }

    @Nullable PaperBanApi getPaperBanApi() {
        return this.paperBanApi;
    }
}
