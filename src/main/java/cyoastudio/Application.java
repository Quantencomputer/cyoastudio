package cyoastudio;

import java.awt.*;
import java.io.*;
import java.util.*;
import java.util.prefs.Preferences;

import com.github.zafarkhaja.semver.Version;

import cyoastudio.gui.MainWindow;
import javafx.beans.value.*;
import javafx.geometry.*;
import javafx.scene.control.Dialog;
import javafx.stage.*;

public class Application extends javafx.application.Application {
	private static Version version;
	public static final Preferences preferences = Preferences.userNodeForPackage(Application.class);

	@Override
	public void start(Stage stage) throws Exception {
		Point mousePosition = MouseInfo.getPointerInfo().getLocation();
		stage.setX(mousePosition.getX());
		stage.setY(mousePosition.getY());

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

	// TODO remove
	public static void positionDialog(Dialog<?> dialog) {
		Point awtMousePosition = MouseInfo.getPointerInfo().getLocation();
		Point2D mousePosition = new Point2D(awtMousePosition.getX(), awtMousePosition.getY());

		Screen screen = Screen.getScreens().filtered(s -> s.getBounds().contains(mousePosition)).get(0);
		Rectangle2D bounds = screen.getBounds();

		double stageCenterX = bounds.getMinX() + bounds.getWidth() / 2;
		double stageCenterY = bounds.getMinY() + bounds.getHeight() / 2;

		dialog.widthProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				dialog.setX(stageCenterX - dialog.getWidth() / 2);
			}
		});
		dialog.heightProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue) {
				dialog.setY(stageCenterY - dialog.getHeight() / 2);
			}
		});
	}

}
