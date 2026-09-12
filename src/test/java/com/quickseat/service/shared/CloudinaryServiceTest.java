package com.quickseat.service.shared;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.quickseat.exception.BadRequestException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class CloudinaryServiceTest {

    @Test
    void uploadsValidMoviePoster() throws Exception {
        Cloudinary cloudinary = mock(Cloudinary.class);
        Uploader uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any(Map.class))).thenReturn(Map.of(
                "secure_url", "https://res.cloudinary.com/demo/poster.jpg",
                "public_id", "quickseat/movies/poster"));
        CloudinaryService service = new CloudinaryService(cloudinary);
        ReflectionTestUtils.setField(service, "enabled", true);

        var response = service.uploadMoviePoster(new MockMultipartFile(
                "file", "poster.jpg", "image/jpeg", new byte[]{1, 2, 3}));

        assertThat(response.url()).startsWith("https://res.cloudinary.com/");
        assertThat(response.publicId()).isEqualTo("quickseat/movies/poster");
    }

    @Test
    void uploadsValidCinemaImageToCinemaFolder() throws Exception {
        Cloudinary cloudinary = mock(Cloudinary.class);
        Uploader uploader = mock(Uploader.class);
        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), any(Map.class))).thenReturn(Map.of(
                "secure_url", "https://res.cloudinary.com/demo/cinema.jpg",
                "public_id", "quickseat/cinemas/cinema"));
        CloudinaryService service = new CloudinaryService(cloudinary);
        ReflectionTestUtils.setField(service, "enabled", true);

        var response = service.uploadCinemaImage(new MockMultipartFile(
                "file", "cinema.jpg", "image/jpeg", new byte[]{1, 2, 3}));

        ArgumentCaptor<Map> options = ArgumentCaptor.forClass(Map.class);
        verify(uploader).upload(any(byte[].class), options.capture());
        assertThat(options.getValue()).containsEntry("folder", "quickseat/cinemas");
        assertThat(response.url()).isEqualTo("https://res.cloudinary.com/demo/cinema.jpg");
        assertThat(response.publicId()).isEqualTo("quickseat/cinemas/cinema");
    }

    @Test
    void rejectsEmptyInvalidAndOversizedCinemaImages() {
        CloudinaryService service = new CloudinaryService(mock(Cloudinary.class));
        ReflectionTestUtils.setField(service, "enabled", true);

        assertThatThrownBy(() -> service.uploadCinemaImage(new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[0])))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Image file is required");
        assertThatThrownBy(() -> service.uploadCinemaImage(new MockMultipartFile(
                "file", "note.txt", "text/plain", new byte[]{1})))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Only image files are allowed");
        assertThatThrownBy(() -> service.uploadCinemaImage(new MockMultipartFile(
                "file", "large.jpg", "image/jpeg", new byte[5 * 1024 * 1024 + 1])))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Image file must not exceed 5 MB");
    }

    @Test
    void rejectsInvalidFileAndDisabledConfiguration() {
        CloudinaryService service = new CloudinaryService(mock(Cloudinary.class));
        ReflectionTestUtils.setField(service, "enabled", false);

        assertThatThrownBy(() -> service.uploadMoviePoster(new MockMultipartFile(
                "file", "note.txt", "text/plain", new byte[]{1})))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Only image files are allowed");
        assertThatThrownBy(() -> service.uploadMoviePoster(new MockMultipartFile(
                "file", "poster.jpg", "image/jpeg", new byte[]{1})))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("Cloudinary upload is not configured");
    }
}
