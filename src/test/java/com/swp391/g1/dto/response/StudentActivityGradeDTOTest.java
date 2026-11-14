package com.swp391.g1.dto.response;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StudentActivityGradeDTOTest {

    @Test
    void organizerUsesRoleCoefficientOfOnePointFive() {
        StudentActivityGradeDTO grade = new StudentActivityGradeDTO();
        grade.setParticipationRole("ORGANIZER");

        assertEquals(new BigDecimal("1.50"), grade.getOrganizerCoefficient());
        assertEquals("1.5", grade.getFormattedOrganizerCoefficient());
    }

    @Test
    void participantUsesNeutralRoleCoefficient() {
        StudentActivityGradeDTO grade = new StudentActivityGradeDTO();
        grade.setParticipationRole("PARTICIPANT");

        assertEquals(BigDecimal.ONE, grade.getOrganizerCoefficient());
    }
}
