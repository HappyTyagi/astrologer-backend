-- Update District Master with Latitude and Longitude for all Indian districts
-- This script updates existing districts and can be extended to add missing ones

USE astrodb;

-- Andhra Pradesh (state_id = 1)
UPDATE district_master SET latitude = 17.6868, longitude = 83.2185 WHERE name = 'Visakhapatnam' AND state_id = 1;
UPDATE district_master SET latitude = 16.5193, longitude = 80.6305 WHERE name = 'Krishna' AND state_id = 1;
UPDATE district_master SET latitude = 16.3067, longitude = 80.4365 WHERE name = 'Guntur' AND state_id = 1;
UPDATE district_master SET latitude = 14.4426, longitude = 79.9865 WHERE name = 'Nellore' AND state_id = 1;
UPDATE district_master SET latitude = 13.4172, longitude = 79.1325 WHERE name = 'Chittoor' AND state_id = 1;
UPDATE district_master SET latitude = 13.6288, longitude = 79.4192 WHERE name = 'Tirupati' AND state_id = 1;
UPDATE district_master SET latitude = 14.6819, longitude = 77.6006 WHERE name = 'Anantapur' AND state_id = 1;
UPDATE district_master SET latitude = 14.4673, longitude = 78.8242 WHERE name = 'Kadapa' AND state_id = 1;
UPDATE district_master SET latitude = 15.8281, longitude = 78.0373 WHERE name = 'Kurnool' AND state_id = 1;
UPDATE district_master SET latitude = 15.5057, longitude = 79.5941 WHERE name = 'Prakasam' AND state_id = 1;
UPDATE district_master SET latitude = 16.1734, longitude = 80.6183 WHERE name = 'Palnadu' AND state_id = 1;
UPDATE district_master SET latitude = 17.0005, longitude = 81.8040 WHERE name = 'East Godavari' AND state_id = 1;
UPDATE district_master SET latitude = 16.9891, longitude = 81.5318 WHERE name = 'West Godavari' AND state_id = 1;
UPDATE district_master SET latitude = 18.1124, longitude = 83.3975 WHERE name = 'Vizianagaram' AND state_id = 1;
UPDATE district_master SET latitude = 18.4386, longitude = 83.4244 WHERE name = 'Srikakulam' AND state_id = 1;

-- Get state_ids for other states to continue
-- You'll need to check your state_master table for correct IDs

-- Example queries to update more districts - Add coordinates for all states:
-- SELECT id, name FROM state_master ORDER BY name;

-- Maharashtra districts (assuming state_id = 21)
UPDATE district_master SET latitude = 18.5204, longitude = 73.8567 WHERE name = 'Pune' AND state_id = (SELECT id FROM state_master WHERE name = 'Maharashtra' LIMIT 1);
UPDATE district_master SET latitude = 19.0760, longitude = 72.8777 WHERE name = 'Mumbai' AND state_id = (SELECT id FROM state_master WHERE name = 'Maharashtra' LIMIT 1);
UPDATE district_master SET latitude = 19.9975, longitude = 73.7898 WHERE name = 'Nashik' AND state_id = (SELECT id FROM state_master WHERE name = 'Maharashtra' LIMIT 1);
UPDATE district_master SET latitude = 21.1458, longitude = 79.0882 WHERE name = 'Nagpur' AND state_id = (SELECT id FROM state_master WHERE name = 'Maharashtra' LIMIT 1);
UPDATE district_master SET latitude = 19.8762, longitude = 75.3433 WHERE name = 'Aurangabad' AND state_id = (SELECT id FROM state_master WHERE name = 'Maharashtra' LIMIT 1);

-- Karnataka districts
UPDATE district_master SET latitude = 12.9716, longitude = 77.5946 WHERE name = 'Bangalore' AND state_id = (SELECT id FROM state_master WHERE name = 'Karnataka' LIMIT 1);
UPDATE district_master SET latitude = 12.2958, longitude = 76.6394 WHERE name = 'Mysore' AND state_id = (SELECT id FROM state_master WHERE name = 'Karnataka' LIMIT 1);
UPDATE district_master SET latitude = 15.3647, longitude = 75.1240 WHERE name = 'Hubli' AND state_id = (SELECT id FROM state_master WHERE name = 'Karnataka' LIMIT 1);
UPDATE district_master SET latitude = 13.3379, longitude = 74.7421 WHERE name = 'Mangalore' AND state_id = (SELECT id FROM state_master WHERE name = 'Karnataka' LIMIT 1);
UPDATE district_master SET latitude = 15.8497, longitude = 74.4977 WHERE name = 'Belgaum' AND state_id = (SELECT id FROM state_master WHERE name = 'Karnataka' LIMIT 1);

