
import org.apache.log4j.BasicConfigurator;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Date;

public class LocalApplication {
  private static EC2 ec2;
  private static S3 s3;
  private static final String QUEUE_NAME = "eranevoqueue";
  private static final String BUCKET_NAME = "eranevobucket";
  private static final Region REGION = Region.US_EAST_1;
  private static final String SECRETS = "NevoEranKeyPair.pem";
  private static final String key = "key" + new Date().getTime();
  private static String arn;
  private static String instanceId;

  public static void main(String[] args) {
    validateArgs(args);
    BasicConfigurator.configure();
    getSecurityDetails();

    ec2 = new EC2(SECRETS, arn, REGION);

    try {
      CreateBucketResponse createBucketResponse = S3.createBucket(BUCKET_NAME);
      S3.uploadJars(args[0], BUCKET_NAME);
      S3.putObject(args[0], S3.getBucketLocation(createBucketResponse), BUCKET_NAME);
      String bucketLocation = S3.getBucketLocation(createBucketResponse);
      SQS.createQueue("managerQueue");
      SQS.sendMessage(bucketLocation, SQS.getUrl("managerQueue")); //send to sqs the input file location in s3
      instanceId = ec2.getOrCreateManager();
      createSummeryHTML();
    }

    finally {
      SQS.terminate(SQS.getUrl("managerQueue"));
      s3.terminate();
      ec2.terminateInstance(instanceId);
    }

  }


  private static void validateArgs(String[] args) throws RuntimeException {
    if (args.length < 3) throw new RuntimeException();
  }


  private static void getSecurityDetails() {
    File file = new File(SECRETS);
    try (BufferedReader bf = new BufferedReader(new FileReader(file))) {
      arn = bf.readLine();
//      keyName = bf.readLine();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void createSummeryHTML(){
    System.out.println("hey");
  }
}




