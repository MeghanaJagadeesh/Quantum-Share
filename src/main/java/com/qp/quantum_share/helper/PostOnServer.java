package com.qp.quantum_share.helper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

@Service
public class PostOnServer {

	@Autowired
	FileConvertion fileConvertion;

	private static final String SFTP_HOST = "pdx1-shared-a2-03.dreamhost.com";
	private static final int SFTP_PORT = 22;
	private static final String SFTP_USER = "dh_q2m9hh";
	private static final String SFTP_PASSWORD = "SriKrishna@0700";
	private static final String SFTP_DIRECTORY = "/home/dh_q2m9hh/quantumshare.quantumparadigm.in/";
	private static final String BASE_URL ="https://quantumshare.quantumparadigm.in/";
	
	private final ExecutorService executorService = Executors.newFixedThreadPool(10);

	public List<String> uploadFile(MultipartFile[] files, String directory) {
		CompletableFuture<Void>[] futures = new CompletableFuture[files.length];
		List<String> url=new ArrayList<String>();
		for (int i = 0; i < files.length; i++) {
			MultipartFile file = files[i];
			try {
				byte[] fileBytes = file.getBytes();
				String originalFilename = file.getOriginalFilename();
				String uniqueFileName = generateUniqueFileName(originalFilename);

				futures[i] = CompletableFuture.runAsync(() -> {
					String fileUrl = uploadFileViaSFTP(fileBytes, uniqueFileName, directory);
					if (fileUrl != null) {
						url.add(fileUrl);
					}
				}, executorService);
			} catch (IOException e) {
				new RuntimeException("File Not supported");
			}
		}
		try {
			CompletableFuture.allOf(futures).get();
		} catch (Exception e) {
			new RuntimeException(e.getMessage());
		}
		return url;
	}

	private String uploadFileViaSFTP(byte[] fileBytes, String fileName, String directory) {
		JSch jsch = new JSch();
		Session session = null;
		ChannelSftp sftpChannel = null;

		try {
			session = jsch.getSession(SFTP_USER, SFTP_HOST, SFTP_PORT);
			session.setPassword(SFTP_PASSWORD);
			session.setConfig("StrictHostKeyChecking", "no");
			session.connect();

			sftpChannel = (ChannelSftp) session.openChannel("sftp");
			sftpChannel.connect();

			try (InputStream fileInputStream = new ByteArrayInputStream(fileBytes)) {
				sftpChannel.put(fileInputStream, SFTP_DIRECTORY +directory+ fileName);
			}
			return BASE_URL+directory + fileName;
		} catch (JSchException | SftpException | IOException e) {
			return null;
		} finally {
			if (sftpChannel != null) {
				try {
					sftpChannel.disconnect();
				} catch (Exception e) {
					return null;
				}
			}
			if (session != null) {
				try {
					session.disconnect();
				} catch (Exception e) {
					return null;
				}
			}
		}
	}

	private String generateUniqueFileName(String originalFilename) {
//		String extension = "";
//		int dotIndex = originalFilename.lastIndexOf(".");
//		if (dotIndex >= 0) {
//			extension = originalFilename.substring(dotIndex);
//		}

		return UUID.randomUUID().toString() + originalFilename;
	}


	}
//	private static final int THREAD_POOL_SIZE = 10; // Optimal for parallel chunk uploads
//	private final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
//	private final ConcurrentLinkedQueue<Session> sessionPool = new ConcurrentLinkedQueue<>();
//
//	// Create and cache SFTP sessions
//	private Session createSession() throws Exception {
//		JSch jsch = new JSch();
//		Session session = jsch.getSession(SFTP_USER, SFTP_HOST, SFTP_PORT);
//		session.setPassword(SFTP_PASSWORD);
//
//		Properties config = new Properties();
//		config.put("StrictHostKeyChecking", "no");
//		config.put("Compression", "zlib@openssh.com"); // Compression for network efficiency
//		config.put("PreferredAuthentications", "password");
//		session.setConfig(config);
//		session.connect();
//		return session;
//	}
//
//	private Session getSession() throws Exception {
//		Session session = sessionPool.poll();
//		return (session != null && session.isConnected()) ? session : createSession();
//	}
//
//	private void returnSession(Session session) {
//		if (session != null && session.isConnected()) {
//			sessionPool.offer(session);
//		} else {
//			closeSession(session);
//		}
//	}
//
//	private void closeSession(Session session) {
//		if (session != null && session.isConnected()) {
//			session.disconnect();
//		}
//	}
//
//	public String uploadFile(MultipartFile file, String directory) {
//		try {
//			Future<String> future = executorService.submit(() -> uploadFileInChunks(file, directory));
//			return future.get();
//		} catch (Exception e) {
//			e.printStackTrace();
//			return null;
//		}
//	}
//
//	private String uploadFileInChunks(MultipartFile file, String directory) {
//		System.err.println("chunck");
//		Session session = null;
//		ChannelSftp channelSftp = null;
//		try {
//			session = getSession();
//			channelSftp = (ChannelSftp) session.openChannel("sftp");
//			channelSftp.connect();
//			String filename = UUID.randomUUID() + "_" + file.getOriginalFilename().replaceAll("\\s", "").hashCode();
//			String remoteFilePath = SFTP_DIRECTORY + directory + filename;
//			byte[] buffer = new byte[8192 * 10];
//			try (InputStream inputStream = file.getInputStream();
//					WritableByteChannel outChannel = Channels
//							.newChannel(channelSftp.put(remoteFilePath, ChannelSftp.OVERWRITE))) {
//
//				int bytesRead;
//				while ((bytesRead = inputStream.read(buffer)) != -1) {
//					outChannel.write(ByteBuffer.wrap(buffer, 0, bytesRead));
//				}
//			}
//			return "https://quantumshare.quantumparadigm.in/" + directory + filename;
//
//		} catch (Exception ex) {
//			ex.printStackTrace();
//			return "Error: " + ex.getMessage();
//		} finally {
//			if (channelSftp != null) {
//				channelSftp.disconnect();
//			}
//			returnSession(session);
//		}
//	}
//
//	public void shutdown() {
//		executorService.shutdown();
//		while (!sessionPool.isEmpty()) {
//			closeSession(sessionPool.poll());
//		}
//	}
//}
