package com.wiseintech.micraa.service;

import com.wiseintech.micraa.dto.*;
import com.wiseintech.micraa.model.*;
import com.wiseintech.micraa.repository.*;
import livekit.LivekitModels;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LiveClassService {
    
    private final LiveClassRepository liveClassRepository;
    private final LiveClassStudentRepository liveClassStudentRepository;
    private final UserRepository userRepository;
    private final AttendanceRepository attendanceRepository;
    private final LiveKitService liveKitService;
    
    /**
     * Create a new live class
     */
    @Transactional
    public LiveClassResponse createLiveClass(CreateLiveClassRequest request) {
        log.info("Creating live class: {}", request.getTitle());
        
        User teacher = userRepository.findById(request.getTeacherId())
            .orElseThrow(() -> new RuntimeException("Teacher not found"));
        
        if (teacher.getRole() != UserRole.TEACHER) {
            throw new RuntimeException("User is not a teacher");
        }
        
        LocalDateTime scheduledAt = null;
        if (request.getScheduledAt() != null && !request.getScheduledAt().isEmpty()) {
            scheduledAt = LocalDateTime.parse(request.getScheduledAt(), DateTimeFormatter.ISO_DATE_TIME);
        }
        
        LiveClass liveClass = LiveClass.builder()
            .title(request.getTitle())
            .teacherId(request.getTeacherId())
            .status(LiveClassStatus.SCHEDULED)
            .scheduledAt(scheduledAt)
            .build();
        
        liveClass = liveClassRepository.save(liveClass);
        log.info("Created live class with ID: {}", liveClass.getId());

        // Auto-enroll all current students so the class is immediately
        // visible to them (MVP: no class roster/selection UI yet).
        List<User> allStudents = userRepository.findByRole(UserRole.STUDENT);
        for (User student : allStudents) {
            LiveClassStudent enrollment = LiveClassStudent.builder()
                .liveClassId(liveClass.getId())
                .studentId(student.getId())
                .build();
            liveClassStudentRepository.save(enrollment);
        }
        log.info("Auto-enrolled {} students in live class {}", allStudents.size(), liveClass.getId());

        return toLiveClassResponse(liveClass, teacher);
    }
    
    /**
     * Get all live classes for a teacher
     */
    public List<LiveClassResponse> getTeacherLiveClasses(Long teacherId) {
        List<LiveClass> classes = liveClassRepository.findByTeacherId(teacherId);
        User teacher = userRepository.findById(teacherId).orElse(null);
        
        return classes.stream()
            .map(lc -> toLiveClassResponse(lc, teacher))
            .collect(Collectors.toList());
    }
    
    /**
     * Get all live classes that a student can attend
     */
    public List<LiveClassResponse> getStudentLiveClasses(Long studentId) {
        List<LiveClassStudent> enrollments = liveClassStudentRepository.findByStudentId(studentId);
        
        return enrollments.stream()
            .map(e -> liveClassRepository.findById(e.getLiveClassId()).orElse(null))
            .filter(lc -> lc != null)
            .map(lc -> {
                User teacher = userRepository.findById(lc.getTeacherId()).orElse(null);
                return toLiveClassResponse(lc, teacher);
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Get a live class by ID
     */
    public LiveClassResponse getLiveClass(Long id) {
        LiveClass liveClass = liveClassRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Live class not found"));
        
        User teacher = userRepository.findById(liveClass.getTeacherId()).orElse(null);
        return toLiveClassResponse(liveClass, teacher);
    }
    
    /**
     * Add students to a live class
     */
    @Transactional
    public void addStudentsToLiveClass(Long liveClassId, AddStudentsRequest request) {
        log.info("Adding {} students to live class {}", request.getStudentIds().size(), liveClassId);
        
        LiveClass liveClass = liveClassRepository.findById(liveClassId)
            .orElseThrow(() -> new RuntimeException("Live class not found"));
        
        for (Long studentId : request.getStudentIds()) {
            User student = userRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Student not found: " + studentId));
            
            if (student.getRole() != UserRole.STUDENT) {
                throw new RuntimeException("User is not a student: " + studentId);
            }
            
            if (!liveClassStudentRepository.existsByLiveClassIdAndStudentId(liveClassId, studentId)) {
                LiveClassStudent enrollment = LiveClassStudent.builder()
                    .liveClassId(liveClassId)
                    .studentId(studentId)
                    .build();
                liveClassStudentRepository.save(enrollment);
                log.info("Added student {} to live class {}", studentId, liveClassId);
            }
        }
    }
    
    /**
     * Start a live class
     */
    @Transactional
    public LiveClassResponse startLiveClass(Long id, Long teacherId) {
        log.info("Starting live class {} by teacher {}", id, teacherId);
        
        LiveClass liveClass = liveClassRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Live class not found"));
        
        if (!liveClass.getTeacherId().equals(teacherId)) {
            throw new RuntimeException("Only the assigned teacher can start this class");
        }
        
        if (liveClass.getStatus() == LiveClassStatus.LIVE) {
            throw new RuntimeException("Class is already live");
        }
        
        if (liveClass.getStatus() == LiveClassStatus.ENDED) {
            throw new RuntimeException("Class has already ended");
        }
        
        liveClass.setStatus(LiveClassStatus.LIVE);
        liveClass.setStartedAt(LocalDateTime.now());
        liveClass = liveClassRepository.save(liveClass);
        
        log.info("Live class {} is now LIVE", id);
        
        User teacher = userRepository.findById(teacherId).orElse(null);
        return toLiveClassResponse(liveClass, teacher);
    }
    
    /**
     * End a live class
     */
    @Transactional
    public LiveClassResponse endLiveClass(Long id, Long teacherId) {
        log.info("Ending live class {} by teacher {}", id, teacherId);
        
        LiveClass liveClass = liveClassRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Live class not found"));
        
        if (!liveClass.getTeacherId().equals(teacherId)) {
            throw new RuntimeException("Only the assigned teacher can end this class");
        }
        
        if (liveClass.getStatus() == LiveClassStatus.ENDED) {
            throw new RuntimeException("Class has already ended");
        }
        
        liveClass.setStatus(LiveClassStatus.ENDED);
        liveClass.setEndedAt(LocalDateTime.now());
        liveClass = liveClassRepository.save(liveClass);
        
        // Mark all active attendance records as left
        List<Attendance> activeAttendances = attendanceRepository.findByLiveClassId(id).stream()
            .filter(a -> a.getLeftAt() == null)
            .collect(Collectors.toList());
        
        for (Attendance attendance : activeAttendances) {
            attendance.setLeftAt(LocalDateTime.now());
            attendanceRepository.save(attendance);
        }
        
        log.info("Live class {} has ENDED", id);
        
        User teacher = userRepository.findById(teacherId).orElse(null);
        return toLiveClassResponse(liveClass, teacher);
    }
    
    /**
     * Join a live class (generate LiveKit token)
     */
    @Transactional
    public JoinLiveClassResponse joinLiveClass(Long id, Long userId) {
        log.info("User {} joining live class {}", userId, id);
        
        LiveClass liveClass = liveClassRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Live class not found"));
        
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Check if class is live
        if (liveClass.getStatus() != LiveClassStatus.LIVE) {
            throw new RuntimeException("Class is not currently live");
        }
        
        // Check authorization
        boolean authorized = false;
        if (user.getRole() == UserRole.TEACHER && liveClass.getTeacherId().equals(userId)) {
            authorized = true;
        } else if (user.getRole() == UserRole.STUDENT) {
            authorized = liveClassStudentRepository.existsByLiveClassIdAndStudentId(id, userId);
        }
        
        if (!authorized) {
            throw new RuntimeException("User is not authorized to join this class");
        }
        
        // Record attendance for students
        if (user.getRole() == UserRole.STUDENT) {
            // Check if there's already an active attendance record
            var existingAttendance = attendanceRepository
                .findByLiveClassIdAndStudentIdAndLeftAtIsNull(id, userId);
            
            if (existingAttendance.isEmpty()) {
                Attendance attendance = Attendance.builder()
                    .liveClassId(id)
                    .studentId(userId)
                    .joinedAt(LocalDateTime.now())
                    .build();
                attendanceRepository.save(attendance);
                log.info("Recorded attendance for student {} in class {}", userId, id);
            }
        }
        
        // Generate LiveKit token
        String roomName = roomNameFor(id);
        String participantIdentity = participantIdentity(user.getRole(), userId);
        String participantName = user.getName();
        
        String token = liveKitService.generateToken(roomName, participantIdentity, participantName);
        
        return JoinLiveClassResponse.builder()
            .livekitUrl(liveKitService.getLiveKitUrl())
            .accessToken(token)
            .roomName(roomName)
            .build();
    }
    
    /**
     * List the participants currently connected to a live class, ordered by connection
     * time (earliest first) - this is the order in which participants joined the audio
     * room, which is the most natural reading for a teacher checking who has been
     * present the longest. Only the class's assigned teacher may call this.
     *
     * LiveKit is the source of truth for who is connected and when they connected
     * (ParticipantInfo.joinedAt); Spring Boot only enforces who is allowed to see that
     * information. No audio is routed through Spring Boot.
     */
    @Transactional(readOnly = true)
    public List<ParticipantResponse> getConnectedParticipants(Long liveClassId, Long requesterId) {
        LiveClass liveClass = liveClassRepository.findById(liveClassId)
            .orElseThrow(() -> new RuntimeException("Live class not found"));

        if (!liveClass.getTeacherId().equals(requesterId)) {
            throw new AccessDeniedException("Only the assigned teacher can view connected participants");
        }

        if (liveClass.getStatus() != LiveClassStatus.LIVE) {
            // Nothing is connected to a room that isn't live yet / has already ended.
            return List.of();
        }

        List<LivekitModels.ParticipantInfo> participants =
            liveKitService.listParticipants(roomNameFor(liveClassId));

        return participants.stream()
            .map(this::toParticipantResponse)
            // Earliest connection first; joinedAt is LiveKit's own monotonic timestamp
            // (epoch seconds), not something derived from unordered map iteration.
            .sorted(Comparator.comparingLong(ParticipantResponse::getConnectedAt))
            .collect(Collectors.toList());
    }

    /**
     * Mute or unmute a student's microphone in a live class. Only the class's assigned
     * teacher may perform this action, only on an enrolled student, and only while the
     * class is live. The command is enforced by LiveKit's server-side track control -
     * not merely hidden behind a Flutter button.
     */
    @Transactional(readOnly = true)
    public MicrophoneActionResponse setParticipantMicrophone(
            Long liveClassId, Long participantUserId, Long requesterId, boolean muted) {

        LiveClass liveClass = liveClassRepository.findById(liveClassId)
            .orElseThrow(() -> new RuntimeException("Live class not found"));

        if (!liveClass.getTeacherId().equals(requesterId)) {
            throw new AccessDeniedException("Only the assigned teacher can control a participant's microphone");
        }

        if (liveClass.getStatus() != LiveClassStatus.LIVE) {
            throw new RuntimeException("Class is not currently live");
        }

        User target = userRepository.findById(participantUserId)
            .orElseThrow(() -> new RuntimeException("Participant not found"));

        if (target.getRole() != UserRole.STUDENT
                || !liveClassStudentRepository.existsByLiveClassIdAndStudentId(liveClassId, participantUserId)) {
            throw new RuntimeException("User is not an enrolled student in this class");
        }

        String roomName = roomNameFor(liveClassId);
        String identity = participantIdentity(UserRole.STUDENT, participantUserId);

        LivekitModels.ParticipantInfo participant = liveKitService.getParticipant(roomName, identity)
            .orElseThrow(() -> new RuntimeException("Student is not currently connected to the live class"));

        LivekitModels.TrackInfo microphoneTrack = participant.getTracksList().stream()
            .filter(track -> track.getType() == LivekitModels.TrackType.AUDIO)
            .findFirst()
            .orElseThrow(() -> new RuntimeException("Student has no active microphone track"));

        liveKitService.setTrackMuted(roomName, identity, microphoneTrack.getSid(), muted);
        log.info("Teacher {} {} the microphone of student {} in live class {}",
            requesterId, muted ? "muted" : "unmuted", participantUserId, liveClassId);

        return MicrophoneActionResponse.builder()
            .participantId(participantUserId)
            .muted(muted)
            .build();
    }

    private ParticipantResponse toParticipantResponse(LivekitModels.ParticipantInfo info) {
        String identity = info.getIdentity();
        UserRole role = roleFromIdentity(identity);
        Long userId = userIdFromIdentity(identity);

        boolean microphoneMuted = info.getTracksList().stream()
            .filter(track -> track.getType() == LivekitModels.TrackType.AUDIO)
            .findFirst()
            .map(LivekitModels.TrackInfo::getMuted)
            // No published audio track at all reads as "no live microphone", shown as muted.
            .orElse(true);

        return ParticipantResponse.builder()
            .identity(identity)
            .userId(userId)
            .name(info.getName())
            .role(role != null ? role.name() : "UNKNOWN")
            .connectedAt(info.getJoinedAt() * 1000L)
            .state(info.getState().name())
            .microphoneMuted(microphoneMuted)
            .build();
    }

    private String roomNameFor(Long liveClassId) {
        return "class-" + liveClassId;
    }

    private String participantIdentity(UserRole role, Long userId) {
        return role.name().toLowerCase() + "-" + userId;
    }

    private UserRole roleFromIdentity(String identity) {
        if (identity.startsWith("teacher-")) {
            return UserRole.TEACHER;
        }
        if (identity.startsWith("student-")) {
            return UserRole.STUDENT;
        }
        return null;
    }

    private Long userIdFromIdentity(String identity) {
        int dash = identity.lastIndexOf('-');
        if (dash < 0 || dash == identity.length() - 1) {
            return null;
        }
        try {
            return Long.parseLong(identity.substring(dash + 1));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private LiveClassResponse toLiveClassResponse(LiveClass liveClass, User teacher) {
        return LiveClassResponse.builder()
            .id(liveClass.getId())
            .title(liveClass.getTitle())
            .teacherId(liveClass.getTeacherId())
            .teacherName(teacher != null ? teacher.getName() : null)
            .status(liveClass.getStatus())
            .scheduledAt(liveClass.getScheduledAt())
            .startedAt(liveClass.getStartedAt())
            .endedAt(liveClass.getEndedAt())
            .createdAt(liveClass.getCreatedAt())
            .build();
    }
}
