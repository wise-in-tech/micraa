# Guide d'Intégration Flutter - Micraa

## Vue d'Ensemble

Ce guide explique comment intégrer l'application Flutter avec le backend Micraa et LiveKit pour les classes en direct.

## Architecture Client-Serveur

```
┌─────────────────────────────────────────────────────┐
│              Flutter Mobile App                     │
│                                                     │
│  ┌─────────────────┐        ┌──────────────────┐  │
│  │  HTTP Client    │        │  LiveKit Client  │  │
│  │  (REST API)     │        │  (WebRTC Audio)  │  │
│  └────────┬────────┘        └────────┬─────────┘  │
└───────────┼──────────────────────────┼─────────────┘
            │                          │
            │ HTTPS                    │ WebSocket/WebRTC
            │                          │
            v                          v
┌───────────────────────┐   ┌─────────────────────┐
│   Spring Boot API     │   │    LiveKit Server   │
│   localhost:8080      │   │    localhost:7880   │
└───────────────────────┘   └─────────────────────┘
```

## Dépendances Flutter Nécessaires

### pubspec.yaml

```yaml
dependencies:
  flutter:
    sdk: flutter
  
  # HTTP client pour l'API REST
  http: ^1.1.0
  
  # LiveKit client SDK
  livekit_client: ^2.0.0
  
  # Permissions (microphone)
  permission_handler: ^11.0.1
  
  # State management (optionnel - Provider recommandé)
  provider: ^6.1.1
  
  # JSON serialization
  json_annotation: ^4.8.1

dev_dependencies:
  json_serializable: ^6.7.1
  build_runner: ^2.4.7
```

## Configuration iOS

### Info.plist

Ajouter les permissions microphone :

```xml
<key>NSMicrophoneUsageDescription</key>
<string>L'application a besoin d'accéder au microphone pour les classes en direct</string>
```

## Configuration Android

### AndroidManifest.xml

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
```

## Modèles de Données Flutter

### User Model

```dart
class User {
  final int id;
  final String name;
  final String email;
  final UserRole role;

  User({
    required this.id,
    required this.name,
    required this.email,
    required this.role,
  });

  factory User.fromJson(Map<String, dynamic> json) {
    return User(
      id: json['id'],
      name: json['name'],
      email: json['email'],
      role: UserRole.values.firstWhere(
        (e) => e.toString() == 'UserRole.${json['role']}'
      ),
    );
  }
}

enum UserRole { TEACHER, STUDENT }
```

### LiveClass Model

```dart
class LiveClass {
  final int id;
  final String title;
  final int teacherId;
  final String? teacherName;
  final LiveClassStatus status;
  final DateTime? scheduledAt;
  final DateTime? startedAt;
  final DateTime? endedAt;

  LiveClass({
    required this.id,
    required this.title,
    required this.teacherId,
    this.teacherName,
    required this.status,
    this.scheduledAt,
    this.startedAt,
    this.endedAt,
  });

  factory LiveClass.fromJson(Map<String, dynamic> json) {
    return LiveClass(
      id: json['id'],
      title: json['title'],
      teacherId: json['teacherId'],
      teacherName: json['teacherName'],
      status: LiveClassStatus.values.firstWhere(
        (e) => e.toString() == 'LiveClassStatus.${json['status']}'
      ),
      scheduledAt: json['scheduledAt'] != null
          ? DateTime.parse(json['scheduledAt'])
          : null,
      startedAt: json['startedAt'] != null
          ? DateTime.parse(json['startedAt'])
          : null,
      endedAt: json['endedAt'] != null
          ? DateTime.parse(json['endedAt'])
          : null,
    );
  }
}

enum LiveClassStatus { SCHEDULED, LIVE, ENDED }
```

## Service API Flutter

### api_service.dart

```dart
import 'dart:convert';
import 'package:http/http.dart' as http;

class ApiService {
  // Production: API publique accessible en HTTPS
  static const String baseUrl = 'https://micraa.be/api';
  
  // Get all users
  Future<List<User>> getUsers() async {
    final response = await http.get(Uri.parse('$baseUrl/users'));
    
    if (response.statusCode == 200) {
      List<dynamic> body = jsonDecode(response.body);
      return body.map((json) => User.fromJson(json)).toList();
    } else {
      throw Exception('Failed to load users');
    }
  }
  
