package com.aref.cloud_assistant_mcp.service.tools;

import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AWSEc2Tools {

    private static final Logger log = LoggerFactory.getLogger(AWSEc2Tools.class);
    private final Ec2Client ec2;

    public AWSEc2Tools(Ec2Client ec2) {
        this.ec2 = ec2;
    }

    @Tool(name = "aws_ec2_create",
            value = "Create an AWS EC2 instance and return its details (id & name).")
    public Map<String, Object> aws_create_ec2(
            @P(value = "EC2 Name") String name,
            @P(value = "Instance type, e.g. t3.micro", required = false) String instanceType,
            @P(value = "AMI ID (optional). If null uses default Amazon Linux 2023", required = false) String amiId,
            @P(value = "Key pair name (optional)", required = false) String keyName,
            @P(value = "Security group IDs (comma-separated, optional)", required = false) String securityGroupIds,
            @P(value = "Subnet ID (optional)", required = false) String subnetId
    ) {
        String type = (instanceType == null || instanceType.isBlank()) ? "t3.micro" : instanceType.trim();
        String image = (amiId == null || amiId.isBlank()) ? "ami-0c101f26f147fa7fd" : amiId.trim();
        try {
            List<String> sgIds = (securityGroupIds == null || securityGroupIds.isBlank())
                    ? Collections.emptyList()
                    : Arrays.stream(securityGroupIds.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();

            RunInstancesRequest.Builder rb = RunInstancesRequest.builder()
                    .imageId(image)
                    .instanceType(InstanceType.fromValue(type))
                    .minCount(1)
                    .maxCount(1);

            if (keyName != null && !keyName.isBlank()) rb.keyName(keyName.trim());
            if (!sgIds.isEmpty()) rb.securityGroupIds(sgIds);
            if (subnetId != null && !subnetId.isBlank()) rb.subnetId(subnetId.trim());

            String nameVal = nameTagSpec(name);
            if (nameVal != null) {
                rb.tagSpecifications(TagSpecification.builder()
                        .resourceType(ResourceType.INSTANCE)
                        .tags(Tag.builder().key("Name").value(nameVal).build())
                        .build());
            }

            RunInstancesResponse run = ec2.runInstances(rb.build());
            String id = run.instances().getFirst().instanceId();

            if (nameVal != null) {
                ec2.createTags(CreateTagsRequest.builder()
                        .resources(id)
                        .tags(Tag.builder().key("Name").value(nameVal).build())
                        .build());
            }

            return ok(Map.of("instanceId", id, "instanceName", nameVal, "instanceState", "PENDING"));
        } catch (Ec2Exception e) {
            log.error("EC2 error on create: {}", e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage(), e);
            return err(e.awsErrorDetails() != null ? e.awsErrorDetails().errorMessage() : e.getMessage());
        } catch (SdkException e) {
            log.error("SDK error on create: {}", e.getMessage(), e);
            return err(e.getMessage());
        }
    }

    @Tool(name = "aws_ec2_list",
            value = "Returns the list of EC2 instances. Optional filter: RUNNING, STOPPED, TERMINATED, PENDING, SHUTTING-DOWN, STOPPING.")
    public List<Map<String, Object>> aws_ec2_list(
            @P(value = "Optional filter for EC2 state", required = false) String ec2State
    ) {
        try {
            DescribeInstancesRequest.Builder db = DescribeInstancesRequest.builder();
            if (ec2State != null && !ec2State.isBlank()) {
                db.filters(Filter.builder()
                        .name("instance-state-name")
                        .values(ec2State.trim().toLowerCase())
                        .build());
            }

            String token = null;
            List<Map<String, Object>> out = new ArrayList<>();
            do {
                DescribeInstancesResponse resp = ec2.describeInstances(db.nextToken(token).build());
                resp.reservations().forEach(r ->
                        r.instances().forEach(i -> out.add(instanceToMap(i))));
                token = resp.nextToken();
            } while (token != null && !token.isEmpty());

            return out;
        } catch (SdkException e) {
            log.error("List error: {}", e.getMessage(), e);
            return List.of(Map.of("ok", false, "error", e.getMessage()));
        }
    }

    @Tool(name = "aws_ec2_describe",
            value = "Describe a single EC2 instance by ID.")
    public Map<String, Object> aws_ec2_describe(@P(value = "Instance ID") String instanceId) {
        try {
            DescribeInstancesResponse resp = ec2.describeInstances(r -> r.instanceIds(instanceId));
            Instance inst = resp.reservations().stream()
                    .flatMap(res -> res.instances().stream())
                    .findFirst().orElse(null);
            if (inst == null) return err("Instance not found: " + instanceId);
            return ok(instanceToMap(inst));
        } catch (SdkException e) {
            log.error("Describe error: {}", e.getMessage(), e);
            return err(e.getMessage());
        }
    }

    @Tool(name = "aws_ec2_start", value = "Start a STOPPED EC2 instance.")
    public Map<String, Object> aws_ec2_start(@P(value = "Instance ID") String instanceId) {
        try {
            StartInstancesResponse r = ec2.startInstances(StartInstancesRequest.builder()
                    .instanceIds(instanceId).build());
            String state = r.startingInstances().isEmpty()
                    ? "UNKNOWN"
                    : r.startingInstances().getFirst().currentState().nameAsString();
            return ok(Map.of("instanceId", instanceId, "instanceState", state));
        } catch (SdkException e) {
            log.error("Start error: {}", e.getMessage(), e);
            return err(e.getMessage());
        }
    }

    @Tool(name = "aws_ec2_stop", value = "Stop a RUNNING EC2 instance.")
    public Map<String, Object> aws_ec2_stop(
            @P(value = "Instance ID") String instanceId,
            @P(value = "Force stop?", required = false) Boolean force
    ) {
        try {
            StopInstancesResponse r = ec2.stopInstances(StopInstancesRequest.builder()
                    .instanceIds(instanceId)
                    .force(Boolean.TRUE.equals(force))
                    .build());
            String state = r.stoppingInstances().isEmpty()
                    ? "UNKNOWN"
                    : r.stoppingInstances().getFirst().currentState().nameAsString();
            return ok(Map.of("instanceId", instanceId, "instanceState", state));
        } catch (SdkException e) {
            log.error("Stop error: {}", e.getMessage(), e);
            return err(e.getMessage());
        }
    }

    @Tool(name = "aws_ec2_reboot", value = "Reboot a RUNNING EC2 instance.")
    public Map<String, Object> aws_ec2_reboot(@P(value = "Instance ID") String instanceId) {
        try {
            ec2.rebootInstances(RebootInstancesRequest.builder().instanceIds(instanceId).build());
            return ok(Map.of("instanceId", instanceId, "message", "Reboot initiated"));
        } catch (SdkException e) {
            log.error("Reboot error: {}", e.getMessage(), e);
            return err(e.getMessage());
        }
    }

    @Tool(name = "aws_ec2_terminate", value = "Terminate an EC2 instance.")
    public Map<String, Object> aws_ec2_terminate(@P(value = "Instance ID") String instanceId) {
        try {
            TerminateInstancesResponse r = ec2.terminateInstances(TerminateInstancesRequest.builder()
                    .instanceIds(instanceId).build());
            String state = r.terminatingInstances().isEmpty()
                    ? "UNKNOWN"
                    : r.terminatingInstances().getFirst().currentState().nameAsString();
            return ok(Map.of("instanceId", instanceId, "instanceState", state));
        } catch (SdkException e) {
            log.error("Terminate error: {}", e.getMessage(), e);
            return err(e.getMessage());
        }
    }

    @Tool(name = "aws_ec2_tag", value = "Add or update a tag on an EC2 instance. Provide key and value.")
    public Map<String, Object> aws_ec2_tag(
            @P(value = "Instance ID") String instanceId,
            @P(value = "Tag key") String key,
            @P(value = "Tag value") String value
    ) {
        if (key == null || key.isBlank()) return err("Tag key is required.");
        if (value == null || value.isBlank()) return err("Value key is required.");
        try {
            ec2.createTags(CreateTagsRequest.builder()
                    .resources(instanceId)
                    .tags(Tag.builder().key(key).value(value).build())
                    .build());
            return ok(Map.of("instanceId", instanceId, "tagSet", Map.of(key, value)));
        } catch (SdkException e) {
            log.error("Tag error: {}", e.getMessage(), e);
            return err(e.getMessage());
        }
    }

    @Tool(name = "aws_ec2_rename", value = "Rename an EC2 instance (sets/updates the Name tag).")
    public Map<String, Object> aws_ec2_rename(
            @P(value = "Instance ID") String instanceId,
            @P(value = "New name") String newName
    ) {
        if (newName == null || newName.isBlank()) return err("New name is required.");
        try {
            ec2.createTags(CreateTagsRequest.builder()
                    .resources(instanceId)
                    .tags(Tag.builder().key("Name").value(newName.trim()).build())
                    .build());
            return ok(Map.of("instanceId", instanceId, "instanceName", newName.trim()));
        } catch (SdkException e) {
            log.error("Rename error: {}", e.getMessage(), e);
            return err(e.getMessage());
        }
    }


    private static Map<String, Object> ok(Map<String, Object> body) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.putAll(body);
        return out;
    }

    private static Map<String, Object> err(String msg) {
        return Map.of("ok", false, "error", msg);
    }

    private static Map<String, Object> instanceToMap(Instance inst) {
        Map<String, String> tags = inst.tags().stream()
                .collect(Collectors.toMap(Tag::key, Tag::value, (a, b) -> b, LinkedHashMap::new));

        return new LinkedHashMap<>(Map.of(
                "instanceId", inst.instanceId(),
                "instanceType", inst.instanceTypeAsString(),
                "instanceState", inst.state() != null ? inst.state().nameAsString() : "UNKNOWN",
                "privateIp", inst.privateIpAddress(),
                "publicIp", inst.publicIpAddress() != null ? inst.publicIpAddress() : "UNKNOWN",
                "launchTime", inst.launchTime().toString(),
                "tags", tags,
                "name", tags.getOrDefault("Name", "UNKNOWN"),
                "az", inst.placement() != null ? inst.placement().availabilityZone() : "UNKNOWN",
                "imageId", inst.imageId()
        ));
    }

    private static String nameTagSpec(String name) {
        return name == null || name.isBlank() ? null : name.trim();
    }
}
