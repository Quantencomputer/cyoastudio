package cyoastudio.data;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.stream.*;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.slf4j.*;

// TODO refactor this class so it isn't used as a global anymore
public class DataStorage {
	final Logger logger = LoggerFactory.getLogger(DataStorage.class);

	private Path storageLocation;

	public DataStorage() throws IOException {
		this.storageLocation = Files.createTempDirectory("cyoastudio");
		logger.info("Saving data in directory " + storageLocation.toString());
	}

	public void flush() throws IOException {
		logger.debug("Flushing data storage");
		FileUtils.deleteDirectory(storageLocation.toFile());
		this.storageLocation = Files.createTempDirectory("cyoastudio");
	}

	public void close() throws IOException {
		FileUtils.deleteDirectory(storageLocation.toFile());
	}

	public Path getFile(String iden) {
		return storageLocation.resolve(iden);
	}

	public List<Path> getFiles() {
		try (Stream<Path> paths = Files.walk(storageLocation)) {
			return paths
					.filter(Files::isRegularFile)
					.collect(Collectors.toList());
		} catch (IOException e) {
			throw new RuntimeException("Couldn't list files in working directory", e);
		}
	}

	public String makeIdentifier() {
		String iden = null;
		do {
			iden = RandomStringUtils.randomAlphanumeric(32);
		} while (hasFile(iden));
		return iden;
	}

	public void delete(String identifier) throws IOException {
		Files.delete(getFile(identifier));
	}

	public Path getPath() {
		return storageLocation;
	}

	public boolean hasFile(String data) {
		return Files.exists(storageLocation.resolve(data));
	}
}
