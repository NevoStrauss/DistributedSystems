import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.model.Message;

import java.util.List;
import java.util.Objects;

public class Worker {

  private PdfConverter pdfConverter = new PdfConverter();
  private static boolean shouldTerminate = false;
  private static SQS sqs = new SQS("eranevoqueue", Region.US_EAST_1);
  private static S3 s3 = new S3("eranevobucket", "eranevoqueue", Region.US_EAST_1, "input.txt");
  private String instanceId;

  public Worker() {

  }

  private static void handleMessage (String msg) throws Exception {
    String[] parsedMsg = msg.split("\t");
    if(parsedMsg.length < 2){
      if(parsedMsg.length > 0 && Objects.equals(parsedMsg[0], "terminate")){
        shouldTerminate = true;
        return;
      }
      sqs.sendMessage("bad line exception");
      throw new Exception("not enough arguments in message");
    }
    String op = parsedMsg[0];
    String url = parsedMsg[1];
    String localAppId = parsedMsg[2];
    String path = pdf_handler.handleInput(op, url);
    s3.putFileInBucketFromFile(localAppId+"pdfs", "Result", new File(path) );
    sqs.sendMessage(url+"\t"+path+"\t"+op, workerToManagerQueue);
  }

  public static void main() throws Exception {
    while (!shouldTerminate) {
      List<Message> messages = sqs.receiveMessages();
      for (Message message : messages) {
        handleMessage(message.toString());
      }
      sqs.deleteMessages(messages);
    }
  }

}
