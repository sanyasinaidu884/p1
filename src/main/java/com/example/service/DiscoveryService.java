package com.example.service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeInstancesResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.Reservation;

import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.ListBucketsResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;

import com.example.repository.*;
import com.example.model.*;

@Service
public class DiscoveryService {
	
    @Autowired
    private JobRepository jobRepository;
  
    @Autowired
    private EC2InstanceRepository ec2InstanceRepository;
    
    @Autowired
    private S3BucketRepository s3BucketRepository;
    
    @Autowired
    private S3ObjectRepository s3ObjectRepository;
    
    private final S3Client s3Client;
    private final Ec2Client ec2Client;

    @Autowired
    public DiscoveryService(S3Client s3Client, Ec2Client ec2Client) {
        this.s3Client = s3Client;
        this.ec2Client = ec2Client;
    }
    
    public Long discoverServices(List<String> services) { //EC2 & S3
	   //This method should asynchronously discover EC2 instances in the Mumbai 
	   //Region in one thread and S3 buckets in another thread and persist the result in DB
       // Create and save the job with initial status
       final Job job = new Job();
       job.setServiceType(String.join(", ", services));
       job.setStatus("In Progress");
       job.setStartTime(LocalDateTime.now());
       jobRepository.save(job);    // Save and reassign job to capture the generated ID
      
       for (String service : services) {
           if (service.equalsIgnoreCase("EC2")) {
        	   CompletableFuture<Void> task = CompletableFuture.runAsync(() -> {
        		   
                   System.out.println("EC2-Task is running...");
                   discoverEC2(job);
                   // Simulate some delay
                   try {
                       Thread.sleep(2000);
                   } catch (InterruptedException e) {
                	   job.setStatus("Failed");
                	   jobRepository.save(job);
                       e.printStackTrace();
                   }
                   System.out.println("EC2-Task completed");
               });
           } 
           if (service.equalsIgnoreCase("S3")) {
        	   CompletableFuture<Void> task = CompletableFuture.runAsync(() -> {
        		   
                   System.out.println("S3-Task is running...");
                   discoverS3(job);
                   // Simulate some delay
                   try {
                       Thread.sleep(2000);
                   } catch (InterruptedException e) {
                	   job.setStatus("Failed");
                	   jobRepository.save(job);
                       e.printStackTrace();
                   }
                   System.out.println("S3-Task completed");
               });
           }
       }
       // Update job status to completed once all done
       job.setStatus("Completed");
       job.setEndTime(LocalDateTime.now());
       jobRepository.save(job);

       return job.getJobId();
    }
	   
	public String getJobResult(Long jobId) {
        Optional<Job> jobOptional = jobRepository.findById(jobId);
        return jobOptional.map(Job::getStatus).orElse("Job Not Found");
    }
	
	public List<String> getAllDiscoveryResult(String service){ //Can be S3 || EC2
		//Input: Service Name  Output: For S3 - List of S3 Buckets  For EC2 - List of Instance IDs
        List<String> results = new ArrayList<>();
        if ("EC2".equalsIgnoreCase(service)) {
            List<EC2Instance> instances = ec2InstanceRepository.findAll(); // Modify as per actual criteria
            for (EC2Instance instance : instances) {
                results.add(instance.getInstanceId());
            }
        } 
        if ("S3".equalsIgnoreCase(service)) {
            List<S3Bucket> buckets = s3BucketRepository.findAll(); // Modify as per actual criteria
            for (S3Bucket bucket : buckets) {
                results.add(bucket.getBucketName());
            }
        }
        return results;
	}
	
