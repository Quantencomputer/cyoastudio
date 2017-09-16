package cyoastudio.data;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.apache.commons.io.output.ByteArrayOutputStream;

import cyoastudio.util.MultiplyComposite;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Rectangle2D;

public class Image {
	private byte[] data;

	public Image(javafx.scene.image.Image image) {
		BufferedImage b = SwingFXUtils.fromFXImage(image, null);
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
		BufferedImage b = new BufferedImage(0, 0, BufferedImage.TYPE_INT_RGB);
		try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
			ImageIO.write(b, "png", stream);
			data = stream.toByteArray();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public BufferedImage toBufferedImage() throws IOException {
		return ImageIO.read(new ByteArrayInputStream(data));
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
		BufferedImage src;
		try {
			src = toBufferedImage();
			BufferedImage target = new BufferedImage(src.getWidth(), src.getHeight(),
					BufferedImage.TYPE_INT_ARGB);
			Graphics2D g2d = target.createGraphics();
			g2d.drawImage(src, null, 0, 0);
			g2d.setComposite(MultiplyComposite.Multiply);
			g2d.setColor(color);
			g2d.fillRect(0, 0, src.getWidth(), src.getHeight());
			g2d.dispose();

			return new Image(target);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
