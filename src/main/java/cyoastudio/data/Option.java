package cyoastudio.data;

public class Option {
	private String title = "";
	private String description = "";
	private Image image = null;
	private String classes = "";
	private String cost = "";
	private int rollRange = 1;

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

	public Image getImage() {
		return image;
	}

	public void setImage(Image image) {
		this.image = image;
	}

	public String getClasses() {
		return classes;
	}

	public void setClasses(String classes) {
		this.classes = classes;
	}

	@Override
	public String toString() {
		return "Option " + (title == null ? "null" : title);
	}

	public String getCost() {
		return cost;
	}

	public void setCost(String cost) {
		this.cost = cost;
	}

	public int getRollRange() {
		return rollRange;
	}

	public void setRollRange(int rollRange) {
		this.rollRange = rollRange;
	}
}
