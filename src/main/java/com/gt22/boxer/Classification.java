package com.gt22.boxer;

import com.gt22.boxer.utils.ImageUtils;
import com.gt22.randomutils.Instances;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

public class Classification {
	private static final int DEFAULT_IMAGE_SIZE = 200;

	public static final Classification IRRELEVANT = new Classification("Irrelevant", "https://vignette3.wikia.nocookie.net/pediaofinterest/images/a/a1/S03-WhiteSquare.svg/revision/latest/scale-to-width-down/", Color.WHITE);
	public static final Classification ASSET = new Classification("Asset", "https://vignette1.wikia.nocookie.net/pediaofinterest/images/a/a4/S03-YellowSquare.svg/revision/latest/scale-to-width-down/", Color.YELLOW);
	public static final Classification ANALOG_INTERFACE = new Classification("Analog Interface", "https://vignette1.wikia.nocookie.net/pediaofinterest/images/2/2e/S03-BlackSquareYellowCorners.svg/revision/latest/scale-to-width-down/", Color.YELLOW);
	public static final Classification IRRELEVANT_THREAT = new Classification("Irrelevant Threat", "https://vignette3.wikia.nocookie.net/pediaofinterest/images/d/d2/S03-WhiteSquareRedCorners.svg/revision/latest/scale-to-width-down/", Color.RED);
	public static final Classification RELEVANT_THREAT = new Classification("Relevant Threat", "https://vignette4.wikia.nocookie.net/pediaofinterest/images/4/4c/S03-RedSquare.svg/revision/latest/scale-to-width-down/", Color.RED);
	public static final Classification CATALYST = new Classification("Catalyst", "https://vignette2.wikia.nocookie.net/pediaofinterest/images/2/2e/S03-BlueSquare.svg/revision/latest/scale-to-width-down/", Color.BLUE);
	public static final Classification RELEVANT_ONE = new Classification("Relevant-One", "https://vignette3.wikia.nocookie.net/pediaofinterest/images/a/a3/S05-BlueSquareWhiteCorners.svg/revision/latest/scale-to-width-down/", Color.BLUE);
	public static final Classification UNKNOWN = new Classification("Unknown", "https://cdn.discordapp.com/attachments/197699632841752576/338403812576329728/classes.png", Color.GRAY);
	private static final Map<String, Classification> CLASS_MAP = new HashMap<>();
	private static final Map<Integer, Future<BufferedImage>> UNKNOWN_IMAGE_CACHE = new HashMap<>();

	static {
		registerClass(IRRELEVANT);
		registerClass(ASSET);
		registerClass(ANALOG_INTERFACE);
		registerClass(IRRELEVANT_THREAT);
		registerClass(RELEVANT_THREAT);
		registerClass(CATALYST);
		registerClass(RELEVANT_ONE);
		registerClass(UNKNOWN);
	}


	private final String name;
	private final String img;
	private final Color color;
	private Classification(String name, String img, Color color) {
		this.name = name;
		this.img = img;
		this.color = color;
	}

	private static void registerClass(Classification classification) {
		CLASS_MAP.put(classification.getUnlocName(), classification);
	}

	public static Classification getClassification(String name) {
		return getClassification(name, false);
	}

	public static Classification getClassification(String name, boolean strict) {
		name = name.toLowerCase();
		if (CLASS_MAP.containsKey(name)) {
			return CLASS_MAP.get(name);
		}
		for (Map.Entry<String, Classification> e : CLASS_MAP.entrySet()) {
			if (name.contains(e.getKey())) {
				return e.getValue();
			}
		}
		return strict ? null : IRRELEVANT;
	}


	public String getName() {
		return name;
	}

	public String getUnlocName() {
		return name.toLowerCase().replace(' ', '_');
	}

	public Future<BufferedImage> getImg() {
		return getImg(DEFAULT_IMAGE_SIZE);
	}

	public Future<BufferedImage> getImg(int size) {
		if(size == DEFAULT_IMAGE_SIZE) { //Preloaded image in resources
			return Instances.getExecutor().submit(() ->
				ImageIO.read(getClass().getResourceAsStream("/boxes/" + getUnlocName() + ".png")));
		}
		if (this == UNKNOWN) {
			if(UNKNOWN_IMAGE_CACHE.containsKey(size)) {
				return UNKNOWN_IMAGE_CACHE.get(size);
			} else {
				Future<BufferedImage> f = Instances.getExecutor().submit(() -> {
					int half = size / 2;
					Future<BufferedImage> //Start all requests asynchronously
						irr = IRRELEVANT.getImg(size),
						trr = RELEVANT_THREAT.getImg(size),
						ctr = CATALYST.getImg(size),
						asr = ASSET.getImg(size);

					BufferedImage //Await request completion
						ir = irr.get(),
						tr = trr.get(),
						ct = ctr.get(),
						as = asr.get();

					BufferedImage res = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
					Graphics2D g = res.createGraphics();
					//top left
					drawImagePart(g, ir, 0, 0, half, half);
					//top right
					drawImagePart(g, tr, half, 0, size, half);
					//Bottom left
					drawImagePart(g, ct, 0, half, half, size);
					//Bottom right
					drawImagePart(g, as, half, half, size, size);
					g.dispose();
					return res;
				});
				UNKNOWN_IMAGE_CACHE.put(size, f);
				return f;
			}
		}
		return ImageUtils.readImg(img + size);
	}

	public Color getColor() {
		return color;
	}

	private static void drawImagePart(Graphics2D g, BufferedImage img, int x1, int y1, int x2, int y2) {
		g.drawImage(img, x1, y1, x2, y2, x1, y1, x2, y2, null);
	}


	@Override
	public String toString() {
		return getName();
	}
}