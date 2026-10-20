package com.swp391.g1.dao;

import com.swp391.g1.model.RegistrationForm;

import java.util.List;

public interface IRegistrationFormDAO {
    List<RegistrationForm> findAll();

    List<RegistrationForm> findOpenForms();

    RegistrationForm findById(int id);

    boolean insert(RegistrationForm form);

    boolean update(RegistrationForm form);

    boolean updateStatus(int id, String status);

    boolean delete(int id);

    boolean registerStudent(int formId, int studentId, String note);

    boolean hasStudentRegistered(int formId, int studentId);
}
