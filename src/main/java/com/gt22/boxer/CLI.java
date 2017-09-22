package com.gt22.boxer;

import com.gt22.randomutils.log.SimpleLog;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class CLI {
	private static final SimpleLog log = SimpleLog.getLog("Boxer#cli");
	private static class Settings {
		String inFile;
		String outFile;
		Classification clazz = Classification.IRRELEVANT;
		float expandBy = 0.7f;
	}

	public static void box(String[] args) throws IOException, InterruptedException, ExecutionException {
		Settings st = processArgs(args);
		log.info("Initiate boxing");


		File src = new File(st.inFile);
		if(!src.exists()) stop("Unable to find " + src.getAbsolutePath());
		if(!src.canRead()) stop("Can't read " + src.getAbsolutePath());
		BufferedImage img = ImageIO.read(src);
		log.info("Image read");

		List<FacialDetector.Face> faces = FacialDetector.detect(img, st.expandBy, st.clazz);
		log.info("Faces detected... " + faces.size() + " recognized.");


		BufferedImage box = st.clazz.getImg().get();

		Graphics2D g = img.createGraphics();
		//Rect should be already squared in FacialDetector#detect, but just to be sure width also used as height
		faces.forEach(f -> g.drawImage(box, f.rect.x(), f.rect.y(), f.rect.width(), f.rect.width(), null));
		g.dispose();
		log.info("Boxes drawn");

		ImageIO.write(img, st.outFile.substring(st.outFile.lastIndexOf('.') + 1), new File(st.outFile));
		log.info("Image saved, boxing complete");
	}

	private static Settings processArgs(String[] args) {
		Settings st = new Settings();
		if(args.length == 0) stop("Specify input image");
		for(int i = 0; i < args.length; i++) {
			switch (args[i]) {
				case "-h":
				case "--help": {
					displayHelp();
				}
				case "-c":
				case "--class": {
					i++;
					if(args.length == i) stop("Specify classification");
					st.clazz = Classification.getClassification(args[i], true);
					if(st.clazz == null) stop("Invalid classification, use --help for list of available classifications");
					log.debug("Classification: " + st.clazz);
					break;
				}
				case "-e":
				case "--expansion": {
					i++;
					if(args.length == i) stop("Specify expansion factor");
					try {
						st.expandBy = Float.parseFloat(args[i]);
					} catch(NumberFormatException e) {
						stop("Invalid expansion factor");
					}
					log.debug("Expansion: " + st.expandBy);
					break;
				}
				case "-o":
				case "--output": {
					i++;
					if(args.length == i) stop("Specify output file");
					st.outFile = args[i];
					log.debug("Output: " + st.outFile);
					break;
				}
				default: {
					st.inFile = args[i];
					log.debug("Input: " + st.inFile);
					break;
				}
			}
		}
		if(st.outFile == null) st.outFile = st.clazz + "_" + st.inFile;
		return st;
	}

	private static void stop(String message) {
		log.warn(message);
		System.exit(1);
	}

	private static void displayHelp() {
		System.out.println("Usage: boxer %input_file% [-c(lass) %classification%=irrelevant] [-e(xpansion) %box-expansion-factor%=0.5] [-o(utput) %output_file%=%classification%_%input_file%");
		System.out.println("Note: full names should use -- instead of - (eg. -c, but --class)");
		System.out.println("Classifications: irrelevant, asset, irrelevant_threat, relevant_threat, catalyst, relevant-one (!!!dash, not underscore!!!), unknown");
		System.exit(0);
	}
}
