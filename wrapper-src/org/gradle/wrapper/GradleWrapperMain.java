package org.gradle.wrapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class GradleWrapperMain {

    private GradleWrapperMain() {
    }

    public static void main(String[] args) throws Exception {
        Path projectDir = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
        Path propertiesPath = projectDir.resolve("gradle/wrapper/gradle-wrapper.properties");
        if (!Files.exists(propertiesPath)) {
            throw new IllegalStateException("Missing gradle-wrapper.properties at " + propertiesPath);
        }

        Properties properties = new Properties();
        try (InputStream inputStream = Files.newInputStream(propertiesPath)) {
            properties.load(inputStream);
        }

        String distributionUrl = requireProperty(properties, "distributionUrl");
        Path gradleHome = prepareDistribution(distributionUrl);
        Path gradleExecutable = findGradleExecutable(gradleHome);
        launchGradle(gradleExecutable, args, projectDir);
    }

    private static String requireProperty(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required wrapper property: " + key);
        }
        return value;
    }

    private static Path prepareDistribution(String distributionUrl) throws Exception {
        String fileName = distributionUrl.substring(distributionUrl.lastIndexOf('/') + 1);
        String baseName = fileName.replaceAll("\\.zip$", "");
        Path cacheDir = Paths.get(System.getProperty("user.home"), ".gradle", "wrapper", "dists", baseName);
        Path zipPath = cacheDir.resolve(fileName);
        Path extractDir = cacheDir.resolve("unzipped");

        if (!Files.exists(extractDir) || !containsGradleExecutable(extractDir)) {
            Files.createDirectories(cacheDir);
            if (!Files.exists(zipPath)) {
                download(distributionUrl, zipPath);
            }
            if (Files.exists(extractDir)) {
                deleteRecursively(extractDir);
            }
            unzip(zipPath, extractDir);
        }

        return findGradleHome(extractDir);
    }

    private static boolean containsGradleExecutable(Path extractDir) throws IOException {
        if (!Files.exists(extractDir)) {
            return false;
        }
        try {
            findGradleExecutable(findGradleHome(extractDir));
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private static void download(String distributionUrl, Path zipPath) throws Exception {
        Files.createDirectories(Objects.requireNonNull(zipPath.getParent()));
        URL url = URI.create(distributionUrl).toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setInstanceFollowRedirects(true);
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(30000);
        int status = connection.getResponseCode();
        if (status >= 300 && status < 400) {
            String location = connection.getHeaderField("Location");
            connection.disconnect();
            if (location == null) {
                throw new IOException("Redirect without Location header for " + distributionUrl);
            }
            download(location, zipPath);
            return;
        }
        if (status != HttpURLConnection.HTTP_OK) {
            throw new IOException("Failed to download Gradle distribution. HTTP status: " + status);
        }
        try (InputStream inputStream = connection.getInputStream();
             OutputStream outputStream = Files.newOutputStream(zipPath)) {
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
        } finally {
            connection.disconnect();
        }
    }

    private static void unzip(Path zipPath, Path targetDir) throws IOException {
        Files.createDirectories(targetDir);
        try (ZipInputStream zipInputStream = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null) {
                Path outputPath = targetDir.resolve(entry.getName()).normalize();
                if (!outputPath.startsWith(targetDir)) {
                    throw new IOException("Zip entry escapes target dir: " + entry.getName());
                }
                if (entry.isDirectory()) {
                    Files.createDirectories(outputPath);
                } else {
                    Files.createDirectories(Objects.requireNonNull(outputPath.getParent()));
                    Files.copy(zipInputStream, outputPath, StandardCopyOption.REPLACE_EXISTING);
                    if (entry.getName().endsWith("/gradle") || entry.getName().endsWith(".sh")) {
                        outputPath.toFile().setExecutable(true);
                    }
                }
                zipInputStream.closeEntry();
            }
        }
    }

    private static Path findGradleHome(Path extractDir) throws IOException {
        try (var stream = Files.walk(extractDir, 2)) {
            return stream
                .filter(Files::isDirectory)
                .filter(path -> path.getFileName().toString().startsWith("gradle-"))
                .findFirst()
                .orElseThrow(() -> new IOException("Could not find extracted Gradle home in " + extractDir));
        }
    }

    private static Path findGradleExecutable(Path gradleHome) throws IOException {
        boolean windows = System.getProperty("os.name").toLowerCase().contains("win");
        Path executable = windows
            ? gradleHome.resolve("bin/gradle.bat")
            : gradleHome.resolve("bin/gradle");
        if (!Files.exists(executable)) {
            throw new IOException("Could not find Gradle executable at " + executable);
        }
        return executable;
    }

    private static void launchGradle(Path gradleExecutable, String[] args, Path projectDir) throws Exception {
        boolean windows = System.getProperty("os.name").toLowerCase().contains("win");
        List<String> command = new ArrayList<>();
        if (windows) {
            command.add("cmd.exe");
            command.add("/c");
            command.add(gradleExecutable.toString());
        } else {
            command.add(gradleExecutable.toString());
        }
        for (String arg : args) {
            command.add(arg);
        }

        Process process = new ProcessBuilder(command)
            .directory(projectDir.toFile())
            .inheritIO()
            .start();
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    private static void deleteRecursively(Path root) throws IOException {
        Files.walkFileTree(root, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.deleteIfExists(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                Files.deleteIfExists(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}