  // Get classes for a user
  Future<List<LiveClass>> getUserClasses(int userId, UserRole role) async {
    final response = await http.get(
      Uri.parse('$baseUrl/live-classes?userId=$userId&role=${role.name}')
    );
    
    if (response.statusCode == 200) {
      List<dynamic> body = jsonDecode(response.body);
      return body.map((json) => LiveClass.fromJson(json)).toList();
    } else {
      throw Exception('Failed to load classes');
    }
  }
  
  // Start a class (teacher only)
  Future<LiveClass> startClass(int classId, int teacherId) async {
    final response = await http.post(
      Uri.parse('$baseUrl/live-classes/$classId/start?teacherId=$teacherId')
    );
    
    if (response.statusCode == 200) {
      return LiveClass.fromJson(jsonDecode(response.body));
    } else {
      throw Exception('Failed to start class');
    }
  }
  
  // Join a class (get LiveKit token)
  Future<JoinClassResponse> joinClass(int classId, int userId) async {
    final response = await http.post(
      Uri.parse('$baseUrl/live-classes/$classId/join?userId=$userId')
    );
    
    if (response.statusCode == 200) {
      return JoinClassResponse.fromJson(jsonDecode(response.body));
    } else {
      final error = jsonDecode(response.body);
      throw Exception(error['error'] ?? 'Failed to join class');
    }
  }
  
  // End a class (teacher only)
  Future<LiveClass> endClass(int classId, int teacherId) async {
    final response = await http.post(
      Uri.parse('$baseUrl/live-classes/$classId/end?teacherId=$teacherId')
    );
    
    if (response.statusCode == 200) {
      return LiveClass.fromJson(jsonDecode(response.body));
    } else {
      throw Exception('Failed to end class');
    }
  }
}

class JoinClassResponse {
  final String livekitUrl;
  final String accessToken;
  final String roomName;

  JoinClassResponse({
    required this.livekitUrl,
    required this.accessToken,
    required this.roomName,
  });

  factory JoinClassResponse.fromJson(Map<String, dynamic> json) {
    return JoinClassResponse(
      livekitUrl: json['livekitUrl'],
      accessToken: json['accessToken'],
      roomName: json['roomName'],
    );
  }
}
```

## Intégration LiveKit

### livekit_service.dart

```dart
import 'package:livekit_client/livekit_client.dart';
import 'package:permission_handler/permission_handler.dart';

class LiveKitService {
  Room? _room;
  EventsListener<RoomEvent>? _listener;

  // Connect to LiveKit room
  Future<Room> connectToRoom({
    required String url,
    required String token,
    Function(Participant)? onParticipantConnected,
    Function(Participant)? onParticipantDisconnected,
    Function(String, RemoteTrackPublication, Participant)? onTrackSubscribed,
  }) async {
    // Request microphone permission
    final micPermission = await Permission.microphone.request();
    if (!micPermission.isGranted) {
      throw Exception('Microphone permission not granted');
    }

    // Create room
    _room = Room();
    
    // Set up event listeners
    _listener = _room!.createListener();
    
    _listener!.on<ParticipantConnectedEvent>((event) {
      print('Participant connected: ${event.participant.identity}');
      onParticipantConnected?.call(event.participant);
    });
    
    _listener!.on<ParticipantDisconnectedEvent>((event) {
      print('Participant disconnected: ${event.participant.identity}');
      onParticipantDisconnected?.call(event.participant);
    });
    
    _listener!.on<TrackSubscribedEvent>((event) {
      print('Track subscribed: ${event.track.sid}');
      onTrackSubscribed?.call(
        event.track.sid!,
        event.publication,
        event.participant,
      );
    });

    // Connect to room
    try {
      await _room!.connect(url, token);
      print('Connected to room: ${_room!.name}');
      
      // Enable microphone
      await _room!.localParticipant?.setMicrophoneEnabled(true);
      
      return _room!;
    } catch (e) {
      print('Failed to connect: $e');
      rethrow;
    }
  }

