package com.example.learnservice.util;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.SecretKeySpec;
import javax.imageio.ImageIO;

import org.apache.commons.io.FilenameUtils;
import org.apache.pdfbox.cos.COSName;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.AccessPermission;
import org.apache.pdfbox.pdmodel.encryption.StandardProtectionPolicy;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.util.Matrix;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.XMPDM;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.example.learnservice.enums.DocumentFormat;
import com.example.learnservice.model.Document;

import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.builder.FFmpegBuilder;

@Slf4j
@Component
public class FileUtil {

    @Value("${file.upload.dir:./uploads}")
    private String uploadDir;

    @Value("${temp.dir:D:/temp}")
    private String tempRepo;

    @Value("${file.encryption.key}")
    private String encryptionKey;

    @Value("${ffmpeg.path:ffmpeg}")
    private String ffmpegPath;

    private final Tika tika = new Tika();

    // Allowed MIME types
    private final Set<String> allowedMimeTypes = Set.of(
            "application/pdf",
            "video/mp4",
            "audio/mp3",
            "video/avi",
            "video/quicktime",
            "video/x-msvideo");

    public Document validateFile(MultipartFile file) throws IOException {
        // Validate file
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // Detect MIME type using Tika
        String detectedMimeType = tika.detect(file.getInputStream());
        if (!allowedMimeTypes.contains(detectedMimeType)) {
            throw new IllegalArgumentException("File type not allowed: " + detectedMimeType);
        }

        // Sanitize filename using Commons IO
        String originalFilename = file.getOriginalFilename();
        String sanitizedName = FilenameUtils.getName(originalFilename); // Chống path traversal

        if (sanitizedName == null || sanitizedName.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid filename");
        }

        // Lấy extension từ tên file (nếu có)
        String ext = FilenameUtils.getExtension(sanitizedName);
        String expectedExtFromMime = switch (detectedMimeType) {
            case "application/pdf" -> "pdf";
            case "video/mp4" -> "mp4";
            case "video/avi", "video/x-msvideo" -> "avi";
            case "video/quicktime" -> "mov";
            case "audio/mpeg" -> "mp3";
            default -> "bin";
        };
        if (ext == null || ext.isBlank()) {
            sanitizedName = sanitizedName + "." + expectedExtFromMime;
        }
        if (!ext.equalsIgnoreCase(expectedExtFromMime)) {
            sanitizedName = FilenameUtils.removeExtension(sanitizedName) + "." + expectedExtFromMime;
        }

        Document document = new Document();
        document.setName(sanitizedName);
        DocumentFormat format = switch (detectedMimeType) {
            case "application/pdf" -> DocumentFormat.PDF;
            case "video/mp4", "video/avi", "video/quicktime", "video/x-msvideo", "audio/mpeg" -> DocumentFormat.VIDEO;
            default -> throw new IllegalArgumentException("File type not allowed: " + detectedMimeType);
        };
        document.setFormat(format);
        document.setSize(file.getSize());

        return document;
    }

    public void analyzeFileContent(MultipartFile file, Document document) {
        try {
            if (document.getFormat() == DocumentFormat.PDF) {
                // Phân tích PDF để lấy số trang
                int pageCount = analyzePdfPages(file);
                document.setPages(pageCount);
                log.info("PDF analysis completed - Pages: {}", pageCount);
            } else if (document.getFormat() == DocumentFormat.VIDEO) {
                // Phân tích video để lấy duration (phút)
                Integer durationMinutes = analyzeVideoDuration(file);
                document.setMinutes(durationMinutes);
                log.info("Video analysis completed - Duration: {} minutes", durationMinutes);
            }
        } catch (Exception e) {
            log.warn("Failed to analyze file content for {}: {}", document.getName(), e.getMessage());
            // Không throw exception để không fail upload, chỉ log warning
        }
    }

