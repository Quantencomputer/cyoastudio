package cyoastudio.data;

import javafx.scene.paint.Color;
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
	private BufferedImage b;

	public Image(javafx.scene.image.Image image) {
		this.b = SwingFXUtils.fromFXImage(image, null);
	}

	public Image(Path source) throws IOException {
		data = Files.readAllBytes(source);
	}

	public Image(BufferedImage b) {
		this.b = b;
	}

	public Image(String base64) {
		this.data = Base64.getDecoder().decode(base64);
	}

	public Image() {
		this.b = new BufferedImage(0, 0, BufferedImage.TYPE_INT_RGB);
	}

	public BufferedImage toBufferedImage() throws IOException {
		if (b == null) {
			b = ImageIO.read(new ByteArrayInputStream(data));
		}
		return b;
	}

	public javafx.scene.image.Image toFX() {
		return new javafx.scene.image.Image(new ByteArrayInputStream(getData()));
	}

	public String toBase64() {
		return Base64.getEncoder().encodeToString(getData());
	}
	
	private byte[] getData() {
		if (data == null) {
			try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
				ImageIO.write(b, "png", stream);
				data = stream.toByteArray();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return data;
	}

	public Image trim(Rectangle2D r) {
		try {
			BufferedImage subimage;
			subimage = toBufferedImage().getSubimage((int) r.getMinX(), (int) r.getMinY(), (int) r.getWidth(),
					(int) r.getHeight());
			return new Image(subimage);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public Image blend(Color color) {
		javafx.scene.image.Image fxImg = toFX();
		Canvas c = new Canvas(fxImg.getHeight(), fxImg.getWidth());
		
		GraphicsContext gc = c.getGraphicsContext2D();
		gc.drawImage(fxImg, 0, 0);
		gc.setGlobalBlendMode(BlendMode.MULTIPLY);
		gc.setFill(color);
		gc.fillRect(0, 0, fxImg.getHeight(), fxImg.getWidth());
		
		WritableImage image = new WritableImage((int) fxImg.getHeight(), (int) fxImg.getWidth());
		c.snapshot(null, image);
		return new Image(image);
	}
}
