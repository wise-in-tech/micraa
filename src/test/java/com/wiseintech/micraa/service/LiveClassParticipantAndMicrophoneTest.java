package com.wiseintech.micraa.service;

import com.wiseintech.micraa.dto.MicrophoneActionResponse;
import com.wiseintech.micraa.dto.ParticipantResponse;
import com.wiseintech.micraa.model.LiveClass;
import com.wiseintech.micraa.model.LiveClassStatus;
import com.wiseintech.micraa.model.User;
import com.wiseintech.micraa.model.UserRole;
import com.wiseintech.micraa.repository.AttendanceRepository;
import com.wiseintech.micraa.repository.LiveClassRepository;
import com.wiseintech.micraa.repository.LiveClassStudentRepository;
import com.wiseintech.micraa.repository.UserRepository;
import livekit.LivekitModels;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers ticket-001's "connected participants for teachers" and "teacher microphone
 * moderation" requirements at the service layer, with LiveKit itself mocked out (the
 * authorization/ordering logic is what belongs to Spring Boot; LiveKit's own behavior is
 * out of scope for a unit test).
 */
@ExtendWith(MockitoExtension.class)
class LiveClassParticipantAndMicrophoneTest {

    private static final Long TEACHER_ID = 1L;
    private static final Long OTHER_TEACHER_ID = 2L;
    private static final Long STUDENT_ID = 10L;
    private static final Long LIVE_CLASS_ID = 100L;

    @Mock
    private LiveClassRepository liveClassRepository;
    @Mock
    private LiveClassStudentRepository liveClassStudentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private AttendanceRepository attendanceRepository;
    @Mock
    private LiveKitService liveKitService;

    private LiveClassService liveClassService;

    @BeforeEach
    void setUp() {
        liveClassService = new LiveClassService(
                liveClassRepository, liveClassStudentRepository, userRepository,
                attendanceRepository, liveKitService);
    }

    private LiveClass liveLiveClass() {
        return LiveClass.builder()
                .id(LIVE_CLASS_ID)
                .title("Algebra")
                .teacherId(TEACHER_ID)
                .status(LiveClassStatus.LIVE)
                .build();
    }

    private LivekitModels.ParticipantInfo participant(String identity, String name, long joinedAtEpochSeconds,
                                                        boolean audioMuted) {
        return LivekitModels.ParticipantInfo.newBuilder()
                .setIdentity(identity)
                .setName(name)
                .setJoinedAt(joinedAtEpochSeconds)
                .setState(LivekitModels.ParticipantInfo.State.ACTIVE)
                .addTracks(LivekitModels.TrackInfo.newBuilder()
                        .setSid("TR_" + identity)
                        .setType(LivekitModels.TrackType.AUDIO)
                        .setMuted(audioMuted)
                        .build())
                .build();
    }

    // ─── Connected participants ────────────────────────────────────────────

