package com.gt22.boxer;

import com.gt22.boxer.gui.GuiCore;
import com.gt22.randomutils.Instances;
import javafx.application.Application;

public class Core {

	public static void main(String[] args) throws Exception {
		if (args.length == 0) {
			Application.launch(GuiCore.class);
		} else {
			CLI.box(args);
			Instances.getExecutor().shutdown();
		}
	}




}
