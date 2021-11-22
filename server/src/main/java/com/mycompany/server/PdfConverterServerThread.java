package com.mycompany.server;

import com.aspose.pdf.DocSaveOptions;
import com.aspose.pdf.Document;
import com.mycompany.common.FileInfo;
import fr.opensagres.poi.xwpf.converter.pdf.PdfConverter;
import fr.opensagres.poi.xwpf.converter.pdf.PdfOptions;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.UUID;
import com.spire.pdf.FileFormat;  
import com.spire.pdf.PdfDocument; 

public class PdfConverterServerThread extends Thread {

    private final String PDF_EXT = "pdf";
    private final String DOC_EXT = "docx";
    private final String TMP_FOLDER = "tmp";
    private ObjectOutputStream oos = null;
    private ObjectInputStream ois = null;
    private Socket socket = null;
    private String filePath = "";
    private String pdfFilePath = "";
    private String docxFilePath = "";

    public PdfConverterServerThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        processRequestFromClient();
    }

    private void processRequestFromClient() {
        try {
            System.out.println("Start process request from client");
            oos = new ObjectOutputStream(socket.getOutputStream());
            ois = new ObjectInputStream(socket.getInputStream());
            FileInfo receivedFromClient = receiveFileFromClient();
            saveFile(receivedFromClient);
            assert receivedFromClient != null;
            FileInfo convertedFile;
            if (isPdfFile(receivedFromClient)) {
                convertedFile = convertFileToDoc(receivedFromClient);
            } else {
                convertedFile = convertFileToPdf(receivedFromClient);
            }
            oos.writeObject(convertedFile);
            DelFile();
            closeSocketConnectionToClient();
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Socket connection error");
        }
    }

    private void DelFile() {
        try {
            if (filePath != "") {
                if (Files.deleteIfExists(Paths.get(filePath))) {
                    System.out.println("File deleted successfully");
                } else {
                    System.out.println("Failed to delete the file");
                }
            }
            if (pdfFilePath != "") {
                if (Files.deleteIfExists(Paths.get(pdfFilePath))) {
                    System.out.println("File PDF deleted successfully");
                } else {
                    System.out.println("Failed to delete the file PDF");
                }
            } else if (docxFilePath != "") {
                if (Files.deleteIfExists(Paths.get(docxFilePath))) {
                    System.out.println("File DOC deleted successfully");
                } else {
                    System.out.println("Failed to delete the file DOC");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private FileInfo receiveFileFromClient() {
        try {
            FileInfo fileInfo = (FileInfo) ois.readObject();
            if (fileInfo != null) {
                System.out.println("Received file from client: " + fileInfo.getFileName() + " " + fileInfo.getDataBytes().length + " bytes.");
                return fileInfo;
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException("[Receiving file from client] Error: " + e.getMessage());
        }
        return null;
    }

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

    private void saveFile(FileInfo fileInfo) throws IOException {
        if (fileInfo != null) {
            System.out.println("Writing file " + fileInfo.getFileName() + " to " + TMP_FOLDER + "/" + fileInfo.getFileName());
            filePath = TMP_FOLDER + "/" + fileInfo.getFileName();
            FileUtils.writeByteArrayToFile(new File(filePath), fileInfo.getDataBytes());
        }
    }

    private synchronized String genTxId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private void convertToPDF(String docPath, String pdfPath) {
        try {
            InputStream doc = new FileInputStream(new File(docPath));
            XWPFDocument document = new XWPFDocument(doc);
            PdfOptions options = PdfOptions.create();
            OutputStream out = new FileOutputStream(new File(pdfPath));
            PdfConverter.getInstance().convert(document, out, options);
            doc.close();
            document.close();
            out.close();
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
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

    private FileInfo convertFileToPdf(FileInfo fileInfo) {
        System.out.println("Converting " + fileInfo.getFileName() + " to PDF");
        pdfFilePath = TMP_FOLDER + "/" + fileInfo.getFileName() + "_" + genTxId() + "." + PDF_EXT;
        convertToPDF(TMP_FOLDER + "/" + fileInfo.getFileName(), pdfFilePath);
        return readFile(pdfFilePath);
    }

    private FileInfo convertFileToDoc(FileInfo fileInfo) {
        System.out.println("Converting " + fileInfo.getFileName() + " to DOCX");
        docxFilePath = TMP_FOLDER + "/" + fileInfo.getFileName() + "_" + genTxId() + "." + DOC_EXT;
        PdfDocument pdf = new PdfDocument();
        pdf.loadFromFile(TMP_FOLDER + "/" + fileInfo.getFileName());
        pdf.saveToFile(docxFilePath,FileFormat.DOCX);
        pdf.close(); 
        return readFile(docxFilePath);
    }

    private void closeSocketConnectionToClient() {
        try {
            if (socket != null) {
                socket.close();
            }
            if (oos != null) {
                oos.close();
            }
            if (ois != null) {
                ois.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Cannot close connection");
        }
    }

}
