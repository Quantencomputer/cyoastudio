package cyoastudio.data;

import java.util.*;

public class Section {
	public enum ImagePositioning {
		TOP, LEFT, RIGHT, ALTERNATING
	}

	private String title = "";
	private String description = "";
	private ImagePositioning imagePositioning = ImagePositioning.TOP;
	private List<Option> options = new ArrayList<>();
	private int optionsPerRow = 3;
	private int aspectX = 0, aspectY = 0;
	private String classes = "";

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public List<Option> getOptions() {
		return options;
	}

	public void setImagePositioning(ImagePositioning imagePositioning) {
		this.imagePositioning = imagePositioning;
	}

	public ImagePositioning getImagePositioning() {
		return imagePositioning;
	}

	public void setOptionsPerRow(int optionsPerRow) {
		this.optionsPerRow = optionsPerRow;
	}

	public int getOptionsPerRow() {
		return optionsPerRow;
	}

	public int getAspectY() {
		return aspectY;
	}

	public void setAspectY(int aspectY) {
		this.aspectY = aspectY;
	}

	public int getAspectX() {
		return aspectX;
	}

	public void setAspectX(int aspectX) {
		this.aspectX = aspectX;
	}

	public boolean hasAspectRatio() {
		return aspectX != 0 && aspectY != 0;
	}

	public double getAspectRatio() {
		if (aspectY == 0) {
			return 0;
		} else {
			return aspectX / aspectY;
		}
	}

	public String getClasses() {
		return classes;
	}

	public void setClasses(String classes) {
		this.classes = classes;
	}

	@Override
	public String toString() {
		return "Section " + (title == null ? "null" : title);
	}
}
