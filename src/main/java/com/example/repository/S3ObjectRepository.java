package com.example.repository;

import com.example.model.*;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface S3ObjectRepository extends JpaRepository<S3Object, Long> {
	List<S3Object> findAllByBucketName(String bucketName);
}