-- Tamil Nadu districts
UPDATE district_master SET latitude = 13.0827, longitude = 80.2707 WHERE name = 'Chennai' AND state_id = (SELECT id FROM state_master WHERE name = 'Tamil Nadu' LIMIT 1);
UPDATE district_master SET latitude = 11.0168, longitude = 76.9558 WHERE name = 'Coimbatore' AND state_id = (SELECT id FROM state_master WHERE name = 'Tamil Nadu' LIMIT 1);
UPDATE district_master SET latitude = 10.7905, longitude = 78.7047 WHERE name = 'Madurai' AND state_id = (SELECT id FROM state_master WHERE name = 'Tamil Nadu' LIMIT 1);
UPDATE district_master SET latitude = 10.9601, longitude = 77.9524 WHERE name = 'Tiruchirappalli' AND state_id = (SELECT id FROM state_master WHERE name = 'Tamil Nadu' LIMIT 1);
UPDATE district_master SET latitude = 11.3410, longitude = 77.7172 WHERE name = 'Salem' AND state_id = (SELECT id FROM state_master WHERE name = 'Tamil Nadu' LIMIT 1);

-- Kerala districts
UPDATE district_master SET latitude = 10.8505, longitude = 76.2711 WHERE name = 'Palakkad' AND state_id = (SELECT id FROM state_master WHERE name = 'Kerala' LIMIT 1);
UPDATE district_master SET latitude = 11.2588, longitude = 75.7804 WHERE name = 'Kozhikode' AND state_id = (SELECT id FROM state_master WHERE name = 'Kerala' LIMIT 1);
UPDATE district_master SET latitude = 9.9312, longitude = 76.2673 WHERE name = 'Kochi' AND state_id = (SELECT id FROM state_master WHERE name = 'Kerala' LIMIT 1);
UPDATE district_master SET latitude = 8.5241, longitude = 76.9366 WHERE name = 'Thiruvananthapuram' AND state_id = (SELECT id FROM state_master WHERE name = 'Kerala' LIMIT 1);
UPDATE district_master SET latitude = 10.5276, longitude = 76.2144 WHERE name = 'Thrissur' AND state_id = (SELECT id FROM state_master WHERE name = 'Kerala' LIMIT 1);

-- Delhi
UPDATE district_master SET latitude = 28.7041, longitude = 77.1025 WHERE name = 'New Delhi' AND state_id = (SELECT id FROM state_master WHERE name = 'Delhi' LIMIT 1);
UPDATE district_master SET latitude = 28.6692, longitude = 77.4538 WHERE name = 'East Delhi' AND state_id = (SELECT id FROM state_master WHERE name = 'Delhi' LIMIT 1);
UPDATE district_master SET latitude = 28.6692, longitude = 77.1750 WHERE name = 'Central Delhi' AND state_id = (SELECT id FROM state_master WHERE name = 'Delhi' LIMIT 1);
UPDATE district_master SET latitude = 28.7041, longitude = 77.1025 WHERE name = 'South Delhi' AND state_id = (SELECT id FROM state_master WHERE name = 'Delhi' LIMIT 1);

-- West Bengal districts
UPDATE district_master SET latitude = 22.5726, longitude = 88.3639 WHERE name = 'Kolkata' AND state_id = (SELECT id FROM state_master WHERE name = 'West Bengal' LIMIT 1);
UPDATE district_master SET latitude = 22.5958, longitude = 88.2636 WHERE name = 'Howrah' AND state_id = (SELECT id FROM state_master WHERE name = 'West Bengal' LIMIT 1);
UPDATE district_master SET latitude = 26.7271, longitude = 88.3953 WHERE name = 'Darjeeling' AND state_id = (SELECT id FROM state_master WHERE name = 'West Bengal' LIMIT 1);
UPDATE district_master SET latitude = 23.3441, longitude = 85.3096 WHERE name = 'Purulia' AND state_id = (SELECT id FROM state_master WHERE name = 'West Bengal' LIMIT 1);

