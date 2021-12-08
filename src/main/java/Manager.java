import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.sqs.model.Message;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class Manager {
  private static final String managerToWorkersQ = "https://sqs.us-east-1.amazonaws.com/497378375097/managerToWorkersQ";
  private static final String localAppToManagerQ = "https://sqs.us-east-1.amazonaws.com/497378375097/localAppToManagerQ";
  private static final String managerToLocalAppQ = "https://sqs.us-east-1.amazonaws.com/497378375097/managerToLoacalAppQ";
  private static boolean shouldTerminateWorkers = false;
  private static final EC2 ec2 = new EC2();
  private static final SQS sqs = new SQS();
  private static final S3 s3 = new S3();

  private static String getWorkerUserData() {
    String script =
      "#! /bin/bash\n" +
        "sudo yum install -y java-1.8.0-openjdk\n" +
        "sudo yum update -y\n" +
        "mkdir jars\n" +
        "aws s3 cp s3://jarfilesbucket/Worker.jar ./jars/Worker.jar\n" +
        "java -jar /jars/Worker.jar\n";
    return new String(java.util.Base64.getEncoder().encode(script.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
  }


  private static void createWorkers() {
    for (int i = 0; i < 2; i++) {
      ec2.createWorkerInstance(
        getWorkerUserData(),
        1
      );
    }
  }

  private static void handleMessage(InputStream inputFile) {
//    String[] messages = msg.split("\t");
//    System.out.println("!!!!!msg.length:" + msg.length());
//    if (messages.length == 0)
//      return;
//    if (Objects.equals(messages[0], "terminate")) {
//      shouldTerminateWorkers = true;
//      return;
//    }
//    if (messages.length == 3) {       //LOCAL_APP_ID    inputFileLocation   NUM_OF_PDF_PER_WORKER
//      String bucketName = messages[0];
//      String bucketKey = messages[1];
//      String numOfPdfPerWorker = messages[2];
//
//      System.out.println("bucketame:" + bucketName);
//      System.out.println("bucketKey:" + bucketKey);
////      int numOfPdfPerWorker = Integer.parseInt(messages[2]);
//
//      InputStream inputFile = s3.getObject(bucketName, bucketKey);
      try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputFile))) {
        while (reader.ready()) {
          String line = reader.readLine();
          s3.createBucket(line);
          sqs.sendMessage(line, managerToWorkersQ);
//          sqs.sendMessage(line + "\t" + bucketName, managerToWorkersQ);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }

      while (true) {
        List<Message> tasks = sqs.receiveMessages(managerToWorkersQ);
        if (tasks.isEmpty()) {
          break;
        }
      }

      try {
        List<S3Object> convertedFiles = s3.getAllObjectsFromBucket("localappoutput");
        File summaryFile = new File("summaryFile.txt");
        if (summaryFile.createNewFile()) {
          FileWriter fw = new FileWriter("summaryFile.txt");
          BufferedWriter bw = new BufferedWriter(fw);
          for (S3Object convertedUrl : convertedFiles) {
            String nextLine = convertedUrl.toString();
            bw.write("<p>" + nextLine + "</p>");
            bw.newLine();
          }
          s3.putObjectAsFile(summaryFile, "summaryFile", "localApp1Output");
        }

      } catch (Exception e) {
        e.printStackTrace();
      } finally {
        sqs.sendMessage("task_completed", managerToLocalAppQ);
        shouldTerminateWorkers = true;
      }
    }

  public static void main(String[] args) {
    createWorkers();
    List<Message> messages;
    do {
      messages = sqs.receiveMessages(localAppToManagerQ);
    } while (messages.isEmpty());
    String initMessage = messages.get(0).body();
    s3.createBucket(initMessage);
    String[] splitted = initMessage.split("\t");
    String inputBucketName = splitted[0];
    String inputFileKey = splitted[1];
    String numOfPdfPerWorker = splitted[2];
    InputStream inputFile = s3.getObject(inputBucketName, inputFileKey);

    while (!shouldTerminateWorkers) {
      handleMessage(inputFile);
    }
  }

}

