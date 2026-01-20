package com.astro.backend.Contlorer;


import com.astro.backend.Repositry.UserRepository;
import com.astro.backend.RequestDTO.VerifyOtpRequest;
import com.astro.backend.Services.EmailService;
import com.astro.backend.Services.OtpService;
import com.astro.backend.Services.SmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/otp")
@RequiredArgsConstructor
public class OtpController {

    private final OtpService otpService;
    private final SmsService smsService;
    private final EmailService emailService;
    private final UserRepository userRepo;

    @PostMapping("/send")
    public String sendOtp(@RequestParam String email, @RequestParam(required=false) String mobile) {

        var user = userRepo.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String otp = otpService.generateOtp(email);

        if (mobile != null && !mobile.isEmpty()) {
            smsService.sendOtpSms(mobile, otp);
        }

        emailService.sendOtpEmail(email, otp);

        return "OTP sent";
    }

    @PostMapping("/verify")
    public String verifyOtp(@RequestBody VerifyOtpRequest req) {

        boolean isValid = otpService.verifyOtp(req.getEmail(), req.getOtp());

        return isValid ? "OTP Verified" : "Invalid OTP";
    }
}
