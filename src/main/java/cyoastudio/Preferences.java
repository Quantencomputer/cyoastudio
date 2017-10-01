package cyoastudio;

import java.nio.file.*;

import org.slf4j.*;

public class Preferences {
	private static final Logger logger = LoggerFactory.getLogger(Preferences.class);
	private static final java.util.prefs.Preferences preferences = java.util.prefs.Preferences
			.userNodeForPackage(Application.class);

	public static Path getPath(String key) {
		try {
			if (preferences.get(key, null) != null
					&& Files.isDirectory(Paths.get(preferences.get(key, null))))
				return Paths.get(preferences.get(key, null));
		} catch (Exception e) {
			logger.warn("Coulnd't access preferences", e);
		}
		return Paths.get(System.getProperty("user.home"));
	}

	public static void setPath(String key, Path path) {
		preferences.put(key, path.toAbsolutePath().toString());
	}
}
