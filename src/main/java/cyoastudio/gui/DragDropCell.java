package cyoastudio.gui;

import java.io.Serializable;

import javafx.scene.control.*;
import javafx.scene.input.*;

public abstract class DragDropCell<T> extends ListCell<T> {
	public static final DataFormat objectFormat = new DataFormat("cyoastudio/listentry");

	public static class DragDropInfo implements Serializable {
		private static final long serialVersionUID = 2474881796722142036L;

		private String source;
		private int index;

		public DragDropInfo(String source, int index) {
			this.source = source;
			this.index = index;
		}

		public String getSource() {
			return source;
		}

		public int getIndex() {
			return index;
		}
	}

	public DragDropCell() {
		// TODO add thumbnail for options?
		setContentDisplay(ContentDisplay.TEXT_ONLY);

		setOnDragDetected(event -> {
			if (getItem() == null) {
				return;
			}

			Dragboard dragboard = startDragAndDrop(TransferMode.MOVE);
			ClipboardContent content = new ClipboardContent();
			content.put(objectFormat, new DragDropInfo(getIdentifier(), getIndex()));
			// TODO dragView?
			// dragboard.setDragView(
			// birdImages.get(
			// items.indexOf(
			// getItem()
			// )
			// )
			// );
			dragboard.setContent(content);

			event.consume();
		});

		setOnDragOver(event -> {
			if (event.getGestureSource() != DragDropCell.this &&
					event.getDragboard().hasContent(objectFormat)) {
				event.acceptTransferModes(TransferMode.MOVE);
			}

			event.consume();
		});

		setOnDragEntered(event -> {
			if (!isEmpty() && event.getDragboard().hasContent(objectFormat) &&
					isCompatible((DragDropInfo) event.getDragboard().getContent(objectFormat))) {
				setOpacity(0.4);
			}
		});

		setOnDragExited(event -> {
			if (!isEmpty() && event.getDragboard().hasContent(objectFormat) &&
					isCompatible((DragDropInfo) event.getDragboard().getContent(objectFormat))) {
				setOpacity(1);
			}
		});

		setOnDragDropped(event -> {
			setOpacity(1);

			if (getItem() == null) {
				return;
			}

			Dragboard db = event.getDragboard();

			if (db.hasContent(objectFormat)) {
				boolean success = receive((DragDropInfo) db.getContent(objectFormat));
				event.setDropCompleted(success);
			} else {
				event.setDropCompleted(false);
			}

			event.consume();
		});

		setOnDragDone(DragEvent::consume);
	}

	@Override
	protected void updateItem(T item, boolean empty) {
		super.updateItem(item, empty);

		if (empty || item == null) {
			setText(null);
		} else {
			setText(stringify(item));
		}
	}

	public boolean isSourceOf(DragDropInfo info) {
		return info.getSource().equals(getIdentifier());
	}

	protected abstract String stringify(T value);

	protected abstract boolean receive(DragDropInfo info);

	protected abstract String getIdentifier();

	protected boolean isCompatible(DragDropInfo info) {
		return info.getSource().equals(getIdentifier());
	}
}