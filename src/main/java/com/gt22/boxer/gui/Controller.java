package com.gt22.boxer.gui;

import com.gt22.boxer.Classification;
import com.gt22.boxer.FacialDetector;
import com.gt22.randomutils.Instances;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.geometry.Bounds;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.bytedeco.javacpp.opencv_core;
import org.jooq.lambda.Unchecked;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;

public class Controller {
	public HBox root;
	public VBox controls;
	public ImageView image;
	public Button open;
	public Button save;
	public TextField outName;
	public ListView<Classification> boxes;

	private List<FacialDetector.Face> recognizedFaces = null;
	private BufferedImage currentImage, boxedImage;
	private FacialDetector.Face selectedFace = null;

	public void initialize() {
		bindSizes();
		initListView();
	}

	private void initListView() {
		boxes.setCellFactory(BoxCell::new);
		boxes.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) ->
			Instances.getExecutor().submit(Unchecked.runnable(() -> {
				if (selectedFace != null) {
					selectedFace.box = newValue;
					selectedFace.boxImg = newValue.getImg().get();
					updateBoxes(currentImage);
				}
			}))));
		boxes.setItems(FXCollections.observableArrayList(
			Classification.IRRELEVANT,
			Classification.ASSET,
			Classification.ANALOG_INTERFACE,
			Classification.CATALYST,
			Classification.RELEVANT_ONE,
			Classification.IRRELEVANT_THREAT,
			Classification.RELEVANT_THREAT,
			Classification.UNKNOWN
		));
	}

	public void select(MouseEvent e) {
		double x = e.getX();
		double y = e.getY();

		//Scale x and y to image coords
		Bounds bounds = image.getLayoutBounds();
		x /= (bounds.getWidth() / image.getImage().getWidth());
		y /= (bounds.getHeight() / image.getImage().getHeight());

		boolean found = false;
		for (FacialDetector.Face f : recognizedFaces) {
			opencv_core.Rect r = f.rect;
			if (x >= r.x() && x <= r.x() + r.width() && y >= r.y() && y <= r.y() + r.height()) {
				selectedFace = f;
				found = true;
				break;
			}
		}
		boxes.setDisable(!found);
	}

	private void bindSizes() {
		controls.prefWidthProperty().bind(root.widthProperty().multiply(0.15f));
		image.fitWidthProperty().bind(root.widthProperty().subtract(controls.prefWidthProperty()));
		image.fitHeightProperty().bind(root.heightProperty());
		controls.prefHeightProperty().bind(root.heightProperty());
		open.prefWidthProperty().bind(controls.prefWidthProperty());
		outName.prefWidthProperty().bind(controls.prefWidthProperty());
		save.prefWidthProperty().bind(controls.prefWidthProperty());
		boxes.prefWidthProperty().bind(controls.prefWidthProperty());
		boxes.prefHeightProperty().bind(controls.prefHeightProperty().subtract(save.prefHeightProperty()).subtract(outName.prefHeightProperty()).subtract(open.prefHeightProperty()));
	}


	public void open(ActionEvent actionEvent) {
		FileChooser chooser = new FileChooser();
		File f = chooser.showOpenDialog(GuiCore.getStage().getOwner());
		Instances.getExecutor().submit(Unchecked.runnable(() -> {
			BufferedImage i = ImageIO.read(f);
			Platform.runLater(() -> updateImage(i));
		}));
	}

	public void save(ActionEvent actionEvent) {
		Instances.getExecutor().submit(() -> {
			String out = outName.getText();
			if (out.isEmpty()) {
				displayMessage("Specify file name", Color.RED, 3000);
			} else {
				File f = new File(out);
				try {
					ImageIO.write(boxedImage, extractType(f), f);
					displayMessage("Saved", Color.GREEN, 1000);
				} catch (Exception e) {
					displayMessage("Unable to save: " + e.getLocalizedMessage(), Color.RED, 5000);
				}
			}
		});
	}

	private String extractType(File f) {
		String name = f.getName();
		int dotIndex = name.lastIndexOf('.');
		if (dotIndex < 0) { //No extension, default to png
			return "png";
		} else {
			return name.substring(dotIndex + 1); //Extract extension
		}
	}

	private void displayMessage(String msg, Color color, long time) {
		Instances.getExecutor().submit(Unchecked.runnable(() -> {
			String content = outName.getText();
			outName.setEditable(false);
			outName.setText(msg);
			Thread.sleep(time);
			outName.setText(content);
			outName.setEditable(true);
		}));
	}

	private void updateImage(BufferedImage img) {
		image.setImage(SwingFXUtils.toFXImage(currentImage = img, null));
		Instances.getExecutor().submit(Unchecked.runnable(() -> {
			recognizedFaces = FacialDetector.detect(img, 0.7f, Classification.IRRELEVANT);
			updateBoxes(img);
		}));
	}

	private void updateBoxes(BufferedImage base) {
		boxedImage = drawBoxes(base);
		Platform.runLater(() -> image.setImage(SwingFXUtils.toFXImage(boxedImage, null)));
	}

	private BufferedImage drawBoxes(BufferedImage src) {
		BufferedImage img = new BufferedImage(src.getWidth(), src.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.drawImage(src, 0, 0, null);
		if (recognizedFaces != null) {
			recognizedFaces.forEach(f -> g.drawImage(f.boxImg, f.rect.x(), f.rect.y(), f.rect.width(), f.rect.width(), null));
		}
		g.dispose();
		return img;
	}
}
