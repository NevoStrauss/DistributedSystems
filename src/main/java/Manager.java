import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.sqs.model.Message;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

public class Manager {
    private static final String managerToWorkerQ = "https://sqs.us-east-1.amazonaws.com/497378375097/managerToWorkersQ";
    private static final String localAppToManagerQ = "https://sqs.us-east-1.amazonaws.com/497378375097/localAppToManagerQ";
    private static final String MANAGER_TO_LOCAL_APP_Q =  "https://sqs.us-east-1.amazonaws.com/497378375097/managerToLocalAppQ";
    private static final String workerToManagerQ = "https://sqs.us-east-1.amazonaws.com/497378375097/workersToManagerQ";


    private static boolean shouldTerminateWorkers = false;

    private static String getWorkerUserData() {
//    return new String(java.util.Base64.getEncoder().encode("".getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
        String script =
                "#! /bin/bash\n" +
                        "sudo yum install -y java-1.8.0-openjdk\n" +
                        "sudo yum update -y\n" +
                        "mkdir jars\n" +
                        "aws s3 cp s3://jarfilesbucket/Worker.jar ./jars/Worker.jar\n" +
                        "java -jar /jars/Worker.jar\n";
        return new String(java.util.Base64.getEncoder().encode(script.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
    }


    private static void createWorkers() {
        for (int i = 0; i < 2; i++) {
            EC2.createWorkerInstance(
                    Ec2Client.builder().region(Region.US_EAST_1).build(),
                    "Worker" + i,
                    getWorkerUserData(),
                    1,
                    "workerTag" + i
            );
        }
    }

    private static void handleMessage(String msg) {
        String[] messages = msg.split("\t");
        System.out.println("!!!!!msg.length:"+ msg.length());
        if (messages.length == 0)
            return;
        if (Objects.equals(messages[0], "terminate")) {
            shouldTerminateWorkers = true;
            return;
        }
        if (messages.length == 3) {       //LOCAL_APP_ID    inputFileLocation   NUM_OF_PDF_PER_WORKER
            String bucketName = messages[0];
            String bucketKey = messages[1];
            System.out.println("bucketame:"+ bucketName);
            System.out.println("bucketKey:"+ bucketKey);
//      int numOfPdfPerWorker = Integer.parseInt(messages[2]);

            InputStream is = S3.getObject(bucketName, bucketKey);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                while (reader.ready()) {
                    String line = reader.readLine();
                    System.out.println("!!!!! line:"+line);
                    SQS.sendMessage(line, managerToWorkerQ);
                    SQS.sendMessage(line + "\t" + bucketName, managerToWorkerQ);
                    System.out.println(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            while (true) {
                List<Message> tasks = SQS.receiveMessages(managerToWorkerQ);
                if (tasks.isEmpty()) {
                    break;
                }
            }

            try {
//                SQS.sendMessage("terminate", MANAGER_TO_LOCAL_APP_Q);
                List<S3Object> convertedFiles = S3.getAllObjectsFromBucket("localappoutput");
                File summaryFile = new File("summaryFile.txt");
                if (summaryFile.createNewFile()) {
                    FileWriter fw = new FileWriter("summaryFile.txt");
                    BufferedWriter bw = new BufferedWriter(fw);
                    for (S3Object convertedUrl : convertedFiles) {
                        String nextLine = convertedUrl.toString();
                        bw.write(nextLine);
                        bw.newLine();
                    }
                    S3.putObjectAsFile(summaryFile, "summaryFile", "localApp1Output");
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
            finally {
                SQS.sendMessage("task_completed", MANAGER_TO_LOCAL_APP_Q);
            }
        }

    }

    public static void main(String[] args) {
        createWorkers();
        while (!shouldTerminateWorkers) {
            List<Message> messages = SQS.receiveMessages(localAppToManagerQ);
            for (Message msg : messages) {
                try {
                    System.out.println("!!!!!\n" +"msg.body:: "+msg.body());
                    System.out.println("!!!!!\n" +"msg:: "+ msg);
                    handleMessage(msg.body());
                } catch (Exception e) {
                    System.out.println("an error occurred handling the file  " + e.getMessage());
                }
            }
        }


    }

}