	public List<String> getDiscoveryResult(String service, Long jobId){ //Can be S3 || EC2
		//Input: Service Name  Output: For S3 - List of S3 Buckets  For EC2 - List of Instance IDs
        List<String> results = new ArrayList<>();
        if ("EC2".equalsIgnoreCase(service)) {
            List<EC2Instance> instances = ec2InstanceRepository.findAllByJobJobId(jobId); // Modify as per actual criteria
            for (EC2Instance instance : instances) {
                results.add(instance.getInstanceId());
            }
        } 
        if ("S3".equalsIgnoreCase(service)) {
            List<S3Bucket> buckets = s3BucketRepository.findAllByJobJobId(jobId); // Modify as per actual criteria
            for (S3Bucket bucket : buckets) {
                results.add(bucket.getBucketName());
            }
        }
        return results;
	}
	
	public Long getS3BucketObjects(String bucketName) {
        // This method would initiate a discovery of all file names in the specified S3 bucket
        // Create a new job record
        final Job job = new Job();
        job.setServiceType("S3BucketDiscovery");
        job.setRegion("Mumbai");
        job.setStatus("InProgress");
        job.setStartTime(LocalDateTime.now());
        // Save the job to get the auto-generated ID
        jobRepository.save(job);

        // Start asynchronous discovery and persisting of S3 objects
        discoverAndSaveObjects(bucketName, job).thenRun(() -> {
            // Update job status after completion
            job.setStatus("Completed");
            job.setEndTime(LocalDateTime.now());
            jobRepository.save(job);
        });

        // Return the Job ID for the client to track
        return job.getJobId();
	}
	
    @Async
    public CompletableFuture<Void> discoverAndSaveObjects(String bucketName, Job job) {
        ListObjectsV2Request listReq = ListObjectsV2Request.builder().bucket(bucketName).build();
        ListObjectsV2Response listRes = s3Client.listObjectsV2(listReq);

        listRes.contents().forEach(obj -> {
            S3Object newObject = new S3Object();
            newObject.setObjectKey(obj.key());
            newObject.setSize(obj.size());
            newObject.setLastModified(obj.lastModified().atZone(ZoneId.systemDefault()).toLocalDateTime());
            newObject.setJob(job);
            newObject.setBucketName(bucketName);
            s3ObjectRepository.save(newObject);
        });
        return CompletableFuture.completedFuture(null);
    }
    
	public int getS3BucketObjectCount(String bucketName) {
        List<S3Object> objects = s3ObjectRepository.findAllByBucketName(bucketName);
        return objects.size();
	}
	
	public List<String> getS3BucketObjectlike(String bucketName, String pattern){
        List<String> matchedObjectKeys = new ArrayList<>();
     
        List<S3Object> allObjectKeys = s3ObjectRepository.findAllByBucketName(bucketName);
        System.out.println("All Objes" + allObjectKeys);
        // Compile the pattern once outside the loop for efficiency
        String regexPattern = ".*\\." + pattern;
        Pattern p = Pattern.compile(regexPattern, Pattern.CASE_INSENSITIVE);
        for (S3Object obj : allObjectKeys) {
        	System.out.println("IN Pattern " + obj);
            if (p.matcher(obj.getObjectKey()).matches()) {
                matchedObjectKeys.add(obj.getObjectKey());
            }
        }
        return matchedObjectKeys;
	}
	
    private void discoverEC2(Job job) {
        DescribeInstancesResponse response = ec2Client.describeInstances();
        for (Reservation reservation : response.reservations()) {
            for (Instance instance : reservation.instances()) {
            	EC2Instance ec2Ins = new EC2Instance();
            	ec2Ins.setInstanceId(instance.instanceId());
            	ec2Ins.setInstanceType(instance.instanceTypeAsString());
            	ec2Ins.setJob(job);
            	ec2InstanceRepository.save(ec2Ins);
            }
        }
    }

	private void discoverS3(Job job) {
        ListBucketsResponse listBucketsResponse = s3Client.listBuckets();
        for (Bucket bucket : listBucketsResponse.buckets()) {
        	S3Bucket s3Bucket = new S3Bucket();
            s3Bucket.setBucketName(bucket.name());
            s3Bucket.setJob(job);
            s3Bucket.setCreationDate(bucket.creationDate()); 
            s3BucketRepository.save(s3Bucket);
        }
	}
}
