package com.quickseat.controller.admin;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.quickseat.dto.response.movie.ImageUploadResponse;
import com.quickseat.service.cinema.CinemaService;
import com.quickseat.service.shared.CloudinaryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AdminCinemaControllerTest {
    private CloudinaryService cloudinaryService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        CinemaService cinemaService = mock(CinemaService.class);
        cloudinaryService = mock(CloudinaryService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new AdminCinemaController(cinemaService, cloudinaryService)).build();
    }

    @Test
    void uploadsCinemaImageUsingFilePart() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "cinema.jpg", "image/jpeg", new byte[]{1, 2, 3});
        when(cloudinaryService.uploadCinemaImage(file)).thenReturn(new ImageUploadResponse(
                "https://res.cloudinary.com/demo/cinema.jpg", "quickseat/cinemas/cinema"));

        mockMvc.perform(multipart("/admin/cinemas/image").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Cinema image uploaded successfully"))
                .andExpect(jsonPath("$.data.publicId").value("quickseat/cinemas/cinema"));

        verify(cloudinaryService).uploadCinemaImage(file);
    }
}
