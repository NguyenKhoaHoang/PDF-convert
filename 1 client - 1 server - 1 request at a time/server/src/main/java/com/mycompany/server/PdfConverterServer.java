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
import java.net.ServerSocket;
import java.net.Socket;
import java.util.UUID;

public class PdfConverterServer {

    private final int SERVER_PORT = 9004;
    private final String PDF_EXT = "pdf";
    private final String DOC_EXT = "docx";
    private final String TMP_FOLDER = "tmp";
    private ObjectOutputStream oos = null;
    private ObjectInputStream ois = null;
    private ServerSocket serverSocket = null;
    private Socket socket = null;

    public static void main(String[] args) throws IOException {
        PdfConverterServer pdfConverterServer = new PdfConverterServer();
        pdfConverterServer.open();
        pdfConverterServer.waitAndProcessRequestFromClient();
    }

    private void open() {
        try {
            serverSocket = new ServerSocket(SERVER_PORT);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Cannot create socket server on port: " + SERVER_PORT);
        }
    }

    private void waitAndProcessRequestFromClient() {
        while (true) {
            try {
                System.out.println("Waiting for client");
                socket = serverSocket.accept();
                oos = new ObjectOutputStream(socket.getOutputStream());
                ois = new ObjectInputStream(socket.getInputStream());
                FileInfo receivedFromClient = receiveFileFromClient();
                saveFile(receivedFromClient);
                assert receivedFromClient != null;
                FileInfo convertedFile;
                if(isPdfFile(receivedFromClient)) {
                    convertedFile = convertFileToDoc(receivedFromClient);
                } else {
                    convertedFile = convertFileToPdf(receivedFromClient);
                }
                oos.writeObject(convertedFile);
                closeSocketConnectionToClient();
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Socket connection error");
            }
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
            FileUtils.writeByteArrayToFile(new File(TMP_FOLDER + "/" + fileInfo.getFileName()), fileInfo.getDataBytes());
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
        String pdfFilePath = TMP_FOLDER + "/" + fileInfo.getFileName() + "_" + genTxId() + "." + PDF_EXT;
        convertToPDF(TMP_FOLDER + "/" + fileInfo.getFileName(), pdfFilePath);
        return readFile(pdfFilePath);
    }

    private FileInfo convertFileToDoc(FileInfo fileInfo) {
        System.out.println("Converting " + fileInfo.getFileName() + " to DOCX");
        String docxFilePath = TMP_FOLDER + "/" + fileInfo.getFileName() + "_" + genTxId() + "." + DOC_EXT;
        Document doc = new Document(TMP_FOLDER + "/" + fileInfo.getFileName());
        DocSaveOptions saveOptions = new DocSaveOptions();
        // Set output file format as DOCX
        saveOptions.setFormat(DocSaveOptions.DocFormat.DocX);
        saveOptions.setMode(DocSaveOptions.RecognitionMode.Flow);
        saveOptions.setRelativeHorizontalProximity(2.5f);
        saveOptions.setRecognizeBullets(true);
        // Save resultant DOCX file
        doc.save(docxFilePath, saveOptions);
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
            throw new RuntimeException("Cannot close connection - PORT: " + SERVER_PORT);
        }
    }

}
