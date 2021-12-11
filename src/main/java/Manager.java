import org.apache.log4j.BasicConfigurator;
import software.amazon.awssdk.services.sqs.model.Message;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

public class Manager {
  private static final String managerToWorkersQ = "https://sqs.us-east-1.amazonaws.com/497378375097/managerToWorkersQ";
  private static final String localAppToManagerQ = "https://sqs.us-east-1.amazonaws.com/497378375097/localAppToManager";
  private static final String managerToLocalAppQ = "https://sqs.us-east-1.amazonaws.com/497378375097/managerToLoacalAppQ";
  private static final String workersToManagerQ = "https://sqs.us-east-1.amazonaws.com/497378375097/workersToManagerQ";
  private static final String outputBucket = "localappoutput";
  private static boolean shouldTerminateWorkers = false;
  private static final EC2 ec2 = new EC2();
  private static final SQS sqs = new SQS();
  private static final S3 s3 = new S3();


  private static void createWorkers(int numOfWorkers) {
    for (int i = 0; i < numOfWorkers; i++) {
      ec2.createWorkerInstance(
        1
      );
    }
  }

  private static void handleLocalAppMessage(InputStream is) throws IOException {
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
    HashMap<String, Boolean> tasksDoneMap = new HashMap<>();
    for (String line : lines) {
      String id = UUID.randomUUID().toString();
      tasksDoneMap.put(id, false);
      sqs.sendMessage(line + "\t" + id, managerToWorkersQ);
    }

    HashSet<String> badUrls = new HashSet();
    HashSet<String> goodUrls = new HashSet();

    checkAllTasksDone(tasksDoneMap, badUrls, goodUrls);
    sqs.sendMessage("terminate", managerToWorkersQ);      //terminate workers
    buildSummaryFile(badUrls, goodUrls);

  }

  private static void checkAllTasksDone(HashMap<String, Boolean> tasksDoneMap, HashSet badUrls, HashSet goodUrls) {
    boolean done = false;
    while (!done) {
      List<Message> tasks = sqs.receiveMessages(workersToManagerQ, 1000);
      for (Message task : tasks) {
        if (task.body().equals("stop")) {
          return;
        }
        String[] maybeBadFile = task.body().split("\t");
        if (maybeBadFile.length > 2) {
          String msgId = maybeBadFile[0];
          String badUrl = maybeBadFile[1];
          tasksDoneMap.put(msgId, true);
          badUrls.add(badUrl);
        } else {
          String msgId = maybeBadFile[0];
          String goodUrl = maybeBadFile[1];
          goodUrls.add(goodUrl);
          tasksDoneMap.put(msgId, true);
        }
      }
      sqs.deleteMessages(tasks, workersToManagerQ);
      done = !tasksDoneMap.containsValue(false);
    }
  }

  private static void buildSummaryFile(HashSet<String> badUrls, HashSet<String> goodUrls) throws IOException {
    File summaryFile = new File("summaryFile.txt");
    FileWriter fw = new FileWriter("summaryFile.txt");
    if (summaryFile.createNewFile()) {
      BufferedWriter bw = new BufferedWriter(fw);
      for (String badUrl : badUrls) {
        bw.write(String.format("<p>%s- bad url!</p>", badUrl));
        bw.newLine();
      }
      for (String goodUrl : goodUrls) {
        bw.write(String.format("<a href = %s></a>", goodUrl));
        bw.newLine();
      }
      s3.putObjectAsFile(summaryFile, "summaryFile", "localappoutput");
      sqs.sendMessage("task_completed", managerToLocalAppQ);
    }
  }

  public static void main(String[] args) throws IOException {
    BasicConfigurator.configure();
    List<Message> messages;
    do {
      messages = sqs.receiveMessages(localAppToManagerQ, 1);
    } while (messages.isEmpty());
    String initMessage = messages.get(0).body();
    String[] splitted = initMessage.split("\t");
    String inputBucketName = splitted[0];
    String inputFileKey = splitted[1];
    String numOfPdfPerWorker = splitted[2];
    InputStream inputFile = s3.getObject(inputBucketName, inputFileKey);
    int numOfWorkers = inputFile.available() / Integer.parseInt(numOfPdfPerWorker);
    if (numOfWorkers <= 0 || numOfWorkers >= 18) {
      numOfWorkers = 15;
    }
    createWorkers(numOfWorkers);
    handleLocalAppMessage(inputFile);

  }

}

