import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

public class EC2 {
  private final Ec2Client ec2;
  private final String keyName;
  private final String arn;
  private static final String amiId = "ami-0ed9277fb7eb570c9";

  public EC2(String keyName, String arn, Region region) {
    ec2 = Ec2Client.builder().region(region).build();
    this.keyName = keyName;
    this.arn = arn;
  }

  public static String createEC2Instance(Ec2Client ec2, String name, String userData, int maxCount, String tag ) {
    RunInstancesRequest runRequest = RunInstancesRequest.builder()
      .imageId(amiId)
      .instanceType(InstanceType.T2_MICRO)
      .maxCount(maxCount)
      .minCount(1)
      .userData(Base64.encodeAsString(userData.getBytes()))
      .build();

    RunInstancesResponse response = ec2.runInstances(runRequest);
    String instanceId = response.instances().get(0).instanceId();

    Tag ttag = Tag.builder()
      .key("Name")
      .value(tag)
      .build();

    CreateTagsRequest tagRequest = CreateTagsRequest.builder()
      .resources(instanceId)
      .tags(ttag)
      .build();

    try {
      ec2.createTags(tagRequest);
      System.out.printf(
        "Successfully started EC2 Instance %s based on AMI %s",
        instanceId, amiId);

      return instanceId;

    } catch (Ec2Exception e) {
      System.err.println(e.awsErrorDetails().errorMessage());
      System.exit(1);
    }

    return "";
  }


  public Ec2Client getEc2() {
    return ec2;
  }

  public void startInstance(String instanceId){
    StartInstancesRequest startRequest = StartInstancesRequest.builder()
      .instanceIds(instanceId).build();
    ec2.startInstances(startRequest);
  }

  public void stopInstance(String instanceId){
    StopInstancesRequest request = StopInstancesRequest.builder()
      .instanceIds(instanceId).build();
    ec2.stopInstances(request);
  }

  public void terminateInstance(String instanceId) {
    TerminateInstancesRequest request = TerminateInstancesRequest.builder()
      .instanceIds(instanceId).build();
    ec2.terminateInstances(request);
  }


  public String getOrCreateManager(){ //TODO: Need to add data parameter
    Filter filter = Filter.builder()
      .name("manager")
      .values("running", "stopped")
      .build();

    DescribeInstancesRequest request = DescribeInstancesRequest.builder()
      .filters(filter)
      .build();

    String nextToken;

    do {
      DescribeInstancesResponse response = ec2.describeInstances(request);

      for(Reservation reservation : response.reservations()) {
        for(Instance instance : reservation.instances()) {
          for (Tag tag: instance.tags()) {
            if (tag.value().equals("manager")){
              if(instance.state().name().toString().equals("running") || instance.state().name().toString().equals("pending")){
                return instance.instanceId();
              }
              else if(instance.state().name().toString().equals("stopped")){
                startInstance(instance.instanceId());
                return instance.instanceId();
              }
            }
          }
        }
      }

      nextToken = response.nextToken();

    } while(nextToken != null);

    return createManagerInstance(amiId);
  }

  private String createManagerInstance(String amiId) {
    RunInstancesRequest runRequest = RunInstancesRequest.builder()
      .instanceType(InstanceType.T2_MICRO)
      .imageId(amiId)
      .keyName(keyName)
      .maxCount(1)
      .minCount(1)
//                .securityGroups("launch-wizard-5")
      .userData(geManagerScript())
//                .iamInstanceProfile(IamInstanceProfileSpecification.builder().arn(arn).build())
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
      System.err.println(e.getMessage());
      System.exit(1);
    }

    return instanceId;
  }

  private String geManagerScript() {
    String script = "#!/bin/bash\n";
    script += "sudo mkdir jars\n";
    script += "cd jars\n";
    script += "sudo aws s3 cp s3://bucketqoghawn0ehuw2njlvyexsmxt5dczxfwc/Manager.jar ./\n";
    script += "sudo java -Xmx30g -jar ./Manager.jar ami-0878fb723a9a1c5db " + keyName + " " +  arn;

    return new String(Base64.getEncoder().encode(script.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
  }

}