    private int analyzePdfPages(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream();
                PDDocument document = PDDocument.load(inputStream)) {
            return document.getNumberOfPages();
        } catch (Exception e) {
            log.error("Failed to analyze PDF pages: {}", e.getMessage());
            return 0; // Return 0 if analysis fails
        }
    }

    private Integer analyzeVideoDuration(MultipartFile file) {
        try (InputStream inputStream = file.getInputStream()) {
            AutoDetectParser parser = new AutoDetectParser();
            BodyContentHandler handler = new BodyContentHandler(-1); // No limit
            Metadata metadata = new Metadata();
            ParseContext parseContext = new ParseContext();

            parser.parse(inputStream, handler, metadata, parseContext);

            // Try to get duration from metadata
            String duration = metadata.get(XMPDM.DURATION);
            if (duration != null) {
                try {
                    // Duration might be in seconds, convert to minutes
                    double durationSeconds = Double.parseDouble(duration);
                    return (int) Math.ceil(durationSeconds / 60.0);
                } catch (NumberFormatException e) {
                    log.warn("Could not parse duration: {}", duration);
                }
            }

            // Alternative metadata fields for duration
            String[] durationFields = {
                    "duration", "Duration", "DURATION",
                    "xmpDM:duration", "Content-Duration"
            };

            for (String field : durationFields) {
                String value = metadata.get(field);
                if (value != null) {
                    try {
                        double seconds = Double.parseDouble(value);
                        return (int) Math.ceil(seconds / 60.0);
                    } catch (NumberFormatException e) {
                        // Try next field
                        continue;
                    }
                }
            }

            log.warn("Could not extract video duration from metadata");
            return null;

        } catch (Exception e) {
            log.error("Failed to analyze video duration: {}", e.getMessage());
            return null;
        }
    }

    public String encryptFile(MultipartFile file, Document document) throws Exception {
        // Tạo thư mục doc nếu chưa tồn tại
        File docDirectory = new File(uploadDir, "doc");
        if (!docDirectory.exists()) {
            docDirectory.mkdirs();
        }

        InputStream inputStream = file.getInputStream();
        String extension = FilenameUtils.getExtension(document.getName());
        String fileName = document.getCode() + "." + extension;
        Path filePath = Paths.get(uploadDir, "doc", fileName);
        File outputFile = filePath.toFile();

        Cipher cipher = Cipher.getInstance("AES");
        SecretKeySpec keySpec = new SecretKeySpec(encryptionKey.getBytes(), "AES");
        cipher.init(Cipher.ENCRYPT_MODE, keySpec);

        try (FileOutputStream fos = new FileOutputStream(outputFile);
                CipherOutputStream cos = new CipherOutputStream(fos, cipher)) {

            byte[] buffer = new byte[8192]; // 8KB buffer
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                cos.write(buffer, 0, bytesRead);
            }
        }

        return filePath.toString();
    }

    public String generatePreview(MultipartFile file, Document document) throws Exception {
        String documentCode = document.getCode();
        DocumentFormat format = document.getFormat();
        // Tạo thư mục preview nếu chưa tồn tại
        File previewDirectory = new File(uploadDir, "preview");
        if (!previewDirectory.exists()) {
            previewDirectory.mkdirs();
        }

        String previewFileName = documentCode + ".jpg";
        Path previewPath = Paths.get(uploadDir, "preview", previewFileName);

        try {
            if (format == DocumentFormat.PDF) {
                generatePdfPreview(file, previewPath);
            } else if (format == DocumentFormat.VIDEO) {
                generateVideoPreview(file, previewPath);
            }

            log.info("Preview generated successfully: {}", previewPath);
            return previewPath.toString();
        } catch (Exception e) {
            log.error("Failed to generate preview for {}: {}", documentCode, e.getMessage());
            // Tạo preview mặc định nếu không tạo được
            return createDefaultPreview(previewPath, format);
        }
    }

    private void generatePdfPreview(MultipartFile file, Path previewPath) throws Exception {
        try (InputStream inputStream = file.getInputStream();
                PDDocument document = PDDocument.load(inputStream)) {

            PDFRenderer pdfRenderer = new PDFRenderer(document);
            BufferedImage bufferedImage = pdfRenderer.renderImageWithDPI(0, 150); // Render trang đầu tiên

            // Resize về kích thước chuẩn
            BufferedImage resizedImage = resizeImage(bufferedImage, 300, 200);
            ImageIO.write(resizedImage, "jpg", previewPath.toFile());
        }
    }

    private void generateVideoPreview(MultipartFile file, Path previewPath) throws Exception {
        // Tạo temp file cho video input
        File tempVideoFile = File.createTempFile("temp_video", ".tmp");

        try {
            // Copy MultipartFile to temp file
            try (InputStream inputStream = file.getInputStream();
                    FileOutputStream fos = new FileOutputStream(tempVideoFile)) {
                inputStream.transferTo(fos);
            }

            // Tạo thumbnail bằng FFmpeg
            createVideoThumbnailWithFFmpeg(tempVideoFile.getAbsolutePath(), previewPath.toString());

            log.info("Successfully created video thumbnail with FFmpeg");

        } catch (Exception e) {
            log.warn("Failed to create video thumbnail with FFmpeg: {}", e.getMessage());
            // Fallback to simple icon
            createSimpleVideoIcon(previewPath);
        } finally {
            // Cleanup temp file
            if (tempVideoFile.exists())
                tempVideoFile.delete();
        }
    }

    private void createVideoThumbnailWithFFmpeg(String videoPath, String outputPath) throws Exception {
        try {
            // Initialize FFmpeg với path Windows
            FFmpeg ffmpeg = new FFmpeg(ffmpegPath);

            // Build FFmpeg command
            FFmpegBuilder builder = new FFmpegBuilder()
                    .setInput(videoPath)
                    .overrideOutputFiles(true)
                    .addOutput(outputPath)
                    .setVideoFilter("scale=300:200:force_original_aspect_ratio=decrease,pad=300:200:-1:-1:color=black")
                    .setFrames(1) // Extract only 1 frame
                    .setStartOffset(1, java.util.concurrent.TimeUnit.SECONDS) // Start at 1 second (safer than 5s)
                    .done();

            // Execute FFmpeg command
            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg);
            executor.createJob(builder).run();

        } catch (Exception e) {
            log.error("FFmpeg execution failed: {}", e.getMessage());
            throw e;
        }
    }

    private void createSimpleVideoIcon(Path previewPath) throws Exception {
        BufferedImage image = new BufferedImage(300, 200, BufferedImage.TYPE_INT_RGB);
        var graphics = image.createGraphics();

        // Gradient background
        GradientPaint gradient = new GradientPaint(
                0, 0, Color.decode("#1976D2"),
                300, 200, Color.decode("#42A5F5"));
        graphics.setPaint(gradient);
        graphics.fillRect(0, 0, 300, 200);

        // Play button
        graphics.setColor(Color.WHITE);
        int[] xPoints = { 110, 110, 190 };
        int[] yPoints = { 60, 140, 100 };
        graphics.fillPolygon(xPoints, yPoints, 3);

        // Circle around play button
        graphics.setStroke(new BasicStroke(3));
        graphics.drawOval(85, 55, 130, 90);

        graphics.dispose();
        ImageIO.write(image, "jpg", previewPath.toFile());
    }

    private BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        // Tính toán tỷ lệ để giữ nguyên aspect ratio
        int originalWidth = originalImage.getWidth();
        int originalHeight = originalImage.getHeight();

        double widthRatio = (double) targetWidth / originalWidth;
        double heightRatio = (double) targetHeight / originalHeight;
        double ratio = Math.min(widthRatio, heightRatio);

        int newWidth = (int) (originalWidth * ratio);
        int newHeight = (int) (originalHeight * ratio);

        BufferedImage resizedImage = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        var graphics = resizedImage.createGraphics();

        // Nền đen
        graphics.setColor(java.awt.Color.BLACK);
        graphics.fillRect(0, 0, targetWidth, targetHeight);

        // Vẽ ảnh ở giữa
        int x = (targetWidth - newWidth) / 2;
        int y = (targetHeight - newHeight) / 2;
        graphics.drawImage(originalImage.getScaledInstance(newWidth, newHeight, java.awt.Image.SCALE_SMOOTH),
                x, y, null);

        graphics.dispose();
        return resizedImage;
    }

    private String createDefaultPreview(Path previewPath, DocumentFormat format) {
        try {
            BufferedImage image = new BufferedImage(300, 200, BufferedImage.TYPE_INT_RGB);
            var graphics = image.createGraphics();

            if (format == DocumentFormat.PDF) {
                graphics.setColor(Color.decode("#FFEBEE"));
                graphics.fillRect(0, 0, 300, 200);
                graphics.setColor(Color.decode("#D32F2F"));
                graphics.drawRect(0, 0, 299, 199);
                graphics.setFont(new Font("Arial", Font.BOLD, 24));
                graphics.drawString("PDF", 125, 105);
            } else {
                graphics.setColor(Color.decode("#E3F2FD"));
                graphics.fillRect(0, 0, 300, 200);
                graphics.setColor(Color.decode("#1976D2"));
                graphics.drawRect(0, 0, 299, 199);
                graphics.setFont(new Font("Arial", Font.BOLD, 24));
                graphics.drawString("VIDEO", 110, 105);
            }

            graphics.dispose();
            ImageIO.write(image, "jpg", previewPath.toFile());
            return previewPath.toString();
        } catch (Exception e) {
            log.error("Failed to create default preview: {}", e.getMessage());
            return null;
        }
    }

    public byte[] decryptFile(File inputFile) throws Exception {
        Cipher cipher = Cipher.getInstance("AES");
        SecretKeySpec keySpec = new SecretKeySpec(encryptionKey.getBytes(), "AES");
        cipher.init(Cipher.DECRYPT_MODE, keySpec);

        try (FileInputStream fis = new FileInputStream(inputFile);
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                byte[] decrypted = cipher.update(buffer, 0, bytesRead);
                if (decrypted != null) {
                    baos.write(decrypted);
                }
            }
            byte[] finalDecrypted = cipher.doFinal();
            if (finalDecrypted != null) {
                baos.write(finalDecrypted);
            }
            return baos.toByteArray();
        }
    }

    /**
     * Thêm watermark động (theo CCCD) vào PDF
     */
    public byte[] addWatermark(byte[] pdfBytes, String cccd) throws IOException {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfBytes));
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            for (PDPage page : document.getPages()) {
                PDRectangle mediaBox = page.getMediaBox();
                float pageWidth = mediaBox.getWidth();
                float pageHeight = mediaBox.getHeight();

                try (PDPageContentStream cs = new PDPageContentStream(document, page,
                        PDPageContentStream.AppendMode.APPEND, true, true)) {

                    // Trạng thái trong suốt
                    PDExtendedGraphicsState gs = new PDExtendedGraphicsState();
                    gs.setNonStrokingAlphaConstant(0.4f);
                    COSName gsName = COSName.getPDFName("TransparentGS");
                    page.getResources().put(gsName, gs);
                    cs.setGraphicsStateParameters(gs);

                    // Lấy timestamp theo giờ Việt Nam
                    ZonedDateTime nowVN = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
                    String formattedTime = nowVN.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

                    // Tạo watermark
                    String watermark = "CCCD: " + cccd + " - " + formattedTime;
                    PDFont font = PDType1Font.HELVETICA_OBLIQUE;
                    float fontSize = 18f;

                    // Lặp watermark theo dạng caro, nhưng căn giữa chữ tại điểm grid
                    for (float gridX = 0; gridX < pageWidth; gridX += 300) {
                        for (float gridY = 0; gridY < pageHeight; gridY += 200) {
                            float x = gridX + 20;
                            float y = gridY + 20;

                            cs.beginText();
                            cs.setFont(font, fontSize);
                            cs.setNonStrokingColor(new Color(180, 180, 180));
                            cs.setTextMatrix(Matrix.getRotateInstance(Math.toRadians(45), x, y));
                            cs.showText(watermark);
                            cs.endText();
                        }
                    }
                }
            }
            // thêm quy tắc bảo mật trước khi trả tài liệu cho user
            applyPdfSecurity(document);

            document.save(baos);
            return baos.toByteArray();
        }
    }

    /**
     * Thêm watermark động cho video với vị trí thay đổi theo thời gian
     */
    public byte[] addVideoWatermark(byte[] videoBytes, String cccd) throws Exception {
        // Tạo temp dir cố định
        File tempDir = new File(tempRepo);
        if (!tempDir.exists())
            tempDir.mkdirs();

        File tempInputFile = File.createTempFile("input", ".mp4", tempDir);
        File tempOutputFile = File.createTempFile("output", ".mp4", tempDir);

        try {
            // Write input video to temp file
            Files.write(tempInputFile.toPath(), videoBytes);

            // Lấy timestamp theo giờ Việt Nam
            ZonedDateTime nowVN = ZonedDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
            String formattedTime = nowVN.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String watermarkText = "CCCD: " + cccd + " - " + formattedTime;

            // Create watermark với vị trí động
            String watermarkFilter = createDynamicWatermarkFilter(watermarkText);

            // Initialize FFmpeg
            FFmpeg ffmpeg = new FFmpeg(ffmpegPath);

            // Build FFmpeg command với watermark động
            FFmpegBuilder builder = new FFmpegBuilder()
                    .setInput(tempInputFile.getAbsolutePath())
                    .overrideOutputFiles(true)
                    .addOutput(tempOutputFile.getAbsolutePath())
                    .setVideoFilter(watermarkFilter)
                    .setVideoCodec("libx264")
                    .setAudioCodec("copy") // Copy audio without re-encoding
                    .setFormat("mp4")
                    .done();

            // Execute FFmpeg command
            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg);
            executor.createJob(builder).run();

            // Read watermarked video
            return Files.readAllBytes(tempOutputFile.toPath());

        } finally {
            // Cleanup temp files
            if (tempInputFile.exists())
                tempInputFile.delete();
            if (tempOutputFile.exists())
                tempOutputFile.delete();
        }
    }

    /**
     * Tạo filter động cho watermark video - vị trí thay đổi đơn giản
     */
    private String createDynamicWatermarkFilter(String text) {
        // Escape text
        String escapedText = text.replace(":", "\\:")
                .replace("'", "\\'");

        // Watermark nhảy giữa 4 góc màn hình mỗi 3 giây
        return String.format(
                "drawtext=text='%s':" +
                        "fontsize=20:" +
                        "fontcolor=white@0.8:" +
                        "box=1:" +
                        "boxcolor=black@0.5:" +
                        "boxborderw=3:" +
                        "x='if(lt(mod(t,12),3), 20, if(lt(mod(t,12),6), main_w-text_w-20, if(lt(mod(t,12),9), 20, main_w-text_w-20)))':"
                        +
                        "y='if(lt(mod(t,12),3), 20, if(lt(mod(t,12),6), 20, if(lt(mod(t,12),9), main_h-text_h-20, main_h-text_h-20)))'",
                escapedText);
    }

    /**
     * Tạo watermark tĩnh đơn giản hơn (fallback)
     */
    private String createStaticWatermarkFilter(String text) {
        return "drawtext=" +
                "text='" + text.replace("'", "\\'") + "':" +
                "fontsize=20:" +
                "fontcolor=white@0.6:" +
                "bordercolor=black@0.8:" +
                "borderw=2:" +
                "x=10:" +
                "y=10";
    }

    /**
     * Áp dụng bảo mật cơ bản cho PDF - chặn copy, print, modify
     */
    private void applyPdfSecurity(PDDocument document) {
        try {
            // Tạo AccessPermission - chặn các thao tác
            AccessPermission ap = new AccessPermission();
            ap.setCanExtractContent(false); // Chặn copy text
            ap.setCanPrint(false); // Chặn print
            ap.setCanModify(false); // Chặn sửa
            ap.setCanModifyAnnotations(false); // Chặn sửa annotation
            ap.setCanFillInForm(false); // Chặn fill form

            // Tạo protection policy không cần password
            StandardProtectionPolicy spp = new StandardProtectionPolicy("", "", ap);
            spp.setEncryptionKeyLength(128);

            // Áp dụng bảo vệ
            document.protect(spp);

        } catch (Exception e) {
            log.warn("Failed to apply PDF security: {}", e.getMessage());
            // Không throw exception để không làm fail quá trình watermark
        }
    }

    public String uploadNotEncryptFile(MultipartFile file, String documentCode) throws Exception {
        // Tạo thư mục doc nếu chưa tồn tại
        File docDirectory = new File(uploadDir, "doc");
        if (!docDirectory.exists()) {
            docDirectory.mkdirs();
        }

        InputStream inputStream = file.getInputStream();
        String extension = FilenameUtils.getExtension(file.getOriginalFilename());
        String fileName = documentCode + "." + extension;
        Path filePath = Paths.get(uploadDir, "doc", fileName);
        File outputFile = filePath.toFile();

        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            byte[] buffer = new byte[8192]; // 8KB buffer
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
        }
        return filePath.toString();
    }

    public byte[] getPreviewImage(Path previewPath) throws IOException {
        if (Files.exists(previewPath)) {
            return Files.readAllBytes(previewPath);
        }
        throw new IOException("Preview image not found: " + previewPath.toString());
    }
}