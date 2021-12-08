import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.util.List;

public class S3 {
  private final S3Client s3Client;

  public S3(){
    s3Client = S3Client.builder()
      .region(Region.US_EAST_1)
      .build();
  }

  public static String getBucketLocation(CreateBucketResponse bucketResponse) {
    return bucketResponse.location();
  }

  public  CreateBucketResponse createBucket(String bucketName) {
    return s3Client.createBucket(
      CreateBucketRequest
      .builder()
      .bucket(bucketName)
      .createBucketConfiguration(
        CreateBucketConfiguration.builder()
          .build())
      .build());
  }


  public  void putObject(String filePath, String bucketKey, String bucketName) {
    s3Client.putObject(
      PutObjectRequest.builder()
        .bucket(bucketName)
        .key(bucketKey)
        .build(),
      RequestBody.fromFile(new File(filePath)));
  }

  public  void putObjectAsFile(File file, String bucketKey, String bucketName) {
    s3Client.putObject(
      PutObjectRequest.builder()
        .bucket(bucketName)
        .key(bucketKey)
        .build(),
      RequestBody.fromFile(file));
  }

  public ResponseInputStream<GetObjectResponse> getObject(String bucketName, String key) {
    return s3Client.getObject(GetObjectRequest.builder().bucket(bucketName).key(key).build());
  }

  public  List<S3Object> getAllObjectsFromBucket(String bucketName){
    ListObjectsRequest listObjects = ListObjectsRequest
      .builder()
      .bucket(bucketName)
      .build();

    return s3Client.listObjects(listObjects).contents();
  }


  public  void terminate(String bucketName, String key) {
    s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(key).build());
    s3Client.deleteBucket(DeleteBucketRequest.builder().bucket(bucketName).build());
  }

  public  void uploadJars(String inputFileName, String bucketName){
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