  // Toggle microphone
  Future<void> toggleMicrophone() async {
    if (_room?.localParticipant != null) {
      final enabled = !_room!.localParticipant!.isMicrophoneEnabled();
      await _room!.localParticipant!.setMicrophoneEnabled(enabled);
    }
  }

  // Check if microphone is enabled
  bool get isMicrophoneEnabled {
    return _room?.localParticipant?.isMicrophoneEnabled() ?? false;
  }

  // Get list of participants
  List<Participant> get participants {
    if (_room == null) return [];
    return [
      _room!.localParticipant!,
      ..._room!.remoteParticipants.values,
    ];
  }

  // Disconnect from room
  Future<void> disconnect() async {
    await _room?.disconnect();
    await _listener?.dispose();
    _room = null;
    _listener = null;
  }

  // Get current room
  Room? get room => _room;
}
```

## Exemple d'écran de Classe en Direct

### live_class_screen.dart

```dart
import 'package:flutter/material.dart';
import 'package:livekit_client/livekit_client.dart';

class LiveClassScreen extends StatefulWidget {
  final LiveClass liveClass;
  final User currentUser;

  const LiveClassScreen({
    Key? key,
    required this.liveClass,
    required this.currentUser,
  }) : super(key: key);

  @override
  State<LiveClassScreen> createState() => _LiveClassScreenState();
}

class _LiveClassScreenState extends State<LiveClassScreen> {
  final ApiService _apiService = ApiService();
  final LiveKitService _liveKitService = LiveKitService();
  
  bool _isConnecting = false;
  bool _isConnected = false;
  String? _error;
  List<Participant> _participants = [];

  @override
  void initState() {
    super.initState();
    _joinClass();
  }

  Future<void> _joinClass() async {
    setState(() {
      _isConnecting = true;
      _error = null;
    });

    try {
      // Get LiveKit token from backend
      final joinResponse = await _apiService.joinClass(
        widget.liveClass.id,
        widget.currentUser.id,
      );

      // Connect to LiveKit room
      await _liveKitService.connectToRoom(
        url: joinResponse.livekitUrl,
        token: joinResponse.accessToken,
        onParticipantConnected: (participant) {
          setState(() {
            _participants = _liveKitService.participants;
          });
        },
        onParticipantDisconnected: (participant) {
          setState(() {
            _participants = _liveKitService.participants;
          });
        },
      );

      setState(() {
        _isConnected = true;
        _isConnecting = false;
        _participants = _liveKitService.participants;
      });
    } catch (e) {
      setState(() {
        _error = e.toString();
        _isConnecting = false;
      });
    }
  }

  Future<void> _toggleMicrophone() async {
    await _liveKitService.toggleMicrophone();
    setState(() {});
  }

  Future<void> _leaveClass() async {
    await _liveKitService.disconnect();
    if (mounted) {
      Navigator.pop(context);
    }
  }

