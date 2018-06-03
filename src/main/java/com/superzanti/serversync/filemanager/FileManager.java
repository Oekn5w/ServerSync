package com.superzanti.serversync.filemanager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.superzanti.serversync.util.FileMatcher;
import com.superzanti.serversync.util.Logger;
import com.superzanti.serversync.util.PathBuilder;
import com.superzanti.serversync.util.PathUtils;
import com.superzanti.serversync.util.SyncFile;
import com.superzanti.serversync.util.enums.EFileMatchingMode;

// TODO break this out into objects modelling the various directories
public class FileManager {
	public final Path configurationFilesDirectory;
	public final Path modFilesDirectory;
	public final Path logsDirectory;

	public FileManager() {
		Path modsDir = Paths.get("mods");
		Path configDir;
		Path logsDir;

		if (!Files.exists(modsDir)) {
			modsDir = Paths.get(new PathBuilder().add("..").add("mods").toString());

			if (!Files.exists(modsDir)) {
				throw new Error("Could not find a mods directory, Is serversync in the right location?");
			}

			configDir = Paths.get(new PathBuilder().add("..").add("config").toString());
			logsDir = Paths.get(new PathBuilder().add("..").add("logs").toString());
		} else {
			configDir = Paths.get("config");
			logsDir = Paths.get("logs");
		}

		configurationFilesDirectory = configDir;
		modFilesDirectory = modsDir;
		logsDirectory = logsDir;
	}
	
	public ArrayList<SyncFile> getModFiles(String directory, List<String> fileMatchPatterns, EFileMatchingMode fileMatchingMode) {
		ArrayList<String> dirs = new ArrayList<>();
		dirs.add(directory);
		return getModFiles(dirs, fileMatchPatterns, fileMatchingMode);
	}

	public ArrayList<SyncFile> getModFiles(List<String> includedDirectories, List<String> fileMatchPatterns, EFileMatchingMode fileMatchingMode) {
		return includedDirectories.stream()
				// Check for valid include directories
				.map(dir -> Paths.get(dir))
				.filter(path -> {
					if (Files.exists(path)) {
						return true;
					}
					Logger.debug("Could not find directory: " + path.toString());
					return false;
				})
				// Get files from valid directories
				.map(path -> PathUtils.fileListDeep(path))
				.flatMap(ArrayList::stream)
				// Filter out user ignored files
				.filter(file -> {
					if (fileMatchingMode == EFileMatchingMode.NONE) {
						return true;
					}
					return FileMatcher.shouldIncludeFile(file, fileMatchingMode);
				})
				// Create sync files for the remaining valid list
				.map(file -> SyncFile.StandardSyncFile(file))
				.collect(Collectors.toCollection(ArrayList::new));
	}
	
	public ArrayList<SyncFile> getClientOnlyFiles() {
		return PathUtils.fileListDeep(Paths.get("clientmods")).stream()
				.map(file -> SyncFile.ClientOnlySyncFile(file))
				.collect(Collectors.toCollection(ArrayList::new));
	}

	public ArrayList<SyncFile> getConfigurationFiles(List<String> fileMatchPatterns, EFileMatchingMode fileMatchingMode) {

		ArrayList<Path> configFiles = PathUtils.fileListDeep(configurationFilesDirectory);

		Logger.debug("Found " + configFiles.size() + " files in: config");

		if (fileMatchPatterns != null) {
			Logger.debug("File matching patterns present");

			return FileMatcher.filter(configFiles, fileMatchingMode).stream().map(file -> SyncFile.ConfigSyncFile(file))
					.collect(Collectors.toCollection(ArrayList::new));
		}

		return configFiles.stream().map(file -> SyncFile.ConfigSyncFile(file))
				.collect(Collectors.toCollection(ArrayList::new));
	}
}