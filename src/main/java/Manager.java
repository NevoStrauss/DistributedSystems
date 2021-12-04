import org.apache.log4j.BasicConfigurator;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.sqs.model.Message;

import java.io.*;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

public class Manager {
  private static final String managerToWorkerQ = "https://sqs.us-east-1.amazonaws.com/925545029787/managerToWorkersQ";
  private static final String localAppToManagerQ = "https://sqs.us-east-1.amazonaws.com/925545029787/localAppToManagerQ";

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
      shouldTerminateWorkers = true;
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
          SQS.sendMessage(msgToWorker, managerToWorkerQ);
        }
        while (true){
          List<Message> tasks = SQS.receiveMessages(managerToWorkerQ);
          if (tasks.isEmpty()){
            break;
          }
        }
        SQS.sendMessage("task_completed", "managerTo"+localAppId);
        List<S3Object> convertedFiles = S3.getAllObjectsFromBucket(localAppId+"output");
        File summaryFile = new File("summaryFile.txt");
        if (summaryFile.createNewFile()){
          FileWriter fw = new FileWriter("summaryFile.txt");
          BufferedWriter bw = new BufferedWriter(fw);
          for (S3Object convertedUrl : convertedFiles) {
            String nextLine = convertedUrl.toString();
            bw.write(nextLine);
            bw.newLine();
          }
          S3.putObjectAsFile(summaryFile,"summaryFile",localAppId+"output");
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
      List<Message> messages = SQS.receiveMessages(localAppToManagerQ);
      for(Message msg : messages){
        try {
          handleMessage(msg.body());
        }
        catch (Exception e){
          System.out.println("an error occurred handling the file  "+ e.getMessage());
        }
      }
    }


  }

}

