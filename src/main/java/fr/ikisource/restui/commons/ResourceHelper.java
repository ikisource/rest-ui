package fr.ikisource.restui.commons;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import fr.ikisource.restui.service.Logger;
import fr.ikisource.restui.service.Notifier;

public interface ResourceHelper {

	/**
	 * Copy source to destination
	 * @param source - The source path
	 * @param destination - The destination path directory
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	static void copyResource(final String source, final String destination) throws IOException, URISyntaxException {

		final File file = new File(ResourceHelper.class.getProtectionDomain().getCodeSource().getLocation().getPath());

		if (file.isFile()) {
			// this is a jar file : we copy all the files starting with source
			copyResourceFromJar(file, source, destination);
		} else {
			// application is lauching from an ide
			copyResourceFromIde(source, destination);
		}
	}

	private static void copyResourceFromJar(final File file, final String source, final String destination) throws IOException {

		try (JarFile jar = new JarFile(file)) {
			final Enumeration<JarEntry> entries = jar.entries();
			final String jarPath = source.startsWith("/") ? source.substring(1) : source; // remove '/' at the beginning
			while (entries.hasMoreElements()) {
				final JarEntry entry = entries.nextElement();
				final String entryName = entry.getName();

				if (entryName.startsWith(jarPath)) {
					final Path path = Paths.get(destination, "/", entryName);

					if (entry.isDirectory()) {
						path.toFile().mkdirs();
					} else {
						try (InputStream inputStream = jar.getInputStream(entry)) {
							if (!path.toFile().exists()) {
								Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
							}
						}
					}
				}
			}
		}
	}

	private static void copyResourceFromIde(final String source, final String destination) throws URISyntaxException, IOException {

		final URL url = ResourceHelper.class.getResource(source);

		if (url != null) {
			final Path root = Paths.get(url.toURI());
			final Stream<Path> paths = Files.walk(root).filter(Files::isRegularFile);
			for (final Path sourcePath : paths.collect(Collectors.toList())) {
				final Path destinationPath = Paths.get(destination, "/", source, "/", root.relativize(sourcePath).toString());
				if (!destinationPath.getParent().toFile().exists()) {
					destinationPath.getParent().toFile().mkdirs();
				}
				try (InputStream inputStream = Files.newInputStream(sourcePath)) {
					Files.copy(inputStream, destinationPath, StandardCopyOption.REPLACE_EXISTING);
				} catch (Exception e) {
					Logger.error(e);
					Notifier.notifyError(e.getMessage());
				}
			}
		}
	}

}
