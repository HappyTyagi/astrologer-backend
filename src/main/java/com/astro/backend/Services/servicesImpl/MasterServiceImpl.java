package com.astro.backend.Services.servicesImpl;

import com.astro.backend.Exception.SDDException;
import com.astro.backend.ResponseDTO.MasterResponse;
import com.astro.backend.Services.MasterService;
import com.astro.backend.apiResponse.ApiResponse;
import com.astro.backend.apiResponse.ResponseUtils;
import com.astro.backend.enumData.ErrorCodeEnum;
import com.fasterxml.jackson.core.type.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class MasterServiceImpl implements MasterService {


    @Override
    public ApiResponse<List<MasterResponse>> getVerificationType() {
        try {
            log.info("Fetching verification types");

            List<MasterResponse> responseList = new ArrayList<>();

            MasterResponse m1 = new MasterResponse();
            m1.setId("1001");
            m1.setMaster_name("Aadhaar Number");
            m1.setMaster_description("Aadhaar Number");

            MasterResponse m2 = new MasterResponse();
            m2.setId("1002");
            m2.setMaster_name("Mobile Number");
            m2.setMaster_description("Mobile Number");

            MasterResponse m3 = new MasterResponse();
            m3.setId("1003");
            m3.setMaster_name("Abha Address");
            m3.setMaster_description("Abha Address");

            MasterResponse m4 = new MasterResponse();
            m4.setId("1004");
            m4.setMaster_name("Abha Number");
            m4.setMaster_description("Abha register Mobile Number");

//        MasterResponse m5 = new MasterResponse();
//        m5.setId("1005");
//        m5.setMaster_name("Abha Number");
//        m5.setMaster_description("Abha aadhar register Mobile Number");

            responseList.add(m1);
            responseList.add(m2);
            responseList.add(m3);
            responseList.add(m4);
//        responseList.add(m5);

            log.info("Verification types fetched successfully, total: {}", responseList.size());

            return ResponseUtils.createSuccessResponse(
                    responseList,
                    new TypeReference<List<MasterResponse>>() {
                    },
                    true,
                    ErrorCodeEnum.PR1097.getMessage()
            );

        } catch (Exception ex) {
            log.error("Error while fetching verification types", ex);
            throw new SDDException(
                    false,
                    "Internal error while fetching verification types",
                    ex
            );
        }
    }


}
