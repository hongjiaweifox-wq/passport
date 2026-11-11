package com.tuya.passport.cloud.l4.service;

import com.tuya.passport.cloud.common.support.ApiException;
import com.tuya.passport.cloud.l4.domain.CandidateStatus;
import com.tuya.passport.cloud.l4.domain.PublicationCandidateEntity;
import com.tuya.passport.cloud.l4.repository.PublicationCandidateRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service public class CandidateStatusService {
 private final PublicationCandidateRepository candidates;
 public CandidateStatusService(PublicationCandidateRepository candidates){this.candidates=candidates;}
 @Transactional(propagation=Propagation.REQUIRES_NEW)
 public void markStale(String org,String candidateId){PublicationCandidateEntity candidate=candidates.findByOrgCodeAndId(org,candidateId).orElseThrow(()->ApiException.notFound("GOV_CANDIDATE_NOT_FOUND","Publication candidate not found"));if(candidate.getStatus()==CandidateStatus.READY){candidate.stale();candidates.save(candidate);}}
}
