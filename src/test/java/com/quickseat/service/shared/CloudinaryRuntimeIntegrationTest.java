package com.quickseat.service.shared;

import static org.assertj.core.api.Assertions.assertThat;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.quickseat.dto.response.movie.ImageUploadResponse;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

@SpringBootTest
@EnabledIfEnvironmentVariable(named = "CLOUDINARY_RUNTIME_TEST", matches = "true")
class CloudinaryRuntimeIntegrationTest {
    private static final byte[] ONE_PIXEL_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+A8AAQUBAScY42YAAAAASUVORK5CYII=");

    @Autowired CloudinaryService cloudinaryService;
    @Autowired Cloudinary cloudinary;

    @Test
    void uploadsCinemaImageToCloudinaryAndRemovesTestAsset() throws Exception {
        ImageUploadResponse uploaded = null;
        try {
            uploaded = cloudinaryService.uploadCinemaImage(new MockMultipartFile(
                    "file", "cinema-runtime-test.png", "image/png", ONE_PIXEL_PNG));

            assertThat(uploaded.url()).startsWith("https://res.cloudinary.com/");
            assertThat(uploaded.publicId()).startsWith("quickseat/cinemas/");
        } finally {
            if (uploaded != null) {
                cloudinary.uploader().destroy(uploaded.publicId(), ObjectUtils.asMap(
                        "resource_type", "image",
                        "invalidate", true));
            }
        }
    }
}
