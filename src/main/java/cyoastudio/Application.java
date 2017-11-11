package cyoastudio;

import java.awt.*;
import java.io.*;
import java.util.*;

import org.slf4j.*;

import com.github.zafarkhaja.semver.Version;

import cyoastudio.gui.MainWindow;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class Application extends javafx.application.Application {
	private static final Logger logger = LoggerFactory.getLogger(Application.class);
	private static Version version;

	@Override
	public void start(Stage stage) throws Exception {
		logger.info(getRuntimeDetails());

		Point mousePosition = MouseInfo.getPointerInfo().getLocation();
		stage.setX(mousePosition.getX());
		stage.setY(mousePosition.getY());

		new MainWindow(stage);

		stage.setMaximized(true);
	}

	public static String getRuntimeDetails() {
		StringBuilder sb = new StringBuilder();
		sb.append("CYOA Studio version " + version.toString() + "\n");
		sb.append("Java: " + System.getProperty("java.version") + " by " + System.getProperty("java.vendor") + "\n");
		sb.append("JavaFX Version: " + System.getProperty("javafx.runtime.version") + "\n");
		sb.append("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version") + " "
				+ System.getProperty("os.arch") + "\n");

		WebView web = new WebView();
		sb.append("Internal browser: " + web.getEngine().getUserAgent());

		return sb.toString();
	}

	public static void main(String[] args) {
		Locale.setDefault(Locale.US);
		org.apache.log4j.BasicConfigurator.configure();

		try {
			Properties properties = new Properties();
			InputStream stream = Application.class.getResourceAsStream("application.properties");
			properties.load(stream);
			stream.close();
			version = Version.valueOf(properties.getProperty("version"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		launch(args);
	}

	public static Version getVersion() {
		return version;
	}
}
