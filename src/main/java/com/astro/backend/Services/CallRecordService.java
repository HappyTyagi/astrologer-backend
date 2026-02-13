package com.astro.backend.Services;

import com.astro.backend.Entity.CallRecord;
import com.astro.backend.Repositry.CallRecordRepository;
import com.astro.backend.RequestDTO.CallRecordRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

@Service
@RequiredArgsConstructor
public class CallRecordService {

    private final CallRecordRepository callRecordRepository;

    public CallRecord saveRecord(CallRecordRequest request) {
        CallRecord record = CallRecord.builder()
                .chatId(request.getChatId().trim())
                .callId(request.getCallId().trim())
                .callerUserId(request.getCallerUserId())
                .receiverUserId(request.getReceiverUserId())
                .callType(request.getCallType().trim().toLowerCase())
                .endReason(request.getEndReason().trim().toLowerCase())
                .durationSeconds(request.getDurationSeconds())
                .startedAt(parseIso(request.getStartedAtIso()))
                .endedAt(parseIso(request.getEndedAtIso()))
                .build();
        return callRecordRepository.save(record);
    }

    private LocalDateTime parseIso(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            return LocalDateTime.parse(value.trim());
        } catch (DateTimeParseException ignored) {
            return null;
        }
    }
}
