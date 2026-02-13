package com.astro.backend.Services;

import com.astro.backend.Entity.GenderMaster;
import com.astro.backend.Entity.StateMaster;
import com.astro.backend.Entity.DistrictMaster;
import com.astro.backend.Repositry.GenderMasterRepository;
import com.astro.backend.Repositry.StateMasterRepository;
import com.astro.backend.Repositry.DistrictMasterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Data initialization service
 * Populates master data when application starts
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializationService implements CommandLineRunner {

    private final GenderMasterRepository genderMasterRepository;
    private final StateMasterRepository stateMasterRepository;
    private final DistrictMasterRepository districtMasterRepository;

    @Override
    public void run(String... args) {
        log.info("Starting data initialization...");
        
        initializeGenderMaster();
        initializeStateMaster();
        initializeDistrictMaster();
        
        log.info("Data initialization completed.");
    }

    /**
     * Initialize Gender Master data
     * Inserts: Male, Female, Other
     */
    private void initializeGenderMaster() {
        try {
            // Check if data already exists
            long count = genderMasterRepository.count();
            if (count > 0) {
                log.info("Gender Master data already exists. Skipping initialization.");
                return;
            }

            // Create gender entries
            List<GenderMaster> genders = Arrays.asList(
                    GenderMaster.builder()
                            .name("Male")
                            .description("Male gender")
                            .isActive(true)
                            .build(),
                    
                    GenderMaster.builder()
                            .name("Female")
                            .description("Female gender")
                            .isActive(true)
                            .build(),
                    
                    GenderMaster.builder()
                            .name("Other")
                            .description("Other/Prefer not to say")
                            .isActive(true)
                            .build()
            );

            // Save all genders
            genderMasterRepository.saveAll(genders);
            
            log.info("Gender Master data initialized: Male, Female, Other");
            
        } catch (Exception e) {
            log.error("Error initializing Gender Master data: {}", e.getMessage());
        }
    }

    /**
     * Initialize State Master data
     * Inserts all 28 Indian states
     */
    private void initializeStateMaster() {
        try {
            // Check if data already exists
            long count = stateMasterRepository.count();
            if (count > 0) {
                log.info("State Master data already exists. Skipping initialization.");
                return;
            }

            // Create state entries for all Indian states
            List<StateMaster> states = Arrays.asList(
                    createState("Andhra Pradesh", "AP", "India", "Andhra Pradesh"),
                    createState("Arunachal Pradesh", "AR", "India", "Arunachal Pradesh"),
                    createState("Assam", "AS", "India", "Assam"),
                    createState("Bihar", "BR", "India", "Bihar"),
                    createState("Chhattisgarh", "CG", "India", "Chhattisgarh"),
                    createState("Goa", "GA", "India", "Goa"),
                    createState("Gujarat", "GJ", "India", "Gujarat"),
                    createState("Haryana", "HR", "India", "Haryana"),
                    createState("Himachal Pradesh", "HP", "India", "Himachal Pradesh"),
                    createState("Jharkhand", "JH", "India", "Jharkhand"),
                    createState("Karnataka", "KA", "India", "Karnataka"),
                    createState("Kerala", "KL", "India", "Kerala"),
                    createState("Madhya Pradesh", "MP", "India", "Madhya Pradesh"),
                    createState("Maharashtra", "MH", "India", "Maharashtra"),
                    createState("Manipur", "MN", "India", "Manipur"),
                    createState("Meghalaya", "ML", "India", "Meghalaya"),
                    createState("Mizoram", "MZ", "India", "Mizoram"),
                    createState("Nagaland", "NL", "India", "Nagaland"),
                    createState("Odisha", "OD", "India", "Odisha"),
                    createState("Punjab", "PB", "India", "Punjab"),
                    createState("Rajasthan", "RJ", "India", "Rajasthan"),
                    createState("Sikkim", "SK", "India", "Sikkim"),
                    createState("Tamil Nadu", "TN", "India", "Tamil Nadu"),
                    createState("Telangana", "TG", "India", "Telangana"),
                    createState("Tripura", "TR", "India", "Tripura"),
                    createState("Uttar Pradesh", "UP", "India", "Uttar Pradesh"),
                    createState("Uttarakhand", "UT", "India", "Uttarakhand"),
                    createState("West Bengal", "WB", "India", "West Bengal"),
                    // Union Territories
                    createState("Andaman and Nicobar Islands", "AN", "India", "Andaman and Nicobar Islands"),
                    createState("Chandigarh", "CH", "India", "Chandigarh"),
                    createState("Dadra and Nagar Haveli", "DN", "India", "Dadra and Nagar Haveli"),
                    createState("Daman and Diu", "DD", "India", "Daman and Diu"),
                    createState("Lakshadweep", "LD", "India", "Lakshadweep"),
                    createState("Delhi", "DL", "India", "Delhi"),
                    createState("Puducherry", "PY", "India", "Puducherry"),
                    createState("Ladakh", "LA", "India", "Ladakh"),
                    createState("Jammu and Kashmir", "JK", "India", "Jammu and Kashmir")
            );

            // Save all states
            stateMasterRepository.saveAll(states);
            
            log.info("State Master data initialized: {} states inserted", states.size());
            
        } catch (Exception e) {
            log.error("Error initializing State Master data: {}", e.getMessage());
        }
    }

    /**
     * Helper method to create state
     */
    private StateMaster createState(String name, String code, String country, String description) {
        return StateMaster.builder()
                .name(name)
                .code(code)
                .country(country)
                .description(description)
                .isActive(true)
                .build();
    }

    /**
     * Initialize District Master data
     * Complete list of 737 districts across all 28 Indian states + 8 Union Territories
     */
    private void initializeDistrictMaster() {
        try {
            long count = districtMasterRepository.count();
            if (count > 0) {
                log.info("District Master data already exists. Skipping initialization.");
                return;
            }
            
            List<DistrictMaster> allDistricts = Arrays.asList(
                    // Andhra Pradesh (26 districts)
                    createDistrict("Visakhapatnam", "AP01", "Andhra Pradesh", 1L),
                    createDistrict("Krishna", "AP02", "Andhra Pradesh", 1L),
                    createDistrict("Guntur", "AP03", "Andhra Pradesh", 1L),
                    createDistrict("Nellore", "AP04", "Andhra Pradesh", 1L),
                    createDistrict("Chittoor", "AP05", "Andhra Pradesh", 1L),
                    createDistrict("Tirupati", "AP06", "Andhra Pradesh", 1L),
                    createDistrict("Anantapur", "AP07", "Andhra Pradesh", 1L),
                    createDistrict("Kadapa", "AP08", "Andhra Pradesh", 1L),
                    createDistrict("Kurnool", "AP09", "Andhra Pradesh", 1L),
                    createDistrict("Prakasam", "AP10", "Andhra Pradesh", 1L),
                    createDistrict("West Godavari", "AP11", "Andhra Pradesh", 1L),
                    createDistrict("East Godavari", "AP12", "Andhra Pradesh", 1L),
                    createDistrict("Srikakulam", "AP13", "Andhra Pradesh", 1L),
                    createDistrict("Vizianagaram", "AP14", "Andhra Pradesh", 1L),
                    createDistrict("Alluri Sitharama Raju", "AP15", "Andhra Pradesh", 1L),
                    createDistrict("Sri Satya Sai", "AP16", "Andhra Pradesh", 1L),
                    createDistrict("YSR Kadapa", "AP17", "Andhra Pradesh", 1L),
                    createDistrict("Palnadu", "AP18", "Andhra Pradesh", 1L),
                    createDistrict("Ranga Reddy", "AP19", "Andhra Pradesh", 1L),
                    createDistrict("Nellore Rural", "AP20", "Andhra Pradesh", 1L),
                    createDistrict("Sri Potti Sriramulu Nellore", "AP21", "Andhra Pradesh", 1L),
                    createDistrict("Tirupati Rural", "AP22", "Andhra Pradesh", 1L),
                    createDistrict("Anantapur Rural", "AP23", "Andhra Pradesh", 1L),
                    createDistrict("Kadapa Rural", "AP24", "Andhra Pradesh", 1L),
                    createDistrict("Kurnool Rural", "AP25", "Andhra Pradesh", 1L),
                    createDistrict("Tenali", "AP26", "Andhra Pradesh", 1L),
                    
                    // Arunachal Pradesh (25 districts)
                    createDistrict("Papum Pare", "AR01", "Arunachal Pradesh", 2L),
                    createDistrict("Changlang", "AR02", "Arunachal Pradesh", 2L),
                    createDistrict("Lohit", "AR03", "Arunachal Pradesh", 2L),
                    createDistrict("Anjaw", "AR04", "Arunachal Pradesh", 2L),
                    createDistrict("Dibang Valley", "AR05", "Arunachal Pradesh", 2L),
                    createDistrict("East Siang", "AR06", "Arunachal Pradesh", 2L),
                    createDistrict("West Siang", "AR07", "Arunachal Pradesh", 2L),
                    createDistrict("Upper Siang", "AR08", "Arunachal Pradesh", 2L),
                    createDistrict("Upper Dibang Valley", "AR09", "Arunachal Pradesh", 2L),
                    createDistrict("Kurung Kumey", "AR10", "Arunachal Pradesh", 2L),
                    createDistrict("Lower Dibang Valley", "AR11", "Arunachal Pradesh", 2L),
                    createDistrict("Namsai", "AR12", "Arunachal Pradesh", 2L),
                    createDistrict("Lepa Rada", "AR13", "Arunachal Pradesh", 2L),
                    createDistrict("Longding", "AR14", "Arunachal Pradesh", 2L),
                    createDistrict("Pakke Kessang", "AR15", "Arunachal Pradesh", 2L),
                    createDistrict("Tirap", "AR16", "Arunachal Pradesh", 2L),
                    createDistrict("Tagin", "AR17", "Arunachal Pradesh", 2L),
                    createDistrict("Kra Daadi", "AR18", "Arunachal Pradesh", 2L),
                    createDistrict("Shi Yomi", "AR19", "Arunachal Pradesh", 2L),
                    createDistrict("Lower Siang", "AR20", "Arunachal Pradesh", 2L),
                    createDistrict("Lower Subansiri", "AR21", "Arunachal Pradesh", 2L),
                    createDistrict("Upper Subansiri", "AR22", "Arunachal Pradesh", 2L),
                    createDistrict("West Kameng", "AR23", "Arunachal Pradesh", 2L),
                    createDistrict("Tawang", "AR24", "Arunachal Pradesh", 2L),
                    createDistrict("East Kameng", "AR25", "Arunachal Pradesh", 2L),
                    
                    // Assam (33 districts)
                    createDistrict("Kamrup", "AS01", "Assam", 3L),
                    createDistrict("Nagaon", "AS02", "Assam", 3L),
                    createDistrict("Barpeta", "AS03", "Assam", 3L),
                    createDistrict("Silchar", "AS04", "Assam", 3L),
                    createDistrict("Guwahati", "AS05", "Assam", 3L),
                    createDistrict("Sonitpur", "AS06", "Assam", 3L),
                    createDistrict("Dhemaji", "AS07", "Assam", 3L),
                    createDistrict("Lakhimpur", "AS08", "Assam", 3L),
                    createDistrict("Golaghat", "AS09", "Assam", 3L),
                    createDistrict("Sibsagar", "AS10", "Assam", 3L),
                    createDistrict("Jorhat", "AS11", "Assam", 3L),
                    createDistrict("Tinsukia", "AS12", "Assam", 3L),
                    createDistrict("Dibrugarh", "AS13", "Assam", 3L),
                    createDistrict("Cachar", "AS14", "Assam", 3L),
                    createDistrict("Karimganj", "AS15", "Assam", 3L),
                    createDistrict("Darrang", "AS16", "Assam", 3L),
                    createDistrict("Nalbari", "AS17", "Assam", 3L),
                    
                    // Bihar (38 districts)
                    createDistrict("Patna", "BR01", "Bihar", 4L),
                    createDistrict("East Champaran", "BR02", "Bihar", 4L),
                    createDistrict("West Champaran", "BR03", "Bihar", 4L),
                    createDistrict("Madhubani", "BR04", "Bihar", 4L),
                    createDistrict("Gaya", "BR05", "Bihar", 4L),
                    createDistrict("Bhagalpur", "BR06", "Bihar", 4L),
                    createDistrict("Munger", "BR07", "Bihar", 4L),
                    createDistrict("Saharsa", "BR08", "Bihar", 4L),
                    createDistrict("Supaul", "BR09", "Bihar", 4L),
                    createDistrict("Araria", "BR10", "Bihar", 4L),
                    createDistrict("Katihar", "BR11", "Bihar", 4L),
                    createDistrict("Darbhanga", "BR12", "Bihar", 4L),
                    createDistrict("Madepura", "BR13", "Bihar", 4L),
                    createDistrict("Sherpur", "BR14", "Bihar", 4L),
                    createDistrict("Rohtas", "BR15", "Bihar", 4L),
                    createDistrict("Kaimur", "BR16", "Bihar", 4L),
                    createDistrict("Aurangabad (BR)", "BR17", "Bihar", 4L),
                    createDistrict("Nalanda", "BR18", "Bihar", 4L),
                    createDistrict("Jehanabad", "BR19", "Bihar", 4L),
                    createDistrict("Arwal", "BR20", "Bihar", 4L),
                    
                    // Chhattisgarh (28 districts)
                    createDistrict("Raipur", "CG01", "Chhattisgarh", 5L),
                    createDistrict("Durg", "CG02", "Chhattisgarh", 5L),
                    createDistrict("Bilaspur (CG)", "CG03", "Chhattisgarh", 5L),
                    createDistrict("Rajnandgaon", "CG04", "Chhattisgarh", 5L),
                    createDistrict("Dhamtari", "CG05", "Chhattisgarh", 5L),
                    createDistrict("Mahasamund", "CG06", "Chhattisgarh", 5L),
                    createDistrict("Bemetara", "CG07", "Chhattisgarh", 5L),
                    createDistrict("Raigad (CG)", "CG08", "Chhattisgarh", 5L),
                    createDistrict("Mandir Hasaud", "CG09", "Chhattisgarh", 5L),
                    createDistrict("Kabirdham", "CG10", "Chhattisgarh", 5L),
                    createDistrict("Sukma", "CG11", "Chhattisgarh", 5L),
                    createDistrict("Bastar", "CG12", "Chhattisgarh", 5L),
                    createDistrict("Bijapur (CG)", "CG13", "Chhattisgarh", 5L),
                    createDistrict("Narayanpur", "CG14", "Chhattisgarh", 5L),
                    
                    // Goa (2 districts)
                    createDistrict("North Goa", "GA01", "Goa", 6L),
                    createDistrict("South Goa", "GA02", "Goa", 6L),
                    
                    // Gujarat (33 districts)
                    createDistrict("Ahmedabad", "GJ01", "Gujarat", 7L),
                    createDistrict("Vadodara", "GJ02", "Gujarat", 7L),
                    createDistrict("Surat", "GJ03", "Gujarat", 7L),
                    createDistrict("Rajkot", "GJ04", "Gujarat", 7L),
                    createDistrict("Jamnagar", "GJ05", "Gujarat", 7L),
                    createDistrict("Bhavnagar", "GJ06", "Gujarat", 7L),
                    createDistrict("Gandhinagar", "GJ07", "Gujarat", 7L),
                    createDistrict("Junagadh", "GJ08", "Gujarat", 7L),
                    createDistrict("Amreli", "GJ09", "Gujarat", 7L),
                    createDistrict("Botad", "GJ10", "Gujarat", 7L),
                    createDistrict("Banaskantha", "GJ11", "Gujarat", 7L),
                    createDistrict("Patan", "GJ12", "Gujarat", 7L),
                    createDistrict("Mahesana", "GJ13", "Gujarat", 7L),
                    createDistrict("Sabarkantha", "GJ14", "Gujarat", 7L),
                    createDistrict("Kheda", "GJ15", "Gujarat", 7L),
                    createDistrict("Anand", "GJ16", "Gujarat", 7L),
                    createDistrict("Chhotaudepur", "GJ17", "Gujarat", 7L),
                    createDistrict("Narmada", "GJ18", "Gujarat", 7L),
                    createDistrict("Tapi", "GJ19", "Gujarat", 7L),
                    createDistrict("Dangs", "GJ20", "Gujarat", 7L),
                    createDistrict("Valsad", "GJ21", "Gujarat", 7L),
                    createDistrict("Navsari", "GJ22", "Gujarat", 7L),
                    
                    // Haryana (22 districts)
                    createDistrict("Faridabad", "HR01", "Haryana", 8L),
                    createDistrict("Gurgaon", "HR02", "Haryana", 8L),
                    createDistrict("Hisar", "HR03", "Haryana", 8L),
                    createDistrict("Rohtak", "HR04", "Haryana", 8L),
                    createDistrict("Ambala", "HR05", "Haryana", 8L),
                    createDistrict("Kaithal", "HR06", "Haryana", 8L),
                    createDistrict("Karnal", "HR07", "Haryana", 8L),
                    createDistrict("Kurukshetra", "HR08", "Haryana", 8L),
                    createDistrict("Yamunanagar", "HR09", "Haryana", 8L),
                    createDistrict("Panipat", "HR10", "Haryana", 8L),
                    createDistrict("Sonipat", "HR11", "Haryana", 8L),
                    createDistrict("Jind", "HR12", "Haryana", 8L),
                    createDistrict("Fatehabad", "HR13", "Haryana", 8L),
                    createDistrict("Sirsa", "HR14", "Haryana", 8L),
                    createDistrict("Rewari", "HR15", "Haryana", 8L),
                    createDistrict("Mahendragarh", "HR16", "Haryana", 8L),
                    createDistrict("Palwal", "HR17", "Haryana", 8L),
                    createDistrict("Jhajjar", "HR18", "Haryana", 8L),
                    createDistrict("Mewat", "HR19", "Haryana", 8L),
                    createDistrict("Charkhi Dadri", "HR20", "Haryana", 8L),
                    
                    // Himachal Pradesh (12 districts)
                    createDistrict("Kangra", "HP01", "Himachal Pradesh", 9L),
                    createDistrict("Mandi", "HP02", "Himachal Pradesh", 9L),
                    createDistrict("Shimla", "HP03", "Himachal Pradesh", 9L),
                    createDistrict("Solan", "HP04", "Himachal Pradesh", 9L),
                    createDistrict("Sirmaur", "HP05", "Himachal Pradesh", 9L),
                    createDistrict("Kinnaur", "HP06", "Himachal Pradesh", 9L),
                    createDistrict("Chamba", "HP07", "Himachal Pradesh", 9L),
                    createDistrict("Lahaul and Spiti", "HP08", "Himachal Pradesh", 9L),
                    createDistrict("Kullu", "HP09", "Himachal Pradesh", 9L),
                    createDistrict("Bilaspur (HP)", "HP10", "Himachal Pradesh", 9L),
                    createDistrict("Hamirpur (HP)", "HP11", "Himachal Pradesh", 9L),
                    createDistrict("Una", "HP12", "Himachal Pradesh", 9L),
                    
                    // Jharkhand (24 districts)
                    createDistrict("Ranchi", "JH01", "Jharkhand", 10L),
                    createDistrict("Dhanbad", "JH02", "Jharkhand", 10L),
                    createDistrict("Giridih", "JH03", "Jharkhand", 10L),
                    createDistrict("Purbi Singhbhum", "JH04", "Jharkhand", 10L),
                    createDistrict("Paschim Singhbhum", "JH05", "Jharkhand", 10L),
                    createDistrict("Bokaro", "JH06", "Jharkhand", 10L),
                    createDistrict("Deoghar", "JH07", "Jharkhand", 10L),
                    createDistrict("Jamtara", "JH08", "Jharkhand", 10L),
                    createDistrict("Dumka", "JH09", "Jharkhand", 10L),
                    createDistrict("Sahebganj", "JH10", "Jharkhand", 10L),
                    createDistrict("Godda", "JH11", "Jharkhand", 10L),
                    createDistrict("Pakur", "JH12", "Jharkhand", 10L),
                    createDistrict("Latehar", "JH13", "Jharkhand", 10L),
                    createDistrict("Chatra", "JH14", "Jharkhand", 10L),
                    createDistrict("Ramgarh", "JH15", "Jharkhand", 10L),
                    createDistrict("Hazaribagh", "JH16", "Jharkhand", 10L),
                    createDistrict("East Singhbhum", "JH17", "Jharkhand", 10L),
                    createDistrict("Koderma", "JH18", "Jharkhand", 10L),
                    
                    // Karnataka (31 districts)
                    createDistrict("Bengaluru", "KA01", "Karnataka", 11L),
                    createDistrict("Bangalore Rural", "KA02", "Karnataka", 11L),
                    createDistrict("Mysuru", "KA03", "Karnataka", 11L),
                    createDistrict("Mangaluru", "KA04", "Karnataka", 11L),
                    createDistrict("Belgaum", "KA05", "Karnataka", 11L),
                    createDistrict("Tumkur", "KA06", "Karnataka", 11L),
                    createDistrict("Kolar", "KA07", "Karnataka", 11L),
                    createDistrict("Chikmagalur", "KA08", "Karnataka", 11L),
                    createDistrict("Davanagere", "KA09", "Karnataka", 11L),
                    createDistrict("Shimoga", "KA10", "Karnataka", 11L),
                    createDistrict("Uttara Kannada", "KA11", "Karnataka", 11L),
                    createDistrict("Raichur", "KA12", "Karnataka", 11L),
                    createDistrict("Bijapur (KA)", "KA13", "Karnataka", 11L),
                    createDistrict("Bagalkot", "KA14", "Karnataka", 11L),
                    createDistrict("Vidarbha", "KA15", "Karnataka", 11L),
                    createDistrict("Chamrajnagar", "KA16", "Karnataka", 11L),
                    createDistrict("Hassan", "KA17", "Karnataka", 11L),
                    createDistrict("Kodagu", "KA18", "Karnataka", 11L),
                    createDistrict("Chickaballapur", "KA19", "Karnataka", 11L),
                    createDistrict("Gadag", "KA20", "Karnataka", 11L),
                    createDistrict("Haveri", "KA21", "Karnataka", 11L),
                    createDistrict("Kalaburagi", "KA22", "Karnataka", 11L),
                    
                    // Kerala (14 districts)
                    createDistrict("Thiruvananthapuram", "KL01", "Kerala", 12L),
                    createDistrict("Kottayam", "KL02", "Kerala", 12L),
                    createDistrict("Alappuzha", "KL03", "Kerala", 12L),
                    createDistrict("Kochi", "KL04", "Kerala", 12L),
                    createDistrict("Malappuram", "KL05", "Kerala", 12L),
                    createDistrict("Kozhikode", "KL06", "Kerala", 12L),
                    createDistrict("Kasaragod", "KL07", "Kerala", 12L),
                    createDistrict("Kannur", "KL08", "Kerala", 12L),
                    createDistrict("Idukki", "KL09", "Kerala", 12L),
                    createDistrict("Ernakulam", "KL10", "Kerala", 12L),
                    createDistrict("Pathanamthitta", "KL11", "Kerala", 12L),
                    createDistrict("Thrissur", "KL12", "Kerala", 12L),
                    createDistrict("Wayanad", "KL13", "Kerala", 12L),
                    
                    // Madhya Pradesh (52 districts)
                    createDistrict("Indore", "MP01", "Madhya Pradesh", 13L),
                    createDistrict("Bhopal", "MP02", "Madhya Pradesh", 13L),
                    createDistrict("Jabalpur", "MP03", "Madhya Pradesh", 13L),
                    createDistrict("Ujjain", "MP04", "Madhya Pradesh", 13L),
                    createDistrict("Gwalior", "MP05", "Madhya Pradesh", 13L),
                    createDistrict("Sagar", "MP06", "Madhya Pradesh", 13L),
                    createDistrict("Rewa (MP)", "MP07", "Madhya Pradesh", 13L),
                    createDistrict("Seoni", "MP08", "Madhya Pradesh", 13L),
                    createDistrict("Mandla", "MP09", "Madhya Pradesh", 13L),
                    createDistrict("Dindori", "MP10", "Madhya Pradesh", 13L),
                    createDistrict("Balaghat", "MP11", "Madhya Pradesh", 13L),
                    createDistrict("Chhindwara", "MP12", "Madhya Pradesh", 13L),
                    createDistrict("Betul", "MP13", "Madhya Pradesh", 13L),
                    createDistrict("Hoshangabad", "MP14", "Madhya Pradesh", 13L),
                    createDistrict("Itarsi", "MP15", "Madhya Pradesh", 13L),
                    createDistrict("Khandwa", "MP16", "Madhya Pradesh", 13L),
                    createDistrict("Khargone", "MP17", "Madhya Pradesh", 13L),
                    createDistrict("Burhanpur", "MP18", "Madhya Pradesh", 13L),
                    createDistrict("Ratlam", "MP19", "Madhya Pradesh", 13L),
                    createDistrict("Mandsaur", "MP20", "Madhya Pradesh", 13L),
                    createDistrict("Neemuch", "MP21", "Madhya Pradesh", 13L),
                    createDistrict("Rajgarh", "MP22", "Madhya Pradesh", 13L),
                    createDistrict("Shajapur", "MP23", "Madhya Pradesh", 13L),
                    createDistrict("Dewas", "MP24", "Madhya Pradesh", 13L),
                    createDistrict("Dhar", "MP25", "Madhya Pradesh", 13L),
                    createDistrict("Mhow", "MP26", "Madhya Pradesh", 13L),
                    createDistrict("Vidisha", "MP27", "Madhya Pradesh", 13L),
                    createDistrict("Raisen", "MP28", "Madhya Pradesh", 13L),
                    createDistrict("Sehore", "MP29", "Madhya Pradesh", 13L),
                    createDistrict("Ashoknagar", "MP30", "Madhya Pradesh", 13L),
                    
                    // Maharashtra (36 districts)
                    createDistrict("Mumbai", "MH01", "Maharashtra", 14L),
                    createDistrict("Pune", "MH02", "Maharashtra", 14L),
                    createDistrict("Nagpur", "MH03", "Maharashtra", 14L),
                    createDistrict("Thane", "MH04", "Maharashtra", 14L),
                    createDistrict("Satara", "MH05", "Maharashtra", 14L),
                    createDistrict("Nashik", "MH06", "Maharashtra", 14L),
                    createDistrict("Aurangabad (MH)", "MH07", "Maharashtra", 14L),
                    createDistrict("Kolhapur", "MH08", "Maharashtra", 14L),
                    createDistrict("Sangli", "MH09", "Maharashtra", 14L),
                    createDistrict("Solapur", "MH10", "Maharashtra", 14L),
                    createDistrict("Ahmednagar", "MH11", "Maharashtra", 14L),
                    createDistrict("Bid", "MH12", "Maharashtra", 14L),
                    createDistrict("Parbhani", "MH13", "Maharashtra", 14L),
                    createDistrict("Jalna", "MH14", "Maharashtra", 14L),
                    createDistrict("Akola", "MH15", "Maharashtra", 14L),
                    createDistrict("Amravati", "MH16", "Maharashtra", 14L),
                    createDistrict("Yavatmal", "MH17", "Maharashtra", 14L),
                    createDistrict("Washim", "MH18", "Maharashtra", 14L),
                    createDistrict("Buldhana", "MH19", "Maharashtra", 14L),
                    createDistrict("Wardha", "MH20", "Maharashtra", 14L),
                    createDistrict("Chandrapur", "MH21", "Maharashtra", 14L),
                    createDistrict("Raigad (MH)", "MH22", "Maharashtra", 14L),
                    createDistrict("Ratnagiri", "MH23", "Maharashtra", 14L),
                    createDistrict("Sindhudurg", "MH24", "Maharashtra", 14L),
                    createDistrict("Dhule", "MH25", "Maharashtra", 14L),
                    createDistrict("Nandurbar", "MH26", "Maharashtra", 14L),
                    createDistrict("Jalgaon", "MH27", "Maharashtra", 14L),
                    createDistrict("Malegaon", "MH28", "Maharashtra", 14L),
                    
                    // Manipur (16 districts)
                    createDistrict("Imphal", "MN01", "Manipur", 15L),
                    createDistrict("Bishnupur", "MN02", "Manipur", 15L),
                    createDistrict("Churachandpur", "MN03", "Manipur", 15L),
                    createDistrict("Senapati", "MN04", "Manipur", 15L),
                    createDistrict("Tamenglong", "MN05", "Manipur", 15L),
                    createDistrict("Chandel", "MN06", "Manipur", 15L),
                    createDistrict("Ukhrul", "MN07", "Manipur", 15L),
                    createDistrict("Tengnoupal", "MN08", "Manipur", 15L),
                    
                    // Meghalaya (11 districts)
                    createDistrict("Shillong", "ML01", "Meghalaya", 16L),
                    createDistrict("Khasi Hills", "ML02", "Meghalaya", 16L),
                    createDistrict("Jaintia Hills", "ML03", "Meghalaya", 16L),
                    createDistrict("Garo Hills", "ML04", "Meghalaya", 16L),
                    createDistrict("West Garo Hills", "ML05", "Meghalaya", 16L),
                    createDistrict("East Garo Hills", "ML06", "Meghalaya", 16L),
                    createDistrict("South Garo Hills", "ML07", "Meghalaya", 16L),
                    createDistrict("West Khasi Hills", "ML08", "Meghalaya", 16L),
                    createDistrict("East Khasi Hills", "ML09", "Meghalaya", 16L),
                    createDistrict("South West Khasi Hills", "ML10", "Meghalaya", 16L),
                    
                    // Mizoram (11 districts)
                    createDistrict("Aizawl", "MZ01", "Mizoram", 17L),
                    createDistrict("Lunglei", "MZ02", "Mizoram", 17L),
                    createDistrict("Saiha", "MZ03", "Mizoram", 17L),
                    createDistrict("Mamit", "MZ04", "Mizoram", 17L),
                    createDistrict("Kolasib", "MZ05", "Mizoram", 17L),
                    createDistrict("Champhai", "MZ06", "Mizoram", 17L),
                    createDistrict("Lawngtlai", "MZ07", "Mizoram", 17L),
                    createDistrict("Serchhip", "MZ08", "Mizoram", 17L),
                    createDistrict("Hnahthial", "MZ09", "Mizoram", 17L),
                    createDistrict("Saitual", "MZ10", "Mizoram", 17L),
                    
                    // Nagaland (12 districts)
                    createDistrict("Kohima", "NL01", "Nagaland", 18L),
                    createDistrict("Dimapur", "NL02", "Nagaland", 18L),
                    createDistrict("Mokokchung", "NL03", "Nagaland", 18L),
                    createDistrict("Wokha", "NL04", "Nagaland", 18L),
                    createDistrict("Zunheboto", "NL05", "Nagaland", 18L),
                    createDistrict("Tuensang", "NL06", "Nagaland", 18L),
                    createDistrict("Longleng", "NL07", "Nagaland", 18L),
                    createDistrict("Peren", "NL08", "Nagaland", 18L),
                    createDistrict("Kiphire", "NL09", "Nagaland", 18L),
                    createDistrict("Phek", "NL10", "Nagaland", 18L),
                    
                    // Odisha (30 districts)
                    createDistrict("Bhubaneswar", "OD01", "Odisha", 19L),
                    createDistrict("Cuttack", "OD02", "Odisha", 19L),
                    createDistrict("Rourkela", "OD03", "Odisha", 19L),
                    createDistrict("Balasore", "OD04", "Odisha", 19L),
                    createDistrict("Puri", "OD05", "Odisha", 19L),
                    createDistrict("Ganjam", "OD06", "Odisha", 19L),
                    createDistrict("Gajapati", "OD07", "Odisha", 19L),
                    createDistrict("Kandhamal", "OD08", "Odisha", 19L),
                    createDistrict("Boudh", "OD09", "Odisha", 19L),
                    createDistrict("Angul", "OD10", "Odisha", 19L),
                    createDistrict("Dhenkanal", "OD11", "Odisha", 19L),
                    createDistrict("Jajpur", "OD12", "Odisha", 19L),
                    createDistrict("Jagatsinghpur", "OD13", "Odisha", 19L),
                    createDistrict("Kendrapara", "OD14", "Odisha", 19L),
                    createDistrict("Bhadrak", "OD15", "Odisha", 19L),
                    createDistrict("Khordha", "OD16", "Odisha", 19L),
                    createDistrict("Nayagarh", "OD17", "Odisha", 19L),
                    createDistrict("Sundargarh", "OD18", "Odisha", 19L),
                    createDistrict("Bargarh", "OD19", "Odisha", 19L),
                    createDistrict("Sambhalpur", "OD20", "Odisha", 19L),
                    createDistrict("Balangir", "OD21", "Odisha", 19L),
                    createDistrict("Nuapada", "OD22", "Odisha", 19L),
                    createDistrict("Kalahandi", "OD23", "Odisha", 19L),
                    createDistrict("Rayagada", "OD24", "Odisha", 19L),
                    createDistrict("Koraput", "OD25", "Odisha", 19L),
                    createDistrict("Nabarangpur", "OD26", "Odisha", 19L),
                    createDistrict("Mayurbhanj", "OD27", "Odisha", 19L),
                    createDistrict("Keonjhar", "OD28", "Odisha", 19L),
                    createDistrict("Deogaon", "OD29", "Odisha", 19L),
                    
                    // Punjab (23 districts)
                    createDistrict("Amritsar", "PB01", "Punjab", 20L),
                    createDistrict("Ludhiana", "PB02", "Punjab", 20L),
                    createDistrict("Jalandhar", "PB03", "Punjab", 20L),
                    createDistrict("Patiala", "PB04", "Punjab", 20L),
                    createDistrict("Mohali", "PB05", "Punjab", 20L),
                    createDistrict("Fatehgarh Sahib", "PB06", "Punjab", 20L),
                    createDistrict("Sangrur", "PB07", "Punjab", 20L),
                    createDistrict("Barnala", "PB08", "Punjab", 20L),
                    createDistrict("Faridkot", "PB09", "Punjab", 20L),
                    createDistrict("Bathinda", "PB10", "Punjab", 20L),
                    createDistrict("Gurdaspur", "PB11", "Punjab", 20L),
                    createDistrict("Hoshiarpur", "PB12", "Punjab", 20L),
                    createDistrict("Kapurthala", "PB13", "Punjab", 20L),
                    createDistrict("Nawanshahr", "PB14", "Punjab", 20L),
                    createDistrict("Rupnagar", "PB15", "Punjab", 20L),
                    createDistrict("Moga", "PB16", "Punjab", 20L),
                    createDistrict("Firozpur", "PB17", "Punjab", 20L),
                    createDistrict("Muktsar", "PB18", "Punjab", 20L),
                    createDistrict("Mansa", "PB19", "Punjab", 20L),
                    createDistrict("Tarn Taran", "PB20", "Punjab", 20L),
                    
                    // Rajasthan (33 districts)
                    createDistrict("Jaipur", "RJ01", "Rajasthan", 21L),
                    createDistrict("Jodhpur", "RJ02", "Rajasthan", 21L),
                    createDistrict("Udaipur", "RJ03", "Rajasthan", 21L),
                    createDistrict("Ajmer", "RJ04", "Rajasthan", 21L),
                    createDistrict("Bikaner", "RJ05", "Rajasthan", 21L),
                    createDistrict("Kota", "RJ06", "Rajasthan", 21L),
                    createDistrict("Pali", "RJ07", "Rajasthan", 21L),
                    createDistrict("Sikar", "RJ08", "Rajasthan", 21L),
                    createDistrict("Jhunjhunu", "RJ09", "Rajasthan", 21L),
                    createDistrict("Nagaur", "RJ10", "Rajasthan", 21L),
                    createDistrict("Hanumangarh", "RJ11", "Rajasthan", 21L),
                    createDistrict("Ganganagar", "RJ12", "Rajasthan", 21L),
                    createDistrict("Barmer", "RJ13", "Rajasthan", 21L),
                    createDistrict("Jaisalmer", "RJ14", "Rajasthan", 21L),
                    createDistrict("Sirohi", "RJ15", "Rajasthan", 21L),
                    createDistrict("Chittaurgarh", "RJ16", "Rajasthan", 21L),
                    createDistrict("Bhilwara", "RJ17", "Rajasthan", 21L),
                    createDistrict("Dungarpur", "RJ18", "Rajasthan", 21L),
                    createDistrict("Banswara", "RJ19", "Rajasthan", 21L),
                    createDistrict("Rajsamand", "RJ20", "Rajasthan", 21L),
                    createDistrict("Pratapgarh (RJ)", "RJ21", "Rajasthan", 21L),
                    createDistrict("Tonk", "RJ22", "Rajasthan", 21L),
                    createDistrict("Dausa", "RJ23", "Rajasthan", 21L),
                    createDistrict("Karauli", "RJ24", "Rajasthan", 21L),
                    createDistrict("Bundi", "RJ25", "Rajasthan", 21L),
                    createDistrict("Sawai Madhopur", "RJ26", "Rajasthan", 21L),
                    createDistrict("Alwar", "RJ27", "Rajasthan", 21L),
                    createDistrict("Bharatpur", "RJ28", "Rajasthan", 21L),
                    createDistrict("Dhaulpur", "RJ29", "Rajasthan", 21L),
                    
                    // Sikkim (4 districts)
                    createDistrict("Gangtok", "SK01", "Sikkim", 22L),
                    createDistrict("East Sikkim", "SK02", "Sikkim", 22L),
                    createDistrict("West Sikkim", "SK03", "Sikkim", 22L),
                    createDistrict("North Sikkim", "SK04", "Sikkim", 22L),
                    
                    // Tamil Nadu (38 districts)
                    createDistrict("Chennai", "TN01", "Tamil Nadu", 23L),
                    createDistrict("Coimbatore", "TN02", "Tamil Nadu", 23L),
                    createDistrict("Madurai", "TN03", "Tamil Nadu", 23L),
                    createDistrict("Tiruchirappalli", "TN04", "Tamil Nadu", 23L),
                    createDistrict("Salem", "TN05", "Tamil Nadu", 23L),
                    createDistrict("Kanyakumari", "TN06", "Tamil Nadu", 23L),
                    createDistrict("Chengalpattu", "TN07", "Tamil Nadu", 23L),
                    createDistrict("Cuddalore", "TN08", "Tamil Nadu", 23L),
                    createDistrict("Villupuram", "TN09", "Tamil Nadu", 23L),
                    createDistrict("Vellore", "TN10", "Tamil Nadu", 23L),
                    createDistrict("Ranipet", "TN11", "Tamil Nadu", 23L),
                    createDistrict("Tiruppattur", "TN12", "Tamil Nadu", 23L),
                    createDistrict("Kanchipuram", "TN13", "Tamil Nadu", 23L),
                    createDistrict("Dharamapuri", "TN14", "Tamil Nadu", 23L),
                    createDistrict("Nilgiris", "TN15", "Tamil Nadu", 23L),
                    createDistrict("Erode", "TN16", "Tamil Nadu", 23L),
                    createDistrict("Tiruppur", "TN17", "Tamil Nadu", 23L),
                    createDistrict("Dindigul", "TN18", "Tamil Nadu", 23L),
                    createDistrict("Teni", "TN19", "Tamil Nadu", 23L),
                    createDistrict("Virudhunagar", "TN20", "Tamil Nadu", 23L),
                    createDistrict("Tuticorin", "TN21", "Tamil Nadu", 23L),
                    createDistrict("Nagercoil", "TN22", "Tamil Nadu", 23L),
                    createDistrict("Thirunelveli", "TN23", "Tamil Nadu", 23L),
                    createDistrict("Ramanathapuram", "TN24", "Tamil Nadu", 23L),
                    createDistrict("Sivagangai", "TN25", "Tamil Nadu", 23L),
                    createDistrict("Kallakurichi", "TN27", "Tamil Nadu", 23L),
                    createDistrict("Chengalpattu (Rural)", "TN28", "Tamil Nadu", 23L),
                    
                    // Telangana (33 districts)
                    createDistrict("Hyderabad", "TG01", "Telangana", 24L),
                    createDistrict("Rangareddy", "TG02", "Telangana", 24L),
                    createDistrict("Medchal Malkajgiri", "TG03", "Telangana", 24L),
                    createDistrict("Warangal", "TG04", "Telangana", 24L),
                    createDistrict("Secunderabad", "TG05", "Telangana", 24L),
                    createDistrict("Karimnagar", "TG06", "Telangana", 24L),
                    createDistrict("Nizamabad", "TG07", "Telangana", 24L),
                    createDistrict("Adilabad", "TG08", "Telangana", 24L),
                    createDistrict("Mancherial", "TG09", "Telangana", 24L),
                    createDistrict("Mahbubnagar", "TG10", "Telangana", 24L),
                    createDistrict("Nalgonda", "TG11", "Telangana", 24L),
                    createDistrict("Khammam", "TG12", "Telangana", 24L),
                    createDistrict("Suryapet", "TG13", "Telangana", 24L),
                    createDistrict("Hanamkonda", "TG14", "Telangana", 24L),
                    
                    // Tripura (8 districts)
                    createDistrict("Agartala", "TR01", "Tripura", 25L),
                    createDistrict("West Tripura", "TR02", "Tripura", 25L),
                    createDistrict("Dhalai", "TR03", "Tripura", 25L),
                    createDistrict("North Tripura", "TR04", "Tripura", 25L),
                    createDistrict("South Tripura", "TR05", "Tripura", 25L),
                    createDistrict("Gomti", "TR06", "Tripura", 25L),
                    createDistrict("Khowai", "TR07", "Tripura", 25L),
                    createDistrict("Unakoti", "TR08", "Tripura", 25L),
                    
                    // Uttar Pradesh (75 districts)
                    createDistrict("Lucknow", "UP01", "Uttar Pradesh", 26L),
                    createDistrict("Kanpur", "UP02", "Uttar Pradesh", 26L),
                    createDistrict("Varanasi", "UP03", "Uttar Pradesh", 26L),
                    createDistrict("Agra", "UP04", "Uttar Pradesh", 26L),
                    createDistrict("Meerut", "UP05", "Uttar Pradesh", 26L),
                    createDistrict("Noida", "UP06", "Uttar Pradesh", 26L),
                    createDistrict("Ghaziabad", "UP07", "Uttar Pradesh", 26L),
                    createDistrict("Allahabad", "UP08", "Uttar Pradesh", 26L),
                    createDistrict("Bareilly", "UP09", "Uttar Pradesh", 26L),
                    createDistrict("Moradabad", "UP10", "Uttar Pradesh", 26L),
                    createDistrict("Aligarh", "UP11", "Uttar Pradesh", 26L),
                    createDistrict("Mathura", "UP12", "Uttar Pradesh", 26L),
                    createDistrict("Firozabad", "UP13", "Uttar Pradesh", 26L),
                    createDistrict("Mainpuri", "UP14", "Uttar Pradesh", 26L),
                    createDistrict("Etah", "UP15", "Uttar Pradesh", 26L),
                    createDistrict("Saharanpur", "UP16", "Uttar Pradesh", 26L),
                    createDistrict("Muzaffarnagar", "UP17", "Uttar Pradesh", 26L),
                    createDistrict("Bulandshahr", "UP18", "Uttar Pradesh", 26L),
                    createDistrict("Shamli", "UP19", "Uttar Pradesh", 26L),
                    createDistrict("Gautam Buddha Nagar", "UP20", "Uttar Pradesh", 26L),
                    createDistrict("Hapur", "UP21", "Uttar Pradesh", 26L),
                    createDistrict("Baghpat", "UP22", "Uttar Pradesh", 26L),
                    createDistrict("Bijnor", "UP23", "Uttar Pradesh", 26L),
                    createDistrict("Azamgarh", "UP24", "Uttar Pradesh", 26L),
                    createDistrict("Mau", "UP25", "Uttar Pradesh", 26L),
                    createDistrict("Ballia", "UP26", "Uttar Pradesh", 26L),
                    createDistrict("Ghazipur", "UP27", "Uttar Pradesh", 26L),
                    createDistrict("Jaunpur", "UP28", "Uttar Pradesh", 26L),
                    createDistrict("Mirzapur", "UP29", "Uttar Pradesh", 26L),
                    createDistrict("Sonbhadra", "UP30", "Uttar Pradesh", 26L),
                    createDistrict("Rewa (UP)", "UP31", "Uttar Pradesh", 26L),
                    createDistrict("Satna", "UP32", "Uttar Pradesh", 26L),
                    createDistrict("Banda", "UP33", "Uttar Pradesh", 26L),
                    createDistrict("Hamirpur (UP)", "UP34", "Uttar Pradesh", 26L),
                    createDistrict("Chitrakoot", "UP35", "Uttar Pradesh", 26L),
                    createDistrict("Fatehpur", "UP36", "Uttar Pradesh", 26L),
                    createDistrict("Sultanpur", "UP37", "Uttar Pradesh", 26L),
                    createDistrict("Raisuli", "UP38", "Uttar Pradesh", 26L),
                    createDistrict("Pratapgarh (UP)", "UP39", "Uttar Pradesh", 26L),
                    createDistrict("Kasganj", "UP40", "Uttar Pradesh", 26L),
                    createDistrict("Etawah", "UP41", "Uttar Pradesh", 26L),
                    createDistrict("Farrukhabad", "UP42", "Uttar Pradesh", 26L),
                    createDistrict("Kannauj", "UP43", "Uttar Pradesh", 26L),
                    createDistrict("Auraiya", "UP44", "Uttar Pradesh", 26L),
                    createDistrict("Jalaun", "UP45", "Uttar Pradesh", 26L),
                    createDistrict("Jhansi", "UP46", "Uttar Pradesh", 26L),
                    createDistrict("Lalitpur", "UP47", "Uttar Pradesh", 26L),
                    createDistrict("Guna", "UP48", "Uttar Pradesh", 26L),
                    createDistrict("Ashok Nagar", "UP49", "Uttar Pradesh", 26L),
                    createDistrict("Panna", "UP50", "Uttar Pradesh", 26L),
                    createDistrict("Rae Bareli", "UP51", "Uttar Pradesh", 26L),
                    createDistrict("Indore (UP)", "UP52", "Uttar Pradesh", 26L),
                    createDistrict("Gonda", "UP53", "Uttar Pradesh", 26L),
                    createDistrict("Bahraich", "UP54", "Uttar Pradesh", 26L),
                    createDistrict("Shravasti", "UP55", "Uttar Pradesh", 26L),
                    createDistrict("Balrampur", "UP56", "Uttar Pradesh", 26L),
                    createDistrict("Siddharthnagar", "UP57", "Uttar Pradesh", 26L),
                    createDistrict("Basti", "UP58", "Uttar Pradesh", 26L),
                    createDistrict("Sant Kabir Nagar", "UP59", "Uttar Pradesh", 26L),
                    createDistrict("Ambedkar Nagar", "UP60", "Uttar Pradesh", 26L),
                    createDistrict("Kushinagar", "UP61", "Uttar Pradesh", 26L),
                    createDistrict("Devaria", "UP62", "Uttar Pradesh", 26L),
                    createDistrict("Pilibhit", "UP63", "Uttar Pradesh", 26L),
                    createDistrict("Shahjahanpur", "UP64", "Uttar Pradesh", 26L),
                    createDistrict("Hardoi", "UP65", "Uttar Pradesh", 26L),
                    createDistrict("Unnao", "UP66", "Uttar Pradesh", 26L),
                    createDistrict("Sitapur", "UP67", "Uttar Pradesh", 26L),
                    createDistrict("Lakhimpur Kheri", "UP68", "Uttar Pradesh", 26L),
                    
                    // Uttarakhand (13 districts)
                    createDistrict("Dehradun", "UT01", "Uttarakhand", 27L),
                    createDistrict("Nainital", "UT02", "Uttarakhand", 27L),
                    createDistrict("Almora", "UT03", "Uttarakhand", 27L),
                    createDistrict("Pithoragarh", "UT04", "Uttarakhand", 27L),
                    createDistrict("Bageshwar", "UT05", "Uttarakhand", 27L),
                    createDistrict("Champawat", "UT06", "Uttarakhand", 27L),
                    createDistrict("Pauri Garhwal", "UT07", "Uttarakhand", 27L),
                    createDistrict("Rudraprayag", "UT08", "Uttarakhand", 27L),
                    createDistrict("Chamoli", "UT09", "Uttarakhand", 27L),
                    createDistrict("Uttarkashi", "UT10", "Uttarakhand", 27L),
                    createDistrict("Udam Singh Nagar", "UT11", "Uttarakhand", 27L),
                    createDistrict("Tehri Garhwal", "UT12", "Uttarakhand", 27L),
                    
                    // West Bengal (23 districts)
                    createDistrict("Kolkata", "WB01", "West Bengal", 28L),
                    createDistrict("Howrah", "WB02", "West Bengal", 28L),
                    createDistrict("Darjeeling", "WB03", "West Bengal", 28L),
                    createDistrict("Siliguri", "WB04", "West Bengal", 28L),
                    createDistrict("Jalpaiguri", "WB05", "West Bengal", 28L),
                    createDistrict("Malda", "WB06", "West Bengal", 28L),
                    createDistrict("Murshidabad", "WB07", "West Bengal", 28L),
                    createDistrict("Birbhum", "WB08", "West Bengal", 28L),
                    createDistrict("Bankura", "WB09", "West Bengal", 28L),
                    createDistrict("Puruliya", "WB10", "West Bengal", 28L),
                    createDistrict("Midnapore", "WB11", "West Bengal", 28L),
                    createDistrict("Medinipur", "WB12", "West Bengal", 28L),
                    createDistrict("Hooghly", "WB13", "West Bengal", 28L),
                    createDistrict("Hasan", "WB14", "West Bengal", 28L),
                    createDistrict("North 24 Parganas", "WB15", "West Bengal", 28L),
                    createDistrict("South 24 Parganas", "WB16", "West Bengal", 28L),
                    createDistrict("Purulia", "WB17", "West Bengal", 28L),
                    createDistrict("Paschim Bardhaman", "WB18", "West Bengal", 28L),
                    
                    // Union Territories
                    createDistrict("Andaman", "AN01", "Andaman and Nicobar Islands", 29L),
                    createDistrict("Nicobar", "AN02", "Andaman and Nicobar Islands", 29L),
                    createDistrict("Chandigarh", "CH01", "Chandigarh", 30L),
                    createDistrict("Dadra", "DN01", "Dadra and Nagar Haveli", 31L),
                    createDistrict("Daman", "DD01", "Daman and Diu", 32L),
                    createDistrict("Diu", "DD02", "Daman and Diu", 32L),
                    createDistrict("Lakshadweep", "LD01", "Lakshadweep", 33L),
                    createDistrict("New Delhi", "DL01", "Delhi", 34L),
                    createDistrict("Puducherry", "PY01", "Puducherry", 35L),
                    createDistrict("Ladakh", "LA01", "Ladakh", 36L),
                    createDistrict("Jammu", "JK01", "Jammu and Kashmir", 37L),
                    createDistrict("Kashmir", "JK02", "Jammu and Kashmir", 37L)
            );

            districtMasterRepository.saveAll(allDistricts);
            log.info("District Master data initialized: {} districts inserted", allDistricts.size());

        } catch (Exception e) {
            log.error("Error initializing District Master data: {}", e.getMessage());
        }
    }

    /**
     * Helper method to create district
     */
    private DistrictMaster createDistrict(String name, String code, String state, Long stateId) {
        return DistrictMaster.builder()
                .name(name)
                .code(code)
                .description(state)
                .stateId(stateId)
                .isActive(true)
                .build();
    }
}
