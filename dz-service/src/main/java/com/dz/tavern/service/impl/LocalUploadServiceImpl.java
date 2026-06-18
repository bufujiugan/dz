package com.dz.tavern.service.impl;

import com.dz.tavern.common.exception.BizException;
import com.dz.tavern.common.exception.ErrorCode;
import com.dz.tavern.service.UploadService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.UUID;

@Service
@Slf4j
public class LocalUploadServiceImpl implements UploadService {
    private static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    private final Path storageRoot;
    private final String publicPrefix;

    public LocalUploadServiceImpl(@Value("${storage.local.root}") String storageRoot,
                                  @Value("${storage.local.public-prefix:/uploads}") String publicPrefix) {
        this.storageRoot = Path.of(storageRoot).toAbsolutePath().normalize();
        this.publicPrefix = publicPrefix;
    }

    @Override
    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.INVALID_PARAMETER);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new BizException(ErrorCode.FILE_TOO_LARGE);
        }
        String extension = detectExtension(file);
        LocalDate today = LocalDate.now();
        Path relativeDirectory = Path.of(String.valueOf(today.getYear()),
                String.format("%02d", today.getMonthValue()));
        Path targetDirectory = storageRoot.resolve(relativeDirectory).normalize();
        // 归一化后再次校验根目录，阻断任何可能的目录穿越写入。
        if (!targetDirectory.startsWith(storageRoot)) {
            throw new BizException(ErrorCode.INVALID_PARAMETER);
        }
        String fileName = UUID.randomUUID().toString().replace("-", "") + extension;
        Path target = targetDirectory.resolve(fileName);
        try {
            Files.createDirectories(targetDirectory);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            log.error("文件写入失败 targetDirectory={} fileSize={}",
                    targetDirectory, file.getSize(), exception);
            throw new BizException(ErrorCode.SYSTEM_ERROR);
        }
        String publicUrl = publicPrefix + "/"
                + relativeDirectory.toString().replace('\\', '/') + "/" + fileName;
        log.info("文件上传完成 publicUrl={} fileSize={}", publicUrl, file.getSize());
        return publicUrl;
    }

    private String detectExtension(MultipartFile file) {
        // 不信任客户端文件名和 Content-Type，使用文件头魔数判断允许的图片类型。
        byte[] header = new byte[8];
        try (InputStream inputStream = file.getInputStream()) {
            int read = inputStream.read(header);
            if (read >= 4 && (header[0] & 0xFF) == 0x89 && header[1] == 0x50
                    && header[2] == 0x4E && header[3] == 0x47) {
                return ".png";
            }
            if (read >= 3 && (header[0] & 0xFF) == 0xFF
                    && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF) {
                return ".jpg";
            }
        } catch (IOException exception) {
            log.warn("读取上传文件头失败 fileSize={}", file.getSize(), exception);
            throw new BizException(ErrorCode.INVALID_PARAMETER);
        }
        throw new BizException(ErrorCode.FILE_TYPE_INVALID);
    }
}
