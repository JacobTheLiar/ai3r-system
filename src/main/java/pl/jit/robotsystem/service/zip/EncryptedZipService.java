package pl.jit.robotsystem.service.zip;

import lombok.extern.java.Log;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.model.FileHeader;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Log
public class EncryptedZipService {

    public List<File> unzipFile(File zipArchive, String outputDir, char[] password) {
        Path targetDir = zipArchive.toPath().getParent().resolve(outputDir);
        log.info("Unzipping %s to %s (using Zip4j, password protected: %s)"
                .formatted(zipArchive.getName(), targetDir, (password != null && password.length > 0 ? "yes" : "no")));

        try (ZipFile zipFile = new net.lingala.zip4j.ZipFile(zipArchive)) {
            if (password != null && password.length > 0) {
                zipFile.setPassword(password);
            }

            if (!zipFile.isValidZipFile()) {
                log.severe("Invalid ZIP file: " + zipArchive.getPath());
                throw new RuntimeException("Invalid ZIP file: " + zipArchive.getPath());
            }

            Files.createDirectories(targetDir);

            return zipFile.getFileHeaders().stream()
                    .peek(fileHeader -> log.fine("Processing with Zip4j: " + fileHeader.getFileName()))
                    .map(fileHeader -> extractZip4jEntry(zipFile, fileHeader, targetDir))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .collect(Collectors.toList());

        } catch (ZipException e) {
            log.severe("Error while unzipping with Zip4j: " + e.getMessage() +
                       (e.getCause() != null ? " Cause: " + e.getCause().getMessage() : ""));
            throw new RuntimeException("Failed to unzip password-protected ZIP file with Zip4j", e);
        } catch (IOException e) {
            log.severe("IO Error during unzipping setup with Zip4j: " + e.getMessage());
            throw new RuntimeException("Failed to unzip password-protected ZIP file due to IO error", e);
        }
    }

    private Optional<File> extractZip4jEntry(net.lingala.zip4j.ZipFile zipFileInstance,
                                             FileHeader fileHeader,
                                             Path targetDir) {
        try {
            Path entryPath = targetDir.resolve(fileHeader.getFileName());

            if (!entryPath.normalize().startsWith(targetDir.normalize())) {
                log.warning("Attempt to extract file outside target directory (Zip Slip with Zip4j): " + fileHeader.getFileName());
                return Optional.empty();
            }

            if (fileHeader.isDirectory()) {
                Files.createDirectories(entryPath);
                return Optional.empty();
            } else {
                if (entryPath.getParent() != null) {
                    Files.createDirectories(entryPath.getParent());
                }
                zipFileInstance.extractFile(fileHeader, targetDir.toString());
                return Optional.of(entryPath.toFile());
            }
        } catch (ZipException e) {
            log.warning("Cannot extract " + fileHeader.getFileName() + " using Zip4j: " + e.getMessage() +
                        (e.getCause() != null ? " Cause: " + e.getCause().getMessage() : ""));
            return Optional.empty();
        } catch (IOException e) {
            log.warning("IO error while preparing to extract " + fileHeader.getFileName() + " with Zip4j: " + e.getMessage());
            return Optional.empty();
        }
    }

    @SuppressWarnings("unused")
    public List<File> unzipFile(File zipArchive, char[] password) {
        return unzipFile(zipArchive, ".", password);
    }
}
