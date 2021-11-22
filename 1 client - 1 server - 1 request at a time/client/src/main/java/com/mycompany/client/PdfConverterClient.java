package com.mycompany.client;

import com.mycompany.common.FileInfo;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

public class PdfConverterClient {

    private String serverHost = "localhost";
    private int serverPort = 9004;
    private ObjectOutputStream oos = null;
    private ObjectInputStream ois = null;
    private Socket socketOfClient = null;

    public PdfConverterClient(String serverHost, int serverPort) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
    }

    public void createSocketConnectionToServer() {
        try {
            socketOfClient = new Socket(serverHost, serverPort);
            oos = new ObjectOutputStream(socketOfClient.getOutputStream());
            ois = new ObjectInputStream(socketOfClient.getInputStream());
        } catch (UnknownHostException e) {
            e.printStackTrace();
            throw new RuntimeException("Don't know about host: " + serverHost + " : " + serverPort);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Couldn't get I/O for the connection to " + serverHost + " : " + serverPort);
        }
    }

    public void closeSocketConnectionToServer() {
        try {
            if (socketOfClient != null) {
                socketOfClient.close();
            }
            if (oos != null) {
                oos.close();
            }
            if (ois != null) {
                ois.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Cannot close connection: " + serverHost + " : " + serverPort);
        }
    }

    private FileInfo readFile(String filePath) {
        FileInfo fileInfo;
        BufferedInputStream bis = null;
        try {
            File file = new File(filePath);
            bis = new BufferedInputStream(new FileInputStream(file));
            fileInfo = new FileInfo();
            byte[] fileBytes = new byte[(int) file.length()];
            int u = bis.read(fileBytes, 0, fileBytes.length);
            fileInfo.setFileName(file.getName());
            fileInfo.setDataBytes(fileBytes);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("File not found: " + filePath);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Cannot read file: " + filePath);
        } finally {
            if (bis != null) {
                try {
                    bis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return fileInfo;
    }
    
    private final String PDF_EXT = "pdf";
    private final String DOC_EXT = "docx";
    private final String TMP_FOLDER = "tmp";
    
    private boolean isPdfFile(FileInfo fileInfo) {
        String fileExt = FilenameUtils.getExtension(fileInfo.getFileName()).toLowerCase();
        System.out.println("File " + fileInfo.getFileName() + " - Ext: " + fileExt + " - Is PDF: " + fileExt.equals(PDF_EXT));
        return fileExt.equals(PDF_EXT);
    }
    
    private boolean isDocxFile(FileInfo fileInfo) {
        String fileExt = FilenameUtils.getExtension(fileInfo.getFileName()).toLowerCase();
        System.out.println("File " + fileInfo.getFileName() + " - Ext: " + fileExt + " - Is Docx: " + fileExt.equals(DOC_EXT));
        return fileExt.equals(DOC_EXT);
    }
    
    private void saveFile(FileInfo fileInfo) {
        if (fileInfo != null) {
            System.out.println("Writing file " + fileInfo.getFileName() + " to " + TMP_FOLDER + "/" + fileInfo.getFileName());
            try {
                FileUtils.writeByteArrayToFile(new File(TMP_FOLDER + "/" + fileInfo.getFileName()), fileInfo.getDataBytes());
            } catch (IOException ex) {
                ex.printStackTrace();
                throw new RuntimeException("Save converted file from server error: " + ex.getMessage());
            }
        }
        System.out.println("File: to save is null");
    }

    private FileInfo receiveFileFromServer() {
        try {
            FileInfo fileInfo = (FileInfo) ois.readObject();
            if (fileInfo != null) {
                System.out.println("Received file from server: " + fileInfo.getFileName() + " " + fileInfo.getDataBytes().length + " bytes.");
                return fileInfo;
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("[Receiving file from server] Error: " + e.getMessage());
        }
        return null;
    }
    
    public String sendFileToServer(String filePath) {
        FileInfo fileInfo = readFile(filePath);
        if (fileInfo != null && (isPdfFile(fileInfo) || isDocxFile(fileInfo))) {
            System.out.println("Sending: " + filePath + " to server");
            try {
                oos.writeObject(fileInfo);
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Cannot send file : " + filePath + " to server. I/O Exception");
            }
            FileInfo convertedFile = receiveFileFromServer();
            saveFile(convertedFile);
            return TMP_FOLDER + "/" + convertedFile.getFileName();
        } else {
            throw new RuntimeException("File: " + filePath + " is not a valid file (or not pdf or docx file)");
        }
    }

}
