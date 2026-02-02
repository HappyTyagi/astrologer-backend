# Astrology System - Mobile Integration Guide

## Overview
Complete guide to integrate the new Astrology Services API endpoints into the Flutter mobile application.

---

## 📱 Mobile API Configuration

### Update API Config (ApiConfig.dart or similar)

```dart
class AstroApiConfig {
  // Astrology Services Base URL
  static const String astroBase = '/api/v1/astro-services';
  
  // Kundli Endpoints
  static const String fullKundli = '$astroBase/kundli/calculate';
  
  // Dasha Endpoints
  static const String dashaCalculations = '$astroBase/dasha';
  static const String yoginiDasha = '$astroBase/dasha/yogini';
  
  // Compatibility Endpoints
  static const String compatibilityMatch = '$astroBase/compatibility/match';
  
  // Remedy Endpoints
  static const String remedySuggestions = '$astroBase/remedy';
  
  // Muhurat Endpoints
  static const String findMuhurat = '$astroBase/muhurat/find';
  static const String monthlyMuhurat = '$astroBase/muhurat/monthly';
  
  // Prediction Endpoints
  static const String dailyHoroscope = '$astroBase/prediction/daily-horoscope';
  static const String weeklyHoroscope = '$astroBase/prediction/weekly-horoscope';
  static const String monthlyHoroscope = '$astroBase/prediction/monthly-horoscope';
  static const String transitAnalysis = '$astroBase/prediction/transit-analysis';
  static const String sadeSatiAnalysis = '$astroBase/prediction/sade-sati';
  static const String dhaiyaAnalysis = '$astroBase/prediction/dhaiya';
  
  // System Endpoints
  static const String astroHealth = '$astroBase/health';
}
```

---

## 🔗 API Usage Examples

### 1. Generate Full Kundli/Birth Chart

```dart
Future<Map> generateFullKundli({
  required int userId,
  required String dateOfBirth,  // Format: "2000-01-15"
  required String timeOfBirth,   // Format: "14:30"
  double? latitude,
  double? longitude,
  String? timezone,
}) async {
  try {
    final response = await httpClient.post(
      Uri.parse(AstroApiConfig.fullKundli),
      queryParameters: {
        'userId': userId.toString(),
        'dateOfBirth': dateOfBirth,
        'timeOfBirth': timeOfBirth,
        if (latitude != null) 'latitude': latitude.toString(),
        if (longitude != null) 'longitude': longitude.toString(),
        if (timezone != null) 'timezone': timezone,
      },
      headers: {'Authorization': 'Bearer $token'},
    );
    
    if (response.statusCode == 200) {
      return jsonDecode(response.body);
    }
    throw Exception('Failed to generate kundli');
  } catch (e) {
    print('Error: $e');
    rethrow;
  }
}
```

### 2. Get Dasha Calculations

```dart
Future<Map> getDashaCalculations(int chartId) async {
  try {
    final response = await httpClient.get(
      Uri.parse('${AstroApiConfig.dashaCalculations}/$chartId'),
      headers: {'Authorization': 'Bearer $token'},
    );
    
    if (response.statusCode == 200) {
      final data = jsonDecode(response.body);
      return data['data']; // Contains mahadasha, antardasha, etc.
    }
    throw Exception('Failed to fetch dasha');
  } catch (e) {
    print('Error: $e');
    rethrow;
  }
}
```

### 3. Check Compatibility (Marriage Matching)

```dart
Future<Map> checkCompatibility({
  required int groomChartId,
  required int brideChartId,
}) async {
  try {
    final response = await httpClient.post(
      Uri.parse(AstroApiConfig.compatibilityMatch),
      queryParameters: {
        'groomChartId': groomChartId.toString(),
        'brideChartId': brideChartId.toString(),
      },
      headers: {'Authorization': 'Bearer $token'},
    );
    
    if (response.statusCode == 200) {
      final data = jsonDecode(response.body);
      return data['compatibility']; // Returns gun milan score, doshas, prediction
    }
    throw Exception('Failed to check compatibility');
  } catch (e) {
    print('Error: $e');
    rethrow;
  }
}
```

