package com.qp.quantum_share.helper;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.jcraft.jsch.*;

@Service
public class PostOnServer {

    @Autowired
    FileConvertion fileConvertion;

    private static final String SFTP_HOST = "pdx1-shared-a2-03.dreamhost.com";
    private static final int SFTP_PORT = 22;
    private static final String SFTP_USER = "dh_q2m9hh";
    private static final String SFTP_PASSWORD = "SriKrishna@0700";
    private static final String SFTP_DIRECTORY = "/home/dh_q2m9hh/quantumshare.quantumparadigm.in/";

    private static final int THREAD_POOL_SIZE = 10; // Optimal for parallel chunk uploads
    private final ExecutorService executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    private final ConcurrentLinkedQueue<Session> sessionPool = new ConcurrentLinkedQueue<>();

    // Create and cache SFTP sessions
    private Session createSession() throws Exception {
        JSch jsch = new JSch();
        Session session = jsch.getSession(SFTP_USER, SFTP_HOST, SFTP_PORT);
        session.setPassword(SFTP_PASSWORD);

        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        config.put("Compression", "zlib@openssh.com"); // Compression for network efficiency
        config.put("PreferredAuthentications", "password");
        session.setConfig(config);
        session.connect();
        return session;
    }

    private Session getSession() throws Exception {
        Session session = sessionPool.poll();
        return (session != null && session.isConnected()) ? session : createSession();
    }

    private void returnSession(Session session) {
        if (session != null && session.isConnected()) {
            sessionPool.offer(session);
        } else {
            closeSession(session);
        }
    }

    private void closeSession(Session session) {
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }

    public String uploadFile(MultipartFile file, String directory) {
        try {
            Future<String> future = executorService.submit(() -> uploadFileInChunks(file, directory));
            return future.get(); // Wait for the result of the upload
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String uploadFileInChunks(MultipartFile file, String directory) {
        System.err.println("chunck");
    	Session session = null;
        ChannelSftp channelSftp = null;
        try {
            session = getSession();
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            // Generate unique file name
            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename().replaceAll("\\s", "");
            String remoteFilePath = SFTP_DIRECTORY + directory + filename;

            // Split file into chunks and upload in parallel
            byte[] buffer = new byte[8192 * 10]; // Larger buffer for faster transfer
            try (InputStream inputStream = file.getInputStream();
                 WritableByteChannel outChannel = Channels.newChannel(channelSftp.put(remoteFilePath, ChannelSftp.OVERWRITE))) {

                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outChannel.write(ByteBuffer.wrap(buffer, 0, bytesRead));
                }
            }
            return "https://quantumshare.quantumparadigm.in/" + directory + filename;

        } catch (Exception ex) {
            ex.printStackTrace();
            return "Error: " + ex.getMessage();
        } finally {
            if (channelSftp != null) {
                channelSftp.disconnect();
            }
            returnSession(session);
        }
    }

    public void shutdown() {
        executorService.shutdown();
        while (!sessionPool.isEmpty()) {
            closeSession(sessionPool.poll());
        }
    }
}
