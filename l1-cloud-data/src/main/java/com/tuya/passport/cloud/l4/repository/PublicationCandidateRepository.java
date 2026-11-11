package com.tuya.passport.cloud.l4.repository;

import com.tuya.passport.cloud.l4.domain.CandidateStatus;
import com.tuya.passport.cloud.l4.domain.PublicationCandidateEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PublicationCandidateRepository extends JpaRepository<PublicationCandidateEntity,String>{Optional<PublicationCandidateEntity> findByOrgCodeAndId(String org,String id);Optional<PublicationCandidateEntity> findFirstByOrgCodeAndSubjectTypeAndSubjectIdAndStatusOrderByCreatedAtDesc(String org,String type,String subject,CandidateStatus status);List<PublicationCandidateEntity> findByOrgCodeAndSubjectTypeAndSubjectIdOrderByCreatedAtDesc(String org,String type,String subject);long countByOrgCodeAndStatus(String org,CandidateStatus status);}
