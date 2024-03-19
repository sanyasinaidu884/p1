package com.example.repository;

import com.example.model.*;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface EC2InstanceRepository extends JpaRepository<EC2Instance, String> {
	List<EC2Instance> findAllByJobJobId(Long jobId);
}