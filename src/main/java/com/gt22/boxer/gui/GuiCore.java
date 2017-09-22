package com.gt22.boxer.gui;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class GuiCore extends Application {

	private static Controller controller;
	private static Stage stage;
	@Override
	public void start(Stage primaryStage) throws Exception {
		FXMLLoader loader = new FXMLLoader(getClass().getResource("/gui/layout.fxml"));
		loader.load();
		Parent root = loader.getRoot();
		controller = loader.getController();
		Scene scene = new Scene(root);
		stage = primaryStage;
		primaryStage.setOnCloseRequest(e -> System.exit(0));
		primaryStage.setScene(scene);
		primaryStage.show();
	}

	public static Stage getStage() {
		return stage;
	}

	public static Controller getController() {
		return controller;
	}
}
