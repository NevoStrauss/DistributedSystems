import software.amazon.awssdk.services.sqs.model.Message;

import java.util.List;

public class Worker {

  private static boolean shouldTerminate = false;
  private static final String managerToWorkersQ = "https://sqs.us-east-1.amazonaws.com/497378375097/managerToWorkersQ";
  private static final String workersToManagerQ = "https://sqs.us-east-1.amazonaws.com/497378375097/workersToManagerQ";
  private static final String bucketName = "localappoutput";
  private static final String bucketKey = "summaryFile";
  private static final SQS sqs = new SQS();
  private static final S3 s3 = new S3();


  private static void handleMessage(Message msg) throws Exception {
    String msgAsString = msg.body();
    String[] parsedMsg = msgAsString.split("\t");
//    if(parsedMsg.length == 1){
    if(msgAsString.equals("terminate")){
        shouldTerminate = true;
        return;
    }

    try {
      String converted = PdfConverter.handleInput(msgAsString);
      s3.putObject(converted, bucketKey, bucketName); //,localAppId+"output");
//      SQS.sendMessage(converted, workerToManagerQ);
      sqs.deleteMessage(msg, managerToWorkersQ);
    }
    catch (Exception ex) {
//      S3.putObject(pdfUrl+" got error: "+ex.getMessage(),pdfUrl,localAppId+"output");
      s3.putObject(msgAsString.split("\t")[0] + "Error !!",bucketKey , bucketName); //localAppId+"output");
    }
    finally {
      List<Message> remainedMessages = sqs.receiveMessages(managerToWorkersQ);
      sqs.deleteMessages(remainedMessages, managerToWorkersQ);
      sqs.sendMessage("task_completed", workersToManagerQ);
    }

  }

  public static void main(String[] args) throws Exception {
    while (!shouldTerminate) {
      List<Message> messages = sqs.receiveMessages(managerToWorkersQ);
        for (Message message : messages) {
        handleMessage(message);
      }
      sqs.deleteMessages(messages, managerToWorkersQ);
    }
  }

}
