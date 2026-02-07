package com.ai.studybuddy.repository;

import com.ai.studybuddy.model.quiz.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface QuestionRepository extends JpaRepository<Question, UUID> {

    // Trova domande per quiz ordinate
    List<Question> findByQuizIdOrderByQuestionOrderAsc(UUID quizId);

    // Conta domande per quiz
    long countByQuizId(UUID quizId);

    // Conta domande corrette per quiz
    @Query("SELECT COUNT(q) FROM Question q WHERE q.quiz.id = :quizId AND q.isCorrect = true")
    long countCorrectByQuizId(@Param("quizId") UUID quizId);

    // Conta domande risposte per quiz
    @Query("SELECT COUNT(q) FROM Question q WHERE q.quiz.id = :quizId AND q.userAnswer IS NOT NULL")
    long countAnsweredByQuizId(@Param("quizId") UUID quizId);

    // Elimina tutte le domande di un quiz
    void deleteByQuizId(UUID quizId);
}