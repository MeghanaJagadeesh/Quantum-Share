package com.qp.quantum_share.helper;

import org.springframework.stereotype.Service;

@Service
public class FileConvertion {

//	@Async // Enables asynchronous processing
//	public CompletableFuture<MultipartFile> convertVideoToMp4(MultipartFile videoFile) {
//		File inputFile = null;
//		File outputFile = null;
//		try {
//			// Create temporary files for input and output
//			inputFile = File.createTempFile("input", ".tmp");
//			videoFile.transferTo(inputFile);
//			outputFile = File.createTempFile("output", ".mp4");
//
//			// Use FFmpegFrameGrabber to grab the input video
//			try (FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(inputFile)) {
//				grabber.start();
//
//				// Set up FFmpegFrameRecorder to record the output video
//				try (FFmpegFrameRecorder recorder = new FFmpegFrameRecorder(outputFile, grabber.getImageWidth(),
//						grabber.getImageHeight())) {
//					recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264); // Set video codec to H264
//					recorder.setFormat("mp4");
//
//					// Set audio codec if there are audio channels
//					if (grabber.getAudioChannels() > 0) {
//						recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC); // Set audio codec to AAC
//						recorder.setAudioChannels(grabber.getAudioChannels());
//						recorder.setSampleRate(grabber.getSampleRate());
//					}
//
//					recorder.setFrameRate(grabber.getFrameRate());
//					recorder.start();
//
//					// Record video frames
//					Frame frame;
//					while ((frame = grabber.grabFrame()) != null) {
//						recorder.record(frame);
//					}
//
//					// Record audio frames if present
//					Frame audioFrame;
//					while ((audioFrame = grabber.grabSamples()) != null) {
//						recorder.record(audioFrame);
//					}
//
//					recorder.stop();
//				}
//				grabber.stop();
//			}
//
//			// Read the output file to byte array
//			byte[] outputBytes;
//			try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
//				java.nio.file.Files.copy(outputFile.toPath(), outputStream);
//				outputBytes = outputStream.toByteArray();
//			}
//			return CompletableFuture.completedFuture(
//					new ByteArrayMultipartFile(outputBytes, videoFile.getOriginalFilename(), "video/mp4"));
//		} catch (Exception e) {
//			e.printStackTrace(); // Handle exceptions and log errors appropriately
//			throw new RuntimeException("Video conversion failed", e);
//		} finally {
//			// Clean up temporary files
//			if (inputFile != null && inputFile.exists()) {
//				inputFile.delete();
//			}
//			if (outputFile != null && outputFile.exists()) {
//				outputFile.delete();
//			}
//		}
//	}
//
//	public MultipartFile convertImageToJpg(MultipartFile file) throws IOException {
//		System.out.println(file.getInputStream().toString());
//		InputStream inputStream = file.getInputStream();
//		BufferedImage originalImage = ImageIO.read(inputStream);
//		System.err.println(originalImage);
//		if (originalImage.getType() != BufferedImage.TYPE_INT_RGB) {
//			BufferedImage rgbImage = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(),
//					BufferedImage.TYPE_INT_RGB);
//			Graphics2D g = rgbImage.createGraphics();
//			g.drawImage(originalImage, 0, 0, null);
//			g.dispose();
//			originalImage = rgbImage;
//		}
//
//		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//
//		Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
//		if (!writers.hasNext()) {
//			throw new IllegalStateException("No writers found for format: jpg");
//		}
//		ImageWriter writer = writers.next();
//		ImageOutputStream ios = ImageIO.createImageOutputStream(outputStream);
//		writer.setOutput(ios);
//		ImageWriteParam writeParam = writer.getDefaultWriteParam();
//		writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
//		writeParam.setCompressionQuality(1.0f);
//
//		writer.write(null, new javax.imageio.IIOImage(originalImage, null, null), writeParam);
//		writer.dispose();
//
//		byte[] convertedBytes = outputStream.toByteArray();
//		return new ByteArrayMultipartFile(convertedBytes, file.getOriginalFilename(), "image/jpeg");
//	}
//	
	
}
