package com.gt22.boxer.utils;

import com.gt22.randomutils.Instances;
import com.gt22.randomutils.log.SimpleLog;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.RequestBuilder;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.concurrent.Future;

public class ImageUtils {
	private static final HashMap<String, Future<BufferedImage>> IMAGE_CACHE = new HashMap<>();

	private static final SimpleLog log = SimpleLog.getLog("ImageUtils");

	public static BufferedImage mergeImages(BufferedImage img1, BufferedImage img2) {
		img2 = resize(
			img2,
			img1.getWidth(),
			img1.getHeight()
		);
		BufferedImage ret = new BufferedImage(img1.getWidth(), img1.getHeight(), BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = ret.createGraphics();
		g.drawImage(img1, 0, 0, null);
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_ATOP, 1f));
		g.drawImage(img2, 0, 0, null);
		g.dispose();

		return ret;
	}

	public static BufferedImage resize(BufferedImage img, int newW, int newH) {
		Image tmp = img.getScaledInstance(newW, newH, Image.SCALE_AREA_AVERAGING);
		BufferedImage ret = new BufferedImage(newW, newH, BufferedImage.TYPE_INT_ARGB);

		Graphics2D g = ret.createGraphics();
		g.drawImage(tmp, 0, 0, null);
		g.dispose();

		return ret;
	}


	public static Future<BufferedImage> readImg(String url) {

		if (!IMAGE_CACHE.containsKey(url)) {
			log.debug("Loading image " + url);
			Future<BufferedImage> req = Instances.getExecutor().submit(() -> {
				try {
					HttpResponse res = Instances.getHttpClient().execute(RequestBuilder.get(url).build());
					if (res.getStatusLine().getStatusCode() != 200) {
						log.warn("Unable to load " + url + ": " + res.getStatusLine().getStatusCode());
						return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
					}
					BufferedImage read = ImageIO.read(res.getEntity().getContent());
					log.debug("Image " + url + " loaded");
					return read;

				} catch (IOException e) {
					throw new UncheckedIOException(e);
				}
			});
			IMAGE_CACHE.put(url, req);
			return req;
		} else {
			return IMAGE_CACHE.get(url);
		}
	}
}