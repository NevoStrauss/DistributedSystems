import org.apache.log4j.BasicConfigurator;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.UUID;

public class LocalApplication {
  private static final String SECRETS = "NevoEranKeyPair.pem";
  private static String INPUT_FILE_NAME;
  private static String OUTPUT_FILE_NAME;
  private static int NUM_OF_PDF_PER_WORKER;
  private static boolean SHOULD_TERMINATE;
  private static final String LOCAL_APP_TO_MANAGER_Q = "https://sqs.us-east-1.amazonaws.com/925545029787/localAppToManagerQ";
  private static final String LOCAL_APP_ID = "local-app" + UUID.randomUUID();
  private static final String INPUT_BUCKET_NAME = LOCAL_APP_ID + "input";
  private static final String OUTPUT_BUCKET_NAME =  LOCAL_APP_ID + "output";
  private static String inputFileLocation;
  private static String outputFileLocation;
  private static String managerInstanceId;
  private static String arn;


  public static void main(String[] args) {
    validateArgs(args);
    INPUT_FILE_NAME = args[0];
    OUTPUT_FILE_NAME = args[1];
    NUM_OF_PDF_PER_WORKER = Integer.parseInt(args[2]);
    SHOULD_TERMINATE = args.length > 3;
    BasicConfigurator.configure();
    getSecurityDetails();

    try {
      CreateBucketResponse createInoutBucketResponse = S3.createBucket(INPUT_BUCKET_NAME);
      inputFileLocation = S3.getBucketLocation(createInoutBucketResponse);
      S3.putObject(INPUT_FILE_NAME, "inputFile", INPUT_BUCKET_NAME);

      CreateBucketResponse createOutputBucketResponse = S3.createBucket(OUTPUT_BUCKET_NAME);
      outputFileLocation = S3.getBucketLocation(createOutputBucketResponse);

      SQS.createQueue("managerTo"+LOCAL_APP_ID);
      SQS.sendMessage(LOCAL_APP_ID+"\t"+inputFileLocation, LOCAL_APP_TO_MANAGER_Q);

      managerInstanceId = EC2.getOrCreateManager(arn);

      boolean taskDone = false;
      while (!taskDone){
        for (Message msg: SQS.receiveMessages("managerTo"+LOCAL_APP_ID)) {
          taskDone = (msg.body().equals("task_completed"));
        }
      }

      ResponseInputStream<GetObjectResponse> getOutputFileResponse = S3.getObject(OUTPUT_BUCKET_NAME, "summaryFile");

      System.out.println(getOutputFileResponse.response());
      createSummeryHTML();
    }

    finally {
      if (SHOULD_TERMINATE){
        S3.terminate(INPUT_FILE_NAME,"inputFile");
        S3.terminate(OUTPUT_FILE_NAME, "outputFile");
        SQS.terminate(SQS.getUrl("managerTo"+LOCAL_APP_ID));
        EC2.terminateInstance(managerInstanceId);
      }
    }
  }


  private static void validateArgs(String[] args) throws RuntimeException {
    if (args.length < 3) throw new RuntimeException();
  }


  private static void getSecurityDetails() {
    File file = new File(SECRETS);
    try (BufferedReader bf = new BufferedReader(new FileReader(file))) {
      arn = bf.readLine();
//      keyName = bf.readLine();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static void createSummeryHTML(){
    System.out.println("hey");
  }
}




