package com.ai.studybuddy.service.impl;

import com.ai.studybuddy.dto.quiz.QuizAnswerRequest;
import com.ai.studybuddy.dto.quiz.QuizGenerateRequest;
import com.ai.studybuddy.dto.quiz.QuizResultResponse;
import com.ai.studybuddy.exception.ResourceNotFoundException;
import com.ai.studybuddy.mapper.QuizMapper;
import com.ai.studybuddy.model.quiz.Question;
import com.ai.studybuddy.model.quiz.Quiz;
import com.ai.studybuddy.model.user.User;
import com.ai.studybuddy.repository.QuestionRepository;
import com.ai.studybuddy.repository.QuizRepository;
import com.ai.studybuddy.service.inter.AIService;
import com.ai.studybuddy.service.inter.QuizService;
import com.ai.studybuddy.util.enums.DifficultyLevel;
import com.ai.studybuddy.util.enums.EducationLevel;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QuizServiceImpl - Test Suite Completo")
class QuizServiceImplTest {

    @Mock
    private QuizRepository quizRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private AIService aiService;

    @Mock
    private QuizMapper quizMapper;

    @Mock
    private QuizService selfProxy;

    @InjectMocks
    private QuizServiceImpl quizService;

    private User testUser;
    private Quiz testQuiz;
    private Question testQuestion;
    private UUID userId;
    private UUID quizId;
    private UUID questionId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        quizId = UUID.randomUUID();
        questionId = UUID.randomUUID();

        testUser = createTestUser();
        testQuiz = createTestQuiz();
        testQuestion = createTestQuestion();

