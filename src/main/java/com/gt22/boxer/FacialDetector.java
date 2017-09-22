package com.gt22.boxer;

import org.bytedeco.javacv.Java2DFrameConverter;
import org.bytedeco.javacv.OpenCVFrameConverter;
import org.jooq.lambda.Unchecked;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.bytedeco.javacpp.opencv_core.*;
import static org.bytedeco.javacpp.opencv_imgproc.CV_BGR2GRAY;
import static org.bytedeco.javacpp.opencv_imgproc.cvtColor;
import static org.bytedeco.javacpp.opencv_objdetect.CascadeClassifier;


public class FacialDetector {

	public static class Face {

		Face(Rect rect, Classification box) throws ExecutionException, InterruptedException {
			this.rect = rect;
			this.box = box;
			this.boxImg = box.getImg().get();
		}

		public Rect rect;
		public Classification box;
		public BufferedImage boxImg;
	}

	private static final CascadeClassifier classifier;
	private static final Java2DFrameConverter imgConv = new Java2DFrameConverter();
	private static final OpenCVFrameConverter.ToMat matConv = new OpenCVFrameConverter.ToMat();

	static {
		try {
			Path classifierFile = Paths.get("tmp_haar_cascade.xml");
			if (Files.exists(classifierFile)) Files.delete(classifierFile);
			Files.copy(FacialDetector.class.getResourceAsStream("/cascade.xml"), classifierFile);
			classifier = new CascadeClassifier(classifierFile.toAbsolutePath().toString());
			Files.delete(classifierFile);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	public static List<Face> detect(BufferedImage frame, float expandFactor, Classification faceClass) {
		Mat m = matConv.convert(imgConv.getFrame(frame));
		cvtColor(m, m, CV_BGR2GRAY);

		RectVector o = new RectVector();
		classifier.detectMultiScale(m, o);

		return IntStream.range(0, (int) o.size())
			.parallel()
			.mapToObj(o::get)
			.map(r -> expandAndSquareRect(r, expandFactor))
			.map(Unchecked.function(r -> new Face(r, faceClass)))
			.collect(Collectors.toList());
	}

	private static Rect expandAndSquareRect(Rect r, float expandFactor) {
		int shift = (int) (r.width() * expandFactor);
		int halfShift = shift / 2;
		r.x(r.x() - halfShift);
		r.y(r.y() - halfShift);
		r.width(r.width() + shift);
		r.height(r.width()); //Make square from rect
		return r;
	}

}