    @Test
    void nonAssignedTeacherCannotViewParticipants() {
        when(liveClassRepository.findById(LIVE_CLASS_ID)).thenReturn(Optional.of(liveLiveClass()));

        assertThatThrownBy(() -> liveClassService.getConnectedParticipants(LIVE_CLASS_ID, OTHER_TEACHER_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void studentCannotViewParticipants() {
        when(liveClassRepository.findById(LIVE_CLASS_ID)).thenReturn(Optional.of(liveLiveClass()));

        assertThatThrownBy(() -> liveClassService.getConnectedParticipants(LIVE_CLASS_ID, STUDENT_ID))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void assignedTeacherSeesParticipantsOrderedByConnectionTimeEarliestFirst() {
        when(liveClassRepository.findById(LIVE_CLASS_ID)).thenReturn(Optional.of(liveLiveClass()));

        // Deliberately returned out of order by LiveKit to prove we sort, not just relay.
        when(liveKitService.listParticipants("class-" + LIVE_CLASS_ID)).thenReturn(List.of(
                participant("student-11", "Bob", 3000L, false),
                participant("teacher-1", "Ms. Martin", 1000L, false),
                participant("student-10", "Alice", 2000L, true)
        ));

        List<ParticipantResponse> result = liveClassService.getConnectedParticipants(LIVE_CLASS_ID, TEACHER_ID);

        assertThat(result).extracting(ParticipantResponse::getIdentity)
                .containsExactly("teacher-1", "student-10", "student-11");
        assertThat(result).extracting(ParticipantResponse::getConnectedAt)
                .containsExactly(1000L * 1000L, 2000L * 1000L, 3000L * 1000L);
        assertThat(result.get(0).getRole()).isEqualTo("TEACHER");
        assertThat(result.get(1).getRole()).isEqualTo("STUDENT");
        assertThat(result.get(1).getUserId()).isEqualTo(10L);
        assertThat(result.get(1).isMicrophoneMuted()).isTrue();
        assertThat(result.get(2).isMicrophoneMuted()).isFalse();
    }

    @Test
    void classNotLiveYieldsEmptyParticipantList() {
        LiveClass scheduled = liveLiveClass();
        scheduled.setStatus(LiveClassStatus.SCHEDULED);
        when(liveClassRepository.findById(LIVE_CLASS_ID)).thenReturn(Optional.of(scheduled));

        assertThat(liveClassService.getConnectedParticipants(LIVE_CLASS_ID, TEACHER_ID)).isEmpty();
    }

    // ─── Microphone moderation ─────────────────────────────────────────────

    private User student() {
        return User.builder().id(STUDENT_ID).name("Alice").email("alice@school.fr")
                .role(UserRole.STUDENT).build();
    }

    @Test
    void onlyAssignedTeacherCanMuteAStudent() {
        when(liveClassRepository.findById(LIVE_CLASS_ID)).thenReturn(Optional.of(liveLiveClass()));

        assertThatThrownBy(() ->
                liveClassService.setParticipantMicrophone(LIVE_CLASS_ID, STUDENT_ID, OTHER_TEACHER_ID, true))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void studentCannotMuteAnotherParticipant() {
        when(liveClassRepository.findById(LIVE_CLASS_ID)).thenReturn(Optional.of(liveLiveClass()));

        // A student calling the endpoint is just another non-assigned-teacher caller from
        // the service's point of view: their id never equals liveClass.teacherId.
        assertThatThrownBy(() ->
                liveClassService.setParticipantMicrophone(LIVE_CLASS_ID, 11L, STUDENT_ID, true))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void teacherMuteUpdatesMicrophoneStateThroughLiveKit() {
        when(liveClassRepository.findById(LIVE_CLASS_ID)).thenReturn(Optional.of(liveLiveClass()));
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student()));
        when(liveClassStudentRepository.existsByLiveClassIdAndStudentId(LIVE_CLASS_ID, STUDENT_ID)).thenReturn(true);
        when(liveKitService.getParticipant("class-" + LIVE_CLASS_ID, "student-" + STUDENT_ID))
                .thenReturn(Optional.of(participant("student-" + STUDENT_ID, "Alice", 1000L, false)));

        MicrophoneActionResponse response =
                liveClassService.setParticipantMicrophone(LIVE_CLASS_ID, STUDENT_ID, TEACHER_ID, true);

        assertThat(response.getParticipantId()).isEqualTo(STUDENT_ID);
        assertThat(response.isMuted()).isTrue();
        verify(liveKitService).setTrackMuted(
                eq("class-" + LIVE_CLASS_ID), eq("student-" + STUDENT_ID), anyString(), eq(true));
    }

    @Test
    void teacherUnmuteUpdatesMicrophoneStateThroughLiveKit() {
        when(liveClassRepository.findById(LIVE_CLASS_ID)).thenReturn(Optional.of(liveLiveClass()));
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student()));
        when(liveClassStudentRepository.existsByLiveClassIdAndStudentId(LIVE_CLASS_ID, STUDENT_ID)).thenReturn(true);
        when(liveKitService.getParticipant("class-" + LIVE_CLASS_ID, "student-" + STUDENT_ID))
                .thenReturn(Optional.of(participant("student-" + STUDENT_ID, "Alice", 1000L, true)));

        MicrophoneActionResponse response =
                liveClassService.setParticipantMicrophone(LIVE_CLASS_ID, STUDENT_ID, TEACHER_ID, false);

        assertThat(response.isMuted()).isFalse();
        verify(liveKitService).setTrackMuted(
                eq("class-" + LIVE_CLASS_ID), eq("student-" + STUDENT_ID), anyString(), eq(false));
    }

    @Test
    void cannotMuteAParticipantWhoIsNotEnrolledInTheClass() {
        when(liveClassRepository.findById(LIVE_CLASS_ID)).thenReturn(Optional.of(liveLiveClass()));
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student()));
        when(liveClassStudentRepository.existsByLiveClassIdAndStudentId(LIVE_CLASS_ID, STUDENT_ID)).thenReturn(false);

        assertThatThrownBy(() ->
                liveClassService.setParticipantMicrophone(LIVE_CLASS_ID, STUDENT_ID, TEACHER_ID, true))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void cannotMuteWhenClassIsNotLive() {
        LiveClass scheduled = liveLiveClass();
        scheduled.setStatus(LiveClassStatus.SCHEDULED);
        when(liveClassRepository.findById(LIVE_CLASS_ID)).thenReturn(Optional.of(scheduled));

        assertThatThrownBy(() ->
                liveClassService.setParticipantMicrophone(LIVE_CLASS_ID, STUDENT_ID, TEACHER_ID, true))
                .isInstanceOf(RuntimeException.class)
                .isNotInstanceOf(AccessDeniedException.class);
    }

    @Test
    void cannotMuteAStudentWhoIsNotCurrentlyConnected() {
        when(liveClassRepository.findById(LIVE_CLASS_ID)).thenReturn(Optional.of(liveLiveClass()));
        when(userRepository.findById(STUDENT_ID)).thenReturn(Optional.of(student()));
        when(liveClassStudentRepository.existsByLiveClassIdAndStudentId(LIVE_CLASS_ID, STUDENT_ID)).thenReturn(true);
        when(liveKitService.getParticipant(anyString(), anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                liveClassService.setParticipantMicrophone(LIVE_CLASS_ID, STUDENT_ID, TEACHER_ID, true))
                .isInstanceOf(RuntimeException.class)
                .isNotInstanceOf(AccessDeniedException.class);
    }
}
