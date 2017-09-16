package cyoastudio.data;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.apache.commons.io.output.ByteArrayOutputStream;

import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.effect.BlendMode;
import javafx.scene.image.WritableImage;

public class Image {
	private byte[] data;
	private transient BufferedImage b;

	public Image(javafx.scene.image.Image image) {
		this.b = SwingFXUtils.fromFXImage(image, null);
		try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
			ImageIO.write(b, "png", stream);
			data = stream.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public Image(Path source) throws IOException {
		data = Files.readAllBytes(source);
	}

	public Image(BufferedImage b) {
		this.b = b;
		try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
			ImageIO.write(b, "png", stream);
			data = stream.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public Image(String base64) {
		this.data = Base64.getDecoder().decode(base64);
	}

	public Image() {
		this.b = new BufferedImage(0, 0, BufferedImage.TYPE_INT_RGB);
		try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
			ImageIO.write(b, "png", stream);
			data = stream.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public BufferedImage toBufferedImage() throws IOException {
		if (b == null) {
			b = ImageIO.read(new ByteArrayInputStream(data));
		}
		return b;
	}

	public javafx.scene.image.Image toFX() {
		return new javafx.scene.image.Image(new ByteArrayInputStream(data));
	}

	public String toBase64() {
		return Base64.getEncoder().encodeToString(data);
	}

	public Image trim(Rectangle2D r) {
		BufferedImage subimage;
		try {
			subimage = toBufferedImage().getSubimage((int) r.getMinX(), (int) r.getMinY(), (int) r.getWidth(),
					(int) r.getHeight());
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		return new Image(subimage);
	}

	public Image blend(Color color) {
		javafx.scene.image.Image fxImg = toFX();
		Canvas c = new Canvas(fxImg.getHeight(), fxImg.getWidth());
		
		GraphicsContext gc = c.getGraphicsContext2D();
		gc.drawImage(fxImg, 0, 0);
		gc.setGlobalBlendMode(BlendMode.MULTIPLY);
		gc.setFill(new javafx.scene.paint.Color(color.getRed() / 255.0, color.getGreen() / 255.0, color.getBlue() / 255.0, color.getAlpha() / 255.0));
		gc.fillRect(0, 0, fxImg.getHeight(), fxImg.getWidth());
		
		WritableImage image = new WritableImage((int) fxImg.getHeight(), (int) fxImg.getWidth());
		c.snapshot(null, image);
		return new Image(image);
	}
}