-- Rajasthan districts
UPDATE district_master SET latitude = 26.9124, longitude = 75.7873 WHERE name = 'Jaipur' AND state_id = (SELECT id FROM state_master WHERE name = 'Rajasthan' LIMIT 1);
UPDATE district_master SET latitude = 26.4499, longitude = 74.6399 WHERE name = 'Ajmer' AND state_id = (SELECT id FROM state_master WHERE name = 'Rajasthan' LIMIT 1);
UPDATE district_master SET latitude = 25.2138, longitude = 75.8648 WHERE name = 'Kota' AND state_id = (SELECT id FROM state_master WHERE name = 'Rajasthan' LIMIT 1);
UPDATE district_master SET latitude = 24.5854, longitude = 73.7125 WHERE name = 'Udaipur' AND state_id = (SELECT id FROM state_master WHERE name = 'Rajasthan' LIMIT 1);
UPDATE district_master SET latitude = 26.2389, longitude = 73.0243 WHERE name = 'Jodhpur' AND state_id = (SELECT id FROM state_master WHERE name = 'Rajasthan' LIMIT 1);

-- Gujarat districts
UPDATE district_master SET latitude = 23.0225, longitude = 72.5714 WHERE name = 'Ahmedabad' AND state_id = (SELECT id FROM state_master WHERE name = 'Gujarat' LIMIT 1);
UPDATE district_master SET latitude = 21.1702, longitude = 72.8311 WHERE name = 'Surat' AND state_id = (SELECT id FROM state_master WHERE name = 'Gujarat' LIMIT 1);
UPDATE district_master SET latitude = 22.3072, longitude = 73.1812 WHERE name = 'Vadodara' AND state_id = (SELECT id FROM state_master WHERE name = 'Gujarat' LIMIT 1);
UPDATE district_master SET latitude = 22.2587, longitude = 71.1924 WHERE name = 'Rajkot' AND state_id = (SELECT id FROM state_master WHERE name = 'Gujarat' LIMIT 1);

-- Uttar Pradesh districts
UPDATE district_master SET latitude = 26.8467, longitude = 80.9462 WHERE name = 'Lucknow' AND state_id = (SELECT id FROM state_master WHERE name = 'Uttar Pradesh' LIMIT 1);
UPDATE district_master SET latitude = 25.4358, longitude = 81.8463 WHERE name = 'Allahabad' AND state_id = (SELECT id FROM state_master WHERE name = 'Uttar Pradesh' LIMIT 1);
UPDATE district_master SET latitude = 25.3176, longitude = 82.9739 WHERE name = 'Varanasi' AND state_id = (SELECT id FROM state_master WHERE name = 'Uttar Pradesh' LIMIT 1);
UPDATE district_master SET latitude = 27.1767, longitude = 78.0081 WHERE name = 'Agra' AND state_id = (SELECT id FROM state_master WHERE name = 'Uttar Pradesh' LIMIT 1);
UPDATE district_master SET latitude = 28.9845, longitude = 77.7064 WHERE name = 'Meerut' AND state_id = (SELECT id FROM state_master WHERE name = 'Uttar Pradesh' LIMIT 1);
UPDATE district_master SET latitude = 26.4499, longitude = 80.3319 WHERE name = 'Kanpur' AND state_id = (SELECT id FROM state_master WHERE name = 'Uttar Pradesh' LIMIT 1);
UPDATE district_master SET latitude = 28.6139, longitude = 77.2090 WHERE name = 'Ghaziabad' AND state_id = (SELECT id FROM state_master WHERE name = 'Uttar Pradesh' LIMIT 1);
UPDATE district_master SET latitude = 28.9931, longitude = 77.0151 WHERE name = 'Gautam Buddha Nagar' AND state_id = (SELECT id FROM state_master WHERE name = 'Uttar Pradesh' LIMIT 1);

-- Madhya Pradesh districts
UPDATE district_master SET latitude = 23.2599, longitude = 77.4126 WHERE name = 'Bhopal' AND state_id = (SELECT id FROM state_master WHERE name = 'Madhya Pradesh' LIMIT 1);
UPDATE district_master SET latitude = 22.7196, longitude = 75.8577 WHERE name = 'Indore' AND state_id = (SELECT id FROM state_master WHERE name = 'Madhya Pradesh' LIMIT 1);
UPDATE district_master SET latitude = 23.1765, longitude = 79.9511 WHERE name = 'Jabalpur' AND state_id = (SELECT id FROM state_master WHERE name = 'Madhya Pradesh' LIMIT 1);
UPDATE district_master SET latitude = 24.5854, longitude = 78.5707 WHERE name = 'Gwalior' AND state_id = (SELECT id FROM state_master WHERE name = 'Madhya Pradesh' LIMIT 1);

