import software.amazon.awssdk.services.sqs.model.Message;

import java.io.File;
import java.util.List;

public class Worker {

  private static final PdfConverter pdfConverter = new PdfConverter();
  private static boolean shouldTerminate = false;
  private static final String managerToWorkerQ = "https://sqs.us-east-1.amazonaws.com/925545029787/managerToWorkersQ";
  private static final String workerToManagerQ = "https://sqs.us-east-1.amazonaws.com/925545029787/workersToManagerQ";

  private static void handleMessage(Message msg) throws Exception {
    String msgAsString = msg.body();
    String[] parsedMsg = msgAsString.split("\t");
    if(parsedMsg.length < 2){
      if(parsedMsg.length > 0 && parsedMsg[0].equals("terminate")){
        shouldTerminate = true;
        return;
      }
      SQS.sendMessage("bad line exception",managerToWorkerQ);
      throw new Exception("not enough arguments in message");
    }

    String operation = parsedMsg[0];
    String pdfUrl = parsedMsg[1];
    String localAppId = parsedMsg[2];

    try {
      String converted = pdfConverter.handleInput(operation, pdfUrl);
      S3.putObject(converted, pdfUrl,localAppId+"output");
      SQS.sendMessage("done pdf: "+ converted, workerToManagerQ);
      SQS.deleteMessage(msg, managerToWorkerQ);
    }
    catch (Exception ex) {
      S3.putObject(pdfUrl+" got error: "+ex.getMessage(),pdfUrl,localAppId+"output");
    }
    finally {
      List<Message> remainedMessages = SQS.receiveMessages(managerToWorkerQ);
      SQS.deleteMessages(remainedMessages, managerToWorkerQ);
      SQS.sendMessage("task_completed", workerToManagerQ);
    }

  }

  public static void main() throws Exception {
    while (!shouldTerminate) {
      List<Message> messages = SQS.receiveMessages(managerToWorkerQ);
      for (Message message : messages) {
        handleMessage(message);
      }
      SQS.deleteMessages(messages, managerToWorkerQ);
    }
  }

}
