import org.apache.log4j.BasicConfigurator;
import software.amazon.awssdk.services.s3.model.CreateBucketResponse;
import software.amazon.awssdk.services.sqs.model.Message;

import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public class LocalApplication {
  private static final String SECRETS = "NevoEranKeyPair.pem";
  private static String INPUT_FILE_NAME;
  private static String OUTPUT_FILE_NAME;
  private static int NUM_OF_PDF_PER_WORKER;
  private static boolean SHOULD_TERMINATE;
  private static final String localAppToManagerQ = "https://sqs.us-east-1.amazonaws.com/497378375097/localAppToManager";
  private static final String managerToLocalAppQ = "https://sqs.us-east-1.amazonaws.com/497378375097/managerToLoacalAppQ";
  private static final String INPUT_BUCKET_NAME = "localappinput";
  private static final String OUTPUT_BUCKET_NAME = "localappoutput";
  private static String inputFileLocation;
  private static String outputFileLocation;
  private static String managerInstanceId;
  private static String arn;
  private static final EC2 ec2 = new EC2();
  private static final SQS sqs = new SQS();
  private static final S3 s3 = new S3();

  public static void main(String[] args) throws IOException {
    validateArgs(args);
    INPUT_FILE_NAME = args[0];
    OUTPUT_FILE_NAME = args[1];
    NUM_OF_PDF_PER_WORKER = Integer.parseInt(args[2]);
    SHOULD_TERMINATE = args.length > 3;
    BasicConfigurator.configure();
    getSecurityDetails();

    try {
      CreateBucketResponse createInoutBucketResponse = s3.createBucket(INPUT_BUCKET_NAME);
      inputFileLocation = S3.getBucketLocation(createInoutBucketResponse);
      s3.putObject(INPUT_FILE_NAME, "inputFile", INPUT_BUCKET_NAME);

      CreateBucketResponse createOutputBucketResponse = s3.createBucket(OUTPUT_BUCKET_NAME);
      outputFileLocation = S3.getBucketLocation(createOutputBucketResponse);
      managerInstanceId = ec2.getOrCreateManager(arn);

      sqs.sendMessage(INPUT_BUCKET_NAME + "\t" + "inputFile" + "\t" + NUM_OF_PDF_PER_WORKER, localAppToManagerQ);

      boolean taskDone = false;
      while (!taskDone) {
        for (Message msg : sqs.receiveMessages(managerToLocalAppQ, 1)) {
          taskDone = (msg.body().equals("task_completed"));
        }
      }

      InputStream summaryFile = s3.getObject(OUTPUT_BUCKET_NAME, "summaryFile");
      sqs.sendMessage("terminate", localAppToManagerQ);
      createHtmlSummery(summaryFile);

    } finally {
      if (SHOULD_TERMINATE) {
        s3.terminate(INPUT_BUCKET_NAME, "inputFile");
        ec2.terminateInstance(managerInstanceId);
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

  private static String inputStreamToString(InputStream inputStream) throws IOException {
    final char[] buffer = new char[8192];
    final StringBuilder result = new StringBuilder();
    try (Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
      int charsRead;
      while ((charsRead = reader.read(buffer, 0, buffer.length)) > 0) {
        result.append(buffer, 0, charsRead);
      }
    } catch (Exception e) {
      e.printStackTrace();
      return "couldn't build html summary";
    }
    return result.toString();
  }

  private static void createHtmlSummery(InputStream summaryFile) throws IOException {
    File htmlSummary = new File("htmlSummary.html");
    BufferedWriter bw = new BufferedWriter(new FileWriter(htmlSummary));
    String htmlPrefix =
      "<!DOCTYPE html>\n" +
        "<html>\n" +
        "<body>";
    bw.write(htmlPrefix);
    try {
      String[] lines = inputStreamToString(summaryFile).split("\n");
      for (String line : lines) {
        bw.write(line);
        bw.newLine();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    String htmlPostFix =
      "</body>\n" +
        "</html>";
    bw.write(htmlPostFix);
    bw.close();
    Desktop.getDesktop().browse(htmlSummary.toURI());
  }
}




