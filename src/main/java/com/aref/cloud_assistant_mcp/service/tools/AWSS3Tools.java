package com.aref.cloud_assistant_mcp.service.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.model.*;
import java.net.URL;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AWSS3Tools {

    private static final Logger log = LoggerFactory.getLogger(AWSS3Tools.class);

    private final S3Client s3;
    private final S3Presigner presigner;

    public AWSS3Tools(S3Client s3, S3Presigner presigner) {
        this.s3 = s3;
        this.presigner = presigner;
    }


    @Tool(name = "aws_s3_create_bucket", value = "Create an S3 bucket. Region must match client region.")
    public Map<String, Object> aws_s3_create_bucket(
            @P("Bucket name") String bucket,
            @P(value = "Enable versioning? (optional)", required = false) Boolean versioningEnabled
    ) {
        if (isBlank(bucket)) return err("Bucket name is required.");
        try {
            s3.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            if (Boolean.TRUE.equals(versioningEnabled)) {
                s3.putBucketVersioning(PutBucketVersioningRequest.builder()
                        .bucket(bucket)
                        .versioningConfiguration(VersioningConfiguration.builder()
                                .status(BucketVersioningStatus.ENABLED).build())
                        .build());
            }
            return ok(Map.of("bucket", bucket, "versioning", Boolean.TRUE.equals(versioningEnabled)));
        } catch (BucketAlreadyOwnedByYouException | BucketAlreadyExistsException e) {
            return err(e.getMessage());
        } catch (SdkException e) {
            log.error("Create bucket error: {}", e.getMessage(), e);
            return err(e.getMessage());
        }
    }

    @Tool(name = "aws_s3_list_buckets", value = "List S3 buckets.")
    public List<Map<String, Object>> aws_s3_list_buckets() {
        try {
            ListBucketsResponse r = s3.listBuckets();
            return r.buckets().stream().map(AWSS3Tools::bucketToMap).collect(Collectors.toList());
        } catch (SdkException e) {
            log.error("List buckets error: {}", e.getMessage(), e);
            return List.of(Map.of("ok", false, "error", e.getMessage()));
        }
    }

    @Tool(name = "aws_s3_delete_bucket", value = "Delete an empty S3 bucket.")
    public Map<String, Object> s3_delete_bucket(@P("Bucket name") String bucket) {
        if (isBlank(bucket)) return err("Bucket name is required.");
        try {
            s3.deleteBucket(DeleteBucketRequest.builder().bucket(bucket).build());
            return ok(Map.of("bucket", bucket, "deleted", true));
        } catch (SdkException e) {
            log.error("Delete bucket error: {}", e.getMessage(), e);
            return err(e.getMessage());
        }
    }

    @Tool(name = "aws_s3_set_versioning", value = "Enable or suspend bucket versioning.")
    public Map<String, Object> aws_s3_set_versioning(
            @P("Bucket name") String bucket,
            @P("Status: ENABLED or SUSPENDED") String status
    ) {
        if (isBlank(bucket) || isBlank(status)) return err("Bucket and status required.");
        try {
            s3.putBucketVersioning(PutBucketVersioningRequest.builder()
                    .bucket(bucket)
                    .versioningConfiguration(VersioningConfiguration.builder()
                            .status(BucketVersioningStatus.fromValue(status))
                            .build())
                    .build());
            return ok(Map.of("bucket", bucket, "status", status));
        } catch (SdkException e) {
            log.error("Set versioning error: {}", e.getMessage(), e);
            return err(e.getMessage());
        }
    }


    //TODO
    /*
    @Tool(name = "s3_put_object",
            value = "Upload an object. Content must be base64 for binary; set contentType & metadata optionally.")
    public Map<String, Object> s3_put_object(
            @P("Bucket") String bucket,
            @P("Key") String key,
            @P(value = "Base64 content (or empty if using text)", required = false) String base64Content,
            @P(value = "Plain text content (used if base64 not provided)", required = false) String textContent,
            @P(value = "Content-Type (e.g. text/plain, image/png)", required = false) String contentType,
            @P(value = "Metadata map (string->string)", required = false) Map<String, Object> metadata
    ) {
        if (isBlank(bucket) || isBlank(key)) return err("Bucket and key are required.");

    }

    @Tool(name = "s3_get_object_base64",
            value = "Download an object and return content with headers/metadata.")
    public Map<String, Object> s3_get_object(
            @P("Bucket") String bucket,
            @P("Key") String key,
            @P(value = "VersionId (optional)", required = false) String versionId
    ) {
        if (isBlank(bucket) || isBlank(key)) return err("Bucket and key are required.");
    }
    */


    @Tool(name = "aws_s3_list_objects", value = "List objects in a bucket with optional prefix and continuation token.")
    public Map<String, Object> aws_s3_list_objects(
            @P("Bucket") String bucket,
            @P(value = "Prefix (optional)", required = false) String prefix,
            @P(value = "Continuation token (optional)", required = false) String token,
            @P(value = "Max keys (optional, default 1000)", required = false) Integer maxKeys
    ) {
        if (isBlank(bucket)) return err("Bucket is required.");
        try {
            ListObjectsV2Request.Builder lb = ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .maxKeys(maxKeys == null || maxKeys < 1 ? 1000 : maxKeys);
            if (!isBlank(prefix)) lb.prefix(prefix);
            if (!isBlank(token)) lb.continuationToken(token);

            ListObjectsV2Response r = s3.listObjectsV2(lb.build());
            List<Map<String, Object>> items = r.contents()
                    .stream()
                    .map(o -> Map.<String, Object>of(
                            "key", o.key(),
                            "size", o.size(),
                            "lastModified", o.lastModified() != null ? o.lastModified().toString() : "",
                            "eTag", o.eTag(),
                            "storageClass", o.storageClassAsString()
                    )).toList();

            return ok(Map.of(
                    "bucket", bucket,
                    "prefix", prefix == null ? "" : prefix,
                    "isTruncated", r.isTruncated(),
                    "nextContinuationToken", r.nextContinuationToken() == null ? "" : r.nextContinuationToken(),
                    "objects", items
            ));
        } catch (SdkException e) {
            log.error("List objects error: {}", e.getMessage(), e);
            return err(e.getMessage());
        }
    }

    @Tool(name = "aws_s3_delete_object", value = "Delete an object (optionally by version).")
    public Map<String, Object> aws_s3_delete_object(
            @P("Bucket") String bucket,
            @P("Key") String key,
            @P(value = "VersionId (optional)", required = false) String versionId
    ) {
        if (isBlank(bucket) || isBlank(key)) return err("Bucket and key are required.");
        try {
            DeleteObjectRequest.Builder db = DeleteObjectRequest.builder().bucket(bucket).key(key);
            if (!isBlank(versionId)) db.versionId(versionId);
            DeleteObjectResponse r = s3.deleteObject(db.build());
            return ok(Map.of("bucket", bucket, "key", key, "versionId", r.versionId()));
        } catch (SdkException e) {
            log.error("Delete object error: {}", e.getMessage(), e);
            return err(e.getMessage());
        }
    }

    @Tool(name = "aws_s3_copy_object", value = "Copy an object to another key/bucket.")
    public Map<String, Object> aws_s3_copy_object(
            @P("Source bucket") String sourceBucket,
            @P("Source key") String sourceKey,
            @P("Destination bucket") String destinationBucket,
            @P("Destination key") String destinationKey,
            @P(value = "Source versionId (optional)", required = false) String sourceVersionId
    ) {
        if (isBlank(sourceBucket) || isBlank(sourceKey) || isBlank(destinationBucket) || isBlank(destinationKey)) {
            return err("Source/Destination bucket and key are required.");
        }
        try {
            String copySource = sourceBucket + "/" + sourceKey + (isBlank(sourceVersionId) ? "" : ("?versionId=" + sourceVersionId));
            CopyObjectResponse r = s3.copyObject(CopyObjectRequest.builder()
                    .sourceBucket(copySource)
                    .destinationBucket(destinationBucket)
                    .destinationKey(destinationKey)
                    .build());
            return ok(Map.of(
                    "eTag", r.copyObjectResult() != null ? r.copyObjectResult().eTag() : "",
                    "bucket", destinationBucket,
                    "key", destinationKey
            ));
        } catch (SdkException e) {
            log.error("Copy object error: {}", e.getMessage(), e);
            return err(e.getMessage());
        }
    }

    @Tool(name = "aws_s3_head_object", value = "Get object metadata (no content).")
    public Map<String, Object> aws_s3_head_object(
            @P("Bucket") String bucket,
            @P("Key") String key,
            @P(value = "VersionId (optional)", required = false) String versionId
    ) {
        if (isBlank(bucket) || isBlank(key)) return err("Bucket and key are required.");
        try {
            HeadObjectRequest.Builder hb = HeadObjectRequest.builder().bucket(bucket).key(key);
            if (!isBlank(versionId)) hb.versionId(versionId);
            HeadObjectResponse r = s3.headObject(hb.build());
            return ok(Map.of(
                    "bucket", bucket,
                    "key", key,
                    "versionId", r.versionId(),
                    "contentType", r.contentType(),
                    "contentLength", r.contentLength(),
                    "eTag", r.eTag(),
                    "metadata", r.metadata()
            ));
        } catch (NoSuchKeyException e) {
            return err("Object not found.");
        } catch (SdkException e) {
            log.error("Head object error: {}", e.getMessage(), e);
            return err(e.getMessage());
        }
    }

    @Tool(name = "aws_s3_presign_get", value = "Generate a presigned GET URL for an object.")
    public Map<String, Object> aws_s3_presign_get(
            @P("Bucket") String bucket,
            @P("Key") String key,
            @P(value = "Expiry seconds (default 900)", required = false) Integer expirySeconds
    ) {
        if (isBlank(bucket) || isBlank(key)) return err("Bucket and key are required.");
        try {
            GetObjectRequest get = GetObjectRequest.builder().bucket(bucket).key(key).build();
            GetObjectPresignRequest preq = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(expirySeconds == null || expirySeconds < 1 ? 900 : expirySeconds))
                    .getObjectRequest(get)
                    .build();
            PresignedGetObjectRequest presigned = presigner.presignGetObject(preq);
            URL url = presigned.url();
            return ok(Map.of("url", url.toString(), "expiresInSeconds", presigned.expiration().getEpochSecond() - (System.currentTimeMillis() / 1000)));
        } catch (SdkException e) {
            log.error("Presign GET error: {}", e.getMessage(), e);
            return err(e.getMessage());
        }
    }

    @Tool(name = "aws_s3_presign_put", value = "Generate a presigned PUT URL for an object (optionally with contentType).")
    public Map<String, Object> aws_s3_presign_put(
            @P("Bucket") String bucket,
            @P("Key") String key,
            @P(value = "Content-Type (optional)", required = false) String contentType,
            @P(value = "Expiry seconds (default 900)", required = false) Integer expirySeconds
    ) {
        if (isBlank(bucket) || isBlank(key)) return err("Bucket and key are required.");
        try {
            PutObjectRequest.Builder pb = PutObjectRequest.builder().bucket(bucket).key(key);
            if (!isBlank(contentType)) pb.contentType(contentType);

            PutObjectPresignRequest preq = PutObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(expirySeconds == null || expirySeconds < 1 ? 900 : expirySeconds))
                    .putObjectRequest(pb.build())
                    .build();
            PresignedPutObjectRequest presigned = presigner.presignPutObject(preq);
            URL url = presigned.url();
            return ok(Map.of("url", url.toString(), "headers", presigned.signedHeaders()));
        } catch (SdkException e) {
            log.error("Presign PUT error: {}", e.getMessage(), e);
            return err(e.getMessage());
        }
    }

    private static Map<String, Object> ok(Map<String, Object> body) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.putAll(body);
        return out;
    }

    private static Map<String, Object> err(String msg) {
        return Map.of("ok", false, "error", msg);
    }

    private static Map<String, Object> bucketToMap(Bucket b) {
        return Map.of("name", b.name(), "creationDate", b.creationDate() != null ? b.creationDate().toString() : "");
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

}

