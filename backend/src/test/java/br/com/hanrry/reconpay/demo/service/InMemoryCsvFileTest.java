package br.com.hanrry.reconpay.demo.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryCsvFileTest {

    private static final String CONTENT = "externalReference,amount\nDEMO-0001,149.90\nAção,1.00\n";
    private static final byte[] UTF8 = CONTENT.getBytes(StandardCharsets.UTF_8);

    private final InMemoryCsvFile file = new InMemoryCsvFile("transacoes-demo.csv", CONTENT);

    @Test
    void shouldExposeTheContentAsUtf8Bytes() throws Exception {
        assertThat(file.getBytes()).isEqualTo(UTF8);
        try (InputStream in = file.getInputStream()) {
            assertThat(in.readAllBytes()).isEqualTo(UTF8);
        }
        assertThat(file.getSize()).isEqualTo(UTF8.length);
        assertThat(file.isEmpty()).isFalse();
    }

    @Test
    void shouldExposeTheFileNameAndCsvContentType() {
        assertThat(file.getOriginalFilename()).isEqualTo("transacoes-demo.csv");
        assertThat(file.getContentType()).isEqualTo("text/csv");
    }

    @Test
    void emptyContentShouldBeEmpty() {
        InMemoryCsvFile empty = new InMemoryCsvFile("vazio.csv", "");

        assertThat(empty.isEmpty()).isTrue();
        assertThat(empty.getSize()).isZero();
    }

    @Test
    void transferToShouldWriteTheContentToTheFile(@TempDir Path dir) throws Exception {
        File target = dir.resolve("copia.csv").toFile();

        file.transferTo(target);

        assertThat(Files.readAllBytes(target.toPath())).isEqualTo(UTF8);
    }
}
