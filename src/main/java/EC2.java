import com.amazonaws.util.Base64;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;

import java.nio.charset.StandardCharsets;

public class EC2 {
  private static final Ec2Client ec2 = Ec2Client.builder().region(Region.US_EAST_1).build();;
  private static final String amiId = "ami-0ed9277fb7eb570c9";
  private static final String keyName = "NevoEranKeyPair";

  public static void createWorkernstance(Ec2Client ec2, String name, String userData, int maxCount, String tag) {
    RunInstancesRequest runRequest = RunInstancesRequest.builder()
      .imageId(amiId)
      .instanceType(InstanceType.T2_MICRO)
      .maxCount(maxCount)
      .minCount(1)
      .userData(Base64.encodeAsString(userData.getBytes()))
      .iamInstanceProfile(IamInstanceProfileSpecification.builder().name("LabInstanceProfile").build())
      .build();

    RunInstancesResponse response = ec2.runInstances(runRequest);
    String instanceId = response.instances().get(0).instanceId();

    Tag tags = Tag.builder()
      .key("worker")
      .value("worker")
      .build();

    CreateTagsRequest tagRequest = CreateTagsRequest.builder()
      .resources(instanceId)
      .tags(tags)
      .build();

    try {
      ec2.createTags(tagRequest);
      System.out.printf(
        "Successfully started EC2 Instance %s based on AMI %s",
        instanceId, amiId);

    } catch (Ec2Exception e) {
      System.err.println(e.awsErrorDetails().errorMessage());
      System.exit(1);
    }

  }

  public static void startInstance(String instanceId){
    StartInstancesRequest startRequest = StartInstancesRequest.builder()
      .instanceIds(instanceId).build();
    ec2.startInstances(startRequest);
  }

  public static void stopInstance(String instanceId){
    StopInstancesRequest request = StopInstancesRequest.builder()
      .instanceIds(instanceId).build();
    ec2.stopInstances(request);
  }

  public static void terminateInstance(String instanceId) {
    TerminateInstancesRequest request = TerminateInstancesRequest.builder()
      .instanceIds(instanceId).build();
    ec2.terminateInstances(request);
  }


  public static String getOrCreateManager(String arn){ //TODO: Need to add data parameter

    DescribeInstancesRequest request = DescribeInstancesRequest.builder()
      .build();

    String nextToken;

    do {
      DescribeInstancesResponse response = ec2.describeInstances(request);

      for(Reservation reservation : response.reservations()) {
        System.out.println("reservation: "+reservation.toString());
        for(Instance instance : reservation.instances()) {
          System.out.println("instance: " + instance.toString());
          for (Tag tag: instance.tags()) {
            System.out.println("tag: "+tag.value());
            if (tag.value().equals("manager")){
              if(instance.state().toString().equals("running") || instance.state().toString().equals("pending")){
                return instance.instanceId();
              }
              else if(instance.state().toString().equals("stopped")){
                startInstance(instance.instanceId());
                return instance.instanceId();
              }
            }
          }
        }
      }

      nextToken = response.nextToken();

    } while(nextToken != null);

    return createManagerInstance(amiId, arn);
  }

  private static String createManagerInstance(String amiId, String arn) {
    RunInstancesRequest runRequest = RunInstancesRequest.builder()
      .instanceType(InstanceType.T2_MICRO)
      .imageId(amiId)
      .keyName(keyName)
      .maxCount(1)
      .minCount(1)
//                .securityGroups("launch-wizard-5")
      .userData(getManagerScript(arn))
      .iamInstanceProfile(IamInstanceProfileSpecification.builder().name("LabInstanceProfile").build())
      .build();

    RunInstancesResponse response = ec2.runInstances(runRequest);

    String instanceId = response.instances().get(0).instanceId();

    Tag tag = Tag.builder()
      .key("manager")
      .value("manager")
      .build();

    CreateTagsRequest tagRequest = CreateTagsRequest.builder()
      .resources(instanceId)
      .tags(tag)
      .build();

    try {
      ec2.createTags(tagRequest);
      System.out.printf(
        "Successfully started EC2 instance %s based on AMI %s",
        instanceId, amiId);

    } catch (Ec2Exception e) {
      e.printStackTrace();
    }

    return instanceId;
  }

  private static String getManagerScript(String arn) {
    String script =
      "#!/bin/bash\n" +
      "sudo yum install -y java-1.8.0-openjdk\n" +
      "sudo yum update -y\n" +
      "mkdir jars\n" +
      "aws s3 cp s3://jarfilesbucket/Manager.jar ./jars/Manager.jar\n" +
      "java -jar /jars/Manager.jar\n";
    return new String(java.util.Base64.getEncoder().encode(script.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
  }

}
