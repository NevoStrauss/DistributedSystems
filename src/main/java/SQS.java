import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.*;

import java.util.List;


public class SQS {
  private final SqsClient sqs;

  public SQS() {
    sqs = SqsClient.builder().region(Region.US_EAST_1).build();
  }

  public void createQueue(String queueName) {
    try {
      CreateQueueRequest request = CreateQueueRequest.builder()
        .queueName(queueName)
        .build();
      sqs.createQueue(request);
    } catch (QueueNameExistsException ex) {
      ex.printStackTrace();
      throw ex;
    }
  }

  public String getUrl(String queueName) {
    GetQueueUrlRequest getQueueRequest = GetQueueUrlRequest.builder()
      .queueName(queueName)
      .build();
    return sqs.getQueueUrl(getQueueRequest).queueUrl();
  }

  public void sendMessage(String msg, String queueUrl) {
    SendMessageRequest send_msg_request = SendMessageRequest.builder()
      .queueUrl(queueUrl)
      .messageBody(msg)
      .delaySeconds(5)
      .build();
    sqs.sendMessage(send_msg_request);
  }


  public List<Message> receiveMessages(String queueUrl, int maxMsgCount) {
    ReceiveMessageRequest receiveRequest = ReceiveMessageRequest.builder()
      .queueUrl(queueUrl)
      .maxNumberOfMessages(maxMsgCount)
      .build();
    return sqs.receiveMessage(receiveRequest).messages();
  }


  public void deleteMessages(List<Message> messages, String queueUrl) {
    for (Message msg : messages) {
      DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder()
        .queueUrl(queueUrl)
        .receiptHandle(msg.receiptHandle())
        .build();
      sqs.deleteMessage(deleteMessageRequest);
    }
  }

  public void deleteMessage(Message msg, String queueUrl) {
    DeleteMessageRequest deleteMessageRequest = DeleteMessageRequest.builder()
      .queueUrl(queueUrl)
      .receiptHandle(msg.receiptHandle())
      .build();
    sqs.deleteMessage(deleteMessageRequest);
  }


  public void terminate(String queueUrl) {
    List<Message> messages = receiveMessages(queueUrl,1);
    for (Message m : messages) {
      sqs.deleteMessage(DeleteMessageRequest.builder().queueUrl(queueUrl).receiptHandle(m.receiptHandle()).build());
      sqs.deleteQueue(DeleteQueueRequest.builder().queueUrl(queueUrl).build());
    }
  }

  public void deleteQueue(String queueUrl) {
    DeleteQueueRequest deleteQueueRequest = DeleteQueueRequest.builder()
      .queueUrl(queueUrl)
      .build();

    sqs.deleteQueue(deleteQueueRequest);
  }

}


