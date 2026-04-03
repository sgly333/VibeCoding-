package com.example.paper.service;

import com.example.paper.config.AppProperties;
import com.example.paper.exception.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.ObjectMetadata;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import org.springframework.http.HttpStatus;

import java.io.File;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;
import java.net.URL;

@Service
public class FileService {

    private final AppProperties appProperties;

    private OSS ossClient;

    public FileService(AppProperties appProperties) {
        this.appProperties = appProperties;
    }

    @PostConstruct
    public void init() {
        String storageType = appProperties.getStorage().getType();
        if (!"oss".equalsIgnoreCase(storageType)) {
            return;
        }

        AppProperties.Storage.Oss oss = appProperties.getStorage().getOss();
        if (isBlank(oss.getEndpoint()) || isBlank(oss.getBucket()) || isBlank(oss.getAccessKeyId()) || isBlank(oss.getAccessKeySecret())) {
            // 配置未填：不阻断服务启动；在上传时给出明确报错
            return;
        }

        this.ossClient = new OSSClientBuilder().build(oss.getEndpoint(), oss.getAccessKeyId(), oss.getAccessKeySecret());
    }

    @PreDestroy
    public void shutdown() {
        if (ossClient != null) {
            ossClient.shutdown();
        }
    }

    public String upload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is empty");
        }

        byte[] pdfBytes = file.getBytes();
        return uploadBytes(pdfBytes, file.getOriginalFilename());
    }

    public File getLocalFile(String filePath) {
        Path dir = Paths.get(appProperties.getStorage().getLocalDir()).toAbsolutePath();
        return dir.resolve(filePath).toFile();
    }

    public String uploadBytes(byte[] pdfBytes, String originalFilename) throws IOException {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalArgumentException("pdfBytes is empty");
        }

        String storageType = appProperties.getStorage().getType();
        String original = Objects.requireNonNullElse(originalFilename, "paper.pdf");
        String extension = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            extension = original.substring(dot);
        }

        String objectKey = UUID.randomUUID() + extension;
        if ("oss".equalsIgnoreCase(storageType)) {
            AppProperties.Storage.Oss oss = appProperties.getStorage().getOss();
            if (isBlank(oss.getEndpoint()) || isBlank(oss.getBucket()) || isBlank(oss.getAccessKeyId()) || isBlank(oss.getAccessKeySecret())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "OSS 配置不完整：请补齐 endpoint/bucket/access-key-id/access-key-secret");
            }
            String prefix = oss.getObjectPrefix() == null ? "" : oss.getObjectPrefix().trim();
            if (!prefix.isEmpty()) {
                prefix = trimSlashes(prefix);
                objectKey = prefix + "/" + objectKey;
            }

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentLength(pdfBytes.length);
            metadata.setContentType("application/pdf");

            ensureOssClient(oss).putObject(oss.getBucket(), objectKey, new ByteArrayInputStream(pdfBytes), metadata);
            return objectKey;
        }

        if ("local".equalsIgnoreCase(storageType)) {
            String localDir = appProperties.getStorage().getLocalDir();
            Path dir = Paths.get(localDir).toAbsolutePath();
            Files.createDirectories(dir);
            Path target = dir.resolve(objectKey);
            Files.write(target, pdfBytes);
            return objectKey;
        }

        throw new ApiException(HttpStatus.BAD_REQUEST, "Unsupported storage type: " + storageType);
    }

    public void delete(String filePath) {
        try {
            String storageType = appProperties.getStorage().getType();
            if ("local".equalsIgnoreCase(storageType)) {
                File local = getLocalFile(filePath);
                if (local.exists() && local.isFile()) {
                    Files.deleteIfExists(local.toPath());
                }
                return;
            }

            if ("oss".equalsIgnoreCase(storageType)) {
                AppProperties.Storage.Oss oss = appProperties.getStorage().getOss();
                if (!isBlank(filePath)) {
                    ensureOssClient(oss).deleteObject(oss.getBucket(), filePath);
                }
                return;
            }
        } catch (Exception ignored) {
            // 删除失败不影响数据库状态；实际项目中应该记录日志
        }
    }

    public String buildPublicUrl(String filePath) {
        if (isBlank(filePath)) {
            return "";
        }
        if (isAbsoluteUrl(filePath)) {
            return filePath;
        }

        String base = appProperties.getStorage().getPublicBaseUrl();
        String storageType = appProperties.getStorage().getType();

        if ("local".equalsIgnoreCase(storageType)) {
            String normalizedBase = normalizeBaseUrl(base, "http://localhost:8080");
            return normalizedBase + "/files/" + trimLeadingSlash(filePath);
        }

        // oss：filePath 即 object key（包含 papers/... 前缀）
        AppProperties.Storage.Oss oss = appProperties.getStorage().getOss();
        if (oss.isSignedUrlEnabled()) {
            try {
                long expireSeconds = oss.getSignedUrlExpireSeconds() > 0 ? oss.getSignedUrlExpireSeconds() : 3600;
                Date expiration = new Date(System.currentTimeMillis() + expireSeconds * 1000);
                URL signed = ensureOssClient(oss).generatePresignedUrl(oss.getBucket(), trimLeadingSlash(filePath), expiration);
                return signed.toString();
            } catch (Exception ignored) {
                // 回退到直链拼接，避免接口报错
            }
        }

        String ossBase = resolveOssBaseUrl();
        if (isBlank(ossBase)) {
            return filePath;
        }
        return normalizeBaseUrl(ossBase, "") + "/" + trimLeadingSlash(filePath);
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String trimSlashes(String s) {
        String out = s;
        while (out.startsWith("/")) out = out.substring(1);
        while (out.endsWith("/")) out = out.substring(0, out.length() - 1);
        return out;
    }

    private String trimLeadingSlash(String s) {
        if (s == null) return "";
        String out = s;
        while (out.startsWith("/")) out = out.substring(1);
        return out;
    }

    private boolean isAbsoluteUrl(String s) {
        if (s == null) return false;
        String lower = s.trim().toLowerCase();
        return lower.startsWith("http://") || lower.startsWith("https://");
    }

    private String normalizeBaseUrl(String raw, String fallback) {
        String base = raw == null ? "" : raw.trim();
        if (base.isEmpty() || "https://".equals(base) || "http://".equals(base)) {
            base = fallback;
        }
        if (!base.startsWith("http://") && !base.startsWith("https://")) {
            base = "https://" + base;
        }
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base;
    }

    private String resolveOssBaseUrl() {
        String configuredBase = appProperties.getStorage().getPublicBaseUrl();
        if (!isBlank(configuredBase)
                && !"https://".equals(configuredBase.trim())
                && !"http://".equals(configuredBase.trim())) {
            return configuredBase;
        }

        AppProperties.Storage.Oss oss = appProperties.getStorage().getOss();
        if (isBlank(oss.getBucket()) || isBlank(oss.getEndpoint())) {
            return "";
        }

        String endpoint = oss.getEndpoint().trim();
        String scheme = "https";
        String host = endpoint;
        int schemeSep = endpoint.indexOf("://");
        if (schemeSep > 0) {
            scheme = endpoint.substring(0, schemeSep);
            host = endpoint.substring(schemeSep + 3);
        }
        host = trimSlashes(host);
        return scheme + "://" + oss.getBucket() + "." + host;
    }

    private OSS ensureOssClient(AppProperties.Storage.Oss oss) {
        if (ossClient == null) {
            ossClient = new OSSClientBuilder().build(oss.getEndpoint(), oss.getAccessKeyId(), oss.getAccessKeySecret());
        }
        return ossClient;
    }
}

