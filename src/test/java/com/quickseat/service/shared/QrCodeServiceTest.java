package com.quickseat.service.shared;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

class QrCodeServiceTest {
    @Test
    void generatesValidPngImage() throws Exception {
        QrCodeService service = new QrCodeService(240);

        byte[] image = service.generatePng("https://quickseat.test/ticket?token=test-token");

        assertThat(image).isNotEmpty();
        assertThat(ImageIO.read(new ByteArrayInputStream(image))).isNotNull();
    }
}
