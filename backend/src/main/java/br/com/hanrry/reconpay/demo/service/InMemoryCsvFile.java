package br.com.hanrry.reconpay.demo.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/*
 * A generated CSV handed to the import services as if it had been uploaded, so the demo
 * scenario goes through the same parsing and validation as a visitor's file.
 */
public final class InMemoryCsvFile implements MultipartFile {

    private final String fileName;
    private final byte[] content;

    public InMemoryCsvFile(String fileName, String content) {
        this.fileName = fileName;
        this.content = content.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public String getName() {
        return "file";
    }

    @Override
    public String getOriginalFilename() {
        return fileName;
    }

    @Override
    public String getContentType() {
        return "text/csv";
    }

    @Override
    public boolean isEmpty() {
        return content.length == 0;
    }

    @Override
    public long getSize() {
        return content.length;
    }

    @Override
    public byte[] getBytes() {
        return content.clone();
    }

    @Override
    public InputStream getInputStream() {
        return new ByteArrayInputStream(content);
    }

    @Override
    public void transferTo(File dest) throws IOException {
        Files.write(dest.toPath(), content);
    }
}
