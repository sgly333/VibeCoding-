package com.example.paper.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {
    private final Storage storage = new Storage();
    private final Llm llm = new Llm();

    public Storage getStorage() {
        return storage;
    }

    public Llm getLlm() {
        return llm;
    }

    public static class Storage {
        /**
         * local: 使用本地目录模拟 OSS
         * oss: 预留扩展（当前实现默认走 local）
         */
        private String type = "local";
        private String localDir = "uploads";
        private String publicBaseUrl = "http://localhost:8080";
        private Oss oss = new Oss();

        public Oss getOss() {
            return oss;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getLocalDir() {
            return localDir;
        }

        public void setLocalDir(String localDir) {
            this.localDir = localDir;
        }

        public String getPublicBaseUrl() {
            return publicBaseUrl;
        }

        public void setPublicBaseUrl(String publicBaseUrl) {
            this.publicBaseUrl = publicBaseUrl;
        }

        public static class Oss {
            /**
             * OSS endpoint，例如：https://oss-cn-hangzhou.aliyuncs.com
             */
            private String endpoint = "";

            /**
             * bucket 名，例如：my-bucket
             */
            private String bucket = "";

            /**
             * AccessKeyId
             */
            private String accessKeyId = "";

            /**
             * AccessKeySecret
             */
            private String accessKeySecret = "";

            /**
             * object key 前缀，例如：papers
             */
            private String objectPrefix = "papers";
            /**
             * 是否返回带签名的临时下载链接（私有桶建议开启）
             */
            private boolean signedUrlEnabled = true;
            /**
             * 签名链接有效期（秒）
             */
            private long signedUrlExpireSeconds = 3600;

            public String getEndpoint() {
                return endpoint;
            }

            public void setEndpoint(String endpoint) {
                this.endpoint = endpoint;
            }

            public String getBucket() {
                return bucket;
            }

            public void setBucket(String bucket) {
                this.bucket = bucket;
            }

            public String getAccessKeyId() {
                return accessKeyId;
            }

            public void setAccessKeyId(String accessKeyId) {
                this.accessKeyId = accessKeyId;
            }

            public String getAccessKeySecret() {
                return accessKeySecret;
            }

            public void setAccessKeySecret(String accessKeySecret) {
                this.accessKeySecret = accessKeySecret;
            }

            public String getObjectPrefix() {
                return objectPrefix;
            }

            public void setObjectPrefix(String objectPrefix) {
                this.objectPrefix = objectPrefix;
            }

            public boolean isSignedUrlEnabled() {
                return signedUrlEnabled;
            }

            public void setSignedUrlEnabled(boolean signedUrlEnabled) {
                this.signedUrlEnabled = signedUrlEnabled;
            }

            public long getSignedUrlExpireSeconds() {
                return signedUrlExpireSeconds;
            }

            public void setSignedUrlExpireSeconds(long signedUrlExpireSeconds) {
                this.signedUrlExpireSeconds = signedUrlExpireSeconds;
            }
        }
    }

    public static class Llm {
        private String apiKey = "";
        private String endpoint = "";
        private String model = "qwen-plus";
        private String responseFormat = "json_array";

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }

        public String getResponseFormat() {
            return responseFormat;
        }

        public void setResponseFormat(String responseFormat) {
            this.responseFormat = responseFormat;
        }
    }
}

