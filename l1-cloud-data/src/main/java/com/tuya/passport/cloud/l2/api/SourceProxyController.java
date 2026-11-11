package com.tuya.passport.cloud.l2.api;

import com.tuya.passport.cloud.l2.api.dto.L2SourceDto;
import com.tuya.passport.cloud.l2.service.L1SourceClient;
import com.tuya.passport.cloud.l2.service.ImportJobService;
import com.tuya.passport.cloud.l2.support.ApiEnvelope;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/l2/sources")
public class SourceProxyController {
    private final L1SourceClient client;
    public SourceProxyController(L1SourceClient client){this.client=client;}
    @GetMapping public ApiEnvelope<List<L2SourceDto>> list(@RequestHeader(value="X-Org-Code",required=false)String org,HttpServletRequest req){String tenant=ImportJobService.normalizeOrg(org);var sources=client.list(tenant).stream().map(s->new L2SourceDto(s.id(),s.orgCode(),s.sourceCode(),s.sourceName(),s.sourceType(),s.status(),s.templateTypes())).toList();Object trace=req.getAttribute("traceId");return new ApiEnvelope<>(sources,trace==null?"":trace.toString());}
}
