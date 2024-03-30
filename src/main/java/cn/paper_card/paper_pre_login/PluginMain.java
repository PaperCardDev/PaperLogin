package cn.paper_card.paper_pre_login;

import cn.paper_card.disallow_all.DisallowAllApi;
import cn.paper_card.little_skin_login.api.LittleSkinLoginApi;
import cn.paper_card.paper_card_auth.api.PaperCardAuthApi;
import cn.paper_card.paper_card_ban.api.PaperCardBanApi;
import cn.paper_card.paper_login.api.PaperSkinLoginApi;
import cn.paper_card.paper_whitelist.api.PaperWhitelistApi;
import cn.paper_card.qq_group_access.api.QqGroupAccessApi;
import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class PluginMain extends JavaPlugin {

    private PaperWhitelistApi paperWhitelistApi = null;

    private DisallowAllApi disallowAllApi = null;

    private LittleSkinLoginApi littleSkinLoginApi = null;

    private PaperSkinLoginApi paperSkinLoginApi = null;

    private PaperCardAuthApi paperCardAuthApi = null;

    private QqGroupAccessApi qqGroupAccessApi = null;

    private PaperCardBanApi paperCardBanApi = null;


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
            this.littleSkinLoginApi = this.getServer().getServicesManager().load(LittleSkinLoginApi.class);
        } catch (NoClassDefFoundError e) {
            this.getSLF4JLogger().warn(e.toString());
        }

        try {
            this.paperSkinLoginApi = this.getServer().getServicesManager().load(PaperSkinLoginApi.class);
        } catch (NoClassDefFoundError e) {
            this.getSLF4JLogger().warn(e.toString());
        }

        try {
            this.paperCardAuthApi = this.getServer().getServicesManager().load(PaperCardAuthApi.class);
        } catch (NoClassDefFoundError e) {
            this.getSLF4JLogger().warn(e.toString());
        }


        try {
            this.qqGroupAccessApi = this.getServer().getServicesManager().load(QqGroupAccessApi.class);
        } catch (NoClassDefFoundError e) {
            this.getSLF4JLogger().warn(e.toString());
        }

        try {
            this.paperCardBanApi = this.getServer().getServicesManager().load(PaperCardBanApi.class);
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
    }

    @Override
    public void onDisable() {
        this.paperWhitelistApi = null;
        this.disallowAllApi = null;
        this.littleSkinLoginApi = null;
        this.paperSkinLoginApi = null;
        this.paperCardAuthApi = null;
        this.qqGroupAccessApi = null;
        this.paperCardBanApi = null;

        this.taskScheduler.cancelTasks(this);
    }

    @NotNull TaskScheduler getTaskScheduler() {
        return this.taskScheduler;
    }

    @Nullable PaperWhitelistApi getPaperWhitelistApi() {
        return this.paperWhitelistApi;
    }

    @Nullable PaperSkinLoginApi getPaperSkinLoginApi() {
        return this.paperSkinLoginApi;
    }

    @Nullable DisallowAllApi getDisallowAllApi() {
        return this.disallowAllApi;
    }

    @Nullable LittleSkinLoginApi getLittleSkinLoginApi() {
        return this.littleSkinLoginApi;
    }

    @Nullable PaperCardAuthApi getPaperCardAuthApi() {
        return this.paperCardAuthApi;
    }

    @Nullable QqGroupAccessApi getQqGroupAccessApi() {
        return this.qqGroupAccessApi;
    }

    @Nullable PaperCardBanApi getPaperCardBanApi() {
        return this.paperCardBanApi;
    }
}
