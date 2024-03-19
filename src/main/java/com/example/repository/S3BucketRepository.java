package com.example.repository;

import com.example.model.*;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface S3BucketRepository extends JpaRepository<S3Bucket, String> {
	List<S3Bucket> findAllByJobJobId(Long jobId);
}