### 4. Get Remedy Suggestions

```dart
Future<Map> getRemedySuggestions(int chartId) async {
  try {
    final response = await httpClient.get(
      Uri.parse('${AstroApiConfig.remedySuggestions}/$chartId'),
      headers: {'Authorization': 'Bearer $token'},
    );
    
    if (response.statusCode == 200) {
      final data = jsonDecode(response.body);
      // Returns grouped remedies by type (gemstones, rudraksha, mantras, etc.)
      return data['remedies'];
    }
    throw Exception('Failed to fetch remedies');
  } catch (e) {
    print('Error: $e');
    rethrow;
  }
}
```

### 5. Find Auspicious Muhurat

```dart
Future<List> findMuhurat({
  required String eventType, // "marriage", "business", "journey", etc.
  int durationDays = 30,
}) async {
  try {
    final response = await httpClient.get(
      Uri.parse(AstroApiConfig.findMuhurat).replace(
        queryParameters: {
          'eventType': eventType,
          'durationDays': durationDays.toString(),
        },
      ),
      headers: {'Authorization': 'Bearer $token'},
    );
    
    if (response.statusCode == 200) {
      final data = jsonDecode(response.body);
      return data['muhurats']; // Returns list of best times
    }
    throw Exception('Failed to find muhurat');
  } catch (e) {
    print('Error: $e');
    rethrow;
  }
}
```

### 6. Get Daily Horoscope

```dart
Future<Map> getDailyHoroscope({
  required String sunSign, // "Aries", "Taurus", etc.
  String? date,            // Format: "2024-01-15", defaults to today
}) async {
  try {
    final response = await httpClient.get(
      Uri.parse(AstroApiConfig.dailyHoroscope).replace(
        queryParameters: {
          'sunSign': sunSign,
          if (date != null) 'date': date,
        },
      ),
      headers: {'Authorization': 'Bearer $token'},
    );
    
    if (response.statusCode == 200) {
      final data = jsonDecode(response.body);
      // Returns love, career, health, finance, lucky details, advice
      return data['horoscope'];
    }
    throw Exception('Failed to fetch horoscope');
  } catch (e) {
    print('Error: $e');
    rethrow;
  }
}
```

### 7. Get Sade Sati Analysis

```dart
Future<Map> getSadeSatiAnalysis(String moonSign) async {
  try {
    final response = await httpClient.get(
      Uri.parse(AstroApiConfig.sadeSatiAnalysis),
      queryParameters: {'moonSign': moonSign},
      headers: {'Authorization': 'Bearer $token'},
    );
    
    if (response.statusCode == 200) {
      final data = jsonDecode(response.body);
      // Returns: isInSadeSati, phase, duration, remedies, advice
      return data['sadeSati'];
    }
    throw Exception('Failed to fetch Sade Sati analysis');
  } catch (e) {
    print('Error: $e');
    rethrow;
  }
}
```

---

## 🎨 UI Components to Build

### 1. Birth Chart Display
```dart
class BirthChartView extends StatelessWidget {
  final Map kundliData;
  
  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        // Planetary positions display
        _buildPlanetaryGrid(kundliData['planets']),
        
        // House cusps display
        _buildHousesGrid(kundliData['houses']),
        
        // Doshas warning section
        _buildDoshasSection(kundliData['doshas']),
        
        // Yogas section
        _buildYogasSection(kundliData['yogas']),
      ],
    );
  }
}
```

