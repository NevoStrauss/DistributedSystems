import software.amazon.awssdk.services.sqs.model.Message;

import java.io.File;
import java.util.List;
import java.util.UUID;

public class Worker {

  private static boolean shouldTerminate = false;
  private static final String managerToWorkersQ = "https://sqs.us-east-1.amazonaws.com/497378375097/managerToWorkersQ";
  private static final String workersToManagerQ = "https://sqs.us-east-1.amazonaws.com/497378375097/workersToManagerQ";
  private static final String outputBucket = "localappoutput";
  private static final SQS sqs = new SQS();
  private static final S3 s3 = new S3();

  private static void handleMessage(Message msg) {
    String msgAsString = msg.body();
    if(msgAsString.equals("terminate")){
        shouldTerminate = true;
        return;
    }

    try {
      String pathToConvertedFile = PdfConverter.handleInput(msgAsString);
      String msgId = msg.body().split("\t")[2];
      s3.putObject(pathToConvertedFile, UUID.randomUUID().toString(),outputBucket);
      sqs.sendMessage( msgId +"\t" + pathToConvertedFile, workersToManagerQ);
    }
    catch (Exception ex) {
      String pdfUrl = msg.body().split("\t")[1];
      String msgId = msg.body().split("\t")[2];
      sqs.sendMessage(msgId+"\t"+ pdfUrl+"\t"+ex.getMessage(), workersToManagerQ);
    }
  }

  public static void main(String[] args) {
    while (!shouldTerminate) {
      List<Message> messages = sqs.receiveMessages(managerToWorkersQ,1);
      for (Message message : messages) {
        handleMessage(message);
      }
      sqs.deleteMessages(messages, managerToWorkersQ);
    }
  }

}
