import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;

public class Manager {

  private static final String workerUserData = "#! /bin/bash\n" +
    "sudo yum install -y java-1.8.0-openjdk\n" +
    "sudo yum update -y\n" +
    "mkdir worker\n" +
    "aws s3 cp s3:<PATH TO JAR FILE IN S3>" +
    "java -jar /worker/Worker.jar\n";

  private static void createWorkers() {
    for (int i = 0; i < 10; i++) {
      EC2.createEC2Instance(
        Ec2Client.builder().region(Region.US_EAST_1).build(),
        "Worker",
        workerUserData,
        1,
        "worker"
      );
    }
  }

  public static void main(String[] args) {
    createWorkers();
    SQS.createQueue("tasksQueue");
    SQS.sendMessage(S3.getBucketLocation(),SQS.getUrl("tasksQueue"));

  }
}

