import org.apache.log4j.BasicConfigurator;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.sqs.model.Message;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Objects;

public class Manager {
  private static final String managerToWorkerQueue = "https://sqs.us-east-1.amazonaws.com/925545029787/managerToWorkersQ";
  private static final String workerToManagerQ = "https://sqs.us-east-1.amazonaws.com/925545029787/workersToManagerQ";
  private static boolean shouldTerminateWorkers = false;
  private static final String workerUserData = "#! /bin/bash\n" +
    "sudo yum install -y java-1.8.0-openjdk\n" +
    "sudo yum update -y\n" +
    "mkdir worker\n" +
    "aws s3 cp s3:<PATH TO JAR FILE IN S3>" +
    "java -jar /worker/Worker.jar\n";

  private static void createWorkers() {
    for (int i = 0; i < 15; i++) {
      EC2.createEC2Instance(
        Ec2Client.builder().region(Region.US_EAST_1).build(),
        "Worker"+i,
        workerUserData,
        1,
        "workerTag"+i
      );
    }
  }

  private static void handleMessage(String msg) throws IOException {
    String[] messages = msg.split("\t");
    if(messages.length == 0)
      return;
    if(Objects.equals(messages[0], "terminate")){
      shouldTerminateWorkers = false;
      return;
    }
    if(messages.length >= 2){
      String localAppId = messages[0];
      String keyInBucket = messages[1];
      InputStream is = S3.getObject(localAppId, keyInBucket);
      try{
        String message = is.toString();
        String[] lines = message.split("\n");
        for(String line : lines){
          String msgToWorker = line + "\t" + localAppId;
          SQS.sendMessage(msgToWorker, managerToWorkerQueue);
        }
      } catch (Exception e){
        e.printStackTrace();
      }
    }

  }

  public static void main(String[] args) {
    BasicConfigurator.configure();
    createWorkers();
    while(!shouldTerminateWorkers){
      List<Message> messages = SQS.receiveMessages("https://sqs.us-east-1.amazonaws.com/925545029787/localAppToManagerQ");
      for(Message msg : messages){
        try {
          handleMessage(msg.body());
        }catch (Exception e){
          System.out.println("an error occurred handling the file  "+ e.getMessage());
        }
      }
    }
    // TODO: 04/12/2021 :
    // 1. create a progress tracker
    //  2.create output file from all objects
  }

}

