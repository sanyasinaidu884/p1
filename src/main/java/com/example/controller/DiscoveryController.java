package com.example.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import com.example.service.DiscoveryService;

@RestController
@RequestMapping("/api")
public class DiscoveryController {

	@Autowired
	private DiscoveryService discoveryService;
	
    // Initiate discovery of services
    @PostMapping("/discoverServices")
    public ResponseEntity<Long> discoverServices(@RequestBody List<String> services) {  //EC2 || S3
        Long jobId = discoveryService.discoverServices(services);
    	return ResponseEntity.ok(jobId);
    }

    // Get the result of a specific job
    @GetMapping("/getJobResult/{jobId}")
    public ResponseEntity<String> getJobResult(@PathVariable Long jobId) {
        String status = discoveryService.getJobResult(jobId);
    	return ResponseEntity.ok(status);
    }

    // Get the discovery results for a specific service
    @GetMapping("/getAllDiscoveryResult/{service}")
    public ResponseEntity<List<String>> getAllDiscoveryResult(@PathVariable String service) {  //Either EC2 or S3
    	List<String> result= discoveryService.getAllDiscoveryResult(service);
    	return ResponseEntity.ok(result);
    }
    
    // Get the discovery results for a specific service
    @GetMapping("/getDiscoveryResult/{service}/{jobId}")
    public ResponseEntity<List<String>> getDiscoveryResult(@PathVariable String service, @PathVariable Long jobId) {  //Either EC2 or S3
    	List<String> result = discoveryService.getDiscoveryResult(service, jobId);
    	return ResponseEntity.ok(result);
    }

    // Discover all file names in an S3 bucket and return Job ID
    @GetMapping("/getS3BucketObjects/{bucketName}")
    public ResponseEntity<Long> getS3BucketObjects(@PathVariable String bucketName) {
    	Long jobId = discoveryService.getS3BucketObjects(bucketName);
    	return ResponseEntity.ok(jobId);
    }

    // Get the count of objects in an S3 bucket
    @GetMapping("/getS3BucketObjectCount/{bucketName}")
    public ResponseEntity<Long> getS3BucketObjectCount(@PathVariable String bucketName) {
        long count = discoveryService.getS3BucketObjectCount(bucketName);
    	return ResponseEntity.ok(count);
    }

    // Get a list of file names matching a pattern in an S3 bucket
    @GetMapping("/getS3BucketObjectlike/{bucketName}")
    public ResponseEntity<List<String>> getS3BucketObjectlike(@PathVariable String bucketName, @RequestParam String pattern) {
    	List<String> result = discoveryService.getS3BucketObjectlike(bucketName, pattern);
    	return ResponseEntity.ok(result);
    }
}
