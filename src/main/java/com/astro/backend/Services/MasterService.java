package com.astro.backend.Services;



import com.astro.backend.ResponseDTO.MasterResponse;
import com.astro.backend.apiResponse.ApiResponse;

import java.util.List;

public interface MasterService {

    ApiResponse<List<MasterResponse>> getVerificationType();

}
