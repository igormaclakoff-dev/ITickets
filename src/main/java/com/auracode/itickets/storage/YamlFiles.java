package com.auracode.itickets.storage;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.logging.Level;
import java.util.logging.Logger;

final class YamlFiles {
    private final Logger logger;
    private final File backupDirectory;

    YamlFiles(Logger logger, File backupDirectory) {
        this.logger = logger;
        this.backupDirectory = backupDirectory;
    }

    YamlConfiguration load(File file, String description) {
        YamlConfiguration config = new YamlConfiguration();
        try {
            config.load(file);
            return config;
        } catch (IOException | InvalidConfigurationException exception) {
            logger.log(Level.WARNING, "Не удалось прочитать " + description + ": " + file.getPath(), exception);
            return null;
        }
    }

    boolean saveSafely(YamlConfiguration config, File target, String description) {
        File parent = target.getParentFile();
        if (parent != null) {
            parent.mkdirs();
        }
        File temp = new File(parent, target.getName() + ".tmp");
        try {
            config.save(temp);
            if (target.exists()) {
                backupDirectory.mkdirs();
                Files.copy(target.toPath(), backupPath(target), StandardCopyOption.REPLACE_EXISTING);
            }
            moveReplacing(temp.toPath(), target.toPath());
            return true;
        } catch (IOException exception) {
            logger.log(Level.SEVERE, "Не удалось сохранить " + description, exception);
            if (temp.exists() && !temp.delete()) {
                logger.warning("Не удалось удалить временный файл: " + temp.getPath());
            }
            return false;
        }
    }

    private Path backupPath(File target) {
        return new File(backupDirectory, target.getName() + ".bak").toPath();
    }

    private void moveReplacing(Path source, Path target) throws IOException {
        try {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
