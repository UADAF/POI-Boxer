package com.gt22.boxer.gui;

import com.gt22.boxer.Classification;
import com.gt22.randomutils.Instances;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.jooq.lambda.Unchecked;

public class BoxCell extends ListCell<Classification> {
	private final ImageView g = new ImageView();


	public BoxCell(ListView<Classification> boxes) {
		g.fitWidthProperty().bind(boxes.prefWidthProperty().subtract(50));
		g.fitHeightProperty().bind(g.fitWidthProperty());
		getStyleClass().add("box-cell");
	}

	@Override
	protected void updateItem(Classification item, boolean empty) {
		if(!isItemChanged(getItem(), item)) return;
		super.updateItem(item, empty);
		setText(null);
		if(empty || item == null) {
			setGraphic(null);
		} else {
			Instances.getExecutor().submit(Unchecked.runnable(() -> {
				Image img = SwingFXUtils.toFXImage(item.getImg().get(), null);
				Platform.runLater(() -> g.setImage(img));
			}, e -> {
				e.printStackTrace();
				System.exit(1);
			}));
			setGraphic(g);
		}
	}

	@Override
	protected boolean isItemChanged(Classification oldItem, Classification newItem) {
		return oldItem != newItem;
	}
}
