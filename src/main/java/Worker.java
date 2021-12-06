import software.amazon.awssdk.services.sqs.model.Message;

import java.util.List;

public class Worker {

  private static boolean shouldTerminate = false;
  private static final String managerToWorkerQ = "https://sqs.us-east-1.amazonaws.com/497378375097/managerToWorkersQ";
  private static final String workerToManagerQ = "https://sqs.us-east-1.amazonaws.com/497378375097/workersToManagerQ";
  private static final String bucketName = "localappoutput";
  private static final String bucketKey = "outputFile";


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
      S3.putObject(converted, bucketKey, bucketName); //,localAppId+"output");
//      SQS.sendMessage(converted, workerToManagerQ);
      SQS.deleteMessage(msg, managerToWorkerQ);
    }
    catch (Exception ex) {
//      S3.putObject(pdfUrl+" got error: "+ex.getMessage(),pdfUrl,localAppId+"output");
      S3.putObject(msgAsString.split("\t")[0] + "Error !!",bucketKey , bucketName); //localAppId+"output");
    }
    finally {
      List<Message> remainedMessages = SQS.receiveMessages(managerToWorkerQ);
      SQS.deleteMessages(remainedMessages, managerToWorkerQ);
      SQS.sendMessage("task_completed", workerToManagerQ);
    }

  }

  public static void main(String[] args) throws Exception {
    while (!shouldTerminate) {
      List<Message> messages = SQS.receiveMessages(managerToWorkerQ);
        for (Message message : messages) {
        handleMessage(message);
      }
      SQS.deleteMessages(messages, managerToWorkerQ);
    }
  }

}
