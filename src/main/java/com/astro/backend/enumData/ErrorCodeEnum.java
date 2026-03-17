package com.astro.backend.enumData;


import lombok.Getter;

import java.util.HashMap;
import java.util.Map;

@Getter
public enum ErrorCodeEnum {

    PR1002("PR1002", "Admission already exists for this PreAuthRequest."),
    PR1003("PR1003", "AdmissionDetails can not be null or empty.."),
    PR1004("PR1004", "Allergy must not be empty."),
    PR1005("PR1005", "Blood Group ID must not be empty."),
    PR1006("PR1006", "Blood Pressure must not be empty."),
    PR1009("PR1009", "Date cannot be in future."),
    PR1010("PR1010", "Date cannot be older than 3 days."),
    PR1012("PR1012", "Document file cannot be null or empty."),
    PR1015("PR1015", "Document list can not be null or empty."),
    PR1021("PR1021", "Enhancement query object cannot be null."),
    PR1022("PR1022", "EnterRemark cannot be null or empty."),
    PR1023("PR1023", "Created by is invalid or inactive"),
    PR1024("PR1024", "Hospital id is inactive or invalid."),
    PR1030("PR1030", "Invalid date format. Valid format: yyyy-MM-dd HH:mm:ss."),
    PR1031("PR1031", "Document path cannot be null or empty."),
    PR1032("PR1032", "Document id cannot null or empty."),
    PR1038("PR1038", "Invalid PreAuth requestId. No data found."),
    PR1041("PR1041", "Invalid UpdatedBy user ID."),
    PR1042("PR1042", "IsApprovedOrRejected must not be empty."),
    PR1043("PR1043", "isChild can not be null or empty."),
    PR1044("PR1044", "Jan Aadhar Number can not be null or empty."),
    PR1045("PR1045", "Limit must not be null or empty."),
    PR1046("PR1046", "NABH amount can not be null or empty."),
    PR1049("PR1049", "No record found for the provided TID."),
    PR1050("PR1050", "Package code can not be null or empty."),
    PR1051("PR1051", "Package code list can not be null or empty."),
    PR1052("PR1052", "Package id list can not be null or empty."),
    PR1053("PR1053", "Package id can not be null or empty."),
    PR1054("PR1054", "Package list can not be empty."),
    PR1061("PR1061", "Page Number cannot be null or empty."),
    PR1066("PR1066", "Prescription path can not be null or empty."),
    PR1071("PR1071", "Fund Enhancement remark cannot be null or empty."),
    PR1072("PR1072", "Return remark is required and cannot be null."),
    PR1073("PR1073", "Reverse wallet amount failed."),
    PR1075("PR1075", "Main Trust Wallet balance is insufficient."),
    PR1076("PR1076", "TID does not belong to an active bis admission."),
    PR1077("PR1077", "TID mismatch detected. Please provide a valid TID."),
    PR1078("PR1078", "TID cannot be null or empty."),
    PR1080("PR1080", "Tid edit request capture successfully."),
    PR1081("PR1081", "No user found for the provided identifiers."),
    PR1082("PR1082", "The uploaded file type is not supported. Please check the file format and try again."),
    PR1083("PR1083", "File upload successfully"),
    PR1084("PR1084", "Some error occurred. Please try again"),
    PR1085("PR1085", "File size exceeds limit. Allowed: 500 KB"),
    PR1086("PR1086", "Fund Enhancement data fetch successfully"),
    PR1087("PR1087", "Fund Enhancement save successfully"),
    PR1088("PR1088", "Fund Enhancement approved successfully"),
    PR1089("PR1089", "Fund Enhancement rejected successfully"),
    PR1090("PR1090", "Fund Enhancement query raised successfully"),
    PR1091("PR1091", "Fund enhancement records not found."),
    PR1092("PR1092", "Pre auth query data fetch successfully"),
    PR1093("PR1093", "Fund enhancement query data fetch successfully"),
    PR1094("PR1094", "Pre auth data fetch against TID"),
    PR1095("PR1095", "Fund Enhancement query save successfully"),
    PR1096("PR1096", "Pre auth query save successfully"),
    PR1097("PR1097", "Abha list data fetch successfully"),
    PR1098("PR1098", "Patient Admission save successfully"),
    PR1099("PR1099", "Patient admission records not found."),
    PR1100("PR1100", "No query found against the fund enhancement."),
    PR1101("PR1101", "No query found against the PreAuth request."),
    PR1102("PR1102", "No data found against edit tid"),
    PR1103("PR1103", "Edit tid data fetch sucessfully"),
    PR1104("PR1104", "Tid edit save successfully"),
    PR1105("PR1105", "No BIS records found."),
    PR1106("PR1106", "Pre auth  list fetch successfully"),
    PR1107("PR1107", "Specialty list fetch successfully"),
    PR1108("PR1108", "Package list fetch successfully"),
    PR1109("PR1109", "Doctor list fetch successfully"),
    PR1110("PR1110", "Mandatory document list fetch successfully"),
    PR1111("PR1111", "Pre auth save successfully"),
    PR1112("PR1112", "Hospital Id can not be null or empty."),
    PR1113("PR1113", "Maximum limit for surgical packages exceeded."),
    PR1114("PR1114", "Package amount can not be null or empty."),
    PR1115("PR1115", "UpdatedBy can not be null or empty"),
    PR1116("PR1116", "Remark cannot null or empty."),
    PR1117("PR1117", "Approved By is required and must not be empty."),
    PR1118("PR1118", "Invalid total package amount — mismatch detected."),
    PR1119("PR1119", "NABH total amount — mismatch detected."),
    PR1120("PR1120", "Data saved successfully. This is a fund enhancement case, so please upload Form 5. Please upload form 5 first."),
    PR1121("PR1121", "Document ID(s) do not match with the provided documentId ID(s)"),
    PR1122("PR1122", "Prescription upload not allowed because the existing prescription is not rejected."),
    PR1123("PR1123", "No matching package found for processing the query");


    private final String code;
    private final String message;

    private static final Map<String, ErrorCodeEnum> lookupByCode = new HashMap<>();
    private static final Map<String, ErrorCodeEnum> lookupByMessage = new HashMap<>();


    static {
        for (ErrorCodeEnum e : ErrorCodeEnum.values()) {
            lookupByCode.put(e.getCode(), e);
            lookupByMessage.put(e.getMessage(), e);
        }
    }


    ErrorCodeEnum(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public static String getMessage(String code) {
        ErrorCodeEnum e = lookupByCode.get(code);
        return e != null ? e.getMessage() : "Invalid Error Code";
    }

    public static String getCode(String message) {
        ErrorCodeEnum e = lookupByMessage.get(message);
        return e != null ? e.getCode() : "Invalid Code";
    }
}