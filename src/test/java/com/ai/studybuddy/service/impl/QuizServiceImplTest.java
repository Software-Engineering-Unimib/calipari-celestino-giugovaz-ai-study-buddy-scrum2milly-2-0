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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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

    @Captor
    private ArgumentCaptor<Quiz> quizCaptor;

    @Captor
    private ArgumentCaptor<Question> questionCaptor;

    private UUID userId;
    private UUID quizId;
    private UUID questionId1;
    private UUID questionId2;
    private User user;
    private Quiz quiz;
    private Question question1;
    private Question question2;
    private QuizGenerateRequest generateRequest;
    private JsonArray quizJson;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        quizId = UUID.randomUUID();
        questionId1 = UUID.randomUUID();
        questionId2 = UUID.randomUUID();

        // Setup User
        user = new User();
        user.setId(userId);
        user.setFirstName("Mario");
        user.setLastName("Rossi");
        user.setEmail("mario.rossi@example.com");

        // Setup Quiz
        quiz = new Quiz();
        quiz.setId(quizId);
        quiz.setUser(user);
        quiz.setTitle("Test Quiz");
        quiz.setTopic("Algebra");
        quiz.setSubject("Mathematics");
        quiz.setDifficultyLevel(DifficultyLevel.INTERMEDIO);
        quiz.setNumberOfQuestions(2);
        quiz.setIsCompleted(false);
        quiz.setIsAiGenerated(true);
        quiz.setCreatedAt(LocalDateTime.now());
        quiz.setUpdatedAt(LocalDateTime.now());

        // Setup Question 1
        question1 = new Question();
        question1.setId(questionId1);
        question1.setQuiz(quiz);
        question1.setQuestionText("Quanto fa 2+2?");
        question1.setQuestionText("MULTIPLE_CHOICE");
        question1.setCorrectAnswer("4");
        question1.setQuestionOrder(1);

        // Setup Question 2
        question2 = new Question();
        question2.setId(questionId2);
        question2.setQuiz(quiz);
        question2.setQuestionText("Quanto fa 3*3?");
        question2.setCorrectAnswer("9");
        question2.setExplanation("3*3 = 9");
        question2.setQuestionOrder(2);

        quiz.setQuestions(List.of(question1, question2));

        // Setup GenerateRequest
        generateRequest = QuizGenerateRequest.builder()
                .topic("Algebra")
                .numberOfQuestions(2)
                .difficultyLevel(DifficultyLevel.INTERMEDIO)
                .subject("Mathematics")
                .build();

        // Setup Quiz JSON from AI
        quizJson = new JsonArray();

        JsonObject q1 = new JsonObject();
        q1.addProperty("question", "Quanto fa 2+2?");
        q1.addProperty("correct_answer", "4");
        JsonArray options1 = new JsonArray();
        options1.add("3");
        options1.add("4");
        options1.add("5");
        options1.add("6");
        q1.add("options", options1);
        q1.addProperty("explanation", "2+2 = 4");
        quizJson.add(q1);

        JsonObject q2 = new JsonObject();
        q2.addProperty("question", "Quanto fa 3*3?");
        q2.addProperty("correct_answer", "9");
        JsonArray options2 = new JsonArray();
        options2.add("6");
        options2.add("9");
        options2.add("12");
        options2.add("15");
        q2.add("options", options2);
        q2.addProperty("explanation", "3*3 = 9");
        quizJson.add(q2);
    }

    // ==================== GENERATE QUIZ TESTS ====================

    @Test
    void generateQuiz_Success() {
        // Arrange
        String aiResponse = "{\"quiz\": [...]}";

        when(quizMapper.toEntity(generateRequest, user)).thenReturn(quiz);
        when(quizRepository.save(any(Quiz.class))).thenReturn(quiz);
        when(aiService.generateQuiz(generateRequest.getTopic(),
                generateRequest.getNumberOfQuestions(),
                generateRequest.getDifficultyLevel()))
                .thenReturn(aiResponse);

        // Mock del metodo privato parseQuizJson
        QuizServiceImpl spyService = spy(quizService);

        when(quizMapper.toQuestionEntity(any(JsonObject.class), eq(quiz), anyInt()))
                .thenReturn(question1, question2);
        when(quizRepository.save(quiz)).thenReturn(quiz);

        // Act
        Quiz result = spyService.generateQuiz(generateRequest, user);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(quizId);
        assertThat(result.getNumberOfQuestions()).isEqualTo(2);

        verify(quizMapper).toEntity(generateRequest, user);
        verify(quizRepository, times(2)).save(quiz);
        verify(aiService).generateQuiz(anyString(), anyInt(), any(DifficultyLevel.class));
        verify(quizMapper, times(2)).toQuestionEntity(any(JsonObject.class), eq(quiz), anyInt());
    }

    @Test
    void generateQuiz_DeprecatedMethod_Success() {
        // Arrange
        String topic = "Algebra";
        int numberOfQuestions = 2;
        String difficulty = "MEDIUM";

        when(selfProxy.generateQuiz(any(QuizGenerateRequest.class), eq(user)))
                .thenReturn(quiz);

        // Act
        Quiz result = quizService.generateQuiz(topic, numberOfQuestions, difficulty, user);

        // Assert
        assertThat(result).isNotNull();
        verify(selfProxy).generateQuiz(any(QuizGenerateRequest.class), eq(user));
    }

    @Test
    void parseQuizJson_CleaningJson_Success() {
        // Arrange
        String aiResponseWithMarkers = "```json\n" + quizJson.toString() + "\n```";
        QuizServiceImpl spyService = spy(quizService);


        // Assert
    }

    // ==================== START QUIZ TESTS ====================

    @Test
    void startQuiz_Success() {
        // Arrange
        when(quizRepository.findByIdAndUserId(quizId, userId)).thenReturn(Optional.of(quiz));
        when(quizRepository.save(quiz)).thenReturn(quiz);

        // Act
        Quiz result = quizService.startQuiz(quizId, userId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getStartedAt()).isNotNull();
        assertThat(result.getIsCompleted()).isFalse();

        verify(quizRepository).findByIdAndUserId(quizId, userId);
        verify(quizRepository).save(quiz);
    }

    @Test
    void startQuiz_NotFound_ThrowsException() {
        // Arrange
        when(quizRepository.findByIdAndUserId(quizId, userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> quizService.startQuiz(quizId, userId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Quiz");

        verify(quizRepository).findByIdAndUserId(quizId, userId);
        verify(quizRepository, never()).save(any());
    }

    // ==================== SUBMIT ANSWERS TESTS ====================

    @Test
    void submitAnswers_AllCorrect_Success() {
        // Arrange
        QuizAnswerRequest request = new QuizAnswerRequest();
        request.setQuizId(quizId);
        Map<UUID, String> answers = new HashMap<>();
        answers.put(questionId1, "4");
        answers.put(questionId2, "9");
        request.setAnswers(answers);

        when(quizRepository.findByIdAndUserId(quizId, userId)).thenReturn(Optional.of(quiz));
        when(quizRepository.save(quiz)).thenReturn(quiz);

        // Act
        QuizResultResponse response = quizService.submitAnswers(request, userId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getScore()).isEqualTo(2);
        assertThat(response.getTotalQuestions()).isEqualTo(2);
        assertThat(response.getScorePercentage()).isEqualTo(100.0);
        assertThat(response.isPassed()).isTrue();
        assertThat(response.getFeedback()).contains("Eccellente");
        assertThat(response.getQuestionResults()).hasSize(2);

        assertThat(question1.getIsCorrect()).isTrue();
        assertThat(question2.getIsCorrect()).isTrue();
        assertThat(quiz.getIsCompleted()).isTrue();
        assertThat(quiz.getCompletedAt()).isNotNull();

        verify(quizRepository).save(quiz);
    }

    @Test
    void submitAnswers_MixedResults_Success() {
        // Arrange
        QuizAnswerRequest request = new QuizAnswerRequest();
        request.setQuizId(quizId);
        Map<UUID, String> answers = new HashMap<>();
        answers.put(questionId1, "4");  // Corretta
        answers.put(questionId2, "6");  // Sbagliata
        request.setAnswers(answers);

        when(quizRepository.findByIdAndUserId(quizId, userId)).thenReturn(Optional.of(quiz));
        when(quizRepository.save(quiz)).thenReturn(quiz);

        // Act
        QuizResultResponse response = quizService.submitAnswers(request, userId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getScore()).isEqualTo(1);
        assertThat(response.getScorePercentage()).isEqualTo(50.0);
        assertThat(response.isPassed()).isFalse();
        assertThat(response.getFeedback()).contains("Quasi");

        assertThat(question1.getIsCorrect()).isTrue();
        assertThat(question2.getIsCorrect()).isFalse();
    }

    @Test
    void submitAnswers_AlreadyCompleted_ReturnsExistingResult() {
        // Arrange
        quiz.setIsCompleted(true);
        quiz.calculateScore();

        QuizAnswerRequest request = new QuizAnswerRequest();
        request.setQuizId(quizId);
        request.setAnswers(new HashMap<>());

        when(quizRepository.findByIdAndUserId(quizId, userId)).thenReturn(Optional.of(quiz));

        // Act
        QuizResultResponse response = quizService.submitAnswers(request, userId);

        // Assert
        assertThat(response).isNotNull();
        verify(quizRepository, never()).save(any());
    }

    @Test
    void submitAnswers_PartialAnswers_Success() {
        // Arrange
        QuizAnswerRequest request = new QuizAnswerRequest();
        request.setQuizId(quizId);
        Map<UUID, String> answers = new HashMap<>();
        answers.put(questionId1, "4");  // Solo prima risposta
        request.setAnswers(answers);

        when(quizRepository.findByIdAndUserId(quizId, userId)).thenReturn(Optional.of(quiz));
        when(quizRepository.save(quiz)).thenReturn(quiz);

        // Act
        QuizResultResponse response = quizService.submitAnswers(request, userId);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getScore()).isEqualTo(1);
        assertThat(question2.getUserAnswer()).isNull();
    }

    // ==================== BUILD RESULT RESPONSE TESTS ====================

    @Test
    void buildQuizResultResponse_WithScore_Success() {
        // Arrange
        quiz.setScore(1);
        quiz.setTotalPoints(2);
        quiz.setPercentage(50.0);
        quiz.setIsCompleted(true);

        // Test metodo privato via reflection
        QuizResultResponse response = buildQuizResultResponseReflection(quiz);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getQuizId()).isEqualTo(quizId);
        assertThat(response.getTopic()).isEqualTo("Algebra");
        assertThat(response.getScore()).isEqualTo(1);
        assertThat(response.getTotalQuestions()).isEqualTo(2);
        assertThat(response.getScorePercentage()).isEqualTo(50.0);
        assertThat(response.isPassed()).isFalse();
        assertThat(response.getQuestionResults()).hasSize(2);
    }

    private QuizResultResponse buildQuizResultResponseReflection(Quiz quiz) {
        try {
            var method = QuizServiceImpl.class.getDeclaredMethod("buildQuizResultResponse", Quiz.class);
            method.setAccessible(true);
            return (QuizResultResponse) method.invoke(quizService, quiz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // ==================== GET QUIZ TESTS ====================

    @Test
    void getQuiz_Success() {
        // Arrange
        when(quizRepository.findByIdAndUserId(quizId, userId)).thenReturn(Optional.of(quiz));

        // Act
        Quiz result = quizService.getQuiz(quizId, userId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(quizId);
        verify(quizRepository).findByIdAndUserId(quizId, userId);
    }

    @Test
    void getUserQuizzes_Success() {
        // Arrange
        List<Quiz> expectedQuizzes = Arrays.asList(quiz);
        when(quizRepository.findByUserIdOrderByCreatedAtDesc(userId)).thenReturn(expectedQuizzes);

        // Act
        List<Quiz> result = quizService.getUserQuizzes(userId);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(quiz);
        verify(quizRepository).findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Test
    void getCompletedQuizzes_Success() {
        // Arrange
        quiz.setIsCompleted(true);
        List<Quiz> expectedQuizzes = Arrays.asList(quiz);
        when(quizRepository.findByUserIdAndIsCompletedTrueOrderByCompletedAtDesc(userId))
                .thenReturn(expectedQuizzes);

        // Act
        List<Quiz> result = quizService.getCompletedQuizzes(userId);

        // Assert
        assertThat(result).hasSize(1);
        verify(quizRepository).findByUserIdAndIsCompletedTrueOrderByCompletedAtDesc(userId);
    }

    @Test
    void getPendingQuizzes_Success() {
        // Arrange
        quiz.setIsCompleted(false);
        List<Quiz> expectedQuizzes = Arrays.asList(quiz);
        when(quizRepository.findByUserIdAndIsCompletedFalseOrderByCreatedAtDesc(userId))
                .thenReturn(expectedQuizzes);

        // Act
        List<Quiz> result = quizService.getPendingQuizzes(userId);

        // Assert
        assertThat(result).hasSize(1);
        verify(quizRepository).findByUserIdAndIsCompletedFalseOrderByCreatedAtDesc(userId);
    }

    @Test
    void searchByTopic_Success() {
        // Arrange
        String searchTerm = "Alg";
        List<Quiz> expectedQuizzes = Arrays.asList(quiz);
        when(quizRepository.findByUserIdAndTopicContainingIgnoreCaseOrderByCreatedAtDesc(userId, searchTerm))
                .thenReturn(expectedQuizzes);

        // Act
        List<Quiz> result = quizService.searchByTopic(userId, searchTerm);

        // Assert
        assertThat(result).hasSize(1);
        verify(quizRepository).findByUserIdAndTopicContainingIgnoreCaseOrderByCreatedAtDesc(userId, searchTerm);
    }

    @Test
    void getBySubject_Success() {
        // Arrange
        String subject = "Mathematics";
        List<Quiz> expectedQuizzes = Arrays.asList(quiz);
        when(quizRepository.findByUserIdAndSubjectOrderByCreatedAtDesc(userId, subject))
                .thenReturn(expectedQuizzes);

        // Act
        List<Quiz> result = quizService.getBySubject(userId, subject);

        // Assert
        assertThat(result).hasSize(1);
        verify(quizRepository).findByUserIdAndSubjectOrderByCreatedAtDesc(userId, subject);
    }

    // ==================== DELETE QUIZ TESTS ====================

    @Test
    void deleteQuiz_Success() {
        // Arrange
        when(quizRepository.findByIdAndUserId(quizId, userId)).thenReturn(Optional.of(quiz));
        doNothing().when(quizRepository).delete(quiz);

        // Act
        quizService.deleteQuiz(quizId, userId);

        // Assert
        verify(quizRepository).findByIdAndUserId(quizId, userId);
        verify(quizRepository).delete(quiz);
    }

    @Test
    void deleteQuiz_NotFound_ThrowsException() {
        // Arrange
        when(quizRepository.findByIdAndUserId(quizId, userId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> quizService.deleteQuiz(quizId, userId))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(quizRepository).findByIdAndUserId(quizId, userId);
        verify(quizRepository, never()).delete(any());
    }

    // ==================== RETRY QUIZ TESTS ====================

    @Test
    void retryQuiz_Success() {
        // Arrange
        question1.setUserAnswer("4");
        question1.setIsCorrect(true);
        question2.setUserAnswer("6");
        question2.setIsCorrect(false);

        quiz.setIsCompleted(true);
        quiz.setScore(1);
        quiz.setPercentage(50.0);
        quiz.setCompletedAt(LocalDateTime.now());

        when(quizRepository.findByIdAndUserId(quizId, userId)).thenReturn(Optional.of(quiz));
        when(quizRepository.save(quiz)).thenReturn(quiz);

        // Act
        Quiz result = quizService.retryQuiz(quizId, userId);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getIsCompleted()).isFalse();
        assertThat(result.getScore()).isNull();
        assertThat(result.getPercentage()).isNull();
        assertThat(result.getCompletedAt()).isNull();

        assertThat(question1.getUserAnswer()).isNull();
        assertThat(question1.getIsCorrect()).isNull();
        assertThat(question2.getUserAnswer()).isNull();
        assertThat(question2.getIsCorrect()).isNull();

        verify(quizRepository).save(quiz);
    }

    // ==================== USER STATS TESTS ====================

    @Test
    void getUserStats_Success() {
        // Arrange
        long totalQuizzes = 10L;
        long completedQuizzes = 8L;
        Double averageScore = 75.5;

        List<Quiz> passedQuizzes = Arrays.asList(quiz);
        List<Quiz> failedQuizzes = Arrays.asList(new Quiz());

        when(quizRepository.countByUserId(userId)).thenReturn(totalQuizzes);
        when(quizRepository.countByUserIdAndIsCompletedTrue(userId)).thenReturn(completedQuizzes);
        when(quizRepository.getAverageScoreByUserId(userId)).thenReturn(averageScore);
        when(quizRepository.findPassedQuizzes(userId)).thenReturn(passedQuizzes);
        when(quizRepository.findFailedQuizzes(userId)).thenReturn(failedQuizzes);

        // Act
        QuizService.QuizStats stats = quizService.getUserStats(userId);

        // Assert
        assertThat(stats).isNotNull();
        assertThat(stats.getTotalQuizzes()).isEqualTo(totalQuizzes);
        assertThat(stats.getCompletedQuizzes()).isEqualTo(completedQuizzes);
        assertThat(stats.getPassedQuizzes()).isEqualTo(1);
        assertThat(stats.getFailedQuizzes()).isEqualTo(1);
        assertThat(stats.getAverageScore()).isEqualTo(averageScore);
    }

    @Test
    void getUserStats_NullAverageScore_Success() {
        // Arrange
        long totalQuizzes = 5L;
        long completedQuizzes = 3L;
        Double averageScore = null;

        List<Quiz> passedQuizzes = new ArrayList<>();
        List<Quiz> failedQuizzes = new ArrayList<>();

        when(quizRepository.countByUserId(userId)).thenReturn(totalQuizzes);
        when(quizRepository.countByUserIdAndIsCompletedTrue(userId)).thenReturn(completedQuizzes);
        when(quizRepository.getAverageScoreByUserId(userId)).thenReturn(averageScore);
        when(quizRepository.findPassedQuizzes(userId)).thenReturn(passedQuizzes);
        when(quizRepository.findFailedQuizzes(userId)).thenReturn(failedQuizzes);

        // Act
        QuizService.QuizStats stats = quizService.getUserStats(userId);

        // Assert
        assertThat(stats).isNotNull();
        assertThat(stats.getAverageScore()).isZero();
    }

    // ==================== RECENT QUIZZES TESTS ====================

    @Test
    void getRecentQuizzes_Success() {
        // Arrange
        int days = 7;
        List<Quiz> recentQuizzes = Arrays.asList(quiz);
        when(quizRepository.findRecentQuizzes(eq(userId), any(LocalDateTime.class)))
                .thenReturn(recentQuizzes);

        // Act
        List<Quiz> result = quizService.getRecentQuizzes(userId, days);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result).containsExactly(quiz);
        verify(quizRepository).findRecentQuizzes(eq(userId), any(LocalDateTime.class));
    }

    // ==================== QUIZ BUSINESS LOGIC TESTS ====================

    @Test
    void quiz_AddQuestion_Success() {
        // Arrange
        Quiz newQuiz = new Quiz();
        Question newQuestion = new Question();
        newQuestion.setId(UUID.randomUUID());

        // Act
        newQuiz.addQuestion(newQuestion);

        // Assert
        assertThat(newQuiz.getQuestions()).hasSize(1);
        assertThat(newQuiz.getNumberOfQuestions()).isEqualTo(1);
        assertThat(newQuestion.getQuiz()).isEqualTo(newQuiz);
        assertThat(newQuestion.getQuestionOrder()).isEqualTo(1);
    }

    @Test
    void quiz_RemoveQuestion_Success() {
        // Arrange
        Quiz testQuiz = new Quiz();
        Question q1 = new Question();
        q1.setId(UUID.randomUUID());
        Question q2 = new Question();
        q2.setId(UUID.randomUUID());

        testQuiz.addQuestion(q1);
        testQuiz.addQuestion(q2);
        assertThat(testQuiz.getQuestions()).hasSize(2);

        // Act
        testQuiz.removeQuestion(q1);

        // Assert
        assertThat(testQuiz.getQuestions()).hasSize(1);
        assertThat(testQuiz.getQuestions().get(0).getId()).isEqualTo(q2.getId());
        assertThat(testQuiz.getNumberOfQuestions()).isEqualTo(1);
        assertThat(testQuiz.getQuestions().get(0).getQuestionOrder()).isEqualTo(1);
    }

    @Test
    void quiz_CalculateScore_Success() {
        // Arrange
        Quiz testQuiz = new Quiz();
        testQuiz.setQuestions(new ArrayList<>());

        Question q1 = new Question();
        q1.setIsCorrect(true);
        Question q2 = new Question();
        q2.setIsCorrect(false);
        Question q3 = new Question();
        q3.setIsCorrect(true);

        testQuiz.addQuestion(q1);
        testQuiz.addQuestion(q2);
        testQuiz.addQuestion(q3);

        // Act
        testQuiz.calculateScore();

        // Assert
        assertThat(testQuiz.getScore()).isEqualTo(2);
        assertThat(testQuiz.getTotalPoints()).isEqualTo(3);
        assertThat(testQuiz.getPercentage()).isEqualTo(66.66666666666666);
    }

    @Test
    void quiz_CalculateScore_NoQuestions_Success() {
        // Arrange
        Quiz testQuiz = new Quiz();
        testQuiz.setQuestions(new ArrayList<>());

        // Act
        testQuiz.calculateScore();

        // Assert
        assertThat(testQuiz.getScore()).isZero();
        assertThat(testQuiz.getTotalPoints()).isZero();
        assertThat(testQuiz.getPercentage()).isZero();
    }

    @Test
    void quiz_AllQuestionsAnswered_ReturnsTrue() {
        // Arrange
        question1.setUserAnswer("4");
        question2.setUserAnswer("9");
        question1.setIsCorrect(true);
        question2.setIsCorrect(true);

        // Act
        boolean allAnswered = quiz.allQuestionsAnswered();

        // Assert
        assertThat(allAnswered).isTrue();
    }

    @Test
    void quiz_AllQuestionsAnswered_ReturnsFalse() {
        // Arrange
        question1.setUserAnswer("4");
        question2.setUserAnswer(null);
        question1.setIsCorrect(true);
        question2.setIsCorrect(null);

        // Act
        boolean allAnswered = quiz.allQuestionsAnswered();

        // Assert
        assertThat(allAnswered).isFalse();
    }

    @Test
    void quiz_GetAnsweredCount_Success() {
        // Arrange
        question1.setUserAnswer("4");
        question2.setUserAnswer(null);

        // Act
        long answeredCount = quiz.getAnsweredCount();

        // Assert
        assertThat(answeredCount).isEqualTo(1);
    }

    @Test
    void quiz_GetCorrectCount_Success() {
        // Arrange
        question1.setUserAnswer("4");
        question1.setIsCorrect(true);
        question2.setUserAnswer("6");
        question2.setIsCorrect(false);

        // Act
        long correctCount = quiz.getCorrectCount();

        // Assert
        assertThat(correctCount).isEqualTo(1);
    }

    @Test
    void quiz_IsPassed_With60Percent_ReturnsTrue() {
        // Arrange
        quiz.setPercentage(60.0);

        // Act
        boolean passed = quiz.isPassed();

        // Assert
        assertThat(passed).isTrue();
    }

    @Test
    void quiz_IsPassed_With59Percent_ReturnsFalse() {
        // Arrange
        quiz.setPercentage(59.0);

        // Act
        boolean passed = quiz.isPassed();

        // Assert
        assertThat(passed).isFalse();
    }

    @Test
    void quiz_GetFormattedTime_Success() {
        // Arrange
        quiz.setTimeSpentSeconds(125); // 2 minuti e 5 secondi

        // Act
        String formattedTime = quiz.getFormattedTime();

        // Assert
        assertThat(formattedTime).isEqualTo("02:05");
    }

    @Test
    void quiz_GetFormattedTime_NullTime_ReturnsDefault() {
        // Arrange
        quiz.setTimeSpentSeconds(null);

        // Act
        String formattedTime = quiz.getFormattedTime();

        // Assert
        assertThat(formattedTime).isEqualTo("00:00");
    }

    // ==================== FEEDBACK TESTS ====================

    @Test
    void buildQuizResultResponse_FeedbackLevels() {
        // Test per i diversi livelli di feedback

        Quiz testQuiz = new Quiz();
        testQuiz.setId(quizId);
        testQuiz.setTopic("Algebra");
        testQuiz.setSubject("Mathematics");
        testQuiz.setQuestions(quiz.getQuestions());
        testQuiz.setNumberOfQuestions(2);

        // 90%+
        testQuiz.setScore(2);
        testQuiz.setTotalPoints(2);
        testQuiz.setPercentage(100.0);
        QuizResultResponse response90 = buildQuizResultResponseReflection(testQuiz);
        assertThat(response90.getFeedback()).contains("Eccellente");

        // 70-89%
        testQuiz.setPercentage(75.0);
        QuizResultResponse response75 = buildQuizResultResponseReflection(testQuiz);
        assertThat(response75.getFeedback()).contains("Molto bene");

        // 60-69%
        testQuiz.setPercentage(65.0);
        QuizResultResponse response65 = buildQuizResultResponseReflection(testQuiz);
        assertThat(response65.getFeedback()).contains("Buono");

        // 40-59%
        testQuiz.setPercentage(50.0);
        QuizResultResponse response50 = buildQuizResultResponseReflection(testQuiz);
        assertThat(response50.getFeedback()).contains("Quasi");

        // <40%
        testQuiz.setPercentage(30.0);
        QuizResultResponse response30 = buildQuizResultResponseReflection(testQuiz);
        assertThat(response30.getFeedback()).contains("Da migliorare");
    }
}