package fr.euphyllia.skyllia.configuration.manager;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import fr.euphyllia.skyllia.Skyllia;
import fr.euphyllia.skyllia.api.configuration.IConfigurationProvider;

import java.io.File;

public class MessagesConfigManager implements IConfigurationProvider {

    private String prefix = "<gradient:#4FC3F7:#0288D1><bold>Skyllia</bold></gradient> <gray>»</gray> ";

    @Override
    public void loadConfig() {
        File file = new File(Skyllia.getInstance().getDataFolder(), "config/messages.toml");
        if (!file.exists()) return;
        CommentedFileConfig config = CommentedFileConfig.builder(file).sync().autosave().build();
        config.load();
        String loadedPrefix = config.get("messages.prefix");
        if (loadedPrefix != null && !loadedPrefix.isBlank()) {
            prefix = loadedPrefix;
        }
        config.close();
    }

    @Override
    public void reloadFromDisk() {
        loadConfig();
    }

    @Override
    public boolean canReloadFromDisk() {
        return true;
    }

    @Override
    public <T> T getOrSetDefault(String path, T defaultValue, Class<T> expectedClass) {
        return defaultValue;
    }

    public String getPrefix() {
        return prefix;
    }
}