  @override
  void dispose() {
    _liveKitService.disconnect();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: Text(widget.liveClass.title),
        actions: [
          IconButton(
            icon: const Icon(Icons.exit_to_app),
            onPressed: _leaveClass,
          ),
        ],
      ),
      body: _buildBody(),
      bottomNavigationBar: _buildControls(),
    );
  }

  Widget _buildBody() {
    if (_isConnecting) {
      return const Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            CircularProgressIndicator(),
            SizedBox(height: 16),
            Text('Connexion à la classe...'),
          ],
        ),
      );
    }

    if (_error != null) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.error_outline, size: 64, color: Colors.red),
            const SizedBox(height: 16),
            Text('Erreur: $_error'),
            const SizedBox(height: 16),
            ElevatedButton(
              onPressed: _joinClass,
              child: const Text('Réessayer'),
            ),
          ],
        ),
      );
    }

    return Column(
      children: [
        // Participants list
        Expanded(
          child: _participants.isEmpty
              ? const Center(child: Text('Aucun participant'))
              : ListView.builder(
                  itemCount: _participants.length,
                  itemBuilder: (context, index) {
                    final participant = _participants[index];
                    final isLocal = participant is LocalParticipant;
                    return ListTile(
                      leading: Icon(
                        participant.isMicrophoneEnabled()
                            ? Icons.mic
                            : Icons.mic_off,
                        color: participant.isMicrophoneEnabled()
                            ? Colors.green
                            : Colors.red,
                      ),
                      title: Text(
                        participant.name ?? participant.identity,
                      ),
                      trailing: isLocal
                          ? const Chip(
                              label: Text('Vous'),
                              backgroundColor: Colors.blue,
                            )
                          : null,
                    );
                  },
                ),
        ),
      ],
    );
  }

  Widget _buildControls() {
    if (!_isConnected) return const SizedBox.shrink();

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: Colors.white,
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.1),
            blurRadius: 4,
            offset: const Offset(0, -2),
          ),
        ],
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
        children: [
          // Microphone toggle
          FloatingActionButton(
            onPressed: _toggleMicrophone,
            backgroundColor: _liveKitService.isMicrophoneEnabled
                ? Colors.blue
                : Colors.red,
            child: Icon(
              _liveKitService.isMicrophoneEnabled
                  ? Icons.mic
                  : Icons.mic_off,
            ),
          ),
          
          // Leave button
          FloatingActionButton(
            onPressed: _leaveClass,
            backgroundColor: Colors.red,
            child: const Icon(Icons.call_end),
          ),
        ],
      ),
    );
  }
}
```

## Configuration des URLs pour Tests

### Android Emulator
```dart
static const String baseUrl = 'http://10.0.2.2:8080/api';
static const String livekitUrl = 'ws://10.0.2.2:7880';
```

### iOS Simulator / Device Réel
```dart
// Remplacer par l'IP de votre machine
static const String baseUrl = 'http://192.168.1.XXX:8080/api';
static const String livekitUrl = 'ws://192.168.1.XXX:7880';
```

## Trouver votre IP locale

### macOS
```bash
ifconfig | grep "inet " | grep -v 127.0.0.1
```

### Windows
```bash
ipconfig
```

## Workflow de Test Complet

### 1. Démarrer le Backend
```bash
cd /Users/mohy/sofiane-devenv/micraa
docker-compose up
```

### 2. Créer des Données de Test
```bash
./test-api.sh
```

### 3. Configurer Flutter
- Mettre à jour `baseUrl` avec votre IP
- Installer les dépendances : `flutter pub get`

### 4. Lancer l'Application
```bash
flutter run
```

### 5. Tester le Scénario
1. Connexion avec un enseignant
2. Démarrer une classe
3. Connexion avec un étudiant sur un autre device/emulator
4. Rejoindre la classe
5. Tester l'audio bidirectionnel
6. Tester mute/unmute
7. Vérifier la liste des participants

## Debugging

### Logs Backend
```bash
docker-compose logs -f app
```

### Logs LiveKit
```bash
docker-compose logs -f livekit
```

### Logs Flutter
```dart
// Ajouter dans le code
print('Connecting to: $url');
print('Token: ${token.substring(0, 20)}...');
```

## Problèmes Courants

### 1. "Failed to connect to LiveKit"
- Vérifier que LiveKit est démarré : `docker ps`
- Vérifier l'URL (ws:// pas wss:// en dev)
- Vérifier le firewall

### 2. "Microphone permission denied"
- Vérifier Info.plist (iOS) / AndroidManifest.xml
- Réinstaller l'app

### 3. "Cannot connect to backend"
- Vérifier l'IP (pas localhost pour device réel)
- Vérifier que le backend est accessible : `curl http://IP:8080/api/health`

### 4. "No audio"
- Vérifier que le micro est activé
- Vérifier les permissions
- Vérifier les logs LiveKit

## Ressources

- LiveKit Flutter SDK: https://docs.livekit.io/client-sdk-flutter/
- LiveKit Examples: https://github.com/livekit/client-sdk-flutter/tree/main/example
- Flutter Permissions: https://pub.dev/packages/permission_handler

## Prochaines Étapes

1. Implémenter l'interface utilisateur complète
2. Ajouter la gestion du chat textuel
3. Améliorer l'UI des participants
4. Ajouter des indicateurs de qualité réseau
5. Implémenter la gestion d'erreurs robuste
6. Ajouter des tests automatisés
