package cyoastudio;

import java.io.*;
import java.util.*;

import com.github.zafarkhaja.semver.Version;

import cyoastudio.gui.MainWindow;
import javafx.stage.Stage;

public class Application extends javafx.application.Application {
	private static Version version;

	@Override
	public void start(Stage stage) throws Exception {
		new MainWindow(stage);

		stage.setMaximized(true);
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
