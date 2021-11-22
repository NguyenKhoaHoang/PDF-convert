package com.mycompany.common;

import java.io.Serializable;

public class FileInfo implements Serializable {

    private static final long serialVersionUID = -2275848513899623575L;

    private String fileName;
    private byte[] dataBytes;

    public FileInfo() {

    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public byte[] getDataBytes() {
        return dataBytes;
    }

    public void setDataBytes(byte[] dataBytes) {
        this.dataBytes = dataBytes;
    }

}
