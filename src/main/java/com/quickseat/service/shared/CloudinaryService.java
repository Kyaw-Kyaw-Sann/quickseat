package com.quickseat.service.shared;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.quickseat.dto.response.movie.ImageUploadResponse;
import com.quickseat.exception.BadRequestException;
import java.io.IOException;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class CloudinaryService {
    private static final long MAX_IMAGE_SIZE = 5 * 1024 * 1024;

    private final Cloudinary cloudinary;

    @Value("${app.cloudinary.enabled:false}")
    private boolean enabled;

    public ImageUploadResponse uploadMoviePoster(MultipartFile file) {
        return uploadImage(file, "quickseat/movies", "movie poster");
    }

    public ImageUploadResponse uploadCinemaImage(MultipartFile file) {
        return uploadImage(file, "quickseat/cinemas", "cinema image");
    }

    private ImageUploadResponse uploadImage(MultipartFile file, String folder, String imageType) {
        validate(file);
        if (!enabled) {
            throw new BadRequestException("Cloudinary upload is not configured");
        }

        try {
            Map<?, ?> result = cloudinary.uploader().upload(file.getBytes(), ObjectUtils.asMap(
                    "folder", folder,
                    "resource_type", "image",
                    "unique_filename", true));
            return new ImageUploadResponse(result.get("secure_url").toString(), result.get("public_id").toString());
        } catch (IOException | RuntimeException exception) {
            log.error("Cloudinary {} upload failed", imageType, exception);
            throw new BadRequestException("Image upload failed");
        }
    }

    private void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Image file is required");
        }
        if (file.getSize() > MAX_IMAGE_SIZE) {
            throw new BadRequestException("Image file must not exceed 5 MB");
        }
        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new BadRequestException("Only image files are allowed");
        }
    }
}