-- Punjab districts
UPDATE district_master SET latitude = 30.9010, longitude = 75.8573 WHERE name = 'Ludhiana' AND state_id = (SELECT id FROM state_master WHERE name = 'Punjab' LIMIT 1);
UPDATE district_master SET latitude = 31.6340, longitude = 74.8723 WHERE name = 'Amritsar' AND state_id = (SELECT id FROM state_master WHERE name = 'Punjab' LIMIT 1);
UPDATE district_master SET latitude = 30.3398, longitude = 76.3869 WHERE name = 'Mohali' AND state_id = (SELECT id FROM state_master WHERE name = 'Punjab' LIMIT 1);
UPDATE district_master SET latitude = 31.3260, longitude = 75.5762 WHERE name = 'Jalandhar' AND state_id = (SELECT id FROM state_master WHERE name = 'Punjab' LIMIT 1);

-- Haryana districts
UPDATE district_master SET latitude = 28.4595, longitude = 77.0266 WHERE name = 'Gurgaon' AND state_id = (SELECT id FROM state_master WHERE name = 'Haryana' LIMIT 1);
UPDATE district_master SET latitude = 28.9845, longitude = 76.8721 WHERE name = 'Faridabad' AND state_id = (SELECT id FROM state_master WHERE name = 'Haryana' LIMIT 1);
UPDATE district_master SET latitude = 29.1492, longitude = 76.0853 WHERE name = 'Hisar' AND state_id = (SELECT id FROM state_master WHERE name = 'Haryana' LIMIT 1);
UPDATE district_master SET latitude = 29.3803, longitude = 76.9783 WHERE name = 'Rohtak' AND state_id = (SELECT id FROM state_master WHERE name = 'Haryana' LIMIT 1);

-- Bihar districts
UPDATE district_master SET latitude = 25.5941, longitude = 85.1376 WHERE name = 'Patna' AND state_id = (SELECT id FROM state_master WHERE name = 'Bihar' LIMIT 1);
UPDATE district_master SET latitude = 25.0961, longitude = 85.3131 WHERE name = 'Gaya' AND state_id = (SELECT id FROM state_master WHERE name = 'Bihar' LIMIT 1);
UPDATE district_master SET latitude = 26.1158, longitude = 85.3131 WHERE name = 'Muzaffarpur' AND state_id = (SELECT id FROM state_master WHERE name = 'Bihar' LIMIT 1);
UPDATE district_master SET latitude = 25.3708, longitude = 86.4636 WHERE name = 'Bhagalpur' AND state_id = (SELECT id FROM state_master WHERE name = 'Bihar' LIMIT 1);

-- Odisha districts
UPDATE district_master SET latitude = 20.2961, longitude = 85.8245 WHERE name = 'Bhubaneswar' AND state_id = (SELECT id FROM state_master WHERE name = 'Odisha' LIMIT 1);
UPDATE district_master SET latitude = 20.9517, longitude = 85.0985 WHERE name = 'Cuttack' AND state_id = (SELECT id FROM state_master WHERE name = 'Odisha' LIMIT 1);
UPDATE district_master SET latitude = 22.2497, longitude = 84.8644 WHERE name = 'Rourkela' AND state_id = (SELECT id FROM state_master WHERE name = 'Odisha' LIMIT 1);

-- Telangana districts
UPDATE district_master SET latitude = 17.3850, longitude = 78.4867 WHERE name = 'Hyderabad' AND state_id = (SELECT id FROM state_master WHERE name = 'Telangana' LIMIT 1);
UPDATE district_master SET latitude = 18.4386, longitude = 79.1288 WHERE name = 'Warangal' AND state_id = (SELECT id FROM state_master WHERE name = 'Telangana' LIMIT 1);
UPDATE district_master SET latitude = 17.9689, longitude = 79.5941 WHERE name = 'Khammam' AND state_id = (SELECT id FROM state_master WHERE name = 'Telangana' LIMIT 1);
UPDATE district_master SET latitude = 16.6944, longitude = 78.1389 WHERE name = 'Mahbubnagar' AND state_id = (SELECT id FROM state_master WHERE name = 'Telangana' LIMIT 1);