### 2. Dasha Timeline View
```dart
class DashaTimelineView extends StatelessWidget {
  final Map dashaData;
  
  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        // Current mahadasha card
        _buildDashaCard(
          title: 'Mahadasha',
          lord: dashaData['currentMahadasha'],
          startDate: dashaData['mahadashaStartDate'],
          endDate: dashaData['mahadashaEndDate'],
          progress: dashaData['progressionPercentage'],
        ),
        
        // Antardasha card
        _buildDashaCard(
          title: 'Antardasha',
          lord: dashaData['currentAntardasha'],
        ),
        
        // Remedy advice
        _buildRemedy(dashaData['remedyAdvice']),
      ],
    );
  }
}
```

### 3. Compatibility Report View
```dart
class CompatibilityReportView extends StatelessWidget {
  final Map compatibility;
  
  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        // Gun Milan Score
        _buildScoreCard(
          totalPoints: compatibility['totalPoints'],
          outOf: compatibility['outOf'],
          percentage: compatibility['percentage'],
        ),
        
        // Category-wise breakdown
        _buildCategoryBreakdown(compatibility['categoryMatches']),
        
        // Doshas alert
        if (compatibility['nadiDosha'] || 
            compatibility['bhakootDosha'] || 
            compatibility['mangalDosha'])
          _buildDoshasAlert(compatibility),
        
        // Prediction
        _buildPredictionCard(compatibility['prediction']),
      ],
    );
  }
}
```

### 4. Muhurat Calendar View
```dart
class MuhuratCalendarView extends StatelessWidget {
  final String eventType;
  final List muhurats;
  
  @override
  Widget build(BuildContext context) {
    return Column(
      children: [
        // Top auspicious slots
        ListView.builder(
          itemCount: muhurats.take(5).length,
          itemBuilder: (context, index) {
            final muhurat = muhurats[index];
            return _buildMuhuratCard(
              date: muhurat['date'],
              time: muhurat['time'],
              nakshatra: muhurat['nakshatra'],
              tithi: muhurat['tithi'],
              score: muhurat['auspiciousityScore'],
            );
          },
        ),
      ],
    );
  }
}
```

### 5. Horoscope View
```dart
class HoroscopeView extends StatelessWidget {
  final Map horoscope;
  final String period; // "daily", "weekly", "monthly"
  
  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      child: Column(
        children: [
          // Overall prediction
          _buildCard('Overall', horoscope['overall']),
          
          // Love section
          _buildCard('❤️ Love', horoscope['love']),
          
          // Career section
          _buildCard('💼 Career', horoscope['career']),
          
          // Health section
          _buildCard('🏥 Health', horoscope['health']),
          
          // Finance section
          _buildCard('💰 Finance', horoscope['finance']),
          
          // Lucky details
          _buildLuckyDetailsCard(horoscope['lucky']),
          
          // Advice
          _buildCard('💡 Advice', horoscope['advice']),
        ],
      ),
    );
  }
}
```

---

## 📊 Data Models for Flutter

```dart
class BirthChartModel {
  final int id;
  final int userId;
  final DateTime dateOfBirth;
  final String timeOfBirth;
  final double latitude;
  final double longitude;
  final String lagna;
  final List<PlanetPosition> planets;
  final Map<int, String> houses;
  final String nakshatra;
  final Doshas doshas;
  
  // Constructor, fromJson, toJson methods...
}

class PlanetPosition {
  final String name;
  final double longitude;
  final String degree;
  final String rashi;
  final double speed;
  final bool retrograde;
  
  // Constructor, fromJson, toJson methods...
}

class Doshas {
  final bool mangal;
  final bool kaalSarp;
  final bool pitru;
  final bool grahan;
  
  // Constructor, fromJson, toJson methods...
}

class DashaModel {
  final String type;
  final String currentMahadasha;
  final DateTime mahadashaStart;
  final DateTime mahadashaEnd;
  final String currentAntardasha;
  final double progressionPercentage;
  
  // Constructor, fromJson, toJson methods...
}

class CompatibilityModel {
  final int totalPoints;
  final double percentage;
  final Map<String, int> categoryMatches;
  final bool nadiDosha;
  final bool bhakootDosha;
  final bool mangalDosha;
  final String prediction;
  
  // Constructor, fromJson, toJson methods...
}
```

