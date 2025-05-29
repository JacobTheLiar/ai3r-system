package pl.jit.robotsystem.service.zip;

import lombok.extern.java.Log;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
@Log
public class ZipService {

    public List<File> unzipFile(File zipFile, String outputDir) {
        Path targetDir = zipFile.toPath().getParent().resolve(outputDir);
        log.info("Unzipping %s to %s".formatted(zipFile.getName(), targetDir));
        try (ZipFile zip = new ZipFile(zipFile)) {
            Files.createDirectories(targetDir);
            return zip.stream()
                    .peek(entry -> log.fine("Processing: " + entry.getName()))
                    .map(entry -> extractEntry(zip, entry, targetDir))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            log.severe("Error while unzipping: " + e.getMessage());
            throw new RuntimeException("Failed to unzip ZIP file", e);
        }
    }

    public List<File> unzipFile(File zipFile) {
        return unzipFile(zipFile, ".");
    }

    private Optional<File> extractEntry(ZipFile zip, ZipEntry entry, Path targetDir) {
        try {
            Path entryPath = targetDir.resolve(entry.getName());
            if (!entryPath.normalize().startsWith(targetDir.normalize())) {
                throw new SecurityException("Attempt to extract outside target directory: " + entry.getName());
            }
            if (entry.isDirectory()) {
                Files.createDirectories(entryPath);
                return Optional.empty();
            } else {
                Files.createDirectories(entryPath.getParent());
                Files.copy(zip.getInputStream(entry), entryPath, StandardCopyOption.REPLACE_EXISTING);
                return Optional.of(entryPath.toFile());
            }
        } catch (IOException e) {
            log.warning("Cannot extract " + entry.getName() + ": " + e.getMessage());
            return Optional.empty();
        }
    }
}