        quizService.setSelfProxy(selfProxy);
    }

    private User createTestUser() {
        User user = new User();
        user.setId(userId);
        user.setEmail("test@example.com");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setPreferredLanguage("it");
        user.setEducationLevel(EducationLevel.UNIVERSITY);
        return user;
    }

    private Quiz createTestQuiz() {
        Quiz quiz = new Quiz();
        quiz.setId(quizId);
        quiz.setTitle("Test Quiz");
        quiz.setTopic("Fotosintesi");
        quiz.setSubject("Biologia");
        quiz.setUser(testUser);
        quiz.setDifficultyLevel(DifficultyLevel.INTERMEDIO);
        quiz.setNumberOfQuestions(5);
        quiz.setCreatedAt(LocalDateTime.now());
        quiz.setQuestions(new ArrayList<>());
        quiz.setIsCompleted(false);
        quiz.setScore(0);
        quiz.setTotalPoints(5);
        return quiz;
    }

    private Question createTestQuestion() {
        Question question = new Question();
        question.setId(questionId);
        question.setQuiz(testQuiz);
        question.setQuestionText("Qual è la funzione della clorofilla?");
        question.setOptionA("Assorbire luce");
        question.setOptionB("Produrre ossigeno");
        question.setOptionC("Trasportare acqua");
        question.setOptionD("Fissare azoto");
        question.setCorrectAnswer("A");
        question.setQuestionOrder(1);
        return question;
    }

    // ========================================
    // TEST: generateQuiz
    // ========================================

    @Test
    @DisplayName("generateQuiz - Successo con QuizGenerateRequest")
    void testGenerateQuiz_Success() {
        // Arrange
        QuizGenerateRequest request = QuizGenerateRequest.builder()
                .topic("Fotosintesi")
                .numberOfQuestions(5)
                .difficultyLevel(DifficultyLevel.INTERMEDIO)
                .language("it")
                .build();

        String aiResponse = """
            [
                {
                    "question": "Domanda 1?",
                    "options": ["A) Opzione 1", "B) Opzione 2", "C) Opzione 3", "D) Opzione 4"],
                    "correct": "A"
                },
                {
                    "question": "Domanda 2?",
                    "options": ["A) Opzione 1", "B) Opzione 2", "C) Opzione 3", "D) Opzione 4"],
                    "correct": "B"
                }
            ]
            """;

        when(quizMapper.toEntity(request, testUser)).thenReturn(testQuiz);
        when(quizRepository.save(any(Quiz.class))).thenReturn(testQuiz);
        when(aiService.generateQuiz(
                eq("Fotosintesi"), 
                eq(5), 
                eq(DifficultyLevel.INTERMEDIO), 
                eq(EducationLevel.UNIVERSITY), 
                eq("it")))
                .thenReturn(aiResponse);
        when(quizMapper.toQuestionEntity(any(JsonObject.class), any(Quiz.class), anyInt()))
                .thenReturn(testQuestion);

        // Act
        Quiz result = quizService.generateQuiz(request, testUser);

        // Assert
        assertNotNull(result);
        assertEquals("Fotosintesi", result.getTopic());
        verify(quizRepository, times(2)).save(testQuiz);
    }

    @Test
    @DisplayName("generateQuiz (deprecated) - Usa selfProxy")
    void testGenerateQuizDeprecated_UsesSelfProxy() {
        // Arrange
        when(selfProxy.generateQuiz(any(QuizGenerateRequest.class), any(User.class)))
                .thenReturn(testQuiz);

        // Act
        Quiz result = quizService.generateQuiz("Topic", 5, "medium", testUser);

        // Assert
        assertNotNull(result);
        verify(selfProxy, times(1)).generateQuiz(any(QuizGenerateRequest.class), eq(testUser));
    }

    // ========================================
    // TEST: startQuiz
    // ========================================

    @Test
    @DisplayName("startQuiz - Inizia quiz con successo")
    void testStartQuiz_Success() {
        // Arrange
        when(quizRepository.findByIdAndUserId(quizId, userId)).thenReturn(Optional.of(testQuiz));
        when(quizRepository.save(any(Quiz.class))).thenReturn(testQuiz);

        // Act
        Quiz result = quizService.startQuiz(quizId, userId);

        // Assert
        assertNotNull(result);
        assertNotNull(testQuiz.getStartedAt());
        verify(quizRepository, times(1)).save(testQuiz);
    }

    @Test
    @DisplayName("startQuiz - Quiz non trovato")
    void testStartQuiz_NotFound() {
        // Arrange
        when(quizRepository.findByIdAndUserId(quizId, userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            quizService.startQuiz(quizId, userId);
        });
    }

    // ========================================
    // TEST: submitAnswers
    // ========================================

    @Test
    @DisplayName("submitAnswers - Risposte corrette")
    void testSubmitAnswers_CorrectAnswers() {
        // Arrange
        Map<UUID, String> answers = new HashMap<>();
        answers.put(questionId, "A");
        
        QuizAnswerRequest request = new QuizAnswerRequest(quizId, answers);
        
        testQuestion.setCorrectAnswer("A");
        testQuiz.setQuestions(Arrays.asList(testQuestion));
        testQuiz.setIsCompleted(false);
        
        when(quizRepository.findByIdAndUserId(quizId, userId)).thenReturn(Optional.of(testQuiz));
        when(quizRepository.save(any(Quiz.class))).thenReturn(testQuiz);

        // Act
        QuizResultResponse result = quizService.submitAnswers(request, userId);

        // Assert
        assertNotNull(result);
        assertEquals(quizId, result.getQuizId());
        verify(quizRepository, times(1)).save(testQuiz);
    }

    @Test
    @DisplayName("submitAnswers - Quiz già completato restituisce risultati")
    void testSubmitAnswers_AlreadyCompleted() {
        // Arrange
        Map<UUID, String> answers = new HashMap<>();
        answers.put(questionId, "Test");
        
        QuizAnswerRequest request = new QuizAnswerRequest(quizId, answers);
        
        testQuiz.setIsCompleted(true);
        testQuiz.setScore(4);
        testQuiz.setQuestions(Arrays.asList(testQuestion));
        
        when(quizRepository.findByIdAndUserId(quizId, userId)).thenReturn(Optional.of(testQuiz));

        // Act
        QuizResultResponse result = quizService.submitAnswers(request, userId);

        // Assert
        assertNotNull(result);
        assertEquals(quizId, result.getQuizId());
        verify(quizRepository, never()).save(any(Quiz.class));
    }

    // ========================================
    // TEST: getQuiz
    // ========================================

    @Test
    @DisplayName("getQuiz - Successo")
    void testGetQuiz_Success() {
        // Arrange
        when(quizRepository.findByIdAndUserId(quizId, userId)).thenReturn(Optional.of(testQuiz));

        // Act
        Quiz result = quizService.getQuiz(quizId, userId);

        // Assert
        assertNotNull(result);
        assertEquals(quizId, result.getId());
    }

    @Test
    @DisplayName("getQuiz - Quiz non trovato")
    void testGetQuiz_NotFound() {
        // Arrange
        when(quizRepository.findByIdAndUserId(quizId, userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(ResourceNotFoundException.class, () -> {
            quizService.getQuiz(quizId, userId);
        });
    }

    // ========================================
    // TEST: getUserQuizzes
    // ========================================

    @Test
    @DisplayName("getUserQuizzes - Restituisce tutti i quiz")
    void testGetUserQuizzes_Success() {
        // Arrange
        List<Quiz> quizzes = Arrays.asList(testQuiz, createTestQuiz());
        when(quizRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(quizzes);

        // Act
        List<Quiz> result = quizService.getUserQuizzes(userId);

        // Assert
        assertEquals(2, result.size());
    }

    // ========================================
    // TEST: getCompletedQuizzes
    // ========================================

    @Test
    @DisplayName("getCompletedQuizzes - Restituisce solo completati")
    void testGetCompletedQuizzes_Success() {
        // Arrange
        testQuiz.setIsCompleted(true);
        List<Quiz> completedQuizzes = Arrays.asList(testQuiz);
        when(quizRepository.findByUserIdAndIsCompletedTrueOrderByCompletedAtDesc(userId))
                .thenReturn(completedQuizzes);

        // Act
        List<Quiz> result = quizService.getCompletedQuizzes(userId);

        // Assert
        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsCompleted());
    }

    // ========================================
    // TEST: getPendingQuizzes
    // ========================================

    @Test
    @DisplayName("getPendingQuizzes - Restituisce non completati")
    void testGetPendingQuizzes_Success() {
        // Arrange
        testQuiz.setIsCompleted(false);
        List<Quiz> pendingQuizzes = Arrays.asList(testQuiz);
        when(quizRepository.findByUserIdAndIsCompletedFalseOrderByCreatedAtDesc(userId))
                .thenReturn(pendingQuizzes);

        // Act
        List<Quiz> result = quizService.getPendingQuizzes(userId);

        // Assert
        assertEquals(1, result.size());
        assertFalse(result.get(0).getIsCompleted());
    }

    // ========================================
    // TEST: searchByTopic
    // ========================================

    @Test
    @DisplayName("searchByTopic - Trova quiz per topic")
    void testSearchByTopic_Success() {
        // Arrange
        String topic = "Fotosintesi";
        List<Quiz> quizzes = Arrays.asList(testQuiz);
        when(quizRepository.findByUserIdAndTopicContainingIgnoreCaseOrderByCreatedAtDesc(userId, topic))
                .thenReturn(quizzes);

        // Act
        List<Quiz> result = quizService.searchByTopic(userId, topic);

        // Assert
        assertEquals(1, result.size());
    }

    // ========================================
    // TEST: getBySubject
    // ========================================

    @Test
    @DisplayName("getBySubject - Filtra per materia")
    void testGetBySubject_Success() {
        // Arrange
        String subject = "Biologia";
        List<Quiz> quizzes = Arrays.asList(testQuiz);
        when(quizRepository.findByUserIdAndSubjectOrderByCreatedAtDesc(userId, subject))
                .thenReturn(quizzes);

        // Act
        List<Quiz> result = quizService.getBySubject(userId, subject);

        // Assert
        assertEquals(1, result.size());
    }

    // ========================================
    // TEST: deleteQuiz
    // ========================================

    @Test
    @DisplayName("deleteQuiz - Elimina con successo")
    void testDeleteQuiz_Success() {
        // Arrange
        when(quizRepository.findByIdAndUserId(quizId, userId)).thenReturn(Optional.of(testQuiz));
        doNothing().when(quizRepository).delete(testQuiz);

        // Act
        quizService.deleteQuiz(quizId, userId);

        // Assert
        verify(quizRepository, times(1)).delete(testQuiz);
    }

    // ========================================
    // TEST: retryQuiz
    // ========================================

    @Test
    @DisplayName("retryQuiz - Resetta quiz")
    void testRetryQuiz_Success() {
        // Arrange
        testQuiz.setIsCompleted(true);
        when(quizRepository.findByIdAndUserId(quizId, userId)).thenReturn(Optional.of(testQuiz));
        when(quizRepository.save(any(Quiz.class))).thenReturn(testQuiz);

        // Act
        Quiz result = quizService.retryQuiz(quizId, userId);

        // Assert
        assertNotNull(result);
        verify(quizRepository, times(1)).save(testQuiz);
    }

    // ========================================
    // TEST: getUserStats
    // ========================================

    @Test
    @DisplayName("getUserStats - Calcola statistiche")
    void testGetUserStats_Success() {
        // Arrange
        when(quizRepository.countByUserId(userId)).thenReturn(10L);
        when(quizRepository.countByUserIdAndIsCompletedTrue(userId)).thenReturn(8L);
        when(quizRepository.getAverageScoreByUserId(userId)).thenReturn(75.5);
        when(quizRepository.findPassedQuizzes(userId)).thenReturn(Arrays.asList(testQuiz, testQuiz));
        when(quizRepository.findFailedQuizzes(userId)).thenReturn(Arrays.asList(testQuiz));

        // Act
        QuizService.QuizStats result = quizService.getUserStats(userId);

        // Assert
        assertNotNull(result);
        assertEquals(10L, result.getTotalQuizzes());
        assertEquals(8L, result.getCompletedQuizzes());
    }

    // ========================================
    // TEST: getRecentQuizzes
    // ========================================

    @Test
    @DisplayName("getRecentQuizzes - Restituisce quiz recenti")
    void testGetRecentQuizzes_Success() {
        // Arrange
        List<Quiz> recentQuizzes = Arrays.asList(testQuiz);
        when(quizRepository.findRecentQuizzes(eq(userId), any(LocalDateTime.class)))
                .thenReturn(recentQuizzes);

        // Act
        List<Quiz> result = quizService.getRecentQuizzes(userId, 7);

        // Assert
        assertEquals(1, result.size());
    }

    // ========================================
    // TEST: Difficulty Levels
    // ========================================

    @Test
    @DisplayName("generateQuiz - Livello EASY")
    void testGenerateQuiz_EasyLevel() {
        // Arrange
        QuizGenerateRequest request = QuizGenerateRequest.builder()
                .topic("Math")
                .numberOfQuestions(3)
                .difficultyLevel(DifficultyLevel.PRINCIPIANTE)
                .language("en")
                .build();

        when(quizMapper.toEntity(request, testUser)).thenReturn(testQuiz);
        when(quizRepository.save(any(Quiz.class))).thenReturn(testQuiz);
        when(aiService.generateQuiz(anyString(), anyInt(), eq(DifficultyLevel.PRINCIPIANTE), any(), anyString()))
                .thenReturn("[]");

        // Act
        quizService.generateQuiz(request, testUser);

        // Assert
        verify(aiService, times(1)).generateQuiz(
                anyString(), anyInt(), eq(DifficultyLevel.PRINCIPIANTE), any(), anyString());
    }

    @Test
    @DisplayName("generateQuiz - Livello HARD")
    void testGenerateQuiz_HardLevel() {
        // Arrange
        QuizGenerateRequest request = QuizGenerateRequest.builder()
                .topic("Physics")
                .numberOfQuestions(5)
                .difficultyLevel(DifficultyLevel.AVANZATO)
                .language("it")
                .build();

        when(quizMapper.toEntity(request, testUser)).thenReturn(testQuiz);
        when(quizRepository.save(any(Quiz.class))).thenReturn(testQuiz);
        when(aiService.generateQuiz(anyString(), anyInt(), eq(DifficultyLevel.AVANZATO), any(), anyString()))
                .thenReturn("[]");

        // Act
        quizService.generateQuiz(request, testUser);

        // Assert
        verify(aiService, times(1)).generateQuiz(
                anyString(), anyInt(), eq(DifficultyLevel.AVANZATO), any(), anyString());
    }
}
