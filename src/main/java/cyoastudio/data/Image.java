package cyoastudio.data;

import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;

import cyoastudio.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.*;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;

public class Image {
	private String identifier;
	private String blendCache;
	private Color cachedColor;

	private Image(String identifier) {
		this.identifier = identifier;
	}

	public static Image fromStorage(String identifier) {
		return new Image(identifier);
	}

	public static Image fromData(javafx.scene.image.Image fxImage) throws IOException {
		return fromData(SwingFXUtils.fromFXImage(fxImage, null));
	}

	public static Image fromData(BufferedImage subimage) throws IOException {
		String iden = Application.getDatastorage().makeIdentifier();
		Path path = Application.getDatastorage().getFile(iden);

		ImageIO.write(subimage, "png", path.toFile());

		return new Image(iden);
	}

	public static Image fromData(String base64) throws IOException {
		byte[] data = Base64.getDecoder().decode(base64);
		return fromData(ImageIO.read(new ByteArrayInputStream(data)));
	}

	public static Image copy(Path source) throws IOException {
		String iden = Application.getDatastorage().makeIdentifier();
		Path path = Application.getDatastorage().getFile(iden);

		Files.copy(source, path);

		return new Image(iden);
	}

	private Path getPath() {
		return Application.getDatastorage().getFile(identifier);
	}

	public Image copy() {
		try {
			return Image.copy(getPath());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public BufferedImage toBufferedImage() throws IOException {
		return ImageIO.read(getPath().toFile());
	}

	public javafx.scene.image.Image toFX() {
		try {
			return new javafx.scene.image.Image(getPath().toUri().toURL().toString());
		} catch (MalformedURLException e) {
			return null;
		}
	}

	public String toBase64() {
		try {
			return "data:image/png;base64," + Base64.getEncoder().encodeToString(getData());
		} catch (IOException e) {
			throw new RuntimeException("Couldn't convert image to base64", e);
		}
	}

	private byte[] getData() throws IOException {
		return IOUtils.toByteArray(getPath().toUri());
	}

	public Image trim(Rectangle2D r) {
		try {
			BufferedImage subimage;
			subimage = toBufferedImage().getSubimage((int) r.getMinX(), (int) r.getMinY(), (int) r.getWidth(),
					(int) r.getHeight());
			return Image.fromData(subimage);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public String blend(Color color) {
		if (color.equals(cachedColor))
			return blendCache;

		javafx.scene.image.Image fxImg = toFX();
		Canvas c = new Canvas(fxImg.getWidth(), fxImg.getHeight());

		GraphicsContext gc = c.getGraphicsContext2D();
		gc.drawImage(fxImg, 0, 0);
		gc.setGlobalBlendMode(BlendMode.MULTIPLY);
		gc.setFill(color);
		gc.fillRect(0, 0, fxImg.getWidth(), fxImg.getHeight());

		WritableImage image = new WritableImage((int) fxImg.getWidth(), (int) fxImg.getHeight());
		c.snapshot(null, image);

		try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
			ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", stream);
			byte[] data = stream.toByteArray();
			blendCache = "data:image/png;base64," + Base64.getEncoder().encodeToString(data);
		} catch (IOException e) {
			throw new RuntimeException("Couldn't convert image to base64", e);
		}
		cachedColor = color;

		return blendCache;
	}

	public URL getURL() {
		try {
			return getPath().toUri().toURL();
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}

	public void delete() throws IOException {
		Application.getDatastorage().delete(identifier);
	}

	public String getIdentifier() {
		return identifier;
	}

	public static Object fromDataOrStorage(String data) throws IOException {
		if (Application.getDatastorage().hasFile(data)) {
			return fromStorage(data);
		} else {
			return fromData(data);
		}
	}
}
