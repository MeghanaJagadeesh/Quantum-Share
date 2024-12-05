package com.qp.quantum_share.services;



import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

@Service
public class TestService {
	
	private static final String SFTP_HOST = "pdx1-shared-a2-03.dreamhost.com";
    private static final int SFTP_PORT = 22;
    private static final String SFTP_USER = "dh_q2m9hh";
    private static final String SFTP_PASSWORD = "SriKrishna@0700";
    private static final String SFTP_DIRECTORY = "/home/dh_q2m9hh/quantumshare.quantumparadigm.in/posts/";

    private final ExecutorService executorService = Executors.newFixedThreadPool(8); // Increased threads for higher parallelism
    private final ConcurrentLinkedQueue<Session> sessionPool = new ConcurrentLinkedQueue<>();

    private Session createSession() throws Exception {
        JSch jsch = new JSch();
        Session session = jsch.getSession(SFTP_USER, SFTP_HOST, SFTP_PORT);
        session.setPassword(SFTP_PASSWORD);

        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        config.put("Compression", "zlib@openssh.com");
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

    
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            Future<ResponseEntity<String>> future = executorService.submit(() -> uploadFileToSftp(file));
            return future.get();
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>("Error: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private ResponseEntity<String> uploadFileToSftp(MultipartFile file) {
        Session session = null;
        ChannelSftp channelSftp = null;
        try {
            session = getSession();
            channelSftp = (ChannelSftp) session.openChannel("sftp");
            channelSftp.connect();

            String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
            String uniqueFileName = SFTP_DIRECTORY + filename;

            try (InputStream inputStream = file.getInputStream();
                 WritableByteChannel outChannel = Channels.newChannel(channelSftp.put(uniqueFileName, ChannelSftp.OVERWRITE))) {
                
                // Streaming file content with minimal buffer overhead
                inputStream.transferTo(Channels.newOutputStream(outChannel));

                return new ResponseEntity<>("File uploaded successfully: https://quantumshare.quantumparadigm.in/posts/" + filename, HttpStatus.OK);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            return new ResponseEntity<>("Error: " + ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        } finally {
            if (channelSftp != null) channelSftp.disconnect();
            returnSession(session);
        }
    }

}