---

## 🛠️ Riverpod State Management Example

```dart
// Providers
final kundliProvider = FutureProvider.autoDispose.family<Map, int>((ref, userId) async {
  final repo = ref.watch(astroRepositoryProvider);
  return repo.generateFullKundli(userId);
});

final dashaProvider = FutureProvider.autoDispose.family<Map, int>((ref, chartId) async {
  final repo = ref.watch(astroRepositoryProvider);
  return repo.getDashaCalculations(chartId);
});

final horoscopeProvider = FutureProvider.autoDispose.family<Map, String>((ref, sunSign) async {
  final repo = ref.watch(astroRepositoryProvider);
  return repo.getDailyHoroscope(sunSign);
});

// Repository
@riverpod
AstroRepository astroRepository(AstroRepositoryRef ref) {
  return AstroRepository(httpClient: ref.watch(httpClientProvider));
}
```

---

## 🎯 Feature Implementation Roadmap

### Phase 1: Basic Integration
- [x] API endpoints available
- [ ] Basic kundli display
- [ ] Daily horoscope view
- [ ] Health check

### Phase 2: Advanced Features
- [ ] Compatibility matching UI
- [ ] Dasha timeline visualization
- [ ] Muhurat calendar
- [ ] Remedy suggestions

### Phase 3: Enhanced UX
- [ ] Offline kundli caching
- [ ] Prediction history
- [ ] Personalized recommendations
- [ ] Push notifications for auspicious times

### Phase 4: Premium Features
- [ ] Advanced compatibility reports
- [ ] Detailed transit analysis
- [ ] Custom remedy plans
- [ ] Astrological consultations

---

## ⚠️ Error Handling

```dart
Future<void> safeFetchKundli(int userId) async {
  try {
    final kundli = await generateFullKundli(userId: userId);
    // Use kundli
  } on SocketException {
    showError('No internet connection');
  } on HttpException {
    showError('Server error. Please try again.');
  } on TimeoutException {
    showError('Request timed out');
  } catch (e) {
    showError('Unknown error: $e');
  }
}
```

---

## 🔐 Security Notes

1. Always include Authorization header with Bearer token
2. Validate all date inputs before sending
3. Handle sensitive birth chart data securely
4. Use HTTPS for all API calls
5. Store user charts locally with encryption if needed

---

## 📝 Testing Endpoints

### Using Postman

**1. Generate Kundli**
```
POST /api/v1/astro-services/kundli/calculate
?userId=1
&dateOfBirth=2000-01-15
&timeOfBirth=14:30
&latitude=28.7041
&longitude=77.1025
&timezone=IST

Headers: Authorization: Bearer <token>
```

**2. Get Dasha**
```
GET /api/v1/astro-services/dasha/1
```

**3. Check Compatibility**
```
POST /api/v1/astro-services/compatibility/match
?groomChartId=1
&brideChartId=2
```

**4. Get Horoscope**
```
GET /api/v1/astro-services/prediction/daily-horoscope
?sunSign=Aries
&date=2024-01-15
```

---

## 📞 Support & Debugging

### Common Issues

**Issue**: Empty planets array
**Solution**: Ensure latitude/longitude are provided or state/district are configured

**Issue**: Timeout on kundli calculation
**Solution**: Calculations are CPU-intensive; increase timeout to 30 seconds

**Issue**: Dosha not detected
**Solution**: Check if planetary positions meet dosha criteria (e.g., Mars in specific houses for Mangal Dosha)

---

## 🚀 Performance Optimization

1. **Caching**: Cache birth charts for 7 days
2. **Lazy Loading**: Load dasha details on demand
3. **Pagination**: Limit muhurat results to top 10
4. **Background Sync**: Sync predictions daily in background

---

Version: 1.0
Created: 2024
Last Updated: 2024
