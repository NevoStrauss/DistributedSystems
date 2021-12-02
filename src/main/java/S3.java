import org.apache.log4j.BasicConfigurator;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.util.List;

public class S3 {
  private static final S3Client s3Client = S3Client.builder()
    .region(Region.US_EAST_1)
    .build();

  CreateBucketResponse response;

  public static String getBucketLocation(CreateBucketResponse bucketResponse) {
    return bucketResponse.location();
  }

  public static CreateBucketResponse createBucket(String bucketName) {
    return s3Client.createBucket(
      CreateBucketRequest
      .builder()
      .bucket(bucketName)
      .createBucketConfiguration(
        CreateBucketConfiguration.builder()
          .build())
      .build());
  }


  public static void putObject(String filePath, String bucketKey, String bucketName) {
    s3Client.putObject(
      PutObjectRequest.builder()
        .bucket(bucketName)
        .key(bucketKey)
        .build(),
      RequestBody.fromFile(new File(filePath)));
  }


  public ResponseInputStream<GetObjectResponse> getObject() {
    return s3Client.getObject(GetObjectRequest.builder().bucket(bucketName).key(queueName).build());
  }

  public void deleteObject() {
    s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key("key").build());
  }

  public void terminate() {
    s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key("key").build());
    s3Client.deleteBucket(DeleteBucketRequest.builder().bucket(bucketName).build());
  }

  public static void uploadJars(String inputFileName, String bucketName){
    try {
      ListObjectsRequest listObjects = ListObjectsRequest
        .builder()
        .bucket(bucketName)
        .build();

      ListObjectsResponse res = s3Client.listObjects(listObjects);
      List<S3Object> objects = res.contents();
      boolean manager = false, worker = false;

      for (S3Object s3Object : objects) {
        if (s3Object.key().equals("Manager.jar"))
          manager = true;
        if (s3Object.key().equals("Worker.jar"))
          worker = true;
      }

      if(!manager){
        putObject(inputFileName, "Manager.jar", bucketName);
      }
      if(!worker){
        putObject(inputFileName, "Worker.jar", bucketName);
      }

    } catch (S3Exception e) {
      System.err.println(e.awsErrorDetails().errorMessage());
      System.exit(1);
    }
  }

}

