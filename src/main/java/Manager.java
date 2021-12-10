import org.apache.log4j.BasicConfigurator;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.sqs.model.Message;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class Manager {
  private static final String managerToWorkersQ = "https://sqs.us-east-1.amazonaws.com/497378375097/managerToWorkersQ";
  private static final String localAppToManagerQ = "https://sqs.us-east-1.amazonaws.com/497378375097/localAppToManager";
  private static final String managerToLocalAppQ = "https://sqs.us-east-1.amazonaws.com/497378375097/managerToLoacalAppQ";
  private static final String workerToManagerQ = "https://sqs.us-east-1.amazonaws.com/497378375097/workersToManagerQ";
  private static boolean shouldTerminateWorkers = false;
  private static final EC2 ec2 = new EC2();
  private static final SQS sqs = new SQS();
  private static final S3 s3 = new S3();

//  private static String getWorkerUserData() {
//    String script =
//      "#!/bin/bash\n" +
//        "sudo yum install -y java-1.8.0-openjdk\n" +
//        "sudo yum update -y\n" +
//        "mkdir jars\n" +
//        "aws s3 cp s3://jarfilesbucket/Worker.jar ./jars/Worker.jar\n" +
//        "java -jar /jars/Worker.jar\n";
//    return new String(java.util.Base64.getEncoder().encode(script.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
//  }


  private static void createWorkers(int numOfWorkers) {
    for (int i = 0; i < numOfWorkers; i++) {
      ec2.createWorkerInstance(
        1
      );
    }
  }

  private static void handleMessage(InputStream is) {
    System.out.println("ERAN TEST - handleMessage");
    final char[] buffer = new char[8192];
    final StringBuilder result = new StringBuilder();

    try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
      int charsRead;
      while ((charsRead = reader.read(buffer, 0, buffer.length)) > 0) {
        result.append(buffer, 0, charsRead);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }

    String fileAsString = result.toString();
    String[] lines = fileAsString.split("\n");

    HashMap<String, Boolean> tasks_done_map = new HashMap<String, Boolean>();

    for(int i = 0 ; i < lines.length ; i++){
      String id = UUID.randomUUID().toString();
      tasks_done_map.put(id, false);
      sqs.sendMessage(lines[i] + "\t" + id, managerToWorkersQ);
    }


    while (true) {
      List<Message> tasks = sqs.receiveMessages(workerToManagerQ);
      if(!tasks.isEmpty()){
        boolean done = true;
        for(int i = 0 ; i < tasks.size() ; i++){
          tasks_done_map.put(tasks.get(i).body(), true);
        }
        sqs.deleteMessages(tasks, workerToManagerQ);

        for(String key : tasks_done_map.keySet()){
          if(!tasks_done_map.get(key)){
            done = false;
            break;
          }
        }
        if(done){
          break;
        }
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
          bw.write(String.format("<a href = \"%s\"></a>", nextLine));
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
//    sqs.sendMessage("task_completed", managerToLocalAppQ);      //todo - delete
    }

  public static void main(String[] args) throws IOException {
    BasicConfigurator.configure();
    List<Message> messages;
    do {
      messages = sqs.receiveMessages(localAppToManagerQ);
    } while (messages.isEmpty());
    String initMessage = messages.get(0).body();
    String[] splitted = initMessage.split("\t");
    String inputBucketName = splitted[0];
    String inputFileKey = splitted[1];
//    String numOfPdfPerWorker = splitted[2];
//    int numOfWorkers = inputFile.available()/ Integer.parseInt(numOfPdfPerWorker);
//    createWorkers(numOfWorkers);
    createWorkers(2);
    InputStream inputFile = s3.getObject(inputBucketName, inputFileKey);
    handleMessage(inputFile);


    while (!shouldTerminateWorkers) {
      handleMessage(inputFile);
    }
  }

}

