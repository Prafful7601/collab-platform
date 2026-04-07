package com.collab.persistenceservice.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.collab.persistenceservice.model.DocumentSnapshot;

public interface DocumentSnapshotRepository extends JpaRepository<DocumentSnapshot, String> {

    List<DocumentSnapshot> findAllByOrderByUpdatedAtDesc();
}