-- Assam districts
UPDATE district_master SET latitude = 26.1445, longitude = 91.7362 WHERE name = 'Guwahati' AND state_id = (SELECT id FROM state_master WHERE name = 'Assam' LIMIT 1);
UPDATE district_master SET latitude = 26.3587, longitude = 92.9376 WHERE name = 'Dibrugarh' AND state_id = (SELECT id FROM state_master WHERE name = 'Assam' LIMIT 1);
UPDATE district_master SET latitude = 26.1433, longitude = 91.7898 WHERE name = 'Jorhat' AND state_id = (SELECT id FROM state_master WHERE name = 'Assam' LIMIT 1);

-- Jharkhand districts
UPDATE district_master SET latitude = 23.3441, longitude = 85.3096 WHERE name = 'Ranchi' AND state_id = (SELECT id FROM state_master WHERE name = 'Jharkhand' LIMIT 1);
UPDATE district_master SET latitude = 22.8046, longitude = 86.2029 WHERE name = 'Jamshedpur' AND state_id = (SELECT id FROM state_master WHERE name = 'Jharkhand' LIMIT 1);
UPDATE district_master SET latitude = 23.6888, longitude = 86.1470 WHERE name = 'Dhanbad' AND state_id = (SELECT id FROM state_master WHERE name = 'Jharkhand' LIMIT 1);

-- Chhattisgarh districts
UPDATE district_master SET latitude = 21.2514, longitude = 81.6296 WHERE name = 'Raipur' AND state_id = (SELECT id FROM state_master WHERE name = 'Chhattisgarh' LIMIT 1);
UPDATE district_master SET latitude = 21.1938, longitude = 81.2460 WHERE name = 'Bhilai' AND state_id = (SELECT id FROM state_master WHERE name = 'Chhattisgarh' LIMIT 1);
UPDATE district_master SET latitude = 22.0797, longitude = 82.1409 WHERE name = 'Bilaspur' AND state_id = (SELECT id FROM state_master WHERE name = 'Chhattisgarh' LIMIT 1);

-- Uttarakhand districts
UPDATE district_master SET latitude = 30.3165, longitude = 78.0322 WHERE name = 'Dehradun' AND state_id = (SELECT id FROM state_master WHERE name = 'Uttarakhand' LIMIT 1);
UPDATE district_master SET latitude = 29.3803, longitude = 79.4636 WHERE name = 'Haridwar' AND state_id = (SELECT id FROM state_master WHERE name = 'Uttarakhand' LIMIT 1);
UPDATE district_master SET latitude = 29.2180, longitude = 79.5288 WHERE name = 'Roorkee' AND state_id = (SELECT id FROM state_master WHERE name = 'Uttarakhand' LIMIT 1);

-- Himachal Pradesh districts
UPDATE district_master SET latitude = 31.1048, longitude = 77.1734 WHERE name = 'Shimla' AND state_id = (SELECT id FROM state_master WHERE name = 'Himachal Pradesh' LIMIT 1);
UPDATE district_master SET latitude = 32.2190, longitude = 76.3234 WHERE name = 'Dharamshala' AND state_id = (SELECT id FROM state_master WHERE name = 'Himachal Pradesh' LIMIT 1);
UPDATE district_master SET latitude = 31.8955, longitude = 76.5359 WHERE name = 'Mandi' AND state_id = (SELECT id FROM state_master WHERE name = 'Himachal Pradesh' LIMIT 1);

-- Jammu and Kashmir districts
UPDATE district_master SET latitude = 34.0837, longitude = 74.7973 WHERE name = 'Srinagar' AND state_id = (SELECT id FROM state_master WHERE name = 'Jammu and Kashmir' LIMIT 1);
UPDATE district_master SET latitude = 32.7266, longitude = 74.8570 WHERE name = 'Jammu' AND state_id = (SELECT id FROM state_master WHERE name = 'Jammu and Kashmir' LIMIT 1);

-- Goa districts
UPDATE district_master SET latitude = 15.2993, longitude = 74.1240 WHERE name = 'Panaji' AND state_id = (SELECT id FROM state_master WHERE name = 'Goa' LIMIT 1);
UPDATE district_master SET latitude = 15.4909, longitude = 73.8278 WHERE name = 'Mapusa' AND state_id = (SELECT id FROM state_master WHERE name = 'Goa' LIMIT 1);

-- Verify update count
SELECT COUNT(*) as updated_districts FROM district_master WHERE latitude IS NOT NULL AND longitude IS NOT NULL;
SELECT COUNT(*) as pending_districts FROM district_master WHERE latitude IS NULL OR longitude IS NULL;

-- Show some sample updated records
SELECT id, name, state_id, latitude, longitude FROM district_master WHERE latitude IS NOT NULL LIMIT 20;